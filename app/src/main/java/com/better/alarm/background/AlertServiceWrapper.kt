package com.better.alarm.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.PhoneStateListener
import com.better.alarm.BuildConfig
import com.better.alarm.R
import com.better.alarm.NotificationImportance
import com.better.alarm.notificationBuilder
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.configuration.Prefs
import com.better.alarm.interfaces.Intents
import com.better.alarm.util.Service
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

class AlertServiceWrapper : Service() {
    private lateinit var alertService: AlertService

    override fun onCreate() {
        val tm = container().telephonyManager()

        val callState: Observable<Int> = Observable.create<Int> { emitter ->
            emitter.onNext(tm.callState)

            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, ignored: String) {
                    emitter.onNext(state)
                }
            }

            emitter.setCancellable {
                // Stop listening for incoming calls.
                tm.listen(listener, PhoneStateListener.LISTEN_NONE)
            }

            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }

        val fadeInTimeInSeconds = container().rxPrefs()
                .getString(Prefs.KEY_FADE_IN_TIME_SEC, "30")
                .asObservable()
                .map { s -> Integer.parseInt(s) * 1000 }

        alertService = AlertService(
                log = container().logger(),
                pm = container().powerManager(),
                alarms = container().alarms(),
                callState = callState,
                prealarmVolume = container().rxPrefs().getInteger(Prefs.KEY_PREALARM_VOLUME, Prefs.DEFAULT_PREALARM_VOLUME).asObservable(),
                fadeInTimeInSeconds = fadeInTimeInSeconds,
                scheduler = AndroidSchedulers.mainThread(),
                plugins = listOf(
                        KlaxonPlugin(
                                log = container().logger(),
                                context = container().context(),
                                resources = container().context.resources
                        ),
                        VibrationPlugin(
                                log = container().logger(),
                                mVibrator = container().vibrator(),
                                vibrate =
                                container().rxPrefs().getBoolean("vibrate").asObservable()
                        ),
                        NotificationsPlugin(
                                mContext = this,
                                wrapper = this,
                                nm = container().notificationManager(),
                                prefs = container().prefs()
                        )
                ),
                goAway = {
                    val notification = notificationBuilder("${BuildConfig.APPLICATION_ID}.AlertServiceWrapper", NotificationImportance.LOW) {
                        setContentTitle("Background")
                        setContentText("Background")
                        setSmallIcon(R.drawable.stat_notify_alarm)
                        setOngoing(true)
                    }
                    startForeground(hashCode(), notification)
                    stopSelf()
                }
        )
    }

    override fun onDestroy() {
        alertService.onDestroy()
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            stopForeground(true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent == null) {
            Service.START_NOT_STICKY
        } else {
            container().wakeLocks().releasePartialWakeLock(intent)
            alertService.onStartCommand(when (intent.action) {
                Intents.ALARM_ALERT_ACTION -> Event.AlarmEvent(intent.getIntExtra(Intents.EXTRA_ID, -1))
                Intents.ALARM_PREALARM_ACTION -> Event.PrealarmEvent(intent.getIntExtra(Intents.EXTRA_ID, -1))
                Intents.ACTION_MUTE -> Event.MuteEvent()
                Intents.ACTION_DEMUTE -> Event.DemuteEvent()
                Intents.ACTION_SOUND_EXPIRED -> Event.Autosilenced(intent.getIntExtra(Intents.EXTRA_ID, -1))
                Intents.ALARM_SNOOZE_ACTION -> Event.SnoozedEvent(intent.getIntExtra(Intents.EXTRA_ID, -1))
                Intents.ALARM_DISMISS_ACTION -> Event.DismissEvent(intent.getIntExtra(Intents.EXTRA_ID, -1))
                else -> throw RuntimeException("Unknown action ${intent.action}")
            })

            return when (intent.action) {
                Intents.ALARM_ALERT_ACTION,
                Intents.ALARM_PREALARM_ACTION,
                Intents.ACTION_MUTE,
                Intents.ACTION_DEMUTE -> Service.START_STICKY
                Intents.ALARM_SNOOZE_ACTION,
                Intents.ALARM_DISMISS_ACTION,
                Intents.ACTION_SOUND_EXPIRED -> Service.START_NOT_STICKY
                else -> throw RuntimeException("Unknown action ${intent.action}")
            }
        }
    }

    /**
     * android.media.AudioManagerDispatches intents to the KlaxonService
     */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.setClass(context, AlertServiceWrapper::class.java)
            container().wakeLocks().acquirePartialWakeLock(intent, "ForAlertServiceWrapper")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}