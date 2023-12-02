package com.better.alarm.bootstrap

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import com.better.alarm.data.AlarmsRepository
import com.better.alarm.data.DataStoreAlarmsRepository
import com.better.alarm.data.DatastoreMigration
import com.better.alarm.data.Prefs
import com.better.alarm.data.contentprovider.DatabaseQuery
import com.better.alarm.data.contentprovider.SQLiteDatabaseQuery
import com.better.alarm.data.stores.SharedRxDataStoreFactory
import com.better.alarm.domain.AlarmCore
import com.better.alarm.domain.AlarmSetter
import com.better.alarm.domain.AlarmStateNotifier
import com.better.alarm.domain.Alarms
import com.better.alarm.domain.AlarmsScheduler
import com.better.alarm.domain.Calendars
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.IAlarmsScheduler
import com.better.alarm.domain.Store
import com.better.alarm.logger.BugReporter
import com.better.alarm.logger.Logger
import com.better.alarm.logger.LoggerFactory
import com.better.alarm.logger.loggerModule
import com.better.alarm.notifications.BackgroundNotifications
import com.better.alarm.platform.WakeLockManager
import com.better.alarm.platform.Wakelocks
import com.better.alarm.receivers.ScheduledReceiver
import com.better.alarm.services.AlertServicePusher
import com.better.alarm.services.KlaxonPlugin
import com.better.alarm.services.PlayerWrapper
import com.better.alarm.ui.details.AlarmDetailsViewModel
import com.better.alarm.ui.list.ListViewModel
import com.better.alarm.ui.main.MainViewModel
import com.better.alarm.ui.state.BackPresses
import com.better.alarm.ui.state.UiStore
import com.better.alarm.ui.themes.DynamicThemeHandler
import com.better.alarm.ui.toast.ToastPresenter
import com.better.alarm.util.Optional
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.binds
import org.koin.dsl.module

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
    single<BugReporter> { BugReporter(logger("BugReporter"), context) }
    factory<Context> { context }
    factory(named("dateFormatOverride")) { "none" }
    factory<Single<Boolean>>(named("dateFormat")) {
      Single.fromCallable {
        get<String>(named("dateFormatOverride")).let { if (it == "none") null else it.toBoolean() }
            ?: DateFormat.is24HourFormat(context)
      }
    }

    single<Prefs> {
      val factory = SharedRxDataStoreFactory.create(get(), logger("preferences"))
      Prefs.create(get(named("dateFormat")), factory)
    }

    single<Store> {
      Store(
          alarmsSubject = BehaviorSubject.createDefault(ArrayList()),
          next = BehaviorSubject.createDefault<Optional<Store.Next>>(Optional.absent()),
          sets = PublishSubject.create(),
          events = PublishSubject.create())
    }

    viewModelOf(::MainViewModel)
    viewModelOf(::AlarmDetailsViewModel)
    viewModelOf(::ListViewModel)
    single { UiStore() }
    single { BackPresses() }

    factory { get<Context>().getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    single<AlarmSetter> { AlarmSetter.AlarmSetterImpl(logger("AlarmSetter"), get(), get()) }
    factory { Calendars { Calendar.getInstance() } }
    single<AlarmsScheduler> {
      AlarmsScheduler(get(), logger("AlarmsScheduler"), get(), get(), get())
    }
    factory<IAlarmsScheduler> { get<AlarmsScheduler>() }
    single<AlarmCore.IStateNotifier> { AlarmStateNotifier(get()) }
    single<AlarmsRepository> {
      DataStoreAlarmsRepository.createBlocking(
          datastoreDir = get(named("datastore")),
          logger = logger("DataStoreAlarmsRepository"),
          ioScope = CoroutineScope(Dispatchers.IO),
      )
    }
    single(named("datastore")) { File(get<Context>().applicationContext.filesDir, "datastore") }
    factory { get<Context>().contentResolver }
    single<DatabaseQuery> { SQLiteDatabaseQuery(get()) }
    single { Alarms(get(), get(), get(), get(), get(), get(), logger("Alarms"), get()) } binds
        arrayOf(IAlarmsManager::class, DatastoreMigration::class)
    single { ScheduledReceiver(get(), get(), get(), get()) }
    single { ToastPresenter(get(), get()) }
    single { AlertServicePusher(get(), get(), get(), logger("AlertServicePusher")) }
    single { BackgroundNotifications(get(), get(), get(), get(), get()) }
    factory<Wakelocks> { get<WakeLockManager>() }
    factory<Scheduler> { AndroidSchedulers.mainThread() }
    single { WakeLockManager(logger("WakeLockManager"), get()) }
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
          prealarmVolume = get<Prefs>().preAlarmVolume.observe(),
          fadeInTimeInMillis = Observable.just(100),
          inCall = Observable.just(false),
          scheduler = get(),
      )
    }
  }

  return startKoin {
        allowOverride(true)
        modules(module)
        modules(loggerModule())
      }
      .koin
}

fun overrideIs24hoursFormatOverride(is24hours: Boolean) {
  loadKoinModules(module { factory(named("dateFormatOverride")) { is24hours.toString() } })
}
