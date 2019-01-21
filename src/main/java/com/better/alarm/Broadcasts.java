package com.better.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.better.alarm.alert.AlarmAlertReceiver;
import com.better.alarm.background.KlaxonService;
import com.better.alarm.background.VibrationService;
import com.better.alarm.interfaces.Intents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.collections.CollectionsKt;

public class Broadcasts {
    private static final Map<String, List<Class<?>>> actionToRecievers = new HashMap<>();

    static {
        actionToRecievers.put(Intents.ALARM_ALERT_ACTION, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ALARM_PREALARM_ACTION, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ALARM_SNOOZE_ACTION, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ACTION_CANCEL_SNOOZE, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ALARM_DISMISS_ACTION, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ACTION_SOUND_EXPIRED, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ACTION_STOP_PREALARM_SAMPLE, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ACTION_START_PREALARM_SAMPLE, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ACTION_MUTE, CollectionsKt.<Class<?>>arrayListOf());
        actionToRecievers.put(Intents.ACTION_DEMUTE, CollectionsKt.<Class<?>>arrayListOf());

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
        actionToRecievers.get(Intents.ACTION_MUTE).add(KlaxonService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_DEMUTE).add(KlaxonService.Receiver.class);

        actionToRecievers.get(Intents.ALARM_ALERT_ACTION).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ALARM_SNOOZE_ACTION).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ALARM_DISMISS_ACTION).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_SOUND_EXPIRED).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_MUTE).add(VibrationService.Receiver.class);
        actionToRecievers.get(Intents.ACTION_DEMUTE).add(VibrationService.Receiver.class);
    }

    private static final List<String> localActions = CollectionsKt.arrayListOf(
            Intents.ALARM_SNOOZE_ACTION,
            Intents.ALARM_DISMISS_ACTION,
            Intents.ACTION_CANCEL_SNOOZE,
            Intents.ACTION_SOUND_EXPIRED
    );

    public static void registerLocal(Context context, BroadcastReceiver receiver, IntentFilter intentFilter) {
        for (int i = 0; i < intentFilter.countActions(); i++) {
            String action = intentFilter.getAction(i);
            if (!localActions.contains(action)) {
                throw new RuntimeException("Cannot use %s for local broadcasts " + action);
            }
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
        if (!localActions.contains(intent.getAction())) {
            throw new RuntimeException("Cannot use %s for local broadcasts " + intent.getAction());
        }
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
