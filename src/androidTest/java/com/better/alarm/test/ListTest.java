package com.better.alarm.test;

import junit.framework.Assert;
import android.test.ActivityInstrumentationTestCase2;

import com.better.alarm.presenter.AlarmsListActivity;
import com.robotium.solo.Solo;

public class ListTest extends ActivityInstrumentationTestCase2<AlarmsListActivity> {

    private Solo solo;

    public ListTest() {
	super(AlarmsListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
	solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testBugreportButton() throws Exception {
	solo.clickOnActionBarItem(com.better.alarm.R.id.menu_item_add_alarm);
	solo.clickOnButton("Cancel");
	solo.clickOnButton("OK");
	Assert.assertFalse(solo.isCheckBoxChecked(0));
    }

    public void testAddNewAlarm() throws Exception {
	solo.sendKey(Solo.MENU);
	solo.clickOnText("Send a bugreport");
	Assert.assertTrue(solo.searchText("Describe"));
	solo.clickOnButton("Cancel");
    }

    public void testDeleteNewAlarm() throws Exception {
	solo.clickLongInList(0, 3);
	Assert.assertTrue(solo.searchText("Describe"));
	solo.clickOnButton("Cancel");
    }

    @Override
    public void tearDown() throws Exception {
	solo.finishOpenedActivities();
    }
}