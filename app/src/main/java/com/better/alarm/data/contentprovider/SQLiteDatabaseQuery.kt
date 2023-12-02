package com.better.alarm.data.contentprovider

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.Alarmtone
import com.better.alarm.data.DaysOfWeek
import java.util.*

interface DatabaseQuery {
  fun query(): List<AlarmValue>

  fun delete(id: Int)
}

/** Created by Yuriy on 10.06.2017. */
class SQLiteDatabaseQuery(
    private val contentResolver: ContentResolver,
) : DatabaseQuery {
  override fun query(): List<AlarmValue> {
    val list = mutableListOf<AlarmValue>()
    contentResolver
        .query(
            Columns.contentUri(),
            Columns.ALARM_QUERY_COLUMNS,
            null,
            null,
            Columns.DEFAULT_SORT_ORDER)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            do {
              list.add(fromCursor(cursor))
            } while (cursor.moveToNext())
          }
        }
    return list
  }

  override fun delete(id: Int) {
    val uri = ContentUris.withAppendedId(Columns.contentUri(), id.toLong())
    contentResolver.delete(uri, "", null)
  }

  private fun fromCursor(c: Cursor): AlarmValue {
    val enabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1
    return AlarmValue(
        id = c.getInt(Columns.ALARM_ID_INDEX),
        isEnabled = enabled,
        hour = c.getInt(Columns.ALARM_HOUR_INDEX),
        minutes = c.getInt(Columns.ALARM_MINUTES_INDEX),
        daysOfWeek = DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX)),
        isVibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1,
        isPrealarm = c.getInt(Columns.ALARM_PREALARM_INDEX) == 1,
        label = c.getString(Columns.ALARM_MESSAGE_INDEX) ?: "",
        alarmtone = Alarmtone.migrateFromString(c.getString(Columns.ALARM_ALERT_INDEX)),
        state = c.getString(Columns.ALARM_STATE_INDEX)
                ?: (if (enabled) "NormalSetState" else "DisabledState"),
        nextTime =
            Calendar.getInstance().apply { timeInMillis = c.getLong(Columns.ALARM_TIME_INDEX) })
  }
}
