package com.better.alarm.test;

import android.content.Intent;
import android.support.test.espresso.FailureHandler;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.better.alarm.R;
import com.better.alarm.configuration.AlarmApplication;
import com.better.alarm.interfaces.PresentationToModelIntents;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.CalendarType;
import com.better.alarm.presenter.AlarmsListActivity;
import com.better.alarm.services.AlarmsService;
import com.google.common.base.Optional;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Locale;

import cortado.Cortado;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ListTest extends BaseTest {
    public ActivityTestRule<AlarmsListActivity> listActivity = new ActivityTestRule<AlarmsListActivity>(
            AlarmsListActivity.class, false, /* autostart*/ true);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(listActivity);

    @Test
    public void newAlarmShouldBeDisabledIfNotEdited() throws Exception {
        sleep();
        onView(withId(R.id.fab)).perform(click());
        sleep();
        onView(withText("Cancel")).perform(click());
        sleep();
        Cortado.onView().withText("OK").perform().click();
        sleep();

        assertThatList().items().hasSize(3);

        ListAsserts.<AlarmValue>assertThatList(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .isEmpty();

        deleteAlarm(0);
        assertThatList().items().hasSize(2);
    }

    @Test
    public void newAlarmShouldBeEnabledIfEdited12() throws Exception {
        AlarmApplication.is24hoursFormatOverride = Optional.of(false);
        newAlarmShouldBeEnabledIfEdited();
    }

    @Test
    public void newAlarmShouldBeEnabledIfEdited24() throws Exception {
        AlarmApplication.is24hoursFormatOverride = Optional.of(true);
        newAlarmShouldBeEnabledIfEdited();
    }

    private void newAlarmShouldBeEnabledIfEdited() throws Exception {
        onView(withId(R.id.fab)).perform(click());
        sleep();
        Cortado.onView().withText("1").perform().click();
        Cortado.onView().withText("2").perform().click();
        Cortado.onView().withText("3").perform().click();
        Cortado.onView().withText("5").perform().click();

        onView(withText("AM"))
                .withFailureHandler(new FailureHandler() {
                    @Override
                    public void handle(Throwable error, Matcher<View> viewMatcher) {
                    }
                })
                .perform(click());

        sleep();
        onView(withText("OK")).perform(click());
        Cortado.onView().withText("OK").perform().click();
        sleep();

        assertThatList().items().hasSize(3);

        ListAsserts.<AlarmValue>assertThatList(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .hasSize(1);

        deleteAlarm(0);
        assertThatList().items().hasSize(2);
    }

    @Test
    public void testDeleteNewAlarmInDetailsActivity() throws Exception {
        onView(withId(R.id.fab)).perform(click());
        sleep();
        onView(withText("Cancel")).perform(click());
        Cortado.onView().withText("OK").perform().click();
        sleep();

        assertThatList().items().hasSize(3);
        sleep();

        onData(anything()).atPosition(0).onChildView(withId(R.id.details_button_container)).perform(click());
        sleep();

        Cortado.onView().withId(R.id.set_alarm_menu_delete_alarm).perform().click();
        sleep();

        Cortado.onView().withText("OK").perform().click();
        sleep();

        assertThatList().items().hasSize(2);
    }

    @Test
    public void newAlarmShouldBeDisabledAfterDismiss() throws Exception {
        onView(withId(R.id.fab)).perform(click());
        sleep();
        Cortado.onView().withText("1").perform().click();
        Cortado.onView().withText("2").perform().click();
        Cortado.onView().withText("3").perform().click();
        Cortado.onView().withText("5").perform().click();

        onView(withText("AM"))
                .withFailureHandler(new FailureHandler() {
                    @Override
                    public void handle(Throwable error, Matcher<View> viewMatcher) {
                        //ignore fails - only use if View is found
                    }
                })
                .perform(click());

        sleep();
        onView(withText("OK")).perform(click());
        Cortado.onView().withText("OK").perform().click();
        sleep();

        ListAsserts.<AlarmValue>assertThatList(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .hasSize(1);

        int id = ListAsserts.<AlarmValue>listObservable(R.id.list_fragment_list).firstOrError().blockingGet().getId();

        //simulate alarm fired
        Intent intent = new Intent(AlarmSetter.ACTION_FIRED);
        intent.putExtra(AlarmSetter.EXTRA_ID, id);
        intent.putExtra(AlarmSetter.EXTRA_TYPE, CalendarType.NORMAL.name());
        listActivity.getActivity().sendBroadcast(intent);

        sleep();

        //simulate dismiss from the notification bar
        Intent dismiss = new Intent(PresentationToModelIntents.ACTION_REQUEST_DISMISS);
        dismiss.putExtra(AlarmSetter.EXTRA_ID, id);
        dismiss.setClass(listActivity.getActivity(), AlarmsService.class);
        listActivity.getActivity().startService(dismiss);

        sleep();

        //alarm must be disabled because there is no repeating
        ListAsserts.<AlarmValue>assertThatList(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .isEmpty();

        deleteAlarm(0);
        assertThatList().items().hasSize(2);
    }

    @Test
    public void editAlarmALot() throws Exception {
        onView(withId(R.id.fab)).perform(click());
        sleep();
        assertTimerView("--:--");
        Cortado.onView().withText("1").perform().click();
        assertTimerView("--:-1");
        Cortado.onView().withText("2").perform().click();
        assertTimerView("--:12");
        Cortado.onView().withText("3").perform().click();
        assertTimerView("-1:23");
        Cortado.onView().withText("5").perform().click();
        assertTimerView("12:35");
        Cortado.onView().withId(R.id.delete).perform().click();
        assertTimerView("-1:23");
        Cortado.onView().withId(R.id.delete).perform().click();
        assertTimerView("--:12");
        Cortado.onView().withId(R.id.delete).perform().longClick();
        assertTimerView("--:--");
        sleep();
        onView(withText("Cancel")).perform(click());
        sleep();
        onView(withText("Cancel")).perform(click());
        assertThatList().items().hasSize(2);
    }

    @Test
    public void editRepeat() throws Exception {
        sleep();
        onView(withId(R.id.fab)).perform(click());
        sleep();
        onView(withText("Cancel")).perform(click());
        sleep();

        onView(withText("Repeat")).perform(click());
        onView(withText("Monday")).perform(click());
        onView(withText("Tuesday")).perform(click());
        onView(withText("Wednesday")).perform(click());
        onView(withText("Thursday")).perform(click());
        onView(withText("Friday")).perform(click());
        onView(withText("Saturday")).perform(click());
        onView(withText("Sunday")).perform(click());
        onView(withText("OK")).perform(click());

        Cortado.onView().withText("OK").perform().click();
        sleep();
        sleep();
        
        assertThatList().items().hasSize(3);

        ListAsserts.<AlarmValue>assertThatList(R.id.list_fragment_list)
                .filter(enabled())
                .items()
                .hasSize(1);

        ListAsserts.<AlarmValue>assertThatList(R.id.list_fragment_list)
                .filter(new Predicate<AlarmValue>() {
                    @Override
                    public boolean test(@NonNull AlarmValue alarmValue) throws Exception {
                        return alarmValue.getDaysOfWeek().isRepeatSet();
                    }
                })
                .items()
                .hasSize(3);

        onView(withText("Every day")).check(matches(isDisplayed()));

        deleteAlarm(0);
        assertThatList().items().hasSize(2);
    }

}