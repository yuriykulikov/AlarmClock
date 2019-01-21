package com.better.alarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.better.alarm.model.AlarmsService;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.InfoFragment;
import com.better.alarm.presenter.alert.AlarmAlertFullScreen;
import com.better.alarm.presenter.alert.AlarmAlertReceiver;
import com.better.alarm.presenter.background.KlaxonService;
import com.better.alarm.presenter.background.ScheduledReceiver;
import com.better.alarm.presenter.background.ToastPresenter;
import com.better.alarm.presenter.background.VibrationService;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Broadcasts {
    private static final Map<String, List<Class<?>>> actionToRecievers = new HashMap<>();

    static {
        actionToRecievers.put(Intents.ALARM_ALERT_ACTION, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ALARM_PREALARM_ACTION, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ALARM_SNOOZE_ACTION, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_CANCEL_SNOOZE, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ALARM_DISMISS_ACTION, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_SOUND_EXPIRED, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_ALARM_SCHEDULED, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_ALARMS_UNSCHEDULED, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_ALARM_CHANGED, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_ALARM_SET, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_STOP_PREALARM_SAMPLE, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_START_PREALARM_SAMPLE, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_STOP_ALARM_SAMPLE, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_START_ALARM_SAMPLE, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_MUTE, Lists.<Class<?>>newArrayList());
        actionToRecievers.put(Intents.ACTION_DEMUTE, Lists.<Class<?>>newArrayList());

        actionToRecievers.get(Intents.ALARM_ALERT_ACTION).add(AlarmAlertReceiver.class);
        actionToRecievers.get(Intents.ALARM_PREALARM_ACTION).add(AlarmAlertReceiver.class);
        actionToRecievers.get(Intents.ALARM_SNOOZE_ACTION).add(AlarmAlertReceiver.class);
        actionToRecievers.get(Intents.ACTION_CANCEL_SNOOZE).add(AlarmAlertReceiver.class);
        actionToRecievers.get(Intents.ALARM_DISMISS_ACTION).add(AlarmAlertReceiver.class);
        actionToRecievers.get(Intents.ACTION_SOUND_EXPIRED).add(AlarmAlertReceiver.class);

        actionToRecievers.get(Intents.ALARM_ALERT_ACTION).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ALARM_PREALARM_ACTION).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ALARM_SNOOZE_ACTION).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ALARM_DISMISS_ACTION).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_SOUND_EXPIRED).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_START_PREALARM_SAMPLE).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_STOP_PREALARM_SAMPLE).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_START_ALARM_SAMPLE).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_STOP_ALARM_SAMPLE).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_MUTE).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_DEMUTE).add(KlaxonService.Receiver.class);

        actionToRecievers.get(Intents.ALARM_ALERT_ACTION).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ALARM_SNOOZE_ACTION).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ALARM_DISMISS_ACTION).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_SOUND_EXPIRED).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_MUTE).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_DEMUTE).add(VibrationService.Receiver.class);

        actionToRecievers.get(Intents.ACTION_ALARMS_UNSCHEDULED).add(ScheduledReceiver.class);
        actionToRecievers.get(Intents.ACTION_ALARM_SCHEDULED).add(ScheduledReceiver.class);

        actionToRecievers.get(Intents.ACTION_ALARM_SET).add(ToastPresenter.class);
    }

    private static final List<String> localActions = Lists.newArrayList(
            Intents.ACTION_ALARM_SCHEDULED,
            Intents.ACTION_ALARMS_UNSCHEDULED,
            Intents.ACTION_ALARM_CHANGED,
            Intents.REQUEST_LAST_SCHEDULED_ALARM,
            Intents.ALARM_SNOOZE_ACTION,
            Intents.ALARM_DISMISS_ACTION,
            Intents.ACTION_CANCEL_SNOOZE,
            Intents.ACTION_SOUND_EXPIRED
    );

    public static void registerLocal(Context context, BroadcastReceiver receiver, IntentFilter intentFilter) {
        for (int i = 0; i < intentFilter.countActions(); i++) {
            String action = intentFilter.getAction(i);
            Preconditions.checkArgument(localActions.contains(action), "Cannot use %s for local broadcasts", action);
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);
    }

    public static void registerLocal(Context context, BroadcastReceiver receiver, String action) {
        registerLocal(context, receiver, new IntentFilter(action));
    }

    public static void unregisterLocal(Context context, BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public static void sendLocal(Context context, Intent intent) {
        Preconditions.checkArgument(localActions.contains(intent.getAction()), "Cannot use %s for local broadcasts", intent.getAction());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendExplicit(Context context, Intent intent) {
        for (Class<?> receiver : actionToRecievers.get(intent.getAction())) {
            Intent copy = new Intent(intent);
            copy.setClass(context, receiver);
            context.sendBroadcast(copy);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Send a system-wide broadcast. This is an implicit broadcast.
     */
    public static void sendSystemBroadcast(Context mContext, Intent intent) {
        mContext.sendBroadcast(intent);
    }

    public static void registerSystem(Context context, BroadcastReceiver receiver, IntentFilter filter) {
        context.registerReceiver(receiver, filter);
    }

    public static void unregisterSystem(Context context, BroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
    }
}
