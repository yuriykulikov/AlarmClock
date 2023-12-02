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
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickerTest {
  @JvmField var listActivity = ActivityScenarioRule(AlarmsListActivity::class.java)

  @JvmField
  @Rule
  var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity)

  @JvmField
  @Rule
  var watcher: TestRule =
      object : TestWatcher() {
        override fun starting(description: Description) {
          println("---- " + description.methodName + " ----")
        }
      }

  @Before
  fun setUp() {
    dropDatabase()
    overrideIs24hoursFormatOverride(true)
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
    allDigits().forEach { it.assertEnabled() }
    half().assertDisabled()
    zeros().assertDisabled()
    ok().assertDisabled()
  }

  @Test
  fun test1() {
    one().click()
    allDigits().forEach { it.assertEnabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test2() {
    two().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test3() {
    three().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test4() {
    four().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test5() {
    five().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test6() {
    six().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test7() {
    seven().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test8() {
    eight().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test9() {
    nine().click()
    arrayOf(zero(), one(), two(), three(), four(), five()).forEach { it.assertEnabled() }
    arrayOf(six(), seven(), eight(), nine()).forEach { it.assertDisabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  @Test
  fun test0() {
    zero().click()
    allDigits().forEach { it.assertEnabled() }
    half().assertEnabled()
    zeros().assertEnabled()
    ok().assertDisabled()
  }

  private fun allDigits() =
      arrayOf(zero(), one(), two(), three(), four(), five(), six(), seven(), eight(), nine())

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

  private fun zeros(): ViewInteraction = onView().with(id = R.id.key_left, text = ":00")

  private fun half(): ViewInteraction = onView().with(id = R.id.key_right, text = ":30")
}
