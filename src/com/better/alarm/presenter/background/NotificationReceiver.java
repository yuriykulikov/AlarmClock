package com.better.alarm.presenter.background;

import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.AlarmDetailsActivity;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String ACTION_CANCEL_SNOOZE_NOTIFICATION = "NotificationReceiver.ACTION_CANCEL_SNOOZE_NOTIFICATION";
    private static final String DM12 = "E h:mm aa";
    private static final String DM24 = "E kk:mm";
    private static final int NOTIFICATION_OFFSET = 1000;
    Context mContext;
    NotificationManager nm;
    IAlarmsManager alarmsManager;
    Alarm alarm;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        alarmsManager = AlarmsManager.getAlarmsManager();
        nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String action = intent.getAction();
        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        alarm = alarmsManager.getAlarm(id);
        if (action.equals(Intents.ALARM_DISMISS_ACTION)) {
            nm.cancel(id + NOTIFICATION_OFFSET);

        } else if (action.equals(Intents.ALARM_SNOOZE_ACTION)) {
            onSnoozed(id);

        } else if (action.equals(ACTION_CANCEL_SNOOZE_NOTIFICATION)) {
            alarmsManager.dismiss(alarm);

        } else if (action.equals(Intents.ALARM_ALERT_ACTION)) {
            // our alarm fired again, remove snooze notification
            if (alarm.getId() == id) {
                nm.cancel(id + NOTIFICATION_OFFSET);
            }
        }

    }

    private void onSnoozed(int id) {
        // Get the display time for the snooze and update the notification.
        // Append (snoozed) to the label.
        String label = alarm.getLabelOrDefault(mContext);
        label = mContext.getString(R.string.alarm_notify_snooze_label, label);

        // Notify the user that the alarm has been snoozed.
        Intent cancelSnooze = new Intent(mContext, NotificationReceiver.class);
        cancelSnooze.setAction(ACTION_CANCEL_SNOOZE_NOTIFICATION);
        cancelSnooze.putExtra(Intents.EXTRA_ID, id);
        PendingIntent broadcast = PendingIntent.getBroadcast(mContext, id, cancelSnooze, 0);
        Notification n = new Notification(R.drawable.stat_notify_alarm, label, 0);

        n.setLatestEventInfo(mContext, label,
                mContext.getString(R.string.alarm_notify_snooze_text, formatTimeString()), broadcast);
        n.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;
        nm.notify(id + NOTIFICATION_OFFSET, n);
    }

    private String formatTimeString() {
        String format = android.text.format.DateFormat.is24HourFormat(mContext) ? DM24 : DM12;
        Calendar calendar = alarm.getSnoozedTime();
        String timeString = (String) DateFormat.format(format, calendar);
        return timeString;
    }

    private void onSoundExpired(int id) {
        // Launch SetAlarm when clicked.
        Intent viewAlarm = new Intent(mContext, AlarmDetailsActivity.class);
        viewAlarm.putExtra(Intents.EXTRA_ID, id);
        PendingIntent intent = PendingIntent.getActivity(mContext, id, viewAlarm, 0);

        // Update the notification to indicate that the alert has been
        // silenced.
        String label = alarm.getLabelOrDefault(mContext);
        Notification n = new Notification(R.drawable.stat_notify_alarm, label, alarm.getNextTime().getTimeInMillis());
        n.setLatestEventInfo(mContext, label,
                mContext.getString(R.string.alarm_alert_alert_silenced, formatTimeString()), intent);
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        // We have to cancel the original notification since it is in the
        // ongoing section and we want the "killed" notification to be a plain
        // notification.
        nm.cancel(id);
        nm.notify(id, n);
    }
}
