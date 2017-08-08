package com.better.alarm.presenter;

import com.better.alarm.model.AlarmValue;

import java.util.Comparator;

/**
 * Created by Yuriy on 05.08.2017.
 */

public class Comparators {
    public static final class RepeatComparator implements Comparator<AlarmValue> {
        @Override
        public int compare(AlarmValue lhs, AlarmValue rhs) {
            return Integer.valueOf(getPrio(lhs)).compareTo(Integer.valueOf(getPrio(rhs)));
        }

        /**
         * First comes on Weekdays, than on weekends and then the rest
         *
         * @param alarm
         * @return
         */
        private int getPrio(AlarmValue alarm) {
            switch (alarm.getDaysOfWeek().getCoded()) {
                case 0x7F:
                    return 1;
                case 0x1F:
                    return 2;
                case 0x60:
                    return 3;
                default:
                    return 0;
            }
        }
    }

    public static final class HourComparator implements Comparator<AlarmValue> {
        @Override
        public int compare(AlarmValue lhs, AlarmValue rhs) {
            return Integer.valueOf(lhs.getHour()).compareTo(Integer.valueOf(rhs.getHour()));
        }
    }

    public static final class MinuteComparator implements Comparator<AlarmValue> {
        @Override
        public int compare(AlarmValue lhs, AlarmValue rhs) {
            return Integer.valueOf(lhs.getMinutes()).compareTo(Integer.valueOf(rhs.getMinutes()));
        }
    }
}
