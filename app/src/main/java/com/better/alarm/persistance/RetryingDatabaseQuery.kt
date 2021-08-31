package com.better.alarm.persistance

import android.content.ContentResolver
import android.database.Cursor
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmStore
import com.better.alarm.model.ContainerFactory
import com.better.alarm.persistance.Columns.Companion.contentUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

interface DatabaseQuery {
  suspend fun query(): List<AlarmStore>
}

/** Created by Yuriy on 10.06.2017. */
class RetryingDatabaseQuery(
    private val contentResolver: ContentResolver,
    private val factory: ContainerFactory,
    private val logger: Logger
) : DatabaseQuery {
  private val retryDelay: Long = 100
  private val timeout = 5_000
  override suspend fun query(): List<AlarmStore> {

    return flow {
          val cursor: Cursor =
              requireNotNull(
                  contentResolver.query(
                      contentUri(),
                      Columns.ALARM_QUERY_COLUMNS,
                      null,
                      null,
                      Columns.DEFAULT_SORT_ORDER))
          emit(cursor)
        }
        .map { cursor -> cursor.drain { factory.create(it) } }
        .retryWhen { cause, attempt ->
          logger.error { "Failed to create alarms: $cause, retry in $retryDelay" }
          delay(retryDelay)
          attempt < timeout / retryDelay
        }
        .catch { cause -> throw RuntimeException("Failed to create alarms: $cause", cause) }
        .first()
  }

  private fun <T : Any> Cursor.drain(mapper: (Cursor) -> T): List<T> {
    return mutableListOf<T>().apply {
      use { cursor ->
        if (cursor.moveToFirst()) {
          do {
            add(mapper(cursor))
          } while (cursor.moveToNext())
        }
      }
    }
  }
}
