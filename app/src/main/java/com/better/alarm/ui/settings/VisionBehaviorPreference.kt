package com.better.alarm.ui.settings

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.better.alarm.R
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.logger.Logger
import com.better.alarm.vision.Behavior
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VisionBehaviorPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
  val logger: Logger by globalLogger("VisionBehaviorPreference")
  var value: String? = null
    set(value) {
      field = value
      persistString(value)
      notifyChanged()
    }

  private lateinit var behaviorListLayout: LinearLayout
  private val items = mutableListOf<VisionBehaviorItemView>()
  private var addButton: ImageButton? = null
  private var initialValueString = ""
  init {
    layoutResource = R.layout.preference_vision_behavior
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)

    behaviorListLayout = holder.itemView.findViewById(R.id.behaviorList)
    behaviorListLayout.removeAllViews()
    items.clear()
    addButton = holder.itemView.findViewById(R.id.addButton)
    addButton?.setOnClickListener {
      addItem()
    }

    if (initialValueString.isNotEmpty()) {
      try {
        val storedValue: Behavior.BehaviorsStoreValue = Json.decodeFromString(initialValueString)
        storedValue.items.forEach {
          addItem(it)
        }
      } catch (e: Exception) {
        logger.error { e.stackTraceToString() }
      }
    }


    holder.itemView.findViewById<Button>(R.id.test).setOnClickListener {
      val intent = Intent(context, CameraTestActivity::class.java)
      context.startActivity(intent)
    }
    holder.itemView.findViewById<ImageButton>(R.id.instruction).setOnClickListener {
      val builder = AlertDialog.Builder(context).apply {
        setTitle("Instructions")
        setView(R.layout.vision_behavior_instruction)
        setPositiveButton("OK") { dialog, _ ->
          dialog.dismiss()
        }
      }

      val dialog = builder.create()
      dialog.show()
    }
  }

  override fun onSetInitialValue(defaultValue: Any?) {
    initialValueString = getPersistedString(defaultValue as? String)

  }

  private fun addItem(item: Behavior.BehaviorStoreItem? = null) {

    val view = LayoutInflater.from(context).inflate(R.layout.vision_behavior_item, behaviorListLayout, false) as VisionBehaviorItemView
    view.onRemove = {
      items.remove(view)
      val animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_out_right)
      animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
        override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

        override fun onAnimationEnd(animation: android.view.animation.Animation?) {
          behaviorListLayout.removeView(view)
        }

        override fun onAnimationStart(animation: android.view.animation.Animation?) {}
      })
      view.startAnimation(animation)
      val newValue = Behavior.BehaviorsStoreValue(items.map { it.currentData })
      val valueString = Json.encodeToString(newValue)
      persistString(valueString)
    }
    view.onUpdate = {
      val newValue = Behavior.BehaviorsStoreValue(items.map { it.currentData })
      val valueString = Json.encodeToString(newValue)
      persistString(valueString)
    }
    if (item != null) {
      view.currentData = item
    }
    val animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
    view.startAnimation(animation)
    behaviorListLayout.addView(view, 0)
    items.add(view)
  }
}
