package com.better.alarm.configuration

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.telephony.TelephonyManager
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.model.Alarms
import com.better.alarm.wakelock.WakeLockManager
import com.f2prateek.rx.preferences2.RxSharedPreferences

/**
 * Created by Yuriy on 09.08.2017.
 */
data class Container(
        val context: Context,
        val loggerFactory: (String) -> Logger,
        val sharedPreferences: SharedPreferences,
        val rxPrefs: RxSharedPreferences,
        val prefs: Prefs,
        val store: Store,
        val rawAlarms: Alarms) {
    private val wlm: WakeLockManager = WakeLockManager(logger(), powerManager())

    fun context(): Context = context

    @Deprecated("Use the factory or createLogger", ReplaceWith("createLogger(\"TODO\")"))
    fun logger(): Logger = loggerFactory("default")

    @Deprecated("Use the factory or createLogger", ReplaceWith("createLogger(\"TODO\")"))
    val logger: Logger = loggerFactory("default")

    fun createLogger(tag: String) = loggerFactory(tag)

    fun sharedPreferences(): SharedPreferences = sharedPreferences

    fun rxPrefs(): RxSharedPreferences = rxPrefs

    fun prefs(): Prefs = prefs

    fun store(): Store = store

    fun rawAlarms(): Alarms = rawAlarms

    fun alarms(): IAlarmsManager {
        return rawAlarms()
    }

    fun wakeLocks(): WakeLockManager {
        return wlm
    }

    fun vibrator(): Vibrator {
        return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun powerManager(): PowerManager {
        return context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    fun telephonyManager(): TelephonyManager {
        return context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    fun notificationManager(): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun audioManager(): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}
