package com.better.alarm.test

import android.content.Intent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.better.alarm.R
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.model.TestReceiver
import com.better.alarm.persistance.AlarmDatabaseHelper
import com.better.alarm.persistance.SQLiteDatabaseQuery
import com.better.alarm.presenter.AlarmsListActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Empty store, non empty db -> migrate Empty store, empty db -> defaults Non-empty store, nothing
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
  @Rule @JvmField var listActivity = ActivityScenarioRule(AlarmsListActivity::class.java)

  @Test
  fun whenMigratingFromAnEmptyDatabaseThenAlarmsListIsEmpty() {
    // when
    sentTestIntent(TestReceiver.ACTION_DROP_AND_MIGRATE_DATABASE)

    // then
    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list).items().hasSize(0)
  }

  @Test
  fun whenMigratingFromDatabaseThenAlarmsListContainsItemsAndDatabaseBecomesEmpty() {
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
    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter { alarmValue ->
          alarmValue.daysOfWeek == DaysOfWeek(31) &&
              alarmValue.hour == 8 &&
              alarmValue.minutes == 31
        }
        .items()
        .hasSize(1)

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter { alarmValue ->
          alarmValue.daysOfWeek == DaysOfWeek(96) && alarmValue.hour == 9 && alarmValue.minutes == 1
        }
        .items()
        .hasSize(1)

    assertThat(SQLiteDatabaseQuery(context.contentResolver).query()).isEmpty()
  }

  @Test
  fun defaultAlarmsAre830amd900() {
    sentTestIntent(TestReceiver.ACTION_DROP)
    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list).items().hasSize(0)

    // when
    sentTestIntent(TestReceiver.ACTION_DROP_AND_INSERT_DEFAULTS)

    // then
    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter { alarmValue ->
          alarmValue.daysOfWeek == DaysOfWeek(31) &&
              alarmValue.hour == 8 &&
              alarmValue.minutes == 30
        }
        .items()
        .hasSize(1)

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter { alarmValue ->
          alarmValue.daysOfWeek == DaysOfWeek(96) && alarmValue.hour == 9 && alarmValue.minutes == 0
        }
        .items()
        .hasSize(1)
  }

  private fun sentTestIntent(action: String) {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val intent = Intent(action)
    intent.setClass(targetContext, TestReceiver::class.java)
    targetContext.sendBroadcast(intent)
    BaseTest.sleep()
  }
}
