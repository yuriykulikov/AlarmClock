package com.better.alarm.model.interfaces;

import android.net.Uri;

import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.DaysOfWeek;

public class AlarmEditor {
    // TODO parcelable
    public static class AlarmChangeData {
        // TODO parcelable
        public boolean prealarm;
        public Uri alert;
        public String label;
        public boolean vibrate;
        public DaysOfWeek daysOfWeek;
        public int hour;
        public int minutes;
        public boolean enabled;

        public boolean isPrealarm() {
            return prealarm;
        }

        public void setPrealarm(boolean prealarm) {
            this.prealarm = prealarm;
        }

        public Uri getAlert() {
            return alert;
        }

        public void setAlert(Uri alert) {
            this.alert = alert;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public boolean isVibrate() {
            return vibrate;
        }

        public void setVibrate(boolean vibrate) {
            this.vibrate = vibrate;
        }

        public DaysOfWeek getDaysOfWeek() {
            return daysOfWeek;
        }

        public void setDaysOfWeek(DaysOfWeek daysOfWeek) {
            this.daysOfWeek = daysOfWeek;
        }

        public int getHour() {
            return hour;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }

        public int getMinutes() {
            return minutes;
        }

        public void setMinutes(int minutes) {
            this.minutes = minutes;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        AlarmChangeData(Alarm alarm) {
            prealarm = alarm.isPrealarm();
            alert = alarm.getAlert();
            label = alarm.getLabel();
            vibrate = alarm.isVibrate();
            daysOfWeek = alarm.getDaysOfWeek();
            hour = alarm.getHour();
            minutes = alarm.getMinutes();
            enabled = alarm.isEnabled();
        }
    }

    private AlarmChangeData data;
    private final Alarm alarm;

    public AlarmEditor(Alarm alarm) {
        data = new AlarmChangeData(alarm);
        this.alarm = alarm;
    }

    public void commit() {
        ((AlarmCore) alarm).change(data);
        data = null;
    }

    public boolean isPrealarm() {
        return data.isPrealarm();
    }

    public AlarmEditor setPrealarm(boolean prealarm) {
        data.setPrealarm(prealarm);
        return this;
    }

    public Uri getAlert() {
        return data.getAlert();
    }

    public AlarmEditor setAlert(Uri alert) {
        data.setAlert(alert);
        return this;
    }

    public String getLabel() {
        return data.getLabel();
    }

    public AlarmEditor setLabel(String label) {
        data.setLabel(label);
        return this;
    }

    public boolean isVibrate() {
        return data.isVibrate();
    }

    public AlarmEditor setVibrate(boolean vibrate) {
        data.setVibrate(vibrate);
        return this;
    }

    public DaysOfWeek getDaysOfWeek() {
        return data.getDaysOfWeek();
    }

    public AlarmEditor setDaysOfWeek(DaysOfWeek daysOfWeek) {
        data.setDaysOfWeek(daysOfWeek);
        return this;
    }

    public int getHour() {
        return data.getHour();
    }

    public int getMinutes() {
        return data.getMinutes();
    }

    public AlarmEditor setHour(int hour) {
        data.setHour(hour);
        return this;
    }

    public AlarmEditor setMinutes(int minutes) {
        data.setMinutes(minutes);
        return this;
    }

    public boolean isEnabled() {
        return data.isEnabled();
    }

    public AlarmEditor setEnabled(boolean enabled) {
        data.setEnabled(enabled);
        return this;
    }
}
