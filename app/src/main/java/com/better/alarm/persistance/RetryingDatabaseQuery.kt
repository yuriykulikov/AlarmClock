package com.better.alarm.persistance

import android.content.ContentResolver
import android.database.Cursor
import com.better.alarm.model.AlarmStore
import com.better.alarm.model.ContainerFactory
import com.better.alarm.persistance.Columns.Companion.contentUri
import kotlinx.coroutines.delay

interface DatabaseQuery {
    suspend fun query(): List<AlarmStore>
}

/**
 * Created by Yuriy on 10.06.2017.
 */
class RetryingDatabaseQuery(
        private val contentResolver: ContentResolver,
        private val factory: ContainerFactory
) : DatabaseQuery {
    override suspend fun query(): List<AlarmStore> {
        return tryQuery()
                ?.drain { cursor -> factory.create(cursor) }
                ?: emptyList()
    }

    private suspend fun tryQuery(): Cursor? {
        repeat(120) {
            val query = contentResolver.query(contentUri(), Columns.ALARM_QUERY_COLUMNS, null, null, Columns.DEFAULT_SORT_ORDER)
            if (query != null) {
                return query
            } else {
                delay(500)
            }
        }
        return null
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