package com.better.alarm.services

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.better.alarm.logger.Logger
import com.better.alarm.platform.oreo
import com.better.alarm.platform.preOreo
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

/** Vibrates when told to. */
class VibrationPlugin(
    private val log: Logger,
    private val vibrator: Vibrator,
    private val fadeInTimeInMillis: Observable<Int>,
    private val scheduler: Scheduler,
    private val vibratePreference: Observable<Boolean>
) : AlertPlugin {
  private val vibratePattern: LongArray = longArrayOf(500, 500)
  private var disposable = Disposables.empty()

  override fun go(
      alarm: PluginAlarmData,
      prealarm: Boolean,
      targetVolume: Observable<TargetVolume>
  ): Disposable {
    disposable.dispose()
    val subscription =
        Observable.combineLatest(
                vibratePreference,
                targetVolume,
                BiFunction<Boolean, TargetVolume, TargetVolume> { isEnabled, volume ->
                  if (isEnabled) volume else TargetVolume.MUTED
                })
            .distinctUntilChanged()
            .switchMap { volume ->
              when (volume) {
                TargetVolume.MUTED -> Observable.just(0)
                TargetVolume.FADED_IN -> fadeInSlow(prealarm)
                TargetVolume.FADED_IN_FAST -> Observable.just(255)
              }
            }
            .distinctUntilChanged()
            .subscribe { amplitude ->
              if (amplitude != 0) {
                oreo {
                  log.debug { "Starting vibration with amplitude $amplitude" }
                  vibrator.vibrate(
                      VibrationEffect.createWaveform(vibratePattern, intArrayOf(0, amplitude), 0))
                }

                preOreo {
                  log.debug { "Starting vibration" }
                  @Suppress("DEPRECATION") // old target API
                  vibrator.vibrate(vibratePattern, 0)
                }
              } else {
                log.debug { "Canceling vibration" }
                vibrator.cancel()
              }
            }

    disposable = CompositeDisposable(subscription, Disposables.fromAction { vibrator.cancel() })

    return disposable
  }

  private val defaultAmplidute: Int
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          VibrationEffect.DEFAULT_AMPLITUDE
        } else {
          -1
        }

  /**
   * TODO try this val amplitudes = when { prealarm -> listOf(5, 20, 45, 80, 128) else -> listOf(10,
   * 40, 90, 160, 255) }
   */
  private fun fadeInSlow(prealarm: Boolean): Observable<Int> {

    return when {
      prealarm -> Observable.just(0)
      else ->
          fadeInTimeInMillis.firstOrError().flatMapObservable { fadeInTimeInMillis ->
            Observable.just(defaultAmplidute)
                .delay(fadeInTimeInMillis.toLong(), TimeUnit.MILLISECONDS, scheduler)
                .startWith(0)
                .doOnComplete { log.debug { "Completed vibration fade-in" } }
          }
    }
  }
}
