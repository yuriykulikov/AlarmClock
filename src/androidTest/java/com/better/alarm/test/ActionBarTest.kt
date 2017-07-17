package com.better.alarm.test

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.test.suitebuilder.annotation.LargeTest
import com.better.alarm.logger.Logger
import com.better.alarm.persistance.AlarmDatabaseHelper
import com.better.alarm.presenter.AlarmsListActivity
import com.robotium.solo.Solo
import junit.framework.Assert
import org.junit.*
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class ActionBarTest {
    var listActivity = ActivityTestRule(
            AlarmsListActivity::class.java, false, /* autostart*/ true)

    @JvmField
    @Rule
    var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity)

    private lateinit var solo: Solo

    companion object {
        @JvmStatic
        @BeforeClass
        @AfterClass
        fun dropDatabase() {
            val context = InstrumentationRegistry.getTargetContext()
            val dbHelper = AlarmDatabaseHelper(context, Logger.create())
            val db = dbHelper.writableDatabase
            db.execSQL("DROP TABLE IF EXISTS alarms")
            dbHelper.onCreate(db)
            db.close()
            println("Dropped database")
        }
    }

    @Before
    fun setup() {
        solo = Solo(InstrumentationRegistry.getInstrumentation(), listActivity.activity)
    }

    @Test
    fun testBugreportButton() {
        solo.sendKey(Solo.MENU)
        solo.clickOnText("bugreport")
        Assert.assertTrue(solo.searchText("Describe"))
        solo.clickOnButton("Cancel")
    }

    @Test
    fun rateTheApp() {
        solo.sendKey(Solo.MENU)
        solo.clickOnText("Rate the app")
        Assert.assertTrue(solo.searchText("Would you like to proceed?"))
        solo.clickOnButton("Cancel")
    }

    @Test
    fun dashClock() {
        solo.sendKey(Solo.MENU)
        solo.clickOnText("Extensions")
        solo.clickOnText("DashClock")
        Assert.assertTrue(solo.searchText("transferred"))
        solo.clickOnButton("Cancel")
    }

    @Test
    fun mp3Cutter() {
        solo.sendKey(Solo.MENU)
        solo.clickOnText("Extensions")
        solo.clickOnText("MP3")
        Assert.assertTrue(solo.searchText("transferred"))
        solo.clickOnButton("Cancel")
    }
}