package com.better.alarm.model;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.content.Context;

import com.better.alarm.R;

/*
 * Days of week code as a single int. 0x00: no day 0x01: Monday 0x02:
 * Tuesday 0x04: Wednesday 0x08: Thursday 0x10: Friday 0x20: Saturday 0x40:
 * Sunday
 */
public final class DaysOfWeek {

    private static int[] DAY_MAP = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,};

    // Bitmask of all repeating days
    private int mDays;

    public DaysOfWeek(int days) {
        mDays = days;
    }

    public String toString(Context context, boolean showNever) {
        StringBuilder ret = new StringBuilder();

        // no days
        if (mDays == 0) return showNever ? context.getText(R.string.never).toString() : "";

        // every day
        if (mDays == 0x7f) return context.getText(R.string.every_day).toString();

        // count selected days
        int dayCount = 0;
        int days = mDays;

        while (days > 0) {
            if ((days & 1) == 1) {
                dayCount++;
            }
            days >>= 1;
        }

        // short or long form?
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] dayList = dayCount > 1 ? dfs.getShortWeekdays() : dfs.getWeekdays();

        // selected days
        for (int i = 0; i < 7; i++) {
            if ((mDays & 1 << i) != 0) {
                ret.append(dayList[DAY_MAP[i]]);
                dayCount -= 1;
                if (dayCount > 0) {
                    ret.append(context.getText(R.string.day_concat));
                }
            }
        }
        return ret.toString();
    }

    private boolean isSet(int day) {
        return (mDays & 1 << day) > 0;
    }

    public void set(int day, boolean set) {
        if (set) {
            mDays |= 1 << day;
        } else {
            mDays &= ~(1 << day);
        }
    }

    public void set(DaysOfWeek dow) {
        mDays = dow.mDays;
    }

    public int getCoded() {
        return mDays;
    }

    // Returns days of week encoded in an array of booleans.
    public boolean[] getBooleanArray() {
        boolean[] ret = new boolean[7];
        for (int i = 0; i < 7; i++) {
            ret[i] = isSet(i);
        }
        return ret;
    }

    public boolean isRepeatSet() {
        return mDays != 0;
    }

    public boolean isEveryDay() {
        return mDays == 0x7f;
    }

    /**
     * returns number of days from today until next alarm
     *
     * @param c must be set to today
     */
    public int getNextAlarm(Calendar c) {
        if (mDays == 0) return -1;

        int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        int day = 0;
        int dayCount = 0;
        for (; dayCount < 7; dayCount++) {
            day = (today + dayCount) % 7;
            if (isSet(day)) {
                break;
            }
        }
        return dayCount;
    }

    @Override
    public String toString() {
        if (mDays == 0) return "never";
        if (mDays == 0x7f) return "everyday";
        StringBuilder ret = new StringBuilder();
        String[] dayList = new DateFormatSymbols().getShortWeekdays();
        for (int i = 0; i < 7; i++) {
            if ((mDays & 1 << i) != 0) {
                ret.append(dayList[DAY_MAP[i]]);
            }
        }
        return ret.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mDays;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DaysOfWeek other = (DaysOfWeek) obj;
        if (mDays != other.mDays) return false;
        return true;
    }
}