package com.better.alarm.model

import com.better.alarm.stores.RxDataStore

/** Created by Yuriy on 24.06.2017. */
interface AlarmsRepository {
  fun create(): AlarmStore
  fun query(): List<AlarmStore>
  val initialized: Boolean
  suspend fun awaitStored()
}

interface AlarmStore {
  val id: Int
  var value: AlarmValue
  fun delete()

  suspend fun awaitStored()
}

/** Change the contents of the [RxDataStore] given the previous value. */
inline fun AlarmStore.modify(func: AlarmValue.(prev: AlarmValue) -> AlarmValue) {
  val prev = value
  value = func(prev, prev)
}
