package com.better.alarm.model;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.better.alarm.logger.Logger;
import com.google.inject.Inject;

/**
 * Created by Yuriy on 24.06.2017.
 */

public interface AlarmSetter {
    public static final String ACTION_FIRED = AlarmsScheduler.ACTION_FIRED;
    public static final String EXTRA_ID = AlarmsScheduler.EXTRA_ID;
    public static final String EXTRA_TYPE = AlarmsScheduler.EXTRA_TYPE;

    void removeRTCAlarm();

    void setUpRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm);

    void fireNow(AlarmsScheduler.ScheduledAlarm firedInThePastAlarm);

    class AlarmSetterImpl implements AlarmSetter {
        private final Context mContext;
        private AlarmManager am;

        private interface ISetAlarmStrategy {
            void setRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm, PendingIntent sender);
        }

        private final ISetAlarmStrategy setAlarmStrategy;
        private final Logger log;

        @Inject
        public AlarmSetterImpl(Logger logger, AlarmManager am, Context context) {
            this.am = am;
            this.log = logger;
            this.setAlarmStrategy = initSetStrategyForVersion();
            log.d("Using " + setAlarmStrategy);
            mContext = context;
        }

        @Override
        public void removeRTCAlarm() {
            PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_FIRED), 0);
            am.cancel(sender);
        }

        @Override
        public void setUpRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm) {
            log.d("Set " + alarm.toString());
            Intent intent = new Intent(ACTION_FIRED);
            intent.putExtra(EXTRA_ID, alarm.id);
            intent.putExtra(EXTRA_TYPE, alarm.type.name());
            PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            setAlarmStrategy.setRTCAlarm(alarm, sender);
        }

        @Override
        public void fireNow(AlarmsScheduler.ScheduledAlarm firedInThePastAlarm) {
            Intent intent = new Intent(ACTION_FIRED);
            intent.putExtra(EXTRA_ID, firedInThePastAlarm.id);
            intent.putExtra(EXTRA_TYPE, firedInThePastAlarm.type.name());
            mContext.sendBroadcast(intent);
        }

        private ISetAlarmStrategy initSetStrategyForVersion() {
            log.d("SDK is " + android.os.Build.VERSION.SDK_INT);
            if (android.os.Build.VERSION.SDK_INT >= 23) return new MarshmallowSetter();
            else if (android.os.Build.VERSION.SDK_INT >= 19) return new KitKatSetter();
            else return new IceCreamSetter();
        }

        private final class IceCreamSetter implements ISetAlarmStrategy {
            @Override
            public void setRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm, PendingIntent sender) {
                am.set(AlarmManager.RTC_WAKEUP, alarm.calendar.getTimeInMillis(), sender);
            }
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        private final class KitKatSetter implements ISetAlarmStrategy {
            @Override
            public void setRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm, PendingIntent sender) {
                am.setExact(AlarmManager.RTC_WAKEUP, alarm.calendar.getTimeInMillis(), sender);
            }
        }

        @TargetApi(23)
        private final class MarshmallowSetter implements ISetAlarmStrategy {
            @Override
            public void setRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm, PendingIntent sender) {
                try {
                    am.getClass()
                            .getMethod("setExactAndAllowWhileIdle", int.class, long.class, PendingIntent.class)
                            .invoke(am, AlarmManager.RTC_WAKEUP, alarm.calendar.getTimeInMillis(), sender);
                } catch (ReflectiveOperationException e) {
                    am.setExact(AlarmManager.RTC_WAKEUP, alarm.calendar.getTimeInMillis(), sender);
                }
            }
        }
    }
}
