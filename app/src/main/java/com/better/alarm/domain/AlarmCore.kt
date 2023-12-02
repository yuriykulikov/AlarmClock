/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm.domain

import com.better.alarm.BuildConfig
import com.better.alarm.data.AlarmStore
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.Alarmtone
import com.better.alarm.data.CalendarType
import com.better.alarm.data.Prefs
import com.better.alarm.data.modify
import com.better.alarm.domain.statemachine.ComplexTransition
import com.better.alarm.domain.statemachine.State
import com.better.alarm.domain.statemachine.StateMachine
import com.better.alarm.logger.Logger
import com.better.alarm.receivers.Intents
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class Event {
  override fun toString(): String = javaClass.simpleName
}

data class Snooze(val hour: Int?, val minute: Int?) : Event()

data class Change(val value: AlarmValue) : Event()

object PrealarmDurationChanged : Event()

object Dismiss : Event()

object RequestSkip : Event()

object Fired : Event()

object Enable : Event()

object Disable : Event()

object Refresh : Event()

object TimeSet : Event()

object InexactFired : Event()

object Delete : Event()

object Create : Event()

/**
 * Alarm is a class which models a real word alarm. It is a simple state machine. External events
 * (e.g. user input [snooze] or [dismiss]) or timer events [onAlarmFired] trigger transitions. Alarm
 * notifies listeners when transitions happen by broadcasting Intents listed in [Intents], e.g.
 * [Intents.ALARM_PREALARM_ACTION] or [Intents.ALARM_DISMISS_ACTION]
 *
 * State and properties of the alarm are stored in the database and are updated every time when
 * changes to alarm happen.
 *
 * ```
 * @startuml
 * State DISABLED
 * State RESCHEDULE
 * State ENABLE
 * State ENABLED {
 * State PREALARM_SET
 * State SET
 * State FIRED
 * State PREALARM_FIRED
 * State SNOOZED
 * State PREALARM_SNOOZED
 * RESCHEDULE :Complex transitiontran
 * PREALARM_FIRED :timer
 * SNOOZED :timer
 * PREALARM_SNOOZED :timer
 * PREALARM_SET :timer
 * SET :timer
 *
 * DISABLED -down-> ENABLE :enable\nchange
 * ENABLED -up-> DISABLED :disable
 * ENABLED -up-> RESCHEDULE :dismiss
 * ENABLED -up-> ENABLE :change\nrefresh
 *
 * PREALARM_SET -down-> PREALARM_FIRED :fired
 * PREALARM_FIRED -down-> PREALARM_SNOOZED :snooze
 * PREALARM_SNOOZED -up-> FIRED
 * SET -down-> FIRED :fired
 * PREALARM_FIRED --right--> FIRED :fired
 * FIRED -down->  SNOOZED :snooze
 * SNOOZED -up-> FIRED :fired
 *
 * RESCHEDULE -up-> DISABLED :disabled
 *
 * RESCHEDULE -down-> PREALARM_SET :PA
 * RESCHEDULE -down-> SET :nPA
 * ENABLE -down-> PREALARM_SET :PA
 * ENABLE -down-> SET :nPA
 * }
 * @enduml
 * ```
 */
class AlarmCore(
    private val alarmStore: AlarmStore,
    private val log: Logger,
    private val mAlarmsScheduler: IAlarmsScheduler,
    private val broadcaster: IStateNotifier,
    private val prefs: Prefs,
    private val store: Store,
    private val calendars: Calendars,
    private val onDelete: (Int) -> Unit,
) : Alarm {
  private val stateMachine: StateMachine<Event>
  private val df: DateFormat
  private val container: AlarmValue
    get() = alarmStore.value

  private val preAlarmDuration: Observable<Int>
  private val snoozeDuration: Observable<Int>
  private val autoSilence: Observable<Int>

  private val disposable = CompositeDisposable()

  init {
    this.df = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY)

    this.preAlarmDuration = prefs.preAlarmDuration.observe()
    this.snoozeDuration = prefs.snoozeDuration.observe()
    this.autoSilence = prefs.autoSilence.observe()

    stateMachine = StateMachine("Alarm " + container.id, log)

    preAlarmDuration
        .skip(1) // not interested in the first update on startup
        .subscribe { stateMachine.sendEvent(PrealarmDurationChanged) }
        .let { disposable.add(it) }
  }

  /** Strategy used to notify other components about alarm state. */
  interface IStateNotifier {
    fun broadcastAlarmState(id: Int, action: String, calendar: Calendar? = null)

    fun broadcastAlarmState(id: Int, action: String)
  }

  private val disabledState = DisabledState()
  private val rescheduleTransition = RescheduleTransition()
  private val enableTransition = EnableTransition()
  private val enabledState = EnabledState()
  private val set = enabledState.SetState()
  private val normalSet = set.NormalSetState()
  private val preAlarmSet = set.PreAlarmSetState()
  private val skipping = enabledState.SkippingSetState()
  private val snoozed = enabledState.SnoozedState()
  private val preAlarmFired = enabledState.PreAlarmFiredState()
  private val preAlarmSnoozed = enabledState.PreAlarmSnoozedState()
  private val fired = enabledState.FiredState()
  private val deletedState = DeletedState()

  fun start() {
    stateMachine.start {
      val root = RootState()
      addState(root)
      addState(disabledState, root)
      addState(enabledState, root)
      addState(deletedState, root)
      addState(rescheduleTransition, root)
      addState(enableTransition, root)

      addState(set, enabledState)
      addState(preAlarmSet, set)
      addState(normalSet, set)
      addState(snoozed, enabledState)
      addState(skipping, enabledState)
      addState(preAlarmFired, enabledState)
      addState(fired, enabledState)
      addState(preAlarmSnoozed, enabledState)

      val initial = mutableTree.keys.firstOrNull { it.name == alarmStore.value.state }
      setInitialState(initial ?: disabledState)
    }

    updateListInStore()
  }

  private inner class RootState : AlarmState() {
    override fun onEvent(event: Event): Boolean {
      if (BuildConfig.DEBUG) {
        throw RuntimeException("Unhandled event: $event")
      } else {
        log.warning { "Unhandled event: $event" }
        return true
      }
    }
  }

  private inner class DeletedState : AlarmState() {
    override fun onEnter(reason: Event) {
      removeAlarm()
      removeFromStore()
      onDelete(id)
      alarmStore.delete()
      disposable.dispose()
    }
  }

  private inner class DisabledState : AlarmState() {
    override fun onEnter(reason: Event) {
      updateListInStore()
    }

    override fun onChange(alarmValue: AlarmValue) {
      writeChangeData(alarmValue)
      updateListInStore()
      if (container.isEnabled) {
        stateMachine.transitionTo(enableTransition)
      }
    }

    override fun onEnable() {
      alarmStore.modify { withIsEnabled(true) }
      stateMachine.transitionTo(enableTransition)
    }

    override fun onDelete() {
      stateMachine.transitionTo(deletedState)
    }

    override fun onRefresh() {
      // NOP
    }

    override fun onTimeSet() {
      // NOP
    }

    override fun onPreAlarmDurationChanged() {
      // NOP
    }

    override fun onSnooze(snooze: Snooze) {
      log.warning { "$this is in DisabledState" }
    }

    override fun onDismiss() {
      log.warning { "$this is in DisabledState" }
    }

    override fun onDisable() {
      log.warning { "$this is in DisabledState" }
    }
  }

  private inner class RescheduleTransition : ComplexTransition<Event>() {
    override fun performComplexTransition() {
      if (container.isRepeatSet) {
        if (container.isPrealarm && preAlarmDuration.blockingFirst() != -1) {
          stateMachine.transitionTo(preAlarmSet)
        } else {
          stateMachine.transitionTo(normalSet)
        }
      } else if (container.isDeleteAfterDismiss) {
        log.debug { "Delete after dismiss!" }
        stateMachine.transitionTo(deletedState)
      } else {
        log.debug { "Repeating is not set, disabling the alarm" }
        alarmStore.modify { withIsEnabled(false) }
        stateMachine.transitionTo(disabledState)
      }
    }
  }

  /**
   * Transition checks if preAlarm for the next alarm is in the future. This is required to prevent
   * the situation when user sets alarm in time which is less than preAlarm duration. In this case
   * main alarm should be set.
   */
  private inner class EnableTransition : ComplexTransition<Event>() {
    override fun performComplexTransition() {
      val preAlarm = calculateNextTime()
      val preAlarmMinutes = preAlarmDuration.blockingFirst()
      preAlarm.add(Calendar.MINUTE, -1 * preAlarmMinutes!!)
      if (container.isPrealarm && preAlarm.after(calendars.now()) && preAlarmMinutes != -1) {
        stateMachine.transitionTo(preAlarmSet)
      } else {
        stateMachine.transitionTo(normalSet)
      }
    }
  }

  /** Master state for all enabled states. Handles disable and delete */
  private inner class EnabledState : AlarmState() {
    override fun onEnter(reason: Event) {
      if (!container.isEnabled) {
        // if due to an exception during development an alarm is not enabled but the state is
        alarmStore.modify { withIsEnabled(true) }
      }
      updateListInStore()
    }

    override fun onChange(alarmValue: AlarmValue) {
      writeChangeData(alarmValue)
      updateListInStore()
      if (container.isEnabled) {
        stateMachine.transitionTo(enableTransition)
      } else {
        stateMachine.transitionTo(disabledState)
      }
    }

    override fun onDismiss() {
      stateMachine.transitionTo(rescheduleTransition)
    }

    override fun onDisable() {
      alarmStore.modify { withIsEnabled(false) }
      stateMachine.transitionTo(disabledState)
    }

    override fun onRefresh() {
      stateMachine.transitionTo(enableTransition)
    }

    override fun onTimeSet() {
      // nothing to do
    }

    override fun onDelete() {
      stateMachine.transitionTo(deletedState)
    }

    inner class SetState : AlarmState() {

      inner class NormalSetState : AlarmState() {
        override fun onEnter(reason: Event) {
          broadcastAlarmSetWithNormalTime(
              calculateNextTime().timeInMillis, reason.isUserInteraction())
        }

        override fun onResume() {
          val nextTime = calculateNextTime()
          setAlarm(nextTime, CalendarType.NORMAL)
          showSkipNotification(nextTime)
        }

        override fun onFired() {
          stateMachine.transitionTo(fired)
        }

        override fun onPreAlarmDurationChanged() {
          stateMachine.transitionTo(enableTransition)
        }
      }

      inner class PreAlarmSetState : AlarmState() {
        override fun onEnter(reason: Event) {
          broadcastAlarmSetWithNormalTime(
              calculateNextPrealarmTime().timeInMillis, reason.isUserInteraction())
        }

        override fun onResume() {
          val nextPrealarmTime = calculateNextPrealarmTime()
          if (nextPrealarmTime.after(calendars.now())) {
            setAlarm(nextPrealarmTime, CalendarType.PREALARM)
            showSkipNotification(nextPrealarmTime)
          } else {
            // TODO this should never happen
            log.e("PreAlarm is still in the past!")
            stateMachine.transitionTo(if (container.isEnabled) enableTransition else disabledState)
          }
        }

        private fun calculateNextPrealarmTime(): Calendar {
          return calculateNextTime().apply {
            add(Calendar.MINUTE, -1 * preAlarmDuration.blockingFirst())
            // since prealarm is before main alarm, it can be already in the
            // past, so it has to be adjusted.
            advanceCalendar()
          }
        }

        override fun onFired() {
          stateMachine.transitionTo(preAlarmFired)
        }

        override fun onPreAlarmDurationChanged() {
          stateMachine.transitionTo(enableTransition)
        }
      }

      private fun showSkipNotification(c: Calendar) {
        val skipTime = prefs.skipDuration.value
        val toShowSkip = (c.clone() as Calendar).apply { add(Calendar.MINUTE, -skipTime) }
        when {
          skipTime == -1 -> {
            // switched off
          }
          toShowSkip.after(calendars.now()) -> {
            mAlarmsScheduler.setInexactAlarm(id, toShowSkip)
          }
          else -> {
            log.debug { "Alarm $id is due in less than $skipTime minutes - show notification" }
            broadcastAlarmState(Intents.ALARM_SHOW_SKIP)
          }
        }
      }

      override fun onEnter(reason: Event) {
        updateListInStore()
      }

      override fun onInexactFired() {
        broadcastAlarmState(Intents.ALARM_SHOW_SKIP)
      }

      override fun onRequestSkip() {
        when {
          container.isRepeatSet -> stateMachine.transitionTo(skipping)
          else -> stateMachine.transitionTo(rescheduleTransition)
        }
      }

      override fun exit(reason: Event?) {
        broadcastAlarmState(Intents.ALARM_REMOVE_SKIP)
        if (!alarmWillBeRescheduled(reason)) {
          removeAlarm()
        }
        mAlarmsScheduler.removeInexactAlarm(id)
      }

      override fun onTimeSet() {
        stateMachine.transitionTo(enableTransition)
      }
    }

    inner class SkippingSetState : AlarmState() {
      override fun onEnter(reason: Event) {
        updateListInStore()
      }

      override fun onResume() {
        val nextTime = calculateNextTime()
        if (nextTime.after(calendars.now())) {
          mAlarmsScheduler.setInexactAlarm(id, nextTime)

          val nextAfterSkip = calculateNextTime()
          nextAfterSkip.add(Calendar.DAY_OF_YEAR, 1)
          val addDays = container.daysOfWeek.getNextAlarm(nextAfterSkip)
          if (addDays > 0) {
            nextAfterSkip.add(Calendar.DAY_OF_WEEK, addDays)
          }

          // this will never (hopefully) fire, but in order to display it everywhere...
          setAlarm(nextAfterSkip, CalendarType.NORMAL)
        } else {
          stateMachine.transitionTo(if (container.isEnabled) enableTransition else disabledState)
        }
      }

      override fun onFired() {
        // yeah should never happen
        stateMachine.transitionTo(fired)
      }

      override fun onInexactFired() {
        stateMachine.transitionTo(enableTransition)
      }

      override fun exit(reason: Event?) {
        mAlarmsScheduler.removeInexactAlarm(id)
        // avoids flicker of the icon
        if (reason !is RequestSkip) {
          removeAlarm()
        }
      }
    }

    /** handles both snoozed and main for now */
    inner class FiredState : AlarmState() {
      override fun onEnter(reason: Event) {
        broadcastAlarmState(Intents.ALARM_ALERT_ACTION)
        val autoSilenceMinutes = autoSilence.blockingFirst()
        if (autoSilenceMinutes > 0) {
          // -1 means OFF
          val nextTime = calendars.now()
          nextTime.add(Calendar.MINUTE, autoSilenceMinutes)
          setAlarm(nextTime, CalendarType.AUTOSILENCE)
        }
      }

      override fun onFired() {
        broadcastAlarmState(Intents.ACTION_SOUND_EXPIRED)
        // this is like a dismiss but we show an additional notification
        stateMachine.transitionTo(rescheduleTransition)
      }

      override fun onSnooze(snooze: Snooze) {
        stateMachine.transitionTo(snoozed)
      }

      override fun exit(reason: Event?) {
        broadcastAlarmState(Intents.ALARM_DISMISS_ACTION)
        removeAlarm()
      }
    }

    inner class PreAlarmFiredState : AlarmState() {
      override fun onEnter(reason: Event) {
        broadcastAlarmState(Intents.ALARM_PREALARM_ACTION)
        setAlarm(calculateNextTime(), CalendarType.NORMAL)
      }

      override fun onFired() {
        stateMachine.transitionTo(fired)
      }

      override fun onSnooze(snooze: Snooze) {
        if (snooze.minute != null) {
          // snooze to time with prealarm -> go to snoozed
          stateMachine.transitionTo(snoozed)
        } else {
          stateMachine.transitionTo(preAlarmSnoozed)
        }
      }

      override fun exit(reason: Event?) {
        removeAlarm()
        if (reason !is Fired) {
          // do not dismiss because we will immediately fire another event at the service
          broadcastAlarmState(Intents.ALARM_DISMISS_ACTION)
        }
      }
    }

    inner class SnoozedState : AlarmState() {
      internal var nextTime: Calendar? = null

      private fun nextRegualarSnoozeCalendar(): Calendar {
        val nextTime = calendars.now()
        val snoozeMinutes = snoozeDuration.blockingFirst()
        nextTime.add(Calendar.MINUTE, snoozeMinutes)
        return nextTime
      }

      override fun onEnter(reason: Event) {
        val now = calendars.now()
        nextTime =
            when {
              reason is Snooze && reason.hour != null && reason.minute != null -> {
                log.debug { "Enter snooze $reason" }
                val customTime =
                    calendars.now().apply {
                      set(Calendar.HOUR_OF_DAY, reason.hour)
                      set(Calendar.MINUTE, reason.minute)
                    }
                when {
                  customTime.after(now) -> customTime
                  else -> nextRegualarSnoozeCalendar()
                }
              }
              else -> nextRegualarSnoozeCalendar()
            }
        // change the next time to show notification properly
        alarmStore.modify { withNextTime(nextTime) }
        // updateListInStore()
        broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION, nextTime) // Yar. 18.08
      }

      override fun onResume() {
        // alarm was started again while snoozed alarm was hanging in there
        if (nextTime == null) {
          nextTime = nextRegualarSnoozeCalendar()
        }

        setAlarm(nextTime!!, CalendarType.NORMAL)
      }

      override fun onFired() {
        stateMachine.transitionTo(fired)
      }

      override fun onSnooze(snooze: Snooze) {
        // reschedule from notification
        enter(snooze)
        onResume()
      }

      override fun exit(reason: Event?) {
        removeAlarm()
        broadcastAlarmState(Intents.ACTION_CANCEL_SNOOZE)
      }
    }

    inner class PreAlarmSnoozedState : AlarmState() {
      override fun onEnter(reason: Event) {
        // Yar 18.08: setAlarm -> resume; setAlarm(calculateNextTime(), CalendarType.NORMAL);
        broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION, calculateNextTime())
      }

      override fun onFired() {
        stateMachine.transitionTo(fired)
      }

      override fun onSnooze(snooze: Snooze) {
        // reschedule from notification
        stateMachine.transitionTo(snoozed)
      }

      override fun exit(reason: Event?) {
        removeAlarm()
        broadcastAlarmState(Intents.ACTION_CANCEL_SNOOZE)
      }

      override fun onResume() {
        setAlarm(calculateNextTime(), CalendarType.NORMAL)
      }
    }
  }

  private fun broadcastAlarmState(action: String, calendar: Calendar? = null) {
    log.trace { "[Alarm ${container.id}] broadcastAlarmState($action)" }
    when {
      calendar != null -> broadcaster.broadcastAlarmState(container.id, action, calendar)
      else -> broadcaster.broadcastAlarmState(container.id, action)
    }
    updateListInStore()
  }

  private fun broadcastAlarmSetWithNormalTime(millis: Long, fromUserInteraction: Boolean) {
    store.sets().onNext(Store.AlarmSet(container, millis, fromUserInteraction))
    updateListInStore()
  }

  private fun setAlarm(calendar: Calendar, calendarType: CalendarType) {
    mAlarmsScheduler.setAlarm(container.id, calendarType, calendar, container)
    alarmStore.modify { withNextTime(calendar) }
  }

  private fun removeAlarm() {
    mAlarmsScheduler.removeAlarm(container.id)
  }

  private fun removeFromStore() {
    store
        .alarms()
        .firstOrError()
        .subscribe { alarmValues ->
          val withoutId = removeWithId(alarmValues, container.id)
          store.alarmsSubject().onNext(withoutId)
        }
        .let { disposable.add(it) }
  }

  private fun writeChangeData(data: AlarmValue) {
    alarmStore.modify { withChangeData(data) }
  }

  private fun calculateNextTime(): Calendar {
    val date = container.date
    return if (date != null) {
      calendars.now().apply {
        timeInMillis = date.timeInMillis
        set(Calendar.HOUR_OF_DAY, container.hour)
        set(Calendar.MINUTE, container.minutes)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    } else {
      calendars.now().apply {
        set(Calendar.HOUR_OF_DAY, container.hour)
        set(Calendar.MINUTE, container.minutes)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        advanceCalendar()
      }
    }
  }

  private fun Calendar.advanceCalendar() {
    val now = calendars.now()
    // if alarm is behind current time, advance one day
    if (before(now)) {
      add(Calendar.DAY_OF_YEAR, 1)
    }
    val addDays = container.daysOfWeek.getNextAlarm(this)
    if (addDays > 0) {
      add(Calendar.DAY_OF_WEEK, addDays)
    }
  }

  private fun alarmWillBeRescheduled(reason: Event?): Boolean {
    return reason is Change && reason.value.isEnabled
  }

  private abstract inner class AlarmState : State<Event>() {
    private var handled: Boolean = false

    final override fun enter(reason: Event?) {
      when (reason) {
        null -> onResume()
        else -> {
          if (this !is EnabledState && this !is ComplexTransition<*>) {
            alarmStore.modify { withState(name) }
          }
          onEnter(reason)
          onResume()
        }
      }
    }

    open fun onEnter(reason: Event) {}

    open fun onResume() {}

    override fun exit(reason: Event?) {}

    override fun onEvent(event: Event): Boolean {
      handled = true
      when (event) {
        Create -> Unit
        is Enable -> onEnable()
        is Disable -> onDisable()
        is Snooze -> onSnooze(event)
        is Dismiss -> onDismiss()
        is Change -> onChange(event.value)
        is Fired -> onFired()
        is PrealarmDurationChanged -> onPreAlarmDurationChanged()
        is Refresh -> onRefresh()
        is TimeSet -> onTimeSet()
        is InexactFired -> onInexactFired()
        is RequestSkip -> onRequestSkip()
        is Delete -> onDelete()
        Create -> {
          check(!BuildConfig.DEBUG) { "Unexpected $event" }
        }
      }
      return handled
    }

    protected fun markNotHandled() {
      handled = false
    }

    protected open fun onEnable() = markNotHandled()

    protected open fun onDisable() = markNotHandled()

    protected open fun onSnooze(snooze: Snooze) = markNotHandled()

    protected open fun onDismiss() = markNotHandled()

    protected open fun onChange(alarmValue: AlarmValue) = markNotHandled()

    protected open fun onFired() = markNotHandled()

    protected open fun onInexactFired() = markNotHandled()

    protected open fun onRequestSkip() = markNotHandled()

    protected open fun onPreAlarmDurationChanged() = markNotHandled()

    protected open fun onRefresh() = markNotHandled()

    protected open fun onTimeSet() = markNotHandled()

    protected open fun onDelete() = markNotHandled()
  }

  private fun updateListInStore() {
    store
        .alarms()
        .take(1)
        .subscribe { alarmValues ->
          val copy = addOrReplace(alarmValues, container)
          store.alarmsSubject().onNext(copy)
        }
        .let { disposable.add(it) }
  }

  fun onAlarmFired() {
    stateMachine.sendEvent(Fired)
  }

  fun onInexactAlarmFired() {
    stateMachine.sendEvent(InexactFired)
  }

  override fun requestSkip() {
    stateMachine.sendEvent(RequestSkip)
  }

  override fun isSkipping(): Boolean = container.skipping

  fun refresh() {
    stateMachine.sendEvent(Refresh)
  }

  fun onTimeSet() {
    stateMachine.sendEvent(TimeSet)
  }

  fun change(data: AlarmValue) {
    stateMachine.sendEvent(Change(data))
  }

  override fun enable(enable: Boolean) {
    stateMachine.sendEvent(if (enable) Enable else Disable)
  }

  override fun snooze() {
    stateMachine.sendEvent(Snooze(null, null))
  }

  override fun snooze(hourOfDay: Int, minute: Int) {
    stateMachine.sendEvent(Snooze(hourOfDay, minute))
  }

  override fun dismiss() {
    stateMachine.sendEvent(Dismiss)
  }

  override fun delete() {
    stateMachine.sendEvent(Delete)
  }

  // ++++++++++++++++++++++++++++++++++++++++++++++++++++++
  // ++++++ getters for GUI +++++++++++++++++++++++++++++++
  // ++++++++++++++++++++++++++++++++++++++++++++++++++++++

  override val id: Int
    get() = container.id

  override val labelOrDefault: String
    get() = container.label

  override val alarmtone: Alarmtone
    get() = container.alarmtone

  override fun toString(): String {
    return "AlarmCore ${container.id} $stateMachine on ${df.format(container.nextTime.time)}"
  }

  override fun edit(func: AlarmValue.() -> AlarmValue) {
    val prev = container
    change(
        func(prev).also {
          require(prev.id == it.id)
          require(prev.state == it.state)
          require(prev.nextTime == it.nextTime)
        })
  }

  override val data: AlarmValue
    get() = container
}

private fun Event.isUserInteraction(): Boolean {
  return when (this) {
    is Enable -> true
    is Change -> true
    Create -> true
    Delete -> true
    Disable -> true
    Dismiss -> true
    RequestSkip -> true
    is Snooze -> true
    Fired -> false
    InexactFired -> false
    PrealarmDurationChanged -> false
    Refresh -> false
    TimeSet -> false
  }
}
