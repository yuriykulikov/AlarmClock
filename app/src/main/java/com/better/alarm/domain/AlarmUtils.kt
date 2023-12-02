package com.better.alarm.domain

import com.better.alarm.data.AlarmValue

fun removeWithId(alarmValues: List<AlarmValue>, id: Int): List<AlarmValue> {
  return alarmValues.filter { it.id != id }
}

/**
 * Replaces the [AlarmValue] if the list contains this ID and appends to the back if it does not
 * contain such id.
 */
fun addOrReplace(alarmValues: List<AlarmValue>, activeRecord: AlarmValue): List<AlarmValue> {
  var replaced = false
  val withReplacedById =
      alarmValues.map { alarmValue ->
        if (activeRecord.id == alarmValue.id) {
          replaced = true
          activeRecord
        } else {
          alarmValue
        }
      }
  return if (replaced) withReplacedById else alarmValues.plus(activeRecord)
}
