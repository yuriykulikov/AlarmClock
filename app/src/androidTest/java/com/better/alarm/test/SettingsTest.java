package com.better.alarm.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.better.alarm.R;
import com.better.alarm.presenter.SettingsActivity;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/** Created by Yuriy on 11.07.2017. */
@RunWith(AndroidJUnit4.class)
public class SettingsTest extends BaseTest {
  public ActivityScenarioRule<SettingsActivity> settings =
      new ActivityScenarioRule<>(SettingsActivity.class);

  @Rule
  public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(settings);

  @Test
  public void controlVolume() {
    sleep();
    onView(withId(R.id.seekbar_dialog_seekbar_master_volume))
        .perform(
            new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER, 0, 0));

    sleep();
    onView(withId(R.id.seekbar_dialog_seekbar_master_volume))
        .perform(
            new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER, 0, 0));

    sleep();
    onView(withId(R.id.seekbar_dialog_seekbar_prealarm_volume))
        .perform(
            new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER, 0, 0));

    sleep();
    onView(withId(R.id.seekbar_dialog_seekbar_prealarm_volume))
        .perform(
            new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER, 0, 0));
  }

  @Test
  public void changeTheme() {
    sleep();
    onView(withText("Interface theme")).perform(scrollTo()).perform(click());

    sleep();
    onView(withText("Light")).perform(click());
  }

  @Test
  public void changeThemeDark() {
    sleep();
    onView(withText("Interface theme")).perform(scrollTo()).perform(click());

    sleep();
    onView(withText("Neon")).perform(click());
  }
}
