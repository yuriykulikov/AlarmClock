package com.better.alarm.background

import android.os.PowerManager
import android.telephony.TelephonyManager
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmValue
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

interface AlertPlugin {
    fun go(alarm: AlarmValue, inCall: Observable<Boolean>, volume: Observable<Float>): Disposable
}

sealed class Event {
    data class AlarmEvent(val id: Int, val actions: String = Intents.ALARM_ALERT_ACTION) : Event()
    data class PrealarmEvent(val id: Int, val actions: String = Intents.ALARM_PREALARM_ACTION) : Event()
    data class DismissEvent(val id: Int, val actions: String = Intents.ALARM_DISMISS_ACTION) : Event()
    data class SnoozedEvent(val id: Int, val actions: String = Intents.ALARM_SNOOZE_ACTION) : Event()
    data class CancelSnoozedEvent(val id: Int, val actions: String = Intents.ACTION_CANCEL_SNOOZE) : Event()
    data class Autosilenced(val id: Int, val actions: String = Intents.ACTION_SOUND_EXPIRED) : Event()

    data class MuteEvent(val actions: String = Intents.ACTION_MUTE) : Event()
    data class DemuteEvent(val actions: String = Intents.ACTION_DEMUTE) : Event()
}

/**
 * Listens to all kinds of events, vibrates, shows notifications and so on.
 * I want to make it testable, not sure yet how.
 */
class AlertService(
        private val log: Logger,
        pm: PowerManager,
        private val alarms: IAlarmsManager,
        private val callState: Observable<Int>,
        private val prealarmVolume: Observable<Int>,
        private val fadeInTimeInSeconds: Observable<Int>,
        private val scheduler: Scheduler,
        private val plugins: List<AlertPlugin>,
        private val goAway: () -> Unit
) {
    companion object {
        private const val FAST_FADE_IN_TIME = 5000
        private const val FADE_IN_STEPS = 100
        private const val IN_CALL_VOLUME = 0.125f
        private const val SILENT = 0f
    }

    private val wakeLock: PowerManager.WakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleAlarmClock:AlertService")
    private val targetVolume: BehaviorSubject<TargetVolume> = BehaviorSubject.createDefault(TargetVolume.MUTED)

    private enum class TargetVolume { MUTED, FADED_IN, FADED_IN_FAST }
    private enum class Type { NORMAL, PREALARM }
    private data class CallState(val initial: Boolean, val inCall: Boolean)

    private var disposable: Disposable = Disposables.empty()

    init {
        wakeLock.acquire(60 * 60_000)
    }

    fun onDestroy() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    var initilized = false
    fun onStartCommand(event: Event) {
        log.d("[AlertService] onStartCommand $event")
        when (event) {
            is Event.AlarmEvent -> soundAlarm(event.id, Type.NORMAL)
            is Event.PrealarmEvent -> soundAlarm(event.id, Type.PREALARM)
            is Event.MuteEvent -> targetVolume.onNext(TargetVolume.MUTED)
            is Event.DemuteEvent -> targetVolume.onNext(TargetVolume.FADED_IN_FAST)
            is Event.DismissEvent, is Event.SnoozedEvent, is Event.Autosilenced -> {
                if (!initilized) {
                    //TODO this is a nice hack
                    log.d("[AlertService] already disposed, kill the service $event")
                    goAway()
                } else {
                    log.d("[AlertService] Cleaning up after $event")
                    initilized = false
                    targetVolume.onNext(TargetVolume.MUTED)
                }
                disposable.dispose()
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    private fun soundAlarm(id: Int, type: Type) {
        disposable.dispose()
        initilized = true

        val alarm = alarms.getAlarm(id)

        val inCall = callState
                .map { it != TelephonyManager.CALL_STATE_IDLE }
                .distinctUntilChanged()

        targetVolume.onNext(TargetVolume.FADED_IN)

        val volume = Observable.combineLatest<TargetVolume, CallState, Observable<Float>>(
                targetVolume,
                inCall.zipWithIndex { callActive, index ->
                    CallState(index == 0, callActive)
                }, BiFunction { volume, state ->
            when {
                !state.initial && state.inCall -> Observable.just(SILENT)
                !state.initial && !state.inCall -> fadeIn(FAST_FADE_IN_TIME, type)
                volume == TargetVolume.MUTED -> Observable.just(SILENT)
                volume == TargetVolume.FADED_IN -> fadeInSlow(type)
                volume == TargetVolume.FADED_IN_FAST -> fadeIn(FAST_FADE_IN_TIME, type)
                else -> Observable.just(SILENT)
            }
        })
                .switchMap { it }
                .replay(1)
                .refCount()

        val disposables = plugins
                .map {
                    log.d("[AlertService] go $it")
                    it.go(alarm.edit(), inCall, volume)
                }

        disposable = CompositeDisposable(disposables)
    }

    private fun fadeInSlow(type: Type) = fadeInTimeInSeconds.firstOrError().flatMapObservable { fadeIn(it, type) }

    private fun fadeIn(time: Int, type: Type): Observable<Float> {
        val fadeInTime: Long = time.toLong()

        val fadeInStep: Long = fadeInTime / FADE_IN_STEPS

        val fadeIn = Observable.interval(fadeInStep, TimeUnit.MILLISECONDS, scheduler)
                .map { it * fadeInStep }
                .takeWhile { it <= fadeInTime }
                .map { elapsed -> elapsed.toFloat() / fadeInTime }
                .map { fraction -> fraction.squared() }
                .doOnComplete { log.d("Completed fade-in in $time milliseconds") }

        return Observable.combineLatest(
                observeVolume(type),
                fadeIn,
                BiFunction<Float, Float, Float> { targetVolume, fadePercentage -> fadePercentage * targetVolume })
    }

    /**
     * Gets 1f doe NORMAL and a fraction of 0.5f for PREALARM
     */
    private fun observeVolume(type: Type): Observable<Float> {
        val maxVolume = 11
        return if (type == Type.NORMAL) Observable.just(1f)
        else prealarmVolume.map {
            it
                    .plus(1)//0 is 1
                    .coerceAtMost(maxVolume)
                    .toFloat()
                    .div(maxVolume)
                    .div(2)
                    .apply { log.d("targetPrealarmVolume=$this") }
        }
    }

    private fun Float.squared() = this * this

    private fun <U, D> Observable<U>.zipWithIndex(function: (U, Int) -> D): Observable<D> {
        return zipWith(generateSequence(0) { it + 1 }.asIterable()) { next, index -> function.invoke(next, index) }
    }
}