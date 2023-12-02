package com.better.alarm.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.better.alarm.bootstrap.overrideIs24hoursFormatOverride
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.Alarmtone
import com.better.alarm.data.DaysOfWeek
import com.better.alarm.domain.Store
import com.better.alarm.ui.list.computeTexts
import java.util.Calendar
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InfoFragmentTest {
  @JvmField @Rule var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US))

  @Before
  fun setUp() {
    overrideIs24hoursFormatOverride(false)
  }

  @Test
  fun prealarmOff3Hours() {
    val now = Calendar.getInstance().timeInMillis
    assertThat(
            computeTexts(
                res = InstrumentationRegistry.getInstrumentation().targetContext.resources,
                alarm =
                    create(
                        Calendar.getInstance().apply {
                          timeInMillis = now
                          add(Calendar.HOUR_OF_DAY, 3)
                        },
                        false,
                    ),
                now = now,
                prealarmDuration = 30,
            ))
        .isEqualTo("3 hours")
  }

  @Test
  fun prealarmOn3Days() {
    val now = Calendar.getInstance().timeInMillis
    assertThat(
            computeTexts(
                res = InstrumentationRegistry.getInstrumentation().targetContext.resources,
                alarm =
                    create(
                        Calendar.getInstance().apply {
                          timeInMillis = now
                          add(Calendar.DAY_OF_YEAR, 3)
                        },
                        true,
                    ),
                now = now,
                prealarmDuration = 30,
            ))
        .isEqualTo("3 days\n30 minutes pre-alarm")
  }

  fun create(time: Calendar, isPrealarm: Boolean): Store.Next {
    return Store.Next(
        isPrealarm,
        AlarmValue(
            id = 1,
            alarmtone = Alarmtone.Default,
            daysOfWeek = DaysOfWeek(0),
            hour = 12,
            isEnabled = true,
            isPrealarm = isPrealarm,
            isVibrate = false,
            label = "",
            minutes = 1,
            nextTime = time,
            state = ""),
        nextNonPrealarmTime = time.timeInMillis)
  }
}
