package com.better.alarm.model

fun removeWithId(alarmValues: List<AlarmValue>, id: Int): List<AlarmValue> {
    return alarmValues.filter { it.id != id }
}

fun addOrReplace(alarmValues: List<AlarmValue>, activeRecord: AlarmActiveRecord): List<AlarmValue> {
    var replaced: Boolean = false
    val withReplacedById = alarmValues.map { alarmValue ->
        if (activeRecord.id == alarmValue.id) {
            replaced = true
            activeRecord
        } else {
            alarmValue
        }
    }
    return if (replaced) withReplacedById else alarmValues.plus(activeRecord)
}
