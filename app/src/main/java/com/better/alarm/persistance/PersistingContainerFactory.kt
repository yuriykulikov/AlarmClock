package com.better.alarm.persistance

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.better.alarm.model.*
import java.util.Calendar

/**
 * Active record container for all alarm data.
 *
 * @author Yuriy
 */
class PersistingContainerFactory(private val calendars: Calendars, private val mContext: Context) : ContainerFactory, AlarmActiveRecord.Persistence {

    override fun create(c: Cursor): AlarmActiveRecord {
        return fromCursor(c, calendars, this)
    }

    /** TODO there is a cheaper way to create an ID */
    override fun create(): AlarmActiveRecord {
        return create(calendars, this) { container -> ContentUris.parseId(mContext.contentResolver.insert(Columns.contentUri(), container.createContentValues())).toInt() }
    }

    /**
     * Persist data in the database
     */
    override fun persist(container: AlarmActiveRecord) {
        val values: ContentValues = container.createContentValues()
        val uriWithAppendedId = ContentUris.withAppendedId(Columns.contentUri(), container.id.toLong())
        mContext.contentResolver.update(uriWithAppendedId, values, null, null)
    }

    override fun delete(container: AlarmActiveRecord) {
        val uri = ContentUris.withAppendedId(Columns.contentUri(), container.id.toLong())
        mContext.contentResolver.delete(uri, "", null)
    }

    private fun fromCursor(c: Cursor, calendars: Calendars, persistence: AlarmActiveRecord.Persistence): AlarmActiveRecord {
        return AlarmActiveRecord(alarmValue = AlarmData(
                id = c.getInt(Columns.ALARM_ID_INDEX),
                isEnabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1,
                hour = c.getInt(Columns.ALARM_HOUR_INDEX),
                minutes = c.getInt(Columns.ALARM_MINUTES_INDEX),
                daysOfWeek = DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX)),
                isVibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1,
                isPrealarm = c.getInt(Columns.ALARM_PREALARM_INDEX) == 1,
                label = c.getString(Columns.ALARM_MESSAGE_INDEX) ?: "",
                alarmtone = Alarmtone.fromString(c.getString(Columns.ALARM_ALERT_INDEX)),
                skipping = false
        ),
                state = c.getString(Columns.ALARM_STATE_INDEX),
                nextTime = calendars.now().apply { timeInMillis = c.getLong(Columns.ALARM_TIME_INDEX) },
                persistence = persistence
        )
    }

    companion object {
        val PERSISTENCE_STUB: AlarmActiveRecord.Persistence = object : AlarmActiveRecord.Persistence {
            override fun persist(activeRecord: AlarmActiveRecord) {
                //STUB
            }

            override fun delete(activeRecord: AlarmActiveRecord) {
                //STUB
            }
        }

        @JvmStatic
        fun create(calendars: Calendars, persistence: AlarmActiveRecord.Persistence, idMapper: (AlarmActiveRecord) -> Int): AlarmActiveRecord {
            val now = calendars.now()

            val defaultActiveRecord = AlarmActiveRecord(alarmValue = AlarmData(
                    id = -1,
                    isEnabled = false,
                    hour = now.get(Calendar.HOUR_OF_DAY),
                    minutes = now.get(Calendar.MINUTE),
                    daysOfWeek = DaysOfWeek(0),
                    isVibrate = true,
                    isPrealarm = false,
                    label = "",
                    alarmtone = Alarmtone.Default(),
                    skipping = false
            ),
                    state = "",
                    nextTime = now,
                    persistence = PERSISTENCE_STUB
            )

            //generate a new id
            val id = idMapper(defaultActiveRecord)
            //assign the id and return an object with it. TODO here we have an unnecessary DB flush
            val withId = defaultActiveRecord.alarmValue.copy(id = id)
            return defaultActiveRecord.copyAndPersist(alarmValue = withId, persistence = persistence)
        }
    }

    private fun AlarmActiveRecord.createContentValues(): ContentValues {
        return ContentValues(12).apply {
            // id
            put(Columns.ENABLED, isEnabled)
            put(Columns.HOUR, hour)
            put(Columns.MINUTES, minutes)
            put(Columns.DAYS_OF_WEEK, daysOfWeek.coded)
            put(Columns.VIBRATE, isVibrate)
            put(Columns.MESSAGE, label)
            put(Columns.ALERT, alarmtone.persistedString)
            put(Columns.PREALARM, isPrealarm)
            put(Columns.ALARM_TIME, nextTime.timeInMillis)
            put(Columns.STATE, state)
        }
    }
}
