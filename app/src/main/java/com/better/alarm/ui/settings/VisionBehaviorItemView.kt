package com.better.alarm.ui.settings

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.better.alarm.R
import com.better.alarm.ui.alert.AlarmAlertVisionFullScreen
import com.better.alarm.vision.Behavior
import com.better.alarm.vision.Gesture
import com.better.alarm.vision.toFingersList
import com.better.alarm.vision.toFingersState

class VisionBehaviorItemView @JvmOverloads constructor(
  context: android.content.Context,
  attrs: android.util.AttributeSet? = null,
  defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var onRemove: (() -> Unit)? = null
  var onUpdate: (() -> Unit)? = null
  private lateinit var fingerButtons: List<ToggleButton>
  private lateinit var behaviorSpinner: Spinner

  override fun onFinishInflate() {
    super.onFinishInflate()
    findViewById<ImageButton>(R.id.delete).setOnClickListener {
      onRemove?.invoke()
    }
    fingerButtons = listOf(
      findViewById(R.id.thumb),
      findViewById(R.id.index),
      findViewById(R.id.middle),
      findViewById(R.id.ring),
      findViewById(R.id.little)
    )
    fingerButtons.forEach {
      it.setOnClickListener {
        updateCurrentData()
        onUpdate?.invoke()
      }
    }
    behaviorSpinner = findViewById(R.id.behavior_spinner)
    behaviorSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, AlarmAlertVisionFullScreen.behaviorList)
    behaviorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        updateCurrentData()
        onUpdate?.invoke()
      }
      override fun onNothingSelected(parent: AdapterView<*>?) { }
    }
  }

  private fun updateCurrentData() {
    currentData = Behavior.BehaviorStoreItem(
      Gesture(
        buildList {
          fingerButtons.forEachIndexed { index, button ->
            if (button.isChecked) add(1) else add(-1)
          }
        }.toFingersState()
      ), behaviorSpinner.selectedItem.toString()
    )
  }

  var currentData: Behavior.BehaviorStoreItem =
    Behavior.BehaviorStoreItem(
      Gesture(Gesture.FingersState(0, 0, 0, 0, 0)),
      AlarmAlertVisionFullScreen.behaviorList[0]
    )
    set(value) {
      val itemFingerStateList = value.gesture.fingers?.toFingersList()
      fingerButtons.forEachIndexed { index, button ->
        button.isChecked = itemFingerStateList?.get(index) == 1
      }
      behaviorSpinner.setSelection(AlarmAlertVisionFullScreen.behaviorList.indexOf(value.operation))
      field = value
    }

}
