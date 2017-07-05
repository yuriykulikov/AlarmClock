package com.better.alarm.test;

import android.app.ListActivity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.better.alarm.*;
import com.better.alarm.R;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.presenter.AlarmsListActivity;
import com.robotium.solo.Condition;
import com.robotium.solo.Solo;

import junit.framework.Assert;

import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;

public class ListTest extends ActivityInstrumentationTestCase2<AlarmsListActivity> {

    private Solo solo;

    public ListTest() {
        super(AlarmsListActivity.class);
    }

    private void setLocale(String language, String country) {
        Locale locale = new Locale(language, country);
        // here we update locale for date formatters
        Locale.setDefault(locale);
        // here we update locale for app resources
        Resources res = getActivity().getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        setLocale("en", "EN");
    }

    private <T> Observable<T> listItems(final int id) {
        return Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(final @NonNull ObservableEmitter<T> e) throws Exception {
                ListAdapter adapter = ((ListView) solo.getView(id)).getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    e.onNext((T) adapter.getItem(i));
                }
                e.onComplete();
            }
        });
    }

    public void testAddNewAlarm() throws Exception {
        solo.clickOnView(solo.getView(R.id.fab));
        solo.clickOnButton("Cancel");
        solo.clickOnButton("OK");

        solo.waitForActivity(AlarmsListActivity.class);
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return listItems(android.R.id.list).test().valueCount() == 3;
            }
        }, 15000);

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

        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return listItems(android.R.id.list).test().valueCount() == 2;
            }
        }, 15000);

        listItems(android.R.id.list).test().assertValueCount(2);
    }

    private void deleteAlarm(int position) {
        solo.clickLongInList(position);
        solo.clickOnText("Delete alarm");
        solo.clickOnButton("OK");
    }

    public void testBugreportButton() throws Exception {
        solo.sendKey(Solo.MENU);
        solo.clickOnText("bugreport");
        Assert.assertTrue(solo.searchText("Describe"));
        solo.clickOnButton("Cancel");
    }

    public void testDeleteNewAlarm() throws Exception {
        //solo.clickLongInList(0, 3);
        //Assert.assertTrue(solo.searchText("Describe"));
        //solo.clickOnButton("Delete");
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }
}