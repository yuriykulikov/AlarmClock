package com.better.alarm.background

import android.content.Intent
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.better.alarm.CHANNEL_ID
import com.better.alarm.R
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.globalInject
import com.better.alarm.configuration.globalLogger
import com.better.alarm.configuration.logger
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.better.alarm.notificationBuilder
import com.better.alarm.oreo
import com.better.alarm.util.Service
import com.better.alarm.wakelock.WakeLockManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

typealias AlertServiceFactory = (Service) -> AlertService
typealias NotificationsPluginFactory = (Service) -> NotificationsPlugin

/**
 * This wraps [AlertService], does dependency injection and delegates everything to it.
 * This way we can unit-test [AlertService], [KlaxonPlugin], [VibrationPlugin] and [NotificationsPlugin].
 */
class AlertServiceWrapper : Service() {
    companion object {
        fun module(): Module = module {

            factory(named("inCall")) {
                val tm: TelephonyManager = get()
                Observable
                        .create<Int> { emitter ->
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
                        .startWith(tm.callState)
                        .map { it != TelephonyManager.CALL_STATE_IDLE }
                        .distinctUntilChanged()
                        .replay(1)
                        .refCount()
            }

            factory(named("fadeInTimeInMillis")) {
                get<RxSharedPreferences>()
                        .getString(Prefs.KEY_FADE_IN_TIME_SEC, "30")
                        .asObservable()
                        .map { s -> s.toInt() * 1000 }
            }


            factory {
                KlaxonPlugin(
                        log = logger("AlertService"),
                        playerFactory = { PlayerWrapper(get(), get(), logger("AlertService")) },
                        prealarmVolume = get<RxSharedPreferences>().getInteger(Prefs.KEY_PREALARM_VOLUME, Prefs.DEFAULT_PREALARM_VOLUME).asObservable(),
                        fadeInTimeInMillis = get(named("fadeInTimeInMillis")),
                        inCall = get(named("inCall")),
                        scheduler = get()
                )
            }

            factory(named("volumePreferenceDemo")) {
                KlaxonPlugin(
                        log = logger("VolumePreference"),
                        playerFactory = { PlayerWrapper(get(), get(), logger("VolumePreference")) },
                        prealarmVolume = get<RxSharedPreferences>().getInteger(Prefs.KEY_PREALARM_VOLUME, Prefs.DEFAULT_PREALARM_VOLUME).asObservable(),
                        fadeInTimeInMillis = Observable.just(100),
                        inCall = Observable.just(false),
                        scheduler = get()
                )
            }

            factory { params ->
                VibrationPlugin(
                        log = logger("AlertService"),
                        vibrator = get(),
                        //TODO move to container
                        fadeInTimeInMillis = when {
                            params.isEmpty() -> get<RxSharedPreferences>()
                                    .getString(Prefs.KEY_FADE_IN_TIME_SEC, "30")
                                    .asObservable()
                                    .map { s -> Integer.parseInt(s) * 1000 }
                            else -> Observable.just(params[0])
                        },
                        scheduler = get(),
                        vibratePreference = get<RxSharedPreferences>().getBoolean("vibrate").asObservable()
                )
            }

            factory<NotificationsPluginFactory>(named("NotificationsPluginFactory")) {
                { service: Service ->
                    NotificationsPlugin(
                            logger = logger("AlertService"),
                            mContext = get(),
                            nm = get(),
                            startForeground = { id, notification -> service.startForeground(id, notification) }
                    )
                }
            }

            factory<AlertServiceFactory>(named("AlertServiceFactory")) {
                { service: Service ->
                    AlertService(
                            log = logger("AlertService"),
                            wakelocks = get(),
                            alarms = get(),
                            inCall = get(named("inCall")),
                            plugins = arrayOf(
                                    get<KlaxonPlugin>(),
                                    get<VibrationPlugin>(),
                                    get<NotificationsPluginFactory>(named("NotificationsPluginFactory"))(service)
                            ),
                            handleUnwantedEvent = {
                                oreo {
                                    val notification = service.notificationBuilder(CHANNEL_ID) {
                                        setContentTitle("Background")
                                        setContentText("Background")
                                        setSmallIcon(R.drawable.stat_notify_alarm)
                                        setOngoing(true)
                                    }
                                    service.startForeground(42, notification)
                                }
                                service.stopSelf()
                            },
                            stopSelf = {
                                service.stopSelf()
                            }
                    )
                }
            }

        }
    }

    private lateinit var alertService: AlertService
    private val tm: TelephonyManager by globalInject()
    private val log: Logger by globalLogger("AlertService")
    private val prefs: RxSharedPreferences by globalInject()
    private val wakelocks: WakeLockManager by globalInject()

    override fun onCreate() {
        alertService = globalInject<AlertServiceFactory>(named("AlertServiceFactory")).value(this)
    }

    override fun onDestroy() {
        log.debug { "destroyed" }
        wakelocks.releaseServiceLock()
        oreo {
            stopForeground(true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent == null) {
            log.warning { "null intent" }
            // this also has to be delivered, because someone has to call startForeground()
            alertService.onStartCommand(Event.NullEvent())
            START_NOT_STICKY
        } else {
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

            val ret = when (intent.action) {
                Intents.ALARM_ALERT_ACTION,
                Intents.ALARM_PREALARM_ACTION,
                Intents.ACTION_MUTE,
                Intents.ACTION_DEMUTE -> START_STICKY
                Intents.ALARM_SNOOZE_ACTION,
                Intents.ALARM_DISMISS_ACTION,
                Intents.ACTION_SOUND_EXPIRED -> START_NOT_STICKY
                else -> throw RuntimeException("Unknown action ${intent.action}")
            }

            wakelocks.releaseTransitionWakeLock(intent)
            ret
        }
    }
}