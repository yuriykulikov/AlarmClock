package com.better.alarm.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.better.alarm.R;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.persistance.AlarmDatabaseHelper;
import com.better.alarm.presenter.AlarmsListActivity;

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
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;

import static android.app.PendingIntent.getActivity;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ListTest {
    public ActivityTestRule<AlarmsListActivity> listActivity = new ActivityTestRule<AlarmsListActivity>(
            AlarmsListActivity.class, false, /* autostart*/ true);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(listActivity);

    private static final boolean DBG = true;

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

    private <T> Observable<T> listItems(final int id) {
        return Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(final @NonNull ObservableEmitter<T> e) throws Exception {
                onView(withId(id)).check(new ViewAssertion() {
                    @Override
                    public void check(View view, NoMatchingViewException noViewFoundException) {
                        ListAdapter adapter = ((ListView) view).getAdapter();

                        for (int i = 0; i < adapter.getCount(); i++) {
                            e.onNext((T) adapter.getItem(i));
                        }
                        e.onComplete();
                    }
                });
            }
        });
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
    public void closeSoftKeyBoard(){
        closeSoftKeyboard();
    }

    @After
    public void tearDown() throws InterruptedException {
        //TODO actually mock database access in these tests
        Thread.sleep(1000);
    }

    @Test
    public void testBugreportButton() throws Exception {
        sleep();

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        sleep();

        onView(withText("Send a bugreport")).perform(scrollTo()).perform(click());
        sleep();

        onView(withText("Cancel")).perform(click());
        sleep();
    }

    @Test
    public void testBugreportButton2() throws Exception {
        sleep();

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        sleep();

        onView(withText("Send a bugreport"))
                .inRoot(withDecorView(not(is(listActivity.getActivity().getWindow().getDecorView()))))
                .perform(click());
        sleep();

        onView(withText("Cancel"))
                .inRoot(withDecorView(not(is(listActivity.getActivity().getWindow().getDecorView()))))
                .perform(click());
        sleep();
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

        listItems(android.R.id.list).test().assertValueCount(3);

        this.<AlarmValue>listItems(android.R.id.list)
                .filter(new Predicate<AlarmValue>() {
                    @Override
                    public boolean test(@NonNull AlarmValue alarmValue) throws Exception {
                        return alarmValue.isEnabled();
                    }
                })
                .test()
                .assertValueCount(0);

        deleteAlarm(0);
        listItems(android.R.id.list).test().assertValueCount(2);
    }

    @Test
    public void newAlarm_shouldBeEnabled_ifEdited() throws Exception {
        onView(withId(R.id.fab)).perform(click());
        sleep();
        Cortado.onView().withText("1").perform().click();
        Cortado.onView().withText("2").perform().click();
        Cortado.onView().withText("3").perform().click();
        Cortado.onView().withText("5").perform().click();
        Cortado.onView().withText("AM").perform().click();
        sleep();
        onView(withText("OK")).perform(click());
        Cortado.onView().withText("OK").perform().click();
        sleep();

        listItems(android.R.id.list)
                .test()
                .assertValueCount(3);

        this.<AlarmValue>listItems(android.R.id.list)
                .filter(new Predicate<AlarmValue>() {
                    @Override
                    public boolean test(@NonNull AlarmValue alarmValue) throws Exception {
                        return alarmValue.isEnabled();
                    }
                })
                .test()
                .assertValueCount(1);

        deleteAlarm(0);
        listItems(android.R.id.list).test().assertValueCount(2);
    }

    @Test
    public void testDeleteNewAlarmInDetailsActivity() throws Exception {
        onView(withId(R.id.fab)).perform(click());
        sleep();
        onView(withText("Cancel")).perform(click());
        Cortado.onView().withText("OK").perform().click();
        sleep();

        listItems(android.R.id.list).test().assertValueCount(3);
        sleep();

        onData(anything()).atPosition(0).onChildView(withId(R.id.details_button_container)).perform(click());
        sleep();

        Cortado.onView().withId(R.id.set_alarm_menu_delete_alarm).perform().click();
        sleep();

        Cortado.onView().withText("OK").perform().click();
        sleep();

        listItems(android.R.id.list).test().assertValueCount(2);
    }
}