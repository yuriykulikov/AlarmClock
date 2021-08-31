package com.better.alarm.model

import android.database.Cursor
import com.better.alarm.stores.RxDataStore

/** Created by Yuriy on 24.06.2017. */
interface ContainerFactory {
  fun create(): AlarmStore
  fun create(cursor: Cursor): AlarmStore
}

interface AlarmStore : RxDataStore<AlarmValue> {
  fun delete()
}
