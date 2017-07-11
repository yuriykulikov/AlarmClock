package com.better.alarm.test;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.SeekBar;

import com.better.alarm.*;
import com.better.alarm.R;
import com.better.alarm.presenter.AlarmsListActivity;
import com.better.alarm.presenter.SettingsActivity;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import cortado.Cortado;

/**
 * Created by Yuriy on 11.07.2017.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SettingsTest extends BaseTest {
    public ActivityTestRule<SettingsActivity> settings = new ActivityTestRule<SettingsActivity>(
            SettingsActivity.class, false, /* autostart*/ true);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(settings);

    @Test
    public void controlVolume() {
        sleep();
        Cortado.onView().withText("Alarm volume").perform().click();
        sleep();
        Cortado.onView()
                .withId(R.id.seekbar_dialog_seekbar_alarm_volume)
                .perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER));

        sleep();
        Cortado.onView()
                .withId(R.id.seekbar_dialog_seekbar_alarm_volume)
                .perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER));
    }
}
