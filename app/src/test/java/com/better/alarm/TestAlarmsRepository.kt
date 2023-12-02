package com.better.alarm

import com.better.alarm.data.AlarmStore
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.AlarmsRepository
import com.better.alarm.data.stores.InMemoryRxDataStoreFactory.Companion.inMemoryRxDataStore

class TestAlarmsRepository : AlarmsRepository {
  private var idCounter: Int = 0
  val createdRecords = mutableListOf<AlarmStore>()

  override fun create(): AlarmStore {
    val storeId = idCounter++
    val inMemory = inMemoryRxDataStore(AlarmValue(id = storeId))
    return object : AlarmStore {
          override var value = inMemory.value
          override val id: Int = storeId

          override fun delete() {
            createdRecords.removeIf { it.value.id == value.id }
          }
        }
        .also { createdRecords.add(it) }
  }

  override fun query(): List<AlarmStore> {
    return createdRecords
  }

  override var initialized: Boolean = true

  override fun awaitStored() {}
}
