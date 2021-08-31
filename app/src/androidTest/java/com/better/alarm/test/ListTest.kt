package com.better.alarm.test

import android.content.Intent
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.better.alarm.R
import com.better.alarm.configuration.overrideIs24hoursFormatOverride
import com.better.alarm.interfaces.PresentationToModelIntents
import com.better.alarm.model.AlarmSetter
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.AlarmsReceiver
import com.better.alarm.model.CalendarType
import com.better.alarm.presenter.AlarmsListActivity
import java.util.Locale
import org.hamcrest.Matchers.anything
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListTest : BaseTest() {
  @JvmField var listActivity = ActivityScenarioRule(AlarmsListActivity::class.java)

  @JvmField
  @Rule
  var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity)

  @Test
  fun newAlarmShouldBeDisabledIfNotEdited() {
    sleep()
    onView(withId(R.id.fab)).perform(click())
    sleep()
    onView(withText("Cancel")).perform(click())
    sleep()
    onView(withText("OK")).perform(click())
    sleep()

    assertThatList().items().hasSize(3)

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter(enabled())
        .items()
        .isEmpty()

    deleteAlarm(0)
    assertThatList().items().hasSize(2)
  }

  @Test
  fun newAlarmShouldBeEnabledIfEdited12() {
    overrideIs24hoursFormatOverride(false)
    newAlarmShouldBeEnabledIfEdited()
  }

  @Test
  fun newAlarmShouldBeEnabledIfEdited24() {
    overrideIs24hoursFormatOverride(true)
    newAlarmShouldBeEnabledIfEdited()
  }

  private fun newAlarmShouldBeEnabledIfEdited() {
    onView(withId(R.id.fab)).perform(click())
    sleep()
    onView(withText("1")).perform(click())
    onView(withText("2")).perform(click())
    onView(withText("3")).perform(click())
    onView(withText("5")).perform(click())

    onView(withText("AM"))
        .withFailureHandler { _, _ ->
          // ignore fails - only use if View is found
        }
        .perform(click())

    sleep()
    onView(withText("OK")).perform(click())
    onView(withText("OK")).perform(click())
    sleep()

    assertThatList().items().hasSize(3)

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter(enabled())
        .items()
        .hasSize(1)

    deleteAlarm(0)
    assertThatList().items().hasSize(2)
  }

  @Test
  fun testDeleteNewAlarmInDetailsActivity() {
    onView(withId(R.id.fab)).perform(click())
    sleep()
    onView(withText("Cancel")).perform(click())
    onView(withText("OK")).perform(click())
    sleep()

    assertThatList().items().hasSize(3)
    sleep()

    onData(anything())
        .atPosition(0)
        .onChildView(withId(R.id.details_button_container))
        .perform(click())
    sleep()

    onView(withId(R.id.set_alarm_menu_delete_alarm)).perform(click())
    sleep()

    onView(withText("OK")).perform(click())
    sleep()

    assertThatList().items().hasSize(2)
  }

  @Test
  fun newAlarmShouldBeDisabledAfterDismiss() {
    onView(withId(R.id.fab)).perform(click())
    Thread.sleep(1000)
    onView(withText("1")).perform(click())
    onView(withText("2")).perform(click())
    onView(withText("3")).perform(click())
    onView(withText("5")).perform(click())

    onView(withText("AM"))
        .withFailureHandler { _, _ ->
          // ignore fails - only use if View is found
        }
        .perform(click())

    Thread.sleep(1000)
    onView(withText("OK")).perform(click())
    onView(withText("OK")).perform(click())
    Thread.sleep(1000)

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter(enabled())
        .items()
        .hasSize(1)

    val id =
        ListAsserts.listObservable<AlarmValue>(R.id.list_fragment_list)
            .firstOrError()
            .blockingGet()
            .id

    // simulate alarm fired
    listActivity.scenario.onActivity { activity ->
      activity.sendBroadcast(
          Intent().apply {
            action = AlarmSetter.ACTION_FIRED
            setClass(activity, AlarmsReceiver::class.java)
            putExtra(AlarmSetter.EXTRA_ID, id)
            putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.NORMAL.name)
          })
    }

    Thread.sleep(1000)

    // simulate dismiss from the notification bar
    listActivity.scenario.onActivity { activity ->
      activity.sendBroadcast(
          Intent().apply {
            action = PresentationToModelIntents.ACTION_REQUEST_DISMISS
            setClass(activity, AlarmsReceiver::class.java)
            putExtra(AlarmSetter.EXTRA_ID, id)
          })
    }

    Thread.sleep(1000)

    // alarm must be disabled because there is no repeating
    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter(enabled())
        .items()
        .isEmpty()

    Thread.sleep(1000)
    deleteAlarm(0)
    assertThatList().items().hasSize(2)
  }

  @Test
  fun editAlarmALot() {
    onView(withId(R.id.fab)).perform(click())
    sleep()
    assertTimerView("--:--")
    onView(withText("1")).perform(click())
    assertTimerView("--:-1")
    onView(withText("2")).perform(click())
    assertTimerView("--:12")
    onView(withText("3")).perform(click())
    assertTimerView("-1:23")
    onView(withText("5")).perform(click())
    assertTimerView("12:35")
    onView(withId(R.id.delete)).perform(click())
    assertTimerView("-1:23")
    onView(withId(R.id.delete)).perform(click())
    assertTimerView("--:12")
    onView(withId(R.id.delete)).perform(longClick())
    assertTimerView("--:--")
    sleep()
    onView(withText("Cancel")).perform(click())
    sleep()
    onView(withText("Cancel")).perform(click())
    assertThatList().items().hasSize(2)
  }

  @Test
  fun editRepeat() {
    sleep()
    onView(withId(R.id.fab)).perform(click())
    sleep()
    onView(withText("Cancel")).perform(click())
    sleep()

    onView(withText("Repeat")).perform(click())
    onView(withText("Monday")).perform(click())
    onView(withText("Tuesday")).perform(click())
    onView(withText("Wednesday")).perform(click())
    onView(withText("Thursday")).perform(click())
    onView(withText("Friday")).perform(click())
    onView(withText("Saturday")).perform(click())
    onView(withText("Sunday")).perform(click())
    onView(withText("OK")).perform(click())

    onView(withText("OK")).perform(click())
    sleep()
    sleep()

    assertThatList().items().hasSize(3)

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter(enabled())
        .items()
        .hasSize(1)

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter { alarmValue -> alarmValue.daysOfWeek.isRepeatSet }
        .items()
        .hasSize(3)

    onView(withText("Every day")).check(matches(isDisplayed()))

    deleteAlarm(0)
    assertThatList().items().hasSize(2)
  }

  @Test
  fun changeTimeInList() {
    onData(anything()).atPosition(0).onChildView(withId(R.id.digital_clock_time)).perform(click())

    sleep()
    onView(withText("1")).perform(click())
    onView(withText("2")).perform(click())
    onView(withText("3")).perform(click())
    onView(withText("5")).perform(click())

    onView(withText("AM"))
        .withFailureHandler { error, viewMatcher ->
          // ignore fails - only use if View is found
        }
        .perform(click())

    sleep()
    onView(withText("OK")).perform(click())
    sleep()

    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter(enabled())
        .items()
        .hasSize(1)

    onData(anything())
        .atPosition(0)
        .onChildView(withId(R.id.list_row_on_off_checkbox_container))
        .perform(click())
    sleep()
    ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
        .filter(enabled())
        .items()
        .isEmpty()
  }
}
