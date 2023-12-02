package com.better.alarm.test

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.better.alarm.test.TestSync.Companion.openActionBarOverflowOrOptionsMenu
import com.better.alarm.ui.main.AlarmsListActivity
import java.util.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionBarTest {
  @JvmField var listActivity = ActivityScenarioRule(AlarmsListActivity::class.java)

  @JvmField
  @Rule
  var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity)

  @Test
  fun testBugreportButton() {
    openActionBarOverflowOrOptionsMenu()
    onView(withText("Send a bugreport")).perform(click())
    onView(withText("Cancel")).perform(click())
  }

  @Test
  fun testAboutButton() {
    openActionBarOverflowOrOptionsMenu()
    onView(withText("About")).perform(click())
    onView(withText("OK")).perform(click())
  }

  @Test
  fun sayThanks() {
    openActionBarOverflowOrOptionsMenu()
    onView(withText("Say thanks")).perform(click())
    onView(withText("OK")).perform(click())
  }
}
