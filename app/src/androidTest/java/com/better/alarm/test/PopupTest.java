package com.better.alarm.test;

import android.content.Intent;
import android.provider.AlarmClock;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.better.alarm.R;
import com.better.alarm.alert.AlarmAlertFullScreen;
import com.better.alarm.configuration.ContainerKt;
import com.better.alarm.configuration.InjectKt;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.AlarmsReceiver;
import com.better.alarm.model.CalendarType;
import com.better.alarm.presenter.AlarmsListActivity;
import com.better.alarm.presenter.HandleSetAlarm;
import com.better.alarm.presenter.TransparentActivity;
import com.better.alarm.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Locale;

import cortado.Cortado;

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Yuriy on 12.07.2017.
 */
@RunWith(AndroidJUnit4.class)
public class PopupTest extends BaseTest {
    public ActivityTestRule<AlarmAlertFullScreen> alertActivity = new ActivityTestRule<AlarmAlertFullScreen>(
            AlarmAlertFullScreen.class, false, /* autostart*/ false);
    public ActivityTestRule<AlarmsListActivity> listActivity = new ActivityTestRule<AlarmsListActivity>(
            AlarmsListActivity.class, false, /* autostart*/ true);
    public ActivityTestRule<TransparentActivity> transparentActivity = new ActivityTestRule<TransparentActivity>(
            TransparentActivity.class, false, /* autostart*/ false);

    @Rule
    public TestRule chain = RuleChain
            .outerRule(new ForceLocaleRule(Locale.US))
            .around(listActivity)
            .around(alertActivity)
            .around(transparentActivity);

    @Before
    public void set24HourMode() {
        ContainerKt.overrideIs24hoursFormatOverride(true);
    }

    public int createAlarmAndFire() {
        Intent createAlarm = new Intent();
        createAlarm.setClass(listActivity.getActivity(), HandleSetAlarm.class);
        createAlarm.setAction(ACTION_SET_ALARM);
        createAlarm.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        createAlarm.putExtra(AlarmClock.EXTRA_HOUR, 0);
        createAlarm.putExtra(AlarmClock.EXTRA_MINUTES, 0);
        createAlarm.putExtra(AlarmClock.EXTRA_MESSAGE, "From outside");

        listActivity.getActivity().startActivity(createAlarm);

        sleep();

        assertThatList()
                .filter(enabled())
                .items()
                .hasSize(1);

        int id = ListAsserts.<AlarmValue>listObservable(R.id.list_fragment_list).firstOrError().blockingGet().getId();

        //simulate alarm fired
        Intent intent = new Intent(AlarmSetter.ACTION_FIRED);
        intent.setClass(listActivity.getActivity(), AlarmsReceiver.class);
        intent.putExtra(AlarmSetter.EXTRA_ID, id);
        intent.putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.NORMAL.name());

        listActivity.getActivity().sendBroadcast(intent);

        sleep();
        return id;
    }

    @Test
    public void dismissViaAlarmAlert() {
        int id = createAlarmAndFire();

        Intent startIntent = new Intent();
        startIntent.putExtra(Intents.EXTRA_ID, id);
        alertActivity.launchActivity(startIntent);

        sleep();

        Cortado.onView().withText("Dismiss").perform().longClick();

        deleteAlarm();
    }

    private void afterLongClickSnoozeAlarmCheckAndDelete() {
        assertTimerView("--:--");
        Cortado.onView().withText("2").perform().click();
        assertTimerView("--:-2");
        Cortado.onView().withText("3").perform().click();
        assertTimerView("--:23");
        Cortado.onView().withText("5").perform().click();
        assertTimerView("-2:35");
        Cortado.onView().withText("9").perform().click();
        assertTimerView("23:59");
        sleep();

        Cortado.onView().withText("OK").perform().click();

        sleep();

        Optional<Store.Next> next = InjectKt.globalInject(Store.class).getValue()
                .next()
                .blockingFirst();


        assertThat(next.isPresent()).isTrue();

        Calendar nextTime = Calendar.getInstance();
        nextTime.setTimeInMillis(next.get().nextNonPrealarmTime());

        assertThat((int) nextTime.get(Calendar.HOUR_OF_DAY)).isEqualTo(23);
        assertThat((int) nextTime.get(Calendar.MINUTE)).isEqualTo(59);
        assertThat(next.get().alarm().isEnabled()).isTrue();

        //disable the snoozed alarm
        Cortado.onView().withId(R.id.list_row_on_off_switch).and().isChecked().perform().click();

        sleep();

        assertThatList()
                .filter(enabled())
                .items()
                .isEmpty();

        deleteAlarm();
    }

    @Ignore("Flakey")
    @Test
    public void snoozeViaClick() {
        int id = createAlarmAndFire();

        Intent startIntent = new Intent();
        startIntent.putExtra(Intents.EXTRA_ID, id);
        alertActivity.launchActivity(startIntent);

        sleep();
        Cortado.onView().withText("Snooze").perform().click();

        deleteAlarm();
    }

    @Ignore
    @Test
    public void snoozeViaNotificationPicker() {
        int id = createAlarmAndFire();

        Intent startIntent = new Intent();
        startIntent.putExtra(Intents.EXTRA_ID, id);
        transparentActivity.launchActivity(startIntent);

        deleteAlarm();
    }

    @Test
    public void letAlarmExpireAndDismissIt() {
        int id = createAlarmAndFire();

        Intent startIntent = new Intent();
        startIntent.putExtra(Intents.EXTRA_ID, id);
        alertActivity.launchActivity(startIntent);

        //simulate timed out
        Intent intent = new Intent(AlarmSetter.ACTION_FIRED);
        intent.setClass(alertActivity.getActivity(), AlarmsReceiver.class);
        intent.putExtra(AlarmSetter.EXTRA_ID, id);
        intent.putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.AUTOSILENCE.name());

        listActivity.getActivity().sendBroadcast(intent);
        sleep();

        //Popup must be closed in order for this to work
        deleteAlarm();
    }


    private void deleteAlarm() {
        sleep();
        deleteAlarm(0);
        assertThatList().items().hasSize(2);
    }
}
