package com.better.alarm.presenter;

import java.util.Calendar;

import com.better.alarm.Log;
import com.better.alarm.R;
import com.better.alarm.model.Alarm;
import com.better.alarm.model.Alarms;
import com.better.alarm.model.Intents;
import com.better.alarm.presenter.AlarmAlert;
import com.better.alarm.presenter.AlarmReceiver;
import com.better.alarm.presenter.SetAlarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationManager extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        // TODO Auto-generated method stub
        
        if (Intents.ALARM_KILLED.equals(intent.getAction())) {
            // The alarm has been killed, update the notification
            updateNotification(context, (Alarm)
                    intent.getParcelableExtra(Intents.ALARM_INTENT_EXTRA),
                    intent.getIntExtra(Intents.ALARM_KILLED_TIMEOUT, -1));
            return;
        } else 
        
        
        // here goes!
        if (!killed) {
            // Cancel the notification and stop playing the alarm
            NotificationManager nm = getNotificationManager();
            nm.cancel(mAlarm.id);

        }
    }

    snoozed () {
        // Get the display time for the snooze and update the notification.
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(snoozeTime);

        // Append (snoozed) to the label.
        String label = mAlarm.getLabelOrDefault(this);
        label = getString(R.string.alarm_notify_snooze_label, label);

        // Notify the user that the alarm has been snoozed.
        Intent cancelSnooze = new Intent(this, AlarmReceiver.class);
        cancelSnooze.setAction(Alarms.CANCEL_SNOOZE);
        cancelSnooze.putExtra(Alarms.ALARM_INTENT_EXTRA, mAlarm);
        PendingIntent broadcast =
                PendingIntent.getBroadcast(this, mAlarm.id, cancelSnooze, 0);
        NotificationManager nm = getNotificationManager();
        Notification n = new Notification(R.drawable.stat_notify_alarm,
                label, 0);
        n.setLatestEventInfo(this, label,
                getString(R.string.alarm_notify_snooze_text,
                    Alarms.formatTime(this, c)), broadcast);
        n.flags |= Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
        nm.notify(mAlarm.id, n);
    }
    
    private void updateNotification(Context context, Alarm alarm, int timeout) {
        NotificationManager nm = getNotificationManager(context);

        // If the alarm is null, just cancel the notification.
        if (alarm == null) {
            if (Log.LOGV) {
                Log.v("Cannot update notification for killer callback");
            }
            return;
        }

        // Launch SetAlarm when clicked.
        Intent viewAlarm = new Intent(context, SetAlarm.class);
        viewAlarm.putExtra(Intents.ALARM_INTENT_EXTRA, alarm);
        PendingIntent intent =
                PendingIntent.getActivity(context, alarm.id, viewAlarm, 0);

        // Update the notification to indicate that the alert has been
        // silenced.
        String label = alarm.getLabelOrDefault(context);
        Notification n = new Notification(R.drawable.stat_notify_alarm,
                label, alarm.time);
        n.setLatestEventInfo(context, label,
                context.getString(R.string.alarm_alert_alert_silenced, timeout),
                intent);
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        // We have to cancel the original notification since it is in the
        // ongoing section and we want the "killed" notification to be a plain
        // notification.
        nm.cancel(alarm.id);
        nm.notify(alarm.id, n);
    }
    
    notifyAlarmKickedOff() {

    }
}
