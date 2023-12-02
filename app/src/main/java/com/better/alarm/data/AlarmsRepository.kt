package com.better.alarm.data

import com.better.alarm.data.stores.RxDataStore

/** Repository for [AlarmValue] with an active record interface. */
interface AlarmsRepository {
  /** Creates a new [AlarmStore] with initial [AlarmValue]. All changes will be stored. */
  fun create(): AlarmStore
  /** Query stored [AlarmValue]s as active records ([AlarmStore]s). */
  fun query(): List<AlarmStore>
  /** Check if the repository is initialized. */
  val initialized: Boolean

  /**
   * Awaits until all pending changes are durably stored. This call is required before the system
   * goes to sleep or the application can be destroyed.
   */
  fun awaitStored()
}

/** Active record for a single [AlarmValue]. */
interface AlarmStore {
  val id: Int
  var value: AlarmValue

  fun delete()
}

/** Change the contents of the [RxDataStore] given the previous value. */
inline fun AlarmStore.modify(func: AlarmValue.(prev: AlarmValue) -> AlarmValue) {
  val prev = value
  value = func(prev, prev)
}
