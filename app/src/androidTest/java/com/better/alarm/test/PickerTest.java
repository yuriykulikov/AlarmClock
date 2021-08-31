package com.better.alarm.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.better.alarm.R;
import com.better.alarm.configuration.ContainerKt;
import com.better.alarm.presenter.AlarmsListActivity;
import java.util.Locale;
import kotlin.collections.CollectionsKt;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PickerTest extends BaseTest {
  public ActivityScenarioRule<AlarmsListActivity> listActivity =
      new ActivityScenarioRule<AlarmsListActivity>(AlarmsListActivity.class);

  @Rule
  public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(listActivity);

  @Before
  public void setUp() {
    ContainerKt.overrideIs24hoursFormatOverride(true);
    onView(withId(R.id.fab)).perform(click());
    // sleep();
  }

  @After
  public void tearDown() {
    onView(withText("Cancel")).perform(click());
    // sleep();
    onView(withText("Cancel")).perform(click());
    assertThatList().items().hasSize(2);
  }

  @Test
  public void testNothing() {
    assertTimerView("--:--");
    assertState(Next.ANY, QuickMinutes.NOT_AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test1() {
    one().perform(click());
    assertState(Next.ANY, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test2() {
    two().perform(click());
    assertState(Next.AFTER_20, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test3() {
    three().perform(click());
    assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test4() {
    four().perform(click());
    assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test5() {
    five().perform(click());
    assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test6() {
    six().perform(click());
    assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test7() {
    seven().perform(click());
    assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test8() {
    eight().perform(click());
    assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test9() {
    nine().perform(click());
    assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  @Test
  public void test0() {
    zero().perform(click());
    assertState(Next.ANY, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
  }

  enum Next {
    ANY,
    AFTER_20,
    TENS_OF_MINUTES,
    NONE
  }

  enum Completion {
    CAN_FINISH,
    NOT_FINISHED
  }

  enum QuickMinutes {
    AVAILABLE,
    NOT_AVAILABLE
  }

  private void assertState(Next next, QuickMinutes quickMinutes, Completion completion) {
    switch (next) {
      case ANY:
        for (ViewInteraction view :
            CollectionsKt.arrayListOf(
                zero(), one(), two(), three(), four(), five(), six(), seven(), eight(), nine())) {
          view.check(matches(isEnabled()));
        }
        break;
      case AFTER_20:
        for (ViewInteraction view :
            CollectionsKt.arrayListOf(zero(), one(), two(), three(), four(), five())) {
          view.check(matches(isEnabled()));
        }

        for (ViewInteraction view : CollectionsKt.arrayListOf(six(), seven(), eight(), nine())) {
          view.check(matches(not(isEnabled())));
        }
        break;
      case TENS_OF_MINUTES:
        for (ViewInteraction view :
            CollectionsKt.arrayListOf(zero(), one(), two(), three(), four(), five())) {
          view.check(matches(isEnabled()));
        }

        for (ViewInteraction view : CollectionsKt.arrayListOf(six(), seven(), eight(), nine())) {
          view.check(matches(not(isEnabled())));
        }
        break;
      case NONE:
        for (ViewInteraction view :
            CollectionsKt.arrayListOf(
                zero(), one(), two(), three(), four(), five(), six(), seven(), eight(), nine())) {
          view.check(matches(not(isEnabled())));
        }
        break;
      default:
    }

    switch (quickMinutes) {
      case AVAILABLE:
        zeros().check(matches(isEnabled()));
        half().check(matches(isEnabled()));
        break;
      case NOT_AVAILABLE:
        zeros().check(matches(not(isEnabled())));
        half().check(matches(not(isEnabled())));
        break;
      default:
    }

    switch (completion) {
      case CAN_FINISH:
        ok().check(matches(isEnabled()));
        break;
      case NOT_FINISHED:
        ok().check(matches(not(isEnabled())));
        break;
      default:
    }
  }

  private ViewInteraction one() {
    return onView(allOf(withId(R.id.key_left), withText("1")));
  }

  private ViewInteraction two() {
    return onView(allOf(withId(R.id.key_middle), withText("2")));
  }

  private ViewInteraction three() {
    return onView(allOf(withId(R.id.key_right), withText("3")));
  }

  private ViewInteraction four() {
    return onView(allOf(withId(R.id.key_left), withText("4")));
  }

  private ViewInteraction five() {
    return onView(allOf(withId(R.id.key_middle), withText("5")));
  }

  private ViewInteraction six() {
    return onView(allOf(withId(R.id.key_right), withText("6")));
  }

  private ViewInteraction seven() {
    return onView(allOf(withId(R.id.key_left), withText("7")));
  }

  private ViewInteraction eight() {
    return onView(allOf(withId(R.id.key_middle), withText("8")));
  }

  private ViewInteraction nine() {
    return onView(allOf(withId(R.id.key_right), withText("9")));
  }

  private ViewInteraction zero() {
    return onView(allOf(withId(R.id.key_middle), withText("0")));
  }

  private ViewInteraction ok() {
    return onView(withText("OK"));
  }

  private ViewInteraction zeros() {
    return onView(withText(":00"));
  }

  private ViewInteraction half() {
    return onView(withText(":30"));
  }
}
