package com.better.alarm.vision

import android.content.Context
import org.tensorflow.lite.support.metadata.MetadataExtractor
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer

object MetaData {

    fun extractNamesFromMetadata(model: MappedByteBuffer): List<String> {
        try {
            val metadataExtractor = MetadataExtractor(model)
            val inputStream = metadataExtractor.getAssociatedFile("temp_meta.txt")
            val metadata = inputStream?.bufferedReader()?.use { it.readText() } ?: return emptyList()

            val regex = Regex("'names': \\{(.*?)\\}", RegexOption.DOT_MATCHES_ALL)

            val match = regex.find(metadata)
            val namesContent = match?.groups?.get(1)?.value ?: return emptyList()

            val regex2 = Regex("\"([^\"]*)\"|'([^']*)'")
            val match2 = regex2.findAll(namesContent)
            val list = match2.map { it.groupValues[1].ifEmpty { it.groupValues[2] }}.toList()

            return list
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun extractNamesFromLabelFile(context: Context, labelPath: String): List<String> {
        val labels = mutableListOf<String>()
        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
            return labels
        } catch (e: IOException) {
            return emptyList()
        }
    }


    val TEMP_CLASSES = List(1000) { "class${it + 1}" }
}
