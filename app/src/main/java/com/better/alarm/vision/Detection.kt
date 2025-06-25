package com.better.alarm.vision

import kotlinx.serialization.Serializable

object Behavior {
  @Serializable
  data class BehaviorStoreItem(val gesture: Gesture, val operation: String)
  @Serializable
  data class BehaviorsStoreValue(val items: List<Behavior.BehaviorStoreItem>)
}

