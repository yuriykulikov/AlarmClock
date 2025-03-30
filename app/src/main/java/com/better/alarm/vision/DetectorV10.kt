/*
Creative Commons Attribution 4.0 International (CC BY 4.0)

You are free to:

- Share — copy and redistribute the material in any medium or format
- Adapt — remix, transform, and build upon the material for any purpose, even commercially.

Under the following terms:

- Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.

Credit must be given to: Surendra Maran (https://github.com/surendramaran)

Full License Text: https://creativecommons.org/licenses/by/4.0/
 */
package com.better.alarm.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.better.alarm.vision.MetaData.extractNamesFromLabelFile
import com.better.alarm.vision.MetaData.extractNamesFromMetadata
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class DetectorV10(
    private val context: Context,
    private val modelPath: String,
    private val detectorListener: DetectorListener,
    private val message: (String) -> Unit
) {
    private var interpreter: Interpreter? = null
    private var interpreterIsValid = false
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0
    private var gpuDelegate: GpuDelegate? = null
    private var operatingLock: Any = Any()

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    init {

        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply{
          if(compatList.isDelegateSupportedOnThisDevice){
            val delegateOptions = compatList.bestOptionsForThisDevice
            gpuDelegate = GpuDelegate(delegateOptions)
            this.addDelegate(gpuDelegate)
          } else {
            this.setNumThreads(4)
          }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        labels.addAll(extractNamesFromMetadata(model))
        if (labels.isEmpty()) {
            labels.addAll(MetaData.TEMP_CLASSES)
        }

        labels.forEach(::println)

        val inputShape = interpreter?.getInputTensor(0)?.shape()
        val outputShape = interpreter?.getOutputTensor(0)?.shape()

        if (inputShape != null) {
            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]

            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numElements = outputShape[1]
            numChannel = outputShape[2]
        }

        interpreterIsValid = true
    }

    fun restart(isGpu: Boolean) {
        interpreter?.close()

        val options = if (isGpu) {
            val compatList = CompatibilityList()
            Interpreter.Options().apply{
                if(compatList.isDelegateSupportedOnThisDevice){
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    gpuDelegate = GpuDelegate(delegateOptions)
                    this.addDelegate(gpuDelegate)
                } else {
                    this.setNumThreads(4)
                }
            }
        } else {
            Interpreter.Options().apply{
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)
    }

    fun close() {
        synchronized(operatingLock) {
          interpreter?.close()
          interpreter = null
          interpreterIsValid = false
          gpuDelegate?.close()
        }
    }

    fun detect(frame: Bitmap) {
        if (!interpreterIsValid) {
          message("Interpreter is not valid")
          return
        }
        val bestBoxes: List<BoundingBox>
        var inferenceTime: Long
        synchronized( operatingLock ) {
          if (tensorWidth == 0
            || tensorHeight == 0
            || numChannel == 0
            || numElements == 0) return

          inferenceTime = SystemClock.uptimeMillis()

          val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

          val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
          tensorImage.load(resizedBitmap)
          val processedImage = imageProcessor.process(tensorImage)
          val imageBuffer = processedImage.buffer

          val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
          interpreter?.run(imageBuffer, output.buffer)

          bestBoxes = bestBox(output.floatArray)
          inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        }
      detectorListener.onDetect(bestBoxes, inferenceTime)
    }

    private fun bestBox(array: FloatArray) : List<BoundingBox> {
        val boundingBoxes = mutableListOf<BoundingBox>()
        for (r in 0 until numElements) {
            val cnf = array[r * numChannel + 4]
            if (cnf > CONFIDENCE_THRESHOLD) {
                val x1 = array[r * numChannel]
                val y1 = array[r * numChannel + 1]
                val x2 = array[r * numChannel + 2]
                val y2 = array[r * numChannel + 3]
                val cx = (x1 + x2) / 2F
                val cy = (y1 + y2) / 2F
                val w = x2 - x1
                val h = y2 - y1
                val cls = array[r * numChannel + 5].toInt()
                val clsName = labels[cls]
                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2, cx = cx, cy = cy, w = w, h = h,
                        cnf = cnf, cls = cls, clsName = clsName
                    )
                )
            }
        }
        return boundingBoxes
    }

    interface DetectorListener {
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
    }
}
