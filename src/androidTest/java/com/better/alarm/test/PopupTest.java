package com.better.alarm.test;

import android.content.Intent;
import android.provider.AlarmClock;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.better.alarm.alert.AlarmAlert;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.CalendarType;
import com.better.alarm.presenter.AlarmsListActivity;
import com.better.alarm.presenter.HandleSetAlarm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Locale;

import cortado.Cortado;

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static com.better.alarm.test.ListAsserts.assertThatList;

/**
 * Created by Yuriy on 12.07.2017.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PopupTest extends BaseTest {
    public ActivityTestRule<AlarmAlert> alertActivity = new ActivityTestRule<AlarmAlert>(
            AlarmAlert.class, false, /* autostart*/ false);
    public ActivityTestRule<AlarmsListActivity> listActivity = new ActivityTestRule<AlarmsListActivity>(
            AlarmsListActivity.class, false, /* autostart*/ true);

    @Rule
    public TestRule chain = RuleChain
            .outerRule(new ForceLocaleRule(Locale.US))
            .around(listActivity)
            .around(alertActivity);

    @Test
    public void popDialog() {
        Intent createAlarm = new Intent();
        createAlarm.setClass(listActivity.getActivity(), HandleSetAlarm.class);
        createAlarm.setAction(ACTION_SET_ALARM);
        createAlarm.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        createAlarm.putExtra(AlarmClock.EXTRA_HOUR, 0);
        createAlarm.putExtra(AlarmClock.EXTRA_MINUTES, 0);
        createAlarm.putExtra(AlarmClock.EXTRA_MESSAGE, "From outside");

        listActivity.getActivity().startActivity(createAlarm);

        ListAsserts.<AlarmValue>assertThatList(android.R.id.list)
                .filter(enabled())
                .items()
                .hasSize(1);

        int id = ListAsserts.<AlarmValue>listObservable(android.R.id.list).firstOrError().blockingGet().getId();

        //simulate alarm fired
        Intent intent = new Intent(AlarmSetter.ACTION_FIRED);
        intent.putExtra(AlarmSetter.EXTRA_ID, id);
        intent.putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.NORMAL.name());

        listActivity.getActivity().sendBroadcast(intent);

        sleep();


        Intent startIntent = new Intent();
        startIntent.putExtra(Intents.EXTRA_ID, id);
        alertActivity.launchActivity(startIntent);

        sleep();

        Cortado.onView().withText("Dismiss").perform().longClick();

        sleep();

        deleteAlarm(0);
        assertThatList(android.R.id.list).items().hasSize(2);
    }
}
