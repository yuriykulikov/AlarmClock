package com.better.alarm.test;

import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.better.alarm.R;
import com.better.alarm.presenter.SettingsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Locale;

import cortado.Cortado;

import static android.support.test.espresso.action.ViewActions.click;

/**
 * Created by Yuriy on 11.07.2017.
 */
@RunWith(AndroidJUnit4.class)
public class SettingsTest extends BaseTest {
    public ActivityTestRule<SettingsActivity> settings = new ActivityTestRule<SettingsActivity>(
            SettingsActivity.class, false, /* autostart*/ true);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(settings);

    @Test
    public void controlVolume() {
        sleep();
        Cortado.onView()
                .withId(R.id.seekbar_dialog_seekbar_master_volume)
                .perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER));

        sleep();
        Cortado.onView()
                .withId(R.id.seekbar_dialog_seekbar_master_volume)
                .perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER));

        sleep();
        Cortado.onView()
                .withId(R.id.seekbar_dialog_seekbar_prealarm_volume)
                .perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER));

        sleep();
        Cortado.onView()
                .withId(R.id.seekbar_dialog_seekbar_prealarm_volume)
                .perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER));
    }

    @Test
    public void changeTheme() {
        sleep();
        Cortado.onView().withText("Interface theme").perform(click());

        sleep();
        Cortado.onView().withText("Light").perform(click());
    }

    @Test
    public void changeThemeDark() {
        sleep();
        Cortado.onView().withText("Interface theme").perform(click());

        sleep();
        Cortado.onView().withText("Dark").perform(click());
    }
}
