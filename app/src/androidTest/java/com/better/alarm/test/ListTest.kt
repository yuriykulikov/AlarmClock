package com.better.alarm.test

import android.content.Intent
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.better.alarm.R
import com.better.alarm.configuration.AlarmApplication
import com.better.alarm.configuration.overrideIs24hoursFormatOverride
import com.better.alarm.interfaces.PresentationToModelIntents
import com.better.alarm.model.AlarmSetter
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.AlarmsReceiver
import com.better.alarm.model.CalendarType
import com.better.alarm.presenter.AlarmsListActivity
import com.better.alarm.util.Optional
import cortado.Cortado
import org.hamcrest.Matchers.anything
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ListTest : BaseTest() {
    @JvmField
    var listActivity = ActivityTestRule(
            AlarmsListActivity::class.java, false, /* autostart*/ true)

    @JvmField
    @Rule
    var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity)

    @Test
    @Throws(Exception::class)
    fun newAlarmShouldBeDisabledIfNotEdited() {
        BaseTest.sleep()
        onView(withId(R.id.fab)).perform(click())
        BaseTest.sleep()
        onView(withText("Cancel")).perform(click())
        BaseTest.sleep()
        Cortado.onView().withText("OK").perform().click()
        BaseTest.sleep()

        assertThatList().items().hasSize(3)

        ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .isEmpty()

        deleteAlarm(0)
        assertThatList().items().hasSize(2)
    }

    @Test
    @Throws(Exception::class)
    fun newAlarmShouldBeEnabledIfEdited12() {
        overrideIs24hoursFormatOverride(false)
        newAlarmShouldBeEnabledIfEdited()
    }

    @Test
    @Throws(Exception::class)
    fun newAlarmShouldBeEnabledIfEdited24() {
        overrideIs24hoursFormatOverride(true)
        newAlarmShouldBeEnabledIfEdited()
    }

    @Throws(Exception::class)
    private fun newAlarmShouldBeEnabledIfEdited() {
        onView(withId(R.id.fab)).perform(click())
        BaseTest.sleep()
        Cortado.onView().withText("1").perform().click()
        Cortado.onView().withText("2").perform().click()
        Cortado.onView().withText("3").perform().click()
        Cortado.onView().withText("5").perform().click()

        onView(withText("AM"))
                .withFailureHandler { error, viewMatcher ->
                    //ignore fails - only use if View is found
                }
                .perform(click())

        BaseTest.sleep()
        onView(withText("OK")).perform(click())
        Cortado.onView().withText("OK").perform().click()
        BaseTest.sleep()

        assertThatList().items().hasSize(3)

        ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .hasSize(1)

        deleteAlarm(0)
        assertThatList().items().hasSize(2)
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteNewAlarmInDetailsActivity() {
        onView(withId(R.id.fab)).perform(click())
        BaseTest.sleep()
        onView(withText("Cancel")).perform(click())
        Cortado.onView().withText("OK").perform().click()
        BaseTest.sleep()

        assertThatList().items().hasSize(3)
        BaseTest.sleep()

        onData(anything()).atPosition(0).onChildView(withId(R.id.details_button_container)).perform(click())
        BaseTest.sleep()

        Cortado.onView().withId(R.id.set_alarm_menu_delete_alarm).perform().click()
        BaseTest.sleep()

        Cortado.onView().withText("OK").perform().click()
        BaseTest.sleep()

        assertThatList().items().hasSize(2)
    }

    @Test
    @Throws(Exception::class)
    fun newAlarmShouldBeDisabledAfterDismiss() {
        onView(withId(R.id.fab)).perform(click())
        BaseTest.sleep()
        Cortado.onView().withText("1").perform().click()
        Cortado.onView().withText("2").perform().click()
        Cortado.onView().withText("3").perform().click()
        Cortado.onView().withText("5").perform().click()

        onView(withText("AM"))
                .withFailureHandler { error, viewMatcher ->
                    //ignore fails - only use if View is found
                }
                .perform(click())

        BaseTest.sleep()
        onView(withText("OK")).perform(click())
        Cortado.onView().withText("OK").perform().click()
        BaseTest.sleep()

        ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .hasSize(1)

        val id = ListAsserts.listObservable<AlarmValue>(R.id.list_fragment_list).firstOrError().blockingGet().id

        //simulate alarm fired
        listActivity.activity.sendBroadcast(Intent().apply {
            action = AlarmSetter.ACTION_FIRED
            setClass(listActivity.activity, AlarmsReceiver::class.java)
            putExtra(AlarmSetter.EXTRA_ID, id)
            putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.NORMAL.name)
        })

        BaseTest.sleep()

        //simulate dismiss from the notification bar
        listActivity.activity.sendBroadcast(Intent().apply {
            action = PresentationToModelIntents.ACTION_REQUEST_DISMISS
            setClass(listActivity.activity, AlarmsReceiver::class.java)
            putExtra(AlarmSetter.EXTRA_ID, id)
        })

        BaseTest.sleep()

        //alarm must be disabled because there is no repeating
        ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .isEmpty()

        deleteAlarm(0)
        assertThatList().items().hasSize(2)
    }

    @Test
    @Throws(Exception::class)
    fun editAlarmALot() {
        onView(withId(R.id.fab)).perform(click())
        BaseTest.sleep()
        assertTimerView("--:--")
        Cortado.onView().withText("1").perform().click()
        assertTimerView("--:-1")
        Cortado.onView().withText("2").perform().click()
        assertTimerView("--:12")
        Cortado.onView().withText("3").perform().click()
        assertTimerView("-1:23")
        Cortado.onView().withText("5").perform().click()
        assertTimerView("12:35")
        Cortado.onView().withId(R.id.delete).perform().click()
        assertTimerView("-1:23")
        Cortado.onView().withId(R.id.delete).perform().click()
        assertTimerView("--:12")
        Cortado.onView().withId(R.id.delete).perform().longClick()
        assertTimerView("--:--")
        BaseTest.sleep()
        onView(withText("Cancel")).perform(click())
        BaseTest.sleep()
        onView(withText("Cancel")).perform(click())
        assertThatList().items().hasSize(2)
    }

    @Test
    @Throws(Exception::class)
    fun editRepeat() {
        BaseTest.sleep()
        onView(withId(R.id.fab)).perform(click())
        BaseTest.sleep()
        onView(withText("Cancel")).perform(click())
        BaseTest.sleep()

        onView(withText("Repeat")).perform(click())
        onView(withText("Monday")).perform(click())
        onView(withText("Tuesday")).perform(click())
        onView(withText("Wednesday")).perform(click())
        onView(withText("Thursday")).perform(click())
        onView(withText("Friday")).perform(click())
        onView(withText("Saturday")).perform(click())
        onView(withText("Sunday")).perform(click())
        onView(withText("OK")).perform(click())

        Cortado.onView().withText("OK").perform().click()
        BaseTest.sleep()
        BaseTest.sleep()

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
    @Throws(Exception::class)
    fun changeTimeInList() {
        onData(anything()).atPosition(0).onChildView(withId(R.id.digital_clock_time)).perform(click())

        BaseTest.sleep()
        Cortado.onView().withText("1").perform().click()
        Cortado.onView().withText("2").perform().click()
        Cortado.onView().withText("3").perform().click()
        Cortado.onView().withText("5").perform().click()

        onView(withText("AM"))
                .withFailureHandler { error, viewMatcher ->
                    //ignore fails - only use if View is found
                }
                .perform(click())

        BaseTest.sleep()
        onView(withText("OK")).perform(click())
        BaseTest.sleep()

        ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .hasSize(1)

        onData(anything()).atPosition(0).onChildView(withId(R.id.list_row_on_off_checkbox_container)).perform(click())
        BaseTest.sleep()
        ListAsserts.assertThatList<AlarmValue>(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .isEmpty()
    }
}