package com.better.alarm.services

import android.app.Notification
import com.better.alarm.BuildConfig
import com.better.alarm.data.Alarmtone
import com.better.alarm.data.Prefs
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.notifications.NotificationsPlugin
import com.better.alarm.platform.Wakelocks
import com.better.alarm.platform.isOreo
import com.better.alarm.receivers.Intents
import com.better.alarm.util.modify
import com.better.alarm.util.requireValue
import com.better.alarm.util.subscribeIn
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.Calendar

interface AlertPlugin {
  fun go(
      alarm: PluginAlarmData,
      prealarm: Boolean,
      targetVolume: Observable<TargetVolume>
  ): Disposable
}

data class PluginAlarmData(val id: Int, val alarmtone: Alarmtone, val label: String)

enum class TargetVolume {
  MUTED,
  FADED_IN,
  FADED_IN_FAST
}

sealed class Event {
  data class NullEvent(val actions: String = "null") : Event()

  data class AlarmEvent(val id: Int, val actions: String = Intents.ALARM_ALERT_ACTION) : Event()

  data class PrealarmEvent(val id: Int, val actions: String = Intents.ALARM_PREALARM_ACTION) :
      Event()

  data class DismissEvent(val id: Int, val actions: String = Intents.ALARM_DISMISS_ACTION) :
      Event()

  data class SnoozedEvent(
      val id: Int,
      val calendar: Calendar,
      val actions: String = Intents.ALARM_SNOOZE_ACTION
  ) : Event()

  data class ShowSkip(val id: Int, val actions: String = Intents.ALARM_SHOW_SKIP) : Event()

  data class HideSkip(val id: Int, val actions: String = Intents.ALARM_REMOVE_SKIP) : Event()

  data class CancelSnoozedEvent(val id: Int, val actions: String = Intents.ACTION_CANCEL_SNOOZE) :
      Event()

  data class Autosilenced(val id: Int, val actions: String = Intents.ACTION_SOUND_EXPIRED) :
      Event()

  data class MuteEvent(val actions: String = Intents.ACTION_MUTE) : Event()

  data class DemuteEvent(val actions: String = Intents.ACTION_DEMUTE) : Event()
}

interface EnclosingService {
  fun handleUnwantedEvent()

  fun stopSelf()

  fun startForeground(id: Int, notification: Notification)
}

/** Listens to all kinds of events, vibrates, shows notifications and so on. */
class AlertService(
    private val log: Logger,
    private val wakelocks: Wakelocks,
    private val alarms: IAlarmsManager,
    private val inCall: Observable<Boolean>,
    private val plugins: List<AlertPlugin>,
    private val notifications: NotificationsPlugin,
    private val enclosing: EnclosingService,
    private val prefs: Prefs,
) {
  private val wantedVolume: BehaviorSubject<TargetVolume> =
      BehaviorSubject.createDefault(TargetVolume.MUTED)

  private enum class Type {
    NORMAL,
    PREALARM
  }

  private data class CallState(val initial: Boolean, val inCall: Boolean)

  private val disposable: CompositeDisposable = CompositeDisposable()
  private var soundAlarmDisposable: CompositeDisposable = CompositeDisposable()

  private val activeAlarms = BehaviorSubject.createDefault(mapOf<Int, Type>())
  private var nowShowing = emptyList<Int>()

  init {
    wakelocks.acquireServiceLock()
    activeAlarms
        .distinctUntilChanged()
        .skipWhile { it.isEmpty() }
        .subscribeIn(disposable) { active ->
          if (active.isNotEmpty()) {
            log.debug { "activeAlarms: $active" }
            playSound(active)
            showNotifications(active)
          } else {
            log.debug { "no alarms anymore, stopSelf()" }
            soundAlarmDisposable.dispose()
            wantedVolume.onNext(TargetVolume.MUTED)
            nowShowing
                .filter { !isOreo() || it != 0 } // not the foreground notification
                .forEach { notifications.cancel(it) }
            enclosing.stopSelf()
            disposable.dispose()
          }
        }
  }

  fun onDestroy() {
    log.debug { "onDestroy" }
    wakelocks.releaseServiceLock()
  }

  fun onStartCommand(event: Event): Boolean {
    log.debug { "onStartCommand $event" }

    return if (stateValid(event)) {
      when (event) {
        is Event.AlarmEvent -> soundAlarm(event.id, Type.NORMAL)
        is Event.PrealarmEvent -> soundAlarm(event.id, Type.PREALARM)
        is Event.MuteEvent -> wantedVolume.onNext(TargetVolume.MUTED)
        is Event.DemuteEvent -> wantedVolume.onNext(TargetVolume.FADED_IN_FAST)
        is Event.DismissEvent -> remove(event.id)
        is Event.SnoozedEvent -> remove(event.id)
        is Event.Autosilenced -> remove(event.id)
        else -> {
          check(!BuildConfig.DEBUG) { "Unexpected event: $event" }
        }
      }

      activeAlarms.requireValue().isNotEmpty()
    } else {
      enclosing.handleUnwantedEvent()
      false
    }
  }

  private fun stateValid(event: Event): Boolean {
    return when {
      activeAlarms.requireValue().isEmpty() -> {
        when (event) {
          is Event.AlarmEvent -> true
          is Event.PrealarmEvent -> true
          else -> {
            check(!BuildConfig.DEBUG) {
              "First event must be AlarmEvent or PrealarmEvent, but was $event"
            }
            false
          }
        }
      }
      disposable.isDisposed -> {
        check(!BuildConfig.DEBUG) { "Already disposed!" }
        false
      }
      else -> true
    }
  }

  private fun remove(id: Int) {
    activeAlarms.modify { minus(id) }
  }

  private fun soundAlarm(id: Int, type: Type) {
    activeAlarms.modify { plus(id to type) }
  }

  private fun showNotifications(active: Map<Int, Type>) {
    require(active.isNotEmpty())
    val toShow =
        active
            .mapNotNull { (id, _) -> alarms.getAlarm(id) }
            .map { alarm ->
              val alarmtone = alarm.alarmtone
              val label = alarm.labelOrDefault
              PluginAlarmData(alarm.id, alarmtone, label)
            }

    log.debug { "Show notifications: $toShow" }

    // Send the notification using the alarm id to easily identify the
    // correct notification.
    toShow.forEachIndexed { index, alarmData ->
      val startForeground = nowShowing.isEmpty() && index == 0
      log.debug { "notifications.show(${alarmData}, $index, $startForeground)" }
      notifications.show(alarmData, index, startForeground)
    }

    // cancel what we don't need anymore
    nowShowing.drop(toShow.size).forEach { notifications.cancel(it) }

    nowShowing = List(toShow.size) { index -> index }
  }

  private fun playSound(active: Map<Int, Type>) {
    require(active.isNotEmpty())
    val (id, type) = active.entries.last()
    play(id, type)
  }

  private fun play(id: Int, type: Type) {
    // new alarm - dispose all current signals
    soundAlarmDisposable.dispose()

    wantedVolume.onNext(TargetVolume.FADED_IN)

    val targetVolumeAccountingForInCallState: Observable<TargetVolume> =
        Observable.combineLatest(
            wantedVolume,
            inCall.zipWithIndex { callActive, index -> CallState(index == 0, callActive) }) {
                volume,
                callState ->
              when {
                !callState.initial && callState.inCall -> TargetVolume.MUTED
                !callState.initial && !callState.inCall -> TargetVolume.FADED_IN_FAST
                else -> volume
              }
            }
    val alarm = alarms.getAlarm(id)
    val alarmtone =
        if (alarm?.alarmtone is Alarmtone.Default) prefs.defaultRingtone()
        else alarm?.alarmtone ?: Alarmtone.Default
    val label = alarm?.labelOrDefault ?: ""
    val pluginDisposables =
        plugins.map {
          it.go(
              PluginAlarmData(id, alarmtone, label),
              prealarm = type == Type.PREALARM,
              targetVolume = targetVolumeAccountingForInCallState)
        }
    soundAlarmDisposable = CompositeDisposable(pluginDisposables)
  }

  private fun <U : Any, D : Any> Observable<U>.zipWithIndex(
      function: (U, Int) -> D
  ): Observable<D> {
    return zipWith(generateSequence(0) { it + 1 }.asIterable()) { next, index ->
      function.invoke(next, index)
    }
  }
}
