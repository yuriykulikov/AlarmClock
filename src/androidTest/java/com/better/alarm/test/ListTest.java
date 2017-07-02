package com.better.alarm.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.FailureHandler;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.better.alarm.R;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.persistance.AlarmDatabaseHelper;
import com.better.alarm.presenter.AlarmsListActivity;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.better.alarm.test.ListAsserts.assertThatList;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ListTest {
    public ActivityTestRule<AlarmsListActivity> listActivity = new ActivityTestRule<AlarmsListActivity>(
            AlarmsListActivity.class, false, /* autostart*/ true);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(listActivity);

    private static final boolean DBG = false;

    private static void sleep(int howLong) {
        if (DBG) {
            try {
                Thread.sleep(howLong);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sleep() {
        sleep(1000);
    }

    private void deleteAlarm(int position) {
        onData(anything()).atPosition(position).perform(longClick());
        sleep(200);
        Cortado.onView().withText("Delete alarm").perform().click();
        sleep(200);
        Cortado.onView().withText("OK").perform().click();
        sleep(200);
    }

    @BeforeClass
    @AfterClass
    public static void dropDatabase() {
        final Context context = InstrumentationRegistry.getTargetContext();
        AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(context, Logger.create());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS alarms");
        dbHelper.onCreate(db);
        db.close();
        System.out.println("Dropped database");
    }

    @Before
    public void closeSoftKeyBoard() {
        closeSoftKeyboard();
    }

    @After
    public void tearDown() throws InterruptedException {
        //TODO actually mock database access in these tests
        Thread.sleep(1000);
    }

    @Test
    public void newAlarm_shouldBeDisabled_ifNotEdited() throws Exception {
        sleep();
        onView(withId(R.id.fab)).perform(click());
        sleep();
        onView(withText("Cancel")).perform(click());
        sleep();
        Cortado.onView().withText("OK").perform().click();
        sleep();

        assertThatList(android.R.id.list).items().hasSize(3);

        ListAsserts.<AlarmValue>assertThatList(android.R.id.list)
                .filter(new Predicate<AlarmValue>() {
                    @Override
                    public boolean test(@NonNull AlarmValue alarmValue) throws Exception {
                        return alarmValue.isEnabled();
                    }
                })
                .items()
                .isEmpty();

        deleteAlarm(0);
        assertThatList(android.R.id.list).items().hasSize(2);
    }

    @Test
    public void newAlarm_shouldBeEnabled_ifEdited() throws Exception {
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

        assertThatList(android.R.id.list).items().hasSize(3);

        ListAsserts.<AlarmValue>assertThatList(android.R.id.list)
                .filter(new Predicate<AlarmValue>() {
                    @Override
                    public boolean test(@NonNull AlarmValue alarmValue) throws Exception {
                        return alarmValue.isEnabled();
                    }
                })
                .items()
                .hasSize(1);

        deleteAlarm(0);
        assertThatList(android.R.id.list).items().hasSize(2);
    }

    @Test
    public void testDeleteNewAlarmInDetailsActivity() throws Exception {
        onView(withId(R.id.fab)).perform(click());
        sleep();
        onView(withText("Cancel")).perform(click());
        Cortado.onView().withText("OK").perform().click();
        sleep();

        assertThatList(android.R.id.list).items().hasSize(3);
        sleep();

        onData(anything()).atPosition(0).onChildView(withId(R.id.details_button_container)).perform(click());
        sleep();

        Cortado.onView().withId(R.id.set_alarm_menu_delete_alarm).perform().click();
        sleep();

        Cortado.onView().withText("OK").perform().click();
        sleep();

        assertThatList(android.R.id.list).items().hasSize(2);
    }
}