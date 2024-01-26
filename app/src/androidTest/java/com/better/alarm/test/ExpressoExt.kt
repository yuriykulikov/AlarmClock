package com.better.alarm.test

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.better.alarm.R
import com.better.alarm.data.AlarmValue
import com.better.alarm.ui.list.AlarmListAdapter
import com.better.alarm.ui.list.AlarmsListFragment
import java.util.function.Predicate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.hamcrest.Matchers

class OnView {
  fun with(id: Int? = null, text: String? = null): ViewInteraction {
    val matcher =
        listOfNotNull(text?.let { ViewMatchers.withText(it) }, id?.let { ViewMatchers.withId(it) })
            .let { Matchers.allOf(it) }
    return Espresso.onView(matcher)
  }
}

fun onView(): OnView {
  return OnView()
}

fun ViewInteraction.click() {
  perform(ViewActions.click())
}

fun ViewInteraction.assertDisabled() {
  check(ViewAssertions.matches(Matchers.not<View>(ViewMatchers.isEnabled())))
}

fun ViewInteraction.assertEnabled() {
  check(ViewAssertions.matches(ViewMatchers.isEnabled()))
}

/** Returns [AlarmValue] currently in the alarms list. */
fun alarmsList(): List<AlarmValue> {
  return buildList {
    Espresso.onView(ViewMatchers.withId(R.id.list_fragment_list)).check { view, noViewFoundException
      ->
      noViewFoundException?.let { throw RuntimeException(noViewFoundException) }
      val adapter = (view as RecyclerView).adapter as AlarmListAdapter
      addAll(adapter.dataset)
    }
  }
}

fun assertTimerView(s: String?) {
  Espresso.onView(ViewMatchers.withId(R.id.time_picker_time))
      .check(ViewAssertions.matches(ViewMatchers.withText(s)))
}

fun enabled(): Predicate<AlarmValue> {
  return Predicate { (_, _, _, isEnabled) -> isEnabled }
}

class TestSync {
  companion object {
    private val ANIMATIONS_OFF = System.getenv("GITHUB_ACTIONS") == "true"

    fun clickFab() {
      if (ANIMATIONS_OFF) {
        onView().with(id = R.id.fab).click()
      } else {
        runBlocking {
          withContext(Dispatchers.Default) {
            withTimeout(2500) {
              while (!clickDone()) {
                // loop
              }
            }
          }
        }
      }
    }

    private suspend fun clickDone(): Boolean {
      AlarmsListFragment.fabSync = Channel(1)
      onView().with(id = R.id.fab).click()
      return withTimeoutOrNull(50) {
        AlarmsListFragment.fabSync?.receive()
        true
      } == true
    }

    fun openActionBarOverflowOrOptionsMenu() {
      if (!ANIMATIONS_OFF) Thread.sleep(500)
      Espresso.openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
    }
  }
}
