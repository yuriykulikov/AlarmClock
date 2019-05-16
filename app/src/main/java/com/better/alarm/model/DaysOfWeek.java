package com.better.alarm.model;

import android.content.Context;

import com.better.alarm.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;

/*
 * Days of week code as a single int. 0x00: no day 0x01: Monday 0x02:
 * Tuesday 0x04: Wednesday 0x08: Thursday 0x10: Friday 0x20: Saturday 0x40:
 * Sunday
 */
public class DaysOfWeek {

    private static final int[] DAY_MAP = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,};
    private int coded;

    public String toString(Context context, boolean showNever) {
        StringBuilder ret = new StringBuilder();

        // no days
        if (getCoded() == 0) return showNever ? context.getText(R.string.never).toString() : "";

        // every day
        if (getCoded() == 0x7f) return context.getText(R.string.every_day).toString();

        // count selected days
        int dayCount = 0;
        int days = getCoded();

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
            if ((getCoded() & 1 << i) != 0) {
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
        return (getCoded() & 1 << day) > 0;
    }


    public DaysOfWeek(int coded) {
        this.coded = coded;
    }

    public int getCoded() {
        return coded;
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
        return getCoded() != 0;
    }

    /**
     * returns number of days from today until next alarm
     *
     * @param c must be set to today
     */
    public int getNextAlarm(Calendar c) {
        if (getCoded() == 0) return -1;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DaysOfWeek that = (DaysOfWeek) o;
        return coded == that.coded;
    }

    @Override
    public int hashCode() {
        return coded;
    }

    @Override
    public String toString() {
        return String.valueOf(ifSet(0, 'm')) +
                ifSet(1, 't') +
                ifSet(2, 'w') +
                ifSet(3, 't') +
                ifSet(4, 'f') +
                ifSet(5, 's') +
                ifSet(6, 's');
    }

    private char ifSet(int day, char letter) {
        return isSet(day) ? letter : '_';
    }
}