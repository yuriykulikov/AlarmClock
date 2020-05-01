package com.better.alarm.test

import androidx.test.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
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
class ActionBarTest {
    var listActivity = ActivityTestRule(
            AlarmsListActivity::class.java, false, /* autostart*/ true)

    @JvmField
    @Rule
    public var chain: TestRule = RuleChain.outerRule(ForceLocaleRule(Locale.US)).around(listActivity)

    private var solo: Solo? = null;

    companion object {
        @JvmStatic
        @BeforeClass
        @AfterClass
        public fun dropDatabase() {
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
    public fun setup() {
        solo = Solo(InstrumentationRegistry.getInstrumentation(), listActivity.activity)
    }

    @Test
    fun testBugreportButton() {
        val solo = solo!!
        solo.sendKey(Solo.MENU)
        solo.clickOnText("bugreport")
        Assert.assertTrue(solo.searchText("Common issues"))
        solo.clickOnButton("Cancel")
    }

    @Test
    fun testAboutButton() {
        val solo = solo!!
        solo.sendKey(Solo.MENU)
        solo.clickOnText("About")
        Assert.assertTrue(solo.searchText("version"))
    }

    @Test
    fun sayThanks() {
        val solo = solo!!
        solo.sendKey(Solo.MENU)
        solo.clickOnText(solo.getString(com.better.alarm.R.string.dialog_say_thanks_title))
        Assert.assertTrue(solo.searchText("really like the app"))
    }
}