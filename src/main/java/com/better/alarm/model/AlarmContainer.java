package com.better.alarm.model;

import java.util.Calendar;

import android.media.RingtoneManager;
import android.net.Uri;

import org.immutables.value.Value;

@Value.Immutable
//@Value.Style(stagedBuilder = true)
public abstract class AlarmContainer implements AlarmChangeData {
    // This string is used to indicate a silent alarm in the db.
    private static final String ALARM_ALERT_SILENT = "silent";
    public static final Persistence PERSISTENCE_STUB = new Persistence() {
        @Override
        public void persist(AlarmContainer container) {
        }

        @Override
        public void delete(AlarmContainer container) {
        }
    };

    public interface Persistence {
        void persist(AlarmContainer container);

        void delete(AlarmContainer container);
    }

    final void delete() {
        persistence().delete(this);
    }

    @Value.Check
    public AlarmContainer persist() {
        persistence().persist(this);
        return this;
    }

    public abstract Persistence persistence();

    @Value.Derived
    public boolean isSilent(){
        return ALARM_ALERT_SILENT.equals(alertString());
    }

    public abstract Calendar getNextTime();

    public abstract String getState();

    @Value.Lazy
    public Uri getAlert() {
        String alertString = alertString();
        if (alertString.length() != 0) {
            try {
                return Uri.parse(alertString);
            } catch (Exception e) {
                // If the database alert is null or it failed to parse, use the
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
        } else {
            // default alert.
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
    }
}