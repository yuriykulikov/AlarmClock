package com.better.alarm.presenter;

import java.util.Calendar;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.github.androidutils.logger.Logger;

/**
 * 
 * @author Yuriy
 * 
 */
public class InfoFragment extends Fragment {

    private final Logger log = Logger.getDefaultLogger();

    private IAlarmsManager alarms;
    private BroadcastReceiver mAlarmsScheduledReceiver;

    private static final String DM12 = "E h:mm aa";
    private static final String DM24 = "E kk:mm";

    private TextView textView;
    private TextView remainingTime;

    private Alarm alarm;

    private TickReceiver mTickReceiver;

    private final class TickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (alarm != null) {
                remainingTime.setText(formatRemainingTimeString(alarm.getNextTime().getTimeInMillis()));
            }
        }
    }

    private class AlarmChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction().equals(Intents.ACTION_ALARM_SCHEDULED)) {
                    int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
                    alarm = alarms.getAlarm(id);

                    log.d(intent.toString() + " " + alarm.toString());
                    String format = android.text.format.DateFormat.is24HourFormat(context) ? DM24 : DM12;
                    Calendar calendar = alarm.isSnoozed() ? alarm.getSnoozedTime() : alarm.getNextTime();
                    String timeString = (String) DateFormat.format(format, calendar);
                    textView.setText(timeString);
                    remainingTime.setText(formatRemainingTimeString(alarm.getNextTime().getTimeInMillis()));

                } else if (intent.getAction().equals(Intents.ACTION_ALARMS_UNSCHEDULED)) {
                    log.d(intent.toString());
                    textView.setText("");
                    remainingTime.setText("");
                    alarm = null;
                }
            } catch (AlarmNotFoundException e) {
                // TODO Auto-generated catch block
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        alarms = AlarmsManager.getAlarmsManager();
        mAlarmsScheduledReceiver = new AlarmChangedReceiver();
        mTickReceiver = new TickReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.info_fragment, container, false);
        textView = (TextView) view.findViewById(R.id.textView1);
        remainingTime = (TextView) view.findViewById(R.id.textViewRemainingTime);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        log.d("onResume");
        IntentFilter intentFilter = new IntentFilter(Intents.ACTION_ALARM_SCHEDULED);
        intentFilter.addAction(Intents.ACTION_ALARMS_UNSCHEDULED);
        getActivity().registerReceiver(mAlarmsScheduledReceiver, intentFilter);
        getActivity().sendBroadcast(new Intent(Intents.REQUEST_LAST_SCHEDULED_ALARM));
        getActivity().registerReceiver(mTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    public void onPause() {
        super.onPause();
        log.d("onPause");
        getActivity().unregisterReceiver(mAlarmsScheduledReceiver);
        getActivity().unregisterReceiver(mTickReceiver);
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from now"
     */
    private String formatRemainingTimeString(long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = days == 0 ? "" : days == 1 ? getActivity().getString(R.string.day) : getActivity().getString(
                R.string.days, Long.toString(days));

        String minSeq = minutes == 0 ? "" : minutes == 1 ? getActivity().getString(R.string.minute) : getActivity()
                .getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = hours == 0 ? "" : hours == 1 ? getActivity().getString(R.string.hour) : getActivity()
                .getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) | (dispHour ? 2 : 0) | (dispMinute ? 4 : 0);

        String[] formats = getActivity().getResources().getStringArray(R.array.alarm_set_short);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
}