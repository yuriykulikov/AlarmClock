package com.better.alarm

import android.database.Cursor
import com.better.alarm.model.AlarmActiveRecord
import com.better.alarm.model.Calendars
import com.better.alarm.model.ContainerFactory
import com.better.alarm.persistance.PersistingContainerFactory

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestContainerFactory(private val calendars: Calendars) : ContainerFactory {
    private var idCounter: Int = 0

    override fun create(): AlarmActiveRecord {
        return PersistingContainerFactory.create(calendars, PersistingContainerFactory.PERSISTENCE_STUB, { _ -> idCounter++ })
    }

    override fun create(cursor: Cursor): AlarmActiveRecord {
        throw UnsupportedOperationException()
    }
}
