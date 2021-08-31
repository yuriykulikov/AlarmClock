package com.better.alarm

import com.better.alarm.model.AlarmStore
import com.better.alarm.model.ContainerFactory
import com.better.alarm.persistance.DatabaseQuery
import com.better.alarm.stores.modify

internal class DatabaseQueryMock {
  companion object {
    @JvmStatic
    fun createStub(list: MutableList<AlarmStore>): DatabaseQuery {
      return object : DatabaseQuery {
        override suspend fun query(): List<AlarmStore> {
          return list
        }
      }
    }

    @JvmStatic
    fun createWithFactory(factory: ContainerFactory): DatabaseQuery {
      return object : DatabaseQuery {
        override suspend fun query(): List<AlarmStore> {
          val container =
              factory.create().apply {
                modify { withId(100500).withIsEnabled(true).withLabel("hello") }
              }
          return listOf(container)
        }
      }
    }
  }
}
