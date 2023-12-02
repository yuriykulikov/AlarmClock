package com.better.alarm.test

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.better.alarm.data.DaysOfWeek
import com.better.alarm.data.contentprovider.AlarmDatabaseHelper
import com.better.alarm.data.contentprovider.SQLiteDatabaseQuery
import com.better.alarm.receivers.TestReceiver
import com.better.alarm.ui.main.AlarmsListActivity
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Empty store, non empty db -> migrate Empty store, empty db -> defaults Non-empty store, nothing
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
  @Rule @JvmField var listActivity = ActivityScenarioRule(AlarmsListActivity::class.java)

  companion object {
    @AfterClass
    @JvmStatic
    fun restore() {
      dropDatabase()
    }
  }

  @Test
  fun whenMigratingFromAnEmptyDatabaseThenAlarmsListIsEmpty() =
      runBlocking<Unit> {
        // when
        sentTestIntent(TestReceiver.ACTION_DROP_AND_MIGRATE_DATABASE)
        // then
        assertThat(alarmsList()).hasSize(0)
      }

  @Test
  fun whenMigratingFromDatabaseThenAlarmsListContainsItemsAndDatabaseBecomesEmpty() =
      runBlocking<Unit> {
        // given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbHelper = AlarmDatabaseHelper(context)
        dbHelper.writableDatabase.use { db ->
          db.execSQL("DROP TABLE IF EXISTS alarms")
          db.execSQL(
              "CREATE TABLE alarms (_id INTEGER PRIMARY KEY,hour INTEGER, minutes INTEGER, daysofweek INTEGER, alarmtime INTEGER, " +
                  "enabled INTEGER, vibrate INTEGER, message TEXT, alert TEXT, prealarm INTEGER, state STRING);")
          val insertMe =
              ("INSERT INTO alarms " +
                  "(hour, minutes, daysofweek, alarmtime, enabled, vibrate, " +
                  "message, alert, prealarm, state) VALUES ")
          db.execSQL("$insertMe(8, 31, 31, 0, 0, 1, '', '', 0, '');")
          db.execSQL("$insertMe(9, 01, 96, 0, 0, 1, '', '', 0, '');")
        }

        assertThat(SQLiteDatabaseQuery(context.contentResolver).query()).isNotEmpty()

        // when
        sentTestIntent(TestReceiver.ACTION_DROP_AND_MIGRATE_DATABASE)

        // then
        assertThat(
                alarmsList().filter { alarmValue ->
                  alarmValue.daysOfWeek == DaysOfWeek(31) &&
                      alarmValue.hour == 8 &&
                      alarmValue.minutes == 31
                })
            .hasSize(1)

        assertThat(
                alarmsList().filter { alarmValue ->
                  alarmValue.daysOfWeek == DaysOfWeek(96) &&
                      alarmValue.hour == 9 &&
                      alarmValue.minutes == 1
                })
            .hasSize(1)

        assertThat(SQLiteDatabaseQuery(context.contentResolver).query()).isEmpty()
      }

  @Test
  fun defaultAlarmsAre830amd900() =
      runBlocking<Unit> {
        sentTestIntent(TestReceiver.ACTION_DROP)
        assertThat(alarmsList()).hasSize(0)

        // when
        sentTestIntent(TestReceiver.ACTION_DROP_AND_INSERT_DEFAULTS)

        // then
        assertThat(
                alarmsList().filter { alarmValue ->
                  alarmValue.daysOfWeek == DaysOfWeek(31) &&
                      alarmValue.hour == 8 &&
                      alarmValue.minutes == 30
                })
            .hasSize(1)

        assertThat(
                alarmsList().filter { alarmValue ->
                  alarmValue.daysOfWeek == DaysOfWeek(96) &&
                      alarmValue.hour == 9 &&
                      alarmValue.minutes == 0
                })
            .hasSize(1)
      }
}
