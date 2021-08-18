package com.better.alarm.test;

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.better.alarm.R;
import com.better.alarm.alert.AlarmAlertFullScreen;
import com.better.alarm.configuration.ContainerKt;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.AlarmsReceiver;
import com.better.alarm.model.CalendarType;
import com.better.alarm.presenter.AlarmsListActivity;
import com.better.alarm.presenter.HandleSetAlarm;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Locale;

/**
 * Created by Yuriy on 12.07.2017.
 */
@RunWith(AndroidJUnit4.class)
public class PopupTest extends BaseTest {
    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    public ActivityScenarioRule<AlarmsListActivity> listActivity = new ActivityScenarioRule<>(AlarmsListActivity.class);

    @Rule
    public TestRule chain = RuleChain
            .outerRule(new ForceLocaleRule(Locale.US))
            .around(listActivity);

    @Before
    public void set24HourMode() {
        ContainerKt.overrideIs24hoursFormatOverride(true);
    }

    public int createAlarmAndFire() {
        listActivity.getScenario().onActivity(activity -> {
            Intent createAlarm = new Intent();
            createAlarm.setClass(activity, HandleSetAlarm.class);
            createAlarm.setAction(ACTION_SET_ALARM);
            createAlarm.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            createAlarm.putExtra(AlarmClock.EXTRA_HOUR, 0);
            createAlarm.putExtra(AlarmClock.EXTRA_MINUTES, 0);
            createAlarm.putExtra(AlarmClock.EXTRA_MESSAGE, "From outside");
            activity.startActivity(createAlarm);
        });

        sleep();

        listActivity.getScenario().recreate();

        assertThatList()
                .filter(enabled())
                .items()
                .hasSize(1);

        int id = ListAsserts.<AlarmValue>listObservable(R.id.list_fragment_list).firstOrError().blockingGet().getId();

        //simulate alarm fired
        Intent intent = new Intent(AlarmSetter.ACTION_FIRED);
        intent.setClass(getContext(), AlarmsReceiver.class);
        intent.putExtra(AlarmSetter.EXTRA_ID, id);
        intent.putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.NORMAL.name());

        getContext().sendBroadcast(intent);

        sleep();
        return id;
    }

    @Test
    public void dismissViaAlarmAlert() {
        int id = createAlarmAndFire();

        Intent startIntent = new Intent(getContext(), AlarmAlertFullScreen.class);
        startIntent.putExtra(Intents.EXTRA_ID, id);
        ActivityScenario.launch(startIntent);

        sleep();

        onView(withText("Dismiss")).perform(longClick());

        deleteAlarm();
    }

    @Test
    public void letAlarmExpireAndDismissIt() throws InterruptedException {
        int id = createAlarmAndFire();

        Intent startIntent = new Intent(getContext(), AlarmAlertFullScreen.class);
        startIntent.putExtra(Intents.EXTRA_ID, id);
        ActivityScenario.launch(startIntent);

        //simulate timed out
        Intent intent = new Intent(AlarmSetter.ACTION_FIRED);
        intent.setClass(getContext(), AlarmsReceiver.class);
        intent.putExtra(AlarmSetter.EXTRA_ID, id);
        intent.putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.AUTOSILENCE.name());

        getContext().sendBroadcast(intent);
        sleep();

        //Popup must be closed in order for this to work
        Thread.sleep(1000);
        deleteAlarm();
    }


    private void deleteAlarm() {
        sleep();
        ActivityScenario.launch(AlarmsListActivity.class);
        deleteAlarm(0);
        assertThatList().items().hasSize(2);
    }

    protected static void sleep() {

    }
}
