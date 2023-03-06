package com.better.alarm.logger;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

@RequiresApi(api = Build.VERSION_CODES.O)
public class DateTransformer {
    private static final String MONTH_FORMAT = "MMMM";
    private static final String DATE_FORMAT = "MMMM d, yyyy";
    private static final String DAY_FORMAT = "EEEE";
    private static final DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern(MONTH_FORMAT);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern(DAY_FORMAT);
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter fullTimeFormat = DateTimeFormatter.ofPattern("MMM dd yyy hh:mm:ss a");
//    private static final DateTimeFormatter fullTimeFormat = DateTimeFormatter.ofPattern("hh:mm a");

    private static final String separator = "-";

    public static String getDate(long epochDay) {
        return getOfEpochDay(epochDay).format(dateFormat);
    }

    private static LocalDate getOfEpochDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay);
    }

    private static final LocalDate firstEpoch = LocalDate.ofEpochDay(0);

    public static long epochMonth(long epochDay){
        return ChronoUnit.MONTHS.between(firstEpoch, getOfEpochDay(epochDay));
    }

    public static String ofEpochMonth(long epochMonth){
        long epochDay = getMonthLocalDate(epochMonth).toEpochDay();
        return getDate(epochDay);
    }

    public static LocalDate getMonthLocalDate(long epochMonth) {
        return firstEpoch.plusMonths(epochMonth).withDayOfMonth(1);
    }

    public static String getDate(double epochDay) {
        return getOfEpochDay((long) epochDay).format(dateFormat);
    }

    public static String getDay(long epochDay) {
        return getOfEpochDay(epochDay).format(dayFormat);
    }

    public static String getStringDayOfMonth(long epochDay) {
        return String.valueOf(getOfEpochDay(epochDay).getDayOfMonth());
    }

    public static int getDayOfWeek(long epochDay) {
        return getOfEpochDay(epochDay).getDayOfWeek().getValue();
    }

//    public static int getMonth(long epochDay) {
//        return getOfEpochDay(epochDay).getMonthValue();
//    }

//    public static int getMonthValue(long epochMonth) {
//        return getMonthLocalDate(epochMonth).getMonthValue();
////        return getOfEpochDay(epochDay).getMonthValue();
//    }

    public static String[] getStringYearMonthRange(LocalDate lowerBound, LocalDate upperBound) {
        long range = ChronoUnit.MONTHS.between(
                YearMonth.from(lowerBound),
                YearMonth.from(upperBound)
        );
        long lowerEpochDay = lowerBound.toEpochDay();
        String[] result = new String[(int) range];
        for (int i = 0; i < range; i++) {
            result[i] = getStringYearMonth(lowerEpochDay, i);
        }
        return result;
    }

    public static String getStringYearMonth(long epochDay, int monthOffset) {
        int year = getOfEpochDay(epochDay).plusMonths(monthOffset).getYear();
        int month = getOfEpochDay(epochDay).plusMonths(monthOffset).getMonthValue();
        return year + separator + month;
    }

    public static String getTime(long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(timeFormat);
    }

    public static String getDateFromTime(long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(fullTimeFormat);
    }

    public static String ofInstant(Instant time) {
        return time.atZone(ZoneId.systemDefault()).format(timeFormat);
    }

    public static String getSecond(long epochSecond) {
        return Instant.ofEpochSecond(epochSecond).atZone(ZoneId.systemDefault()).format(timeFormat);
    }

    public static String monthFormat(TemporalAccessor accessor) {
        return monthFormat.format(accessor);
    }

    /**returns the passed epochMillis plus a day.*/
    public static long addADay(long epochMillis) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        instant = instant.plus(1, ChronoUnit.DAYS);
        return instant.toEpochMilli();
    }

}

