package com.better.alarm.test

import androidx.test.espresso.ViewInteraction
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.better.alarm.R
import com.better.alarm.bootstrap.overrideIs24hoursFormatOverride
import com.better.alarm.test.TestSync.Companion.clickFab
import com.better.alarm.ui.main.AlarmsListActivity
import java.util.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickerAmTest {
  @JvmField var listActivity = ActivityScenarioRule(AlarmsListActivity::class.java)

  @JvmField
  @Rule
  var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity)

  @Before
  fun setUp() {
    dropDatabase()
    overrideIs24hoursFormatOverride(false)
    clickFab()
  }

  @After
  fun tearDown() {
    listActivity.scenario.close()
    dropDatabase()
  }

  @Test
  fun testNothing() {
    assertTimerView("--:--")
    arrayOf(one(), two(), three(), four(), five(), six(), seven(), eight(), nine()).forEach {
      it.assertEnabled()
    }
    zero().assertDisabled()
    ok().assertDisabled()
    am().assertDisabled()
    pm().assertDisabled()
  }

  @Test
  fun test1() {
    one().click()
    arrayOf(zero(), one(), two()).forEach { it.assertEnabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test1pm() {
    one().click()
    arrayOf(zero(), one(), two()).forEach { it.assertEnabled() }
    am().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertDisabled() }
    ok().assertEnabled()
    pm().assertDisabled()
  }

  @Test
  fun test2() {
    two().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test3() {
    three().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test4() {
    four().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test5() {
    five().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test6() {
    six().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test7() {
    seven().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test8() {
    eight().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test9() {
    nine().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    ok().assertDisabled()
    pm().assertEnabled()
  }

  @Test
  fun test1230pm() {
    one().click()
    two().click()
    three().click()
    zero().click()
    pm().click()

    arrayOf<ViewInteraction>().forEach { it.assertEnabled() }
    pm().assertDisabled()
    ok().assertEnabled()
  }

  private fun one(): ViewInteraction = onView().with(id = R.id.key_left, text = "1")

  private fun two(): ViewInteraction = onView().with(id = R.id.key_middle, text = "2")

  private fun three(): ViewInteraction = onView().with(id = R.id.key_right, text = "3")

  private fun four(): ViewInteraction = onView().with(id = R.id.key_left, text = "4")

  private fun five(): ViewInteraction = onView().with(id = R.id.key_middle, text = "5")

  private fun six(): ViewInteraction = onView().with(id = R.id.key_right, text = "6")

  private fun seven(): ViewInteraction = onView().with(id = R.id.key_left, text = "7")

  private fun eight(): ViewInteraction = onView().with(id = R.id.key_middle, text = "8")

  private fun nine(): ViewInteraction = onView().with(id = R.id.key_right, text = "9")

  private fun zero(): ViewInteraction = onView().with(id = R.id.key_middle, text = "0")

  private fun ok(): ViewInteraction = onView().with(text = "OK")

  private fun am(): ViewInteraction = onView().with(id = R.id.key_left, text = "AM")

  private fun pm(): ViewInteraction = onView().with(id = R.id.key_right, text = "PM")
}
