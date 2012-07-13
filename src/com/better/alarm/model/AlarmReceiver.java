package com.better.alarm.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.util.Log;

/**
 * This receiver is a part of the model, but it has to be a separate class.
 * Application can be garbage collected, so we need to register a Receiver in
 * the manifest.
 * 
 * @author Yuriy
 * 
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final boolean DBG = true;
    private static final String ACTION_FIRED = "com.better.alarm.ACTION_FIRED";
    private static final String ACTION_SNOOZED_FIRED = "com.better.alarm.ACTION_SNOOZED_FIRED";
    private static final String ACTION_SOUND_EXPIRED = "com.better.alarm.ACTION_SOUND_EXPIRED";
    /**
     * This extra is the raw Alarm object data. It is used in the
     * AlarmManagerService to avoid a ClassNotFoundException when filling in the
     * Intent extras.
     */
    private static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    @Override
    public void onReceive(Context context, Intent intent) {
        Alarm alarm = null;
        // Grab the alarm from the intent. Since the remote AlarmManagerService
        // fills in the Intent to add some extra data, it must unparcel the
        // Alarm object. It throws a ClassNotFoundException when unparcelling.
        // To avoid this, do the marshalling ourselves.
        final byte[] data = intent.getByteArrayExtra(ALARM_RAW_DATA);
        if (data != null) {
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            alarm = Alarm.CREATOR.createFromParcel(in);
        }

        if (alarm == null) {
            Log.wtf(TAG, "Failed to parse the alarm from the intent");
            return;
        }

        String action = intent.getAction();
        if (action.equals(ACTION_FIRED)) {
            Alarms.getAlarms().onAlarmFired(alarm);

        } else if (action.equals(ACTION_SNOOZED_FIRED)) {
            Alarms.getAlarms().onAlarmSnoozedFired(alarm);

        } else if (action.equals(ACTION_SOUND_EXPIRED)) {
            Alarms.getAlarms().onAlarmSoundExpired(alarm);

        }
    }

    /**
     * @param context
     */
    static void removeRTCAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_FIRED),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
    }

    /**
     * @param context
     * @param alarm
     * @param atTimeInMillis
     */
    static void setUpRTCAlarm(Context context, final Alarm alarm, final long atTimeInMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (DBG) Log.d(TAG, "** setAlert id " + alarm.id + " atTime " + atTimeInMillis);

        Intent intent = new Intent(ACTION_FIRED);

        // XXX: This is a slight hack to avoid an exception in the remote
        // AlarmManagerService process. The AlarmManager adds extra data to
        // this Intent which causes it to inflate. Since the remote process
        // does not know about the Alarm class, it throws a
        // ClassNotFoundException.
        //
        // To avoid this, we marshall the data ourselves and then parcel a plain
        // byte[] array. The AlarmReceiver class knows to build the Alarm
        // object from the byte[] array.
        Parcel out = Parcel.obtain();
        alarm.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(ALARM_RAW_DATA, out.marshall());

        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
    }

}
