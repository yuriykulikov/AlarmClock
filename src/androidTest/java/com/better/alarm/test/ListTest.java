package com.better.alarm.test;

import junit.framework.Assert;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;

import com.better.alarm.R;
import com.better.alarm.presenter.AlarmsListActivity;
import com.robotium.solo.Solo;

import java.util.Locale;

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

    public void testAddNewAlarm() throws Exception {
        //solo.clickOnImageButton(0);//fab
        //solo.clickOnButton("Cancel");
        //Assert.assertFalse(solo.isCheckBoxChecked(0));
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