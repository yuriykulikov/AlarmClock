package com.better.alarm.background

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.Vibrator
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.better.alarm.configuration.AlarmApplication
import com.better.alarm.configuration.Prefs
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.inject.Inject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Function4
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class VibrationService : Service() {
    @Inject
    private lateinit var log: Logger
    @Inject
    private lateinit var mVibrator: Vibrator
    @Inject
    private lateinit var sp: SharedPreferences
    @Inject
    private lateinit var pm: PowerManager
    @Inject
    private lateinit var telephonyManager: TelephonyManager
    @Inject
    private lateinit var rxPrefs: RxSharedPreferences

    private var wakeLock: WakeLock? = null

    //isEnabled && !inCall && !isMuted && isStarted
    private val inCall: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    private val muted: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    private var subscription = Disposables.disposed()

    /**
     * Dispatches intents to the KlaxonService
     */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.setClass(context, VibrationService::class.java)
            AlarmApplication.wakeLocks().acquirePartialWakeLock(intent, "ForVibrationService")
            context.startService(intent)
        }
    }

    override fun onCreate() {
        AlarmApplication.guice().injectMembers(this);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VibrationService")
        wakeLock?.acquire()

        telephonyManager.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                inCall.onNext(state != TelephonyManager.CALL_STATE_IDLE)
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onDestroy() {
        subscription.dispose()
        log.d("Service destroyed")
        wakeLock!!.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            AlarmApplication.wakeLocks().releasePartialWakeLock(intent)

            when (intent.action) {
                Intents.ALARM_ALERT_ACTION -> onAlert()
                Intents.ACTION_MUTE -> muted.onNext(true)
                Intents.ACTION_DEMUTE -> muted.onNext(false)
                else -> stopAndCleanup()
            }

            when (intent.action) {
                Intents.ALARM_ALERT_ACTION,
                Intents.ACTION_MUTE,
                Intents.ACTION_DEMUTE -> return Service.START_STICKY
                else -> return Service.START_NOT_STICKY
            }
        } else {
            return Service.START_NOT_STICKY
        }
    }

    private fun onAlert() {
        muted.onNext(false)
        val asString = sp.getString(Prefs.KEY_FADE_IN_TIME_SEC, "30")
        val time = Integer.parseInt(asString)

        val preference: Observable<Boolean> = rxPrefs.getBoolean("vibrate").asObservable()
        val timer: Observable<Boolean> = Observable
                .just(true)
                .delay(time.toLong(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .startWith(false)

        subscription = Observable
                .combineLatest<Boolean, Boolean, Boolean, Boolean, Boolean>(preference, inCall, muted, timer,
                        Function4 { isEnabled, isInCall, isMuted, timerStarted ->
                            isEnabled && !isInCall && !isMuted && timerStarted
                        })
                .distinctUntilChanged()
                .subscribe({ vibrate ->
                    if (vibrate) {
                        log.d("Starting vibration")
                        mVibrator.vibrate(sVibratePattern, 0)
                    } else {
                        log.d("Canceling vibration")
                        mVibrator.cancel()
                    }
                })
    }

    private fun stopAndCleanup() {
        log.d("stopAndCleanup")
        mVibrator.cancel()
        subscription.dispose()
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private val sVibratePattern: LongArray = longArrayOf(500, 500)
    }
}
