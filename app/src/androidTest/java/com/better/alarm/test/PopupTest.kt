package com.better.alarm.test

import android.app.Activity
import android.content.Intent
import android.provider.AlarmClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.better.alarm.R
import com.better.alarm.bootstrap.overrideIs24hoursFormatOverride
import com.better.alarm.data.CalendarType
import com.better.alarm.domain.AlarmSetter
import com.better.alarm.domain.Store
import com.better.alarm.receivers.AlarmsReceiver
import com.better.alarm.receivers.Intents
import com.better.alarm.ui.alert.AlarmAlertFullScreen
import com.better.alarm.ui.alert.TransparentActivity
import com.better.alarm.ui.exported.HandleSetAlarm
import com.better.alarm.ui.main.AlarmsListActivity
import java.util.*
import java.util.stream.Collectors
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Created by Yuriy on 12.07.2017. */
@RunWith(AndroidJUnit4::class)
class PopupTest {

  var listActivity = ActivityScenarioRule(AlarmsListActivity::class.java)

  @JvmField
  @Rule
  var chain: TestRule =
      RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity) // .around(alertActivity)

  @Before
  fun before() {
    overrideIs24hoursFormatOverride(true)
    dropDatabase()
  }

  @After
  @Throws(InterruptedException::class)
  fun tearDown() {
    dropDatabase()
  }

  private fun createAlarmAndFire(): Int {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    context.startActivity(
        Intent().apply {
          setClass(context, HandleSetAlarm::class.java)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          action = AlarmClock.ACTION_SET_ALARM
          putExtra(AlarmClock.EXTRA_SKIP_UI, true)
          putExtra(AlarmClock.EXTRA_HOUR, 0)
          putExtra(AlarmClock.EXTRA_MINUTES, 0)
          putExtra(AlarmClock.EXTRA_MESSAGE, "From outside")
        })
    assertThat(alarmsList().filter { it.isEnabled }).hasSize(1)
    val id = alarmsList()[0].id

    // simulate alarm fired
    context.sendBroadcast(
        Intent(AlarmSetter.ACTION_FIRED).apply {
          setClass(context, AlarmsReceiver::class.java)
          putExtra(AlarmSetter.EXTRA_ID, id)
          putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.NORMAL.name)
        })
    return id
  }

  @Test
  fun dismissViaAlarmAlert() {
    val id = createAlarmAndFire()
    launchActivity(AlarmAlertFullScreen::class.java, id)
    onView().with(text = "Dismiss").perform(ViewActions.longClick())

    launchActivity(AlarmsListActivity::class.java, id)
    // delete after dismiss
    assertThat(alarmsList()).hasSize(2)
    // all disabled
    assertThat(alarmsList().filter { it.isEnabled }).hasSize(0)
  }

  private fun launchActivity(activityClass: Class<out Activity>, id: Int) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    ActivityScenario.launch<Activity>(
            Intent(context, activityClass).apply { putExtra(Intents.EXTRA_ID, id) })
        .moveToState(Lifecycle.State.RESUMED)
  }

  private fun afterLongClickSnoozeAlarmCheckAndDelete() {
    assertTimerView("--:--")
    Espresso.onView(ViewMatchers.withText("2")).perform(ViewActions.click())
    assertTimerView("--:-2")
    Espresso.onView(ViewMatchers.withText("3")).perform(ViewActions.click())
    assertTimerView("--:23")
    Espresso.onView(ViewMatchers.withText("5")).perform(ViewActions.click())
    assertTimerView("-2:35")
    Espresso.onView(ViewMatchers.withText("9")).perform(ViewActions.click())
    assertTimerView("23:59")
    Espresso.onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
    val next = GlobalContext.get().get<Store>().next().blockingFirst()
    assertThat(next.isPresent()).isTrue
    val nextTime = Calendar.getInstance()
    nextTime.timeInMillis = next.get().nextNonPrealarmTime()
    assertThat(nextTime[Calendar.HOUR_OF_DAY]).isEqualTo(23)
    assertThat(nextTime[Calendar.MINUTE]).isEqualTo(59)
    assertThat(next.get().alarm().isEnabled).isTrue

    // disable the snoozed alarm
    Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.list_row_on_off_switch), ViewMatchers.isChecked()))
        .perform(ViewActions.click())
    assertThat(alarmsList().stream().filter(enabled()).collect(Collectors.toList())).isEmpty()
  }

  @Ignore("Flaky")
  @Test
  fun snoozeViaClick() {
    val id = createAlarmAndFire()
    launchActivity(AlarmAlertFullScreen::class.java, id)
    onView().with(text = "Snooze").click()
  }

  @Ignore("Flaky")
  @Test
  fun snoozeViaNotificationPicker() {
    val id = createAlarmAndFire()
    launchActivity(TransparentActivity::class.java, id)
    afterLongClickSnoozeAlarmCheckAndDelete()
  }

  @Test
  fun letAlarmExpireAndDismissIt() {
    val id = createAlarmAndFire()
    launchActivity(AlarmAlertFullScreen::class.java, id)

    // simulate timed out
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    context.sendBroadcast(
        Intent(context, AlarmsReceiver::class.java).apply {
          action = AlarmSetter.ACTION_FIRED
          putExtra(AlarmSetter.EXTRA_ID, id)
          putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.AUTOSILENCE.name)
        })

    launchActivity(AlarmsListActivity::class.java, id)
    // delete after dismiss
    assertThat(alarmsList()).hasSize(2)
    assertThat(alarmsList().filter { it.isEnabled }).hasSize(0)
  }
}
