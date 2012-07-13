package com.better.alarm.model;


public class Intents {
    /**
     * Broadcasted when an alarm fires.
     */
    public static final String ALARM_ALERT_ACTION = "com.better.alarm.ALARM_ALERT";

    /**
     * Broadcasted when alarm is snoozed.
     */
    public static final String ALARM_SNOOZE_ACTION = "com.better.alarm.ALARM_SNOOZE";

    /**
     * Broadcasted when alarm is dismissed.
     */
    public static final String ALARM_DISMISS_ACTION = "com.better.alarm.ALARM_DISMISS";

    /**
     * Broadcasted when alarm is scheduled
     */
    public static final String  ACTION_ALARM_SCHEDULED = "com.better.alarm.model.Intents.ACTION_ALARM_SCHEDULED";

    /**
     * Broadcasted when alarm is scheduled
     */
    public static final String  ACTION_ALARMS_UNSCHEDULED = "com.better.alarm.model.Intents.ACTION_ALARMS_UNSCHEDULED";

    /**
     * Key of the Alarm attached as a parceble extra
     */
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";
}
