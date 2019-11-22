package com.better.alarm.test;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.better.alarm.R;
import com.better.alarm.configuration.ContainerKt;
import com.better.alarm.presenter.AlarmsListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Locale;

import kotlin.collections.CollectionsKt;

import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static cortado.Cortado.onView;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class PickerTest extends BaseTest {
    public ActivityTestRule<AlarmsListActivity> listActivity = new ActivityTestRule<AlarmsListActivity>(
            AlarmsListActivity.class, false, /* autostart*/ true);

    @Rule
    public TestRule chain = RuleChain.outerRule(new ForceLocaleRule(Locale.US)).around(listActivity);

    @Before
    public void setUp() {
        ContainerKt.overrideIs24hoursFormatOverride(true);
        onView().withId(R.id.fab).perform().click();
        //sleep();
    }

    @After
    public void tearDown() {
        onView().withText("Cancel").perform().click();
        //sleep();
        onView().withText("Cancel").perform().click();
        assertThatList().items().hasSize(2);
    }

    @Test
    public void testNothing() throws Exception {
        assertTimerView("--:--");
        assertState(Next.ANY, QuickMinutes.NOT_AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test1() throws Exception {
        one().perform().click();
        assertState(Next.ANY, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test2() throws Exception {
        two().perform().click();
        assertState(Next.AFTER_20, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test3() throws Exception {
        three().perform().click();
        assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test4() throws Exception {
        four().perform().click();
        assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test5() throws Exception {
        five().perform().click();
        assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test6() throws Exception {
        six().perform().click();
        assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test7() throws Exception {
        seven().perform().click();
        assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test8() throws Exception {
        eight().perform().click();
        assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test9() throws Exception {
        nine().perform().click();
        assertState(Next.TENS_OF_MINUTES, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    @Test
    public void test0() throws Exception {
        zero().perform().click();
        assertState(Next.ANY, QuickMinutes.AVAILABLE, Completion.NOT_FINISHED);
    }

    enum Next {
        ANY, AFTER_20, TENS_OF_MINUTES, NONE
    }

    enum Completion {
        CAN_FINISH, NOT_FINISHED
    }

    enum QuickMinutes {
        AVAILABLE, NOT_AVAILABLE
    }

    private void assertState(Next next, QuickMinutes quickMinutes, Completion completion) {
        switch (next) {
            case ANY:
                for (cortado.ViewInteraction view : CollectionsKt.arrayListOf(zero(), one(), two(), three(), four(), five(), six(), seven(), eight(), nine())) {
                    view.check().matches(isEnabled());
                }
                break;
            case AFTER_20:
                for (cortado.ViewInteraction view : CollectionsKt.arrayListOf(zero(), one(), two(), three(), four(), five())) {
                    view.check().matches(isEnabled());
                }

                for (cortado.ViewInteraction view : CollectionsKt.arrayListOf(six(), seven(), eight(), nine())) {
                    view.check().matches(not(isEnabled()));
                }
                break;
            case TENS_OF_MINUTES:
                for (cortado.ViewInteraction view : CollectionsKt.arrayListOf(zero(), one(), two(), three(), four(), five())) {
                    view.check().matches(isEnabled());
                }

                for (cortado.ViewInteraction view : CollectionsKt.arrayListOf(six(), seven(), eight(), nine())) {
                    view.check().matches(not(isEnabled()));
                }
                break;
            case NONE:
                for (cortado.ViewInteraction view : CollectionsKt.arrayListOf(zero(), one(), two(), three(), four(), five(), six(), seven(), eight(), nine())) {
                    view.check().matches(not(isEnabled()));
                }
                break;
            default:
        }

        switch (quickMinutes) {
            case AVAILABLE:
                zeros().check().matches(isEnabled());
                half().check().matches(isEnabled());
                break;
            case NOT_AVAILABLE:
                zeros().check().matches(not(isEnabled()));
                half().check().matches(not(isEnabled()));
                break;
            default:
        }

        switch (completion) {
            case CAN_FINISH:
                ok().check().matches(isEnabled());
                break;
            case NOT_FINISHED:
                ok().check().matches(not(isEnabled()));
                break;
            default:
        }
    }

    private cortado.ViewInteraction one() {
        return onView().withId(R.id.key_left).and().withText("1");
    }

    private cortado.ViewInteraction two() {
        return onView().withId(R.id.key_middle).and().withText("2");
    }

    private cortado.ViewInteraction three() {
        return onView().withId(R.id.key_right).and().withText("3");
    }

    private cortado.ViewInteraction four() {
        return onView().withId(R.id.key_left).and().withText("4");
    }

    private cortado.ViewInteraction five() {
        return onView().withId(R.id.key_middle).and().withText("5");
    }

    private cortado.ViewInteraction six() {
        return onView().withId(R.id.key_right).and().withText("6");
    }

    private cortado.ViewInteraction seven() {
        return onView().withId(R.id.key_left).and().withText("7");
    }

    private cortado.ViewInteraction eight() {
        return onView().withId(R.id.key_middle).and().withText("8");
    }

    private cortado.ViewInteraction nine() {
        return onView().withId(R.id.key_right).and().withText("9");
    }

    private cortado.ViewInteraction zero() {
        return onView().withId(R.id.key_middle).and().withText("0");
    }

    private cortado.ViewInteraction ok() {
        return onView().withText("OK");
    }

    private cortado.ViewInteraction zeros() {
        return onView().withText(":00");
    }

    private cortado.ViewInteraction half() {
        return onView().withText(":30");
    }
}