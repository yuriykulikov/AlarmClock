package com.better.alarm.configuration

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.preference.PreferenceManager
import android.telephony.TelephonyManager
import com.better.alarm.alert.BackgroundNotifications
import com.better.alarm.background.AlertServicePusher
import com.better.alarm.background.KlaxonPlugin
import com.better.alarm.background.PlayerWrapper
import com.better.alarm.bugreports.BugReporter
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.LogcatLogWriter
import com.better.alarm.logger.Logger
import com.better.alarm.logger.LoggerFactory
import com.better.alarm.logger.StartupLogWriter
import com.better.alarm.model.AlarmCore
import com.better.alarm.model.AlarmCoreFactory
import com.better.alarm.model.AlarmSetter
import com.better.alarm.model.AlarmStateNotifier
import com.better.alarm.model.Alarms
import com.better.alarm.model.AlarmsScheduler
import com.better.alarm.model.Calendars
import com.better.alarm.model.ContainerFactory
import com.better.alarm.model.IAlarmsScheduler
import com.better.alarm.persistance.DatabaseQuery
import com.better.alarm.persistance.PersistingContainerFactory
import com.better.alarm.presenter.AlarmsListActivity
import com.better.alarm.presenter.DynamicThemeHandler
import com.better.alarm.presenter.ScheduledReceiver
import com.better.alarm.presenter.ToastPresenter
import com.better.alarm.util.Optional
import com.better.alarm.wakelock.WakeLockManager
import com.better.alarm.wakelock.Wakelocks
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import java.util.ArrayList
import java.util.Calendar

fun Scope.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun Koin.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun startKoin(context: Context): Koin {
    // The following line triggers the initialization of ACRA

    val module = module {
        single<DynamicThemeHandler> { DynamicThemeHandler(get()) }
        single<StartupLogWriter> { StartupLogWriter.create() }
        single<LoggerFactory> {
            Logger.factory(
                    LogcatLogWriter.create(),
                    get<StartupLogWriter>()
            )
        }
        single<BugReporter> { BugReporter(logger("BugReporter"), context, lazy { get<StartupLogWriter>() }) }

        factory<Context> { context }
        factory(named("dateFormatOverride")) { "none" }
        factory<SharedPreferences> { PreferenceManager.getDefaultSharedPreferences(get()) }
        single<RxSharedPreferences> { RxSharedPreferences.create(get()) }
        factory<Single<Boolean>>(named("dateFormat")) {
            Single.fromCallable {
                get<String>(named("dateFormatOverride")).let { if (it == "none") null else it.toBoolean() }
                        ?: android.text.format.DateFormat.is24HourFormat(context)
            }
        }

        single {
            val prefs = get<RxSharedPreferences>()
            Prefs(get(named("dateFormat")),
                    prefs.getString("prealarm_duration", "30").asObservable().map { it.toInt() },
                    prefs.getString("snooze_duration", "10").asObservable().map { it.toInt() },
                    prefs.getString(Prefs.LIST_ROW_LAYOUT, Prefs.LIST_ROW_LAYOUT_COMPACT).asObservable(),
                    prefs.getString("auto_silence", "10").asObservable().map { it.toInt() })
        }

        single<Store> {
            Store(
                    alarmsSubject = BehaviorSubject.createDefault(ArrayList()),
                    next = BehaviorSubject.createDefault<Optional<Store.Next>>(Optional.absent()),
                    sets = PublishSubject.create(),
                    events = PublishSubject.create())
        }

        factory { get<Context>().getSystemService(Context.ALARM_SERVICE) as AlarmManager }
        single<AlarmSetter> { AlarmSetter.AlarmSetterImpl(logger("AlarmSetter"), get(), get()) }
        factory { Calendars { Calendar.getInstance() } }
        single<AlarmsScheduler> { AlarmsScheduler(get(), logger("AlarmsScheduler"), get(), get(), get()) }
        factory<IAlarmsScheduler> { get<AlarmsScheduler>() }
        single<AlarmCore.IStateNotifier> { AlarmStateNotifier(get()) }
        single<ContainerFactory> { PersistingContainerFactory(get(), get()) }
        factory { get<Context>().contentResolver }
        single<DatabaseQuery> { DatabaseQuery(get(), get()) }
        single<AlarmCoreFactory> { AlarmCoreFactory(logger("AlarmCore"), get(), get(), get(), get(), get()) }
        single<Alarms> { Alarms(get(), get(), get(), get(), logger("Alarms")) }
        factory<IAlarmsManager> { get<Alarms>() }
        single { ScheduledReceiver(get(), get(), get(), get()) }
        single { ToastPresenter(get(), get()) }
        single { AlertServicePusher(get(), get(), get(), logger("AlertServicePusher")) }
        single { BackgroundNotifications(get(), get(), get(), get(), get()) }
        factory<Wakelocks> { get<WakeLockManager>() }
        factory<Scheduler> { AndroidSchedulers.mainThread() }
        single<WakeLockManager> { WakeLockManager(logger("WakeLockManager"), get()) }
        factory { get<Context>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
        factory { get<Context>().getSystemService(Context.POWER_SERVICE) as PowerManager }
        factory { get<Context>().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
        factory { get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
        factory { get<Context>().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        factory { get<Context>().resources }

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
    }

    return startKoin {
        modules(module)
        modules(AlarmsListActivity.uiStoreModule)
    }.koin
}

fun overrideIs24hoursFormatOverride(is24hours: Boolean) {
    loadKoinModules(module = module(override = true) {
        factory(named("dateFormatOverride")) { is24hours.toString() }
    })
}