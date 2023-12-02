package com.better.alarm.services

import com.better.alarm.R
import com.better.alarm.data.Alarmtone
import com.better.alarm.data.ringtoneManagerUri
import com.better.alarm.logger.Logger
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

interface Player {
  fun startAlarm()

  fun setDataSourceFromResource(res: Int)

  fun setPerceivedVolume(perceived: Float)

  /** Stops alarm audio */
  fun stop()

  fun reset()

  fun setDataSource(uri: String)
}

/** Plays sound when told to. Performs a fade-in. */
class KlaxonPlugin(
    private val log: Logger,
    private val playerFactory: () -> Player,
    private val prealarmVolume: Observable<Int>,
    private val fadeInTimeInMillis: Observable<Int>,
    private val inCall: Observable<Boolean>,
    private val scheduler: Scheduler
) : AlertPlugin {
  companion object {
    private const val FAST_FADE_IN_TIME = 5000
    private const val FADE_IN_STEPS = 100
    private const val IN_CALL_VOLUME = 0.125f
    private const val SILENT = 0f
  }

  private var player: Player? = null
  private var disposable = Disposables.empty()

  private fun fadeInSlow(prealarm: Boolean) =
      fadeInTimeInMillis.firstOrError().flatMapObservable { fadeIn(it, prealarm) }

  override fun go(
      alarm: PluginAlarmData,
      prealarm: Boolean,
      targetVolume: Observable<TargetVolume>
  ): Disposable {
    disposable.dispose()
    player = playerFactory()

    val callSub =
        inCall.subscribe { inCall ->
          // Check if we are in a call. If we are, use the in-call alarm
          // resource at a low targetVolume to not disrupt the call.
          when {
            inCall -> playInCallAlarm()
            else -> playAlarm(alarm)
          }
        }

    val volume: Observable<Float> =
        targetVolume.switchMap { vol ->
          when (vol) {
            TargetVolume.MUTED -> Observable.just(SILENT)
            TargetVolume.FADED_IN -> fadeInSlow(prealarm)
            TargetVolume.FADED_IN_FAST -> fadeIn(FAST_FADE_IN_TIME, prealarm)
          }
        }

    log.debug { "[KlaxonPlugin] go ${alarm.alarmtone} (prealarm: $prealarm)" }
    val volumeSub = volume.subscribe { currentVolume -> player?.setPerceivedVolume(currentVolume) }

    disposable =
        CompositeDisposable(callSub, volumeSub, Disposables.fromAction { player?.stopAndCleanup() })
    return disposable
  }

  private fun playAlarm(alarm: PluginAlarmData) {
    if (alarm.alarmtone !is Alarmtone.Silent) {
      player?.run {
        try {
          setPerceivedVolume(0f)
          setDataSource(requireNotNull(alarm.alarmtone.ringtoneManagerUri()))
          startAlarm()
        } catch (ex: Exception) {
          log.warning { "Using the fallback ringtone, because ringtoneManagerUri() failed: $ex" }
          // The alert may be on the sd card which could be busy right
          // now. Use the fallback ringtone.
          // Must reset the media player to clear the error state.
          reset()
          setDataSourceFromResource(R.raw.fallbackring)
          startAlarm()
        }
      }
    }
  }

  private fun playInCallAlarm() {
    log.debug { "Using the in-call alarm" }
    player?.run {
      reset()
      setDataSourceFromResource(R.raw.in_call_alarm)
      startAlarm()
    }
  }

  /** Gets 1f doe NORMAL and a fraction of 0.5f for PREALARM */
  private fun observeVolume(prealarm: Boolean): Observable<Float> {
    val maxVolume = 11
    return if (!prealarm) Observable.just(1f)
    else
        prealarmVolume.map {
          it.plus(1) // 0 is 1
              .coerceAtMost(maxVolume)
              .toFloat()
              .div(maxVolume)
              .div(2)
              .apply { log.debug { "targetPrealarmVolume=$this" } }
        }
  }

  private fun fadeIn(time: Int, prealarm: Boolean): Observable<Float> {
    val fadeInTime: Long = time.toLong()

    val fadeInStep: Long = fadeInTime / FADE_IN_STEPS

    val fadeIn =
        Observable.interval(fadeInStep, TimeUnit.MILLISECONDS, scheduler)
            .map { it * fadeInStep }
            .takeWhile { it <= fadeInTime }
            .map { elapsed -> elapsed.toFloat() / fadeInTime }
            .map { fraction -> fraction.squared() }
            .doOnComplete { log.debug { "Completed fade-in in $time milliseconds" } }

    return Observable.combineLatest(
        observeVolume(prealarm),
        fadeIn,
        BiFunction<Float, Float, Float> { targetVolume, fadePercentage ->
          fadePercentage * targetVolume
        })
  }

  /** Stops alarm audio */
  private fun Player.stopAndCleanup() {
    log.debug { "stopping media player" }
    try {
      stop()
    } finally {
      player = null
    }
  }

  private fun Float.squared() = this * this
}
