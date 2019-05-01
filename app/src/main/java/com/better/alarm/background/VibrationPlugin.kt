package com.better.alarm.background

import android.os.Vibrator
import com.better.alarm.logger.Logger
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction

/**
 * Vibrates when told to.
 */
class VibrationPlugin(
        private val log: Logger,
        private val vibrator: Vibrator,
        private val vibratePreference: Observable<Boolean>
) : AlertPlugin {
    private val vibratePattern: LongArray = longArrayOf(500, 500)
    private var disposable = Disposables.empty()

    override fun go(alarm: PluginAlarmData, prealarm: Boolean, targetVolume: Observable<TargetVolume>): Disposable {
        disposable.dispose()
        // TODO fade in vibration

        val subscription = Observable
                .combineLatest(vibratePreference, targetVolume, BiFunction<Boolean, TargetVolume, Boolean> { isEnabled, volume ->
                    val shouldVibrate = volume == TargetVolume.FADED_IN || volume == TargetVolume.FADED_IN_FAST

                    isEnabled && shouldVibrate
                })
                .subscribe { isEnabled ->
                    if (isEnabled) {
                        log.d("Starting vibration")
                        vibrator.vibrate(vibratePattern, 0)
                    } else {
                        log.d("Canceling vibration")
                        vibrator.cancel()
                    }
                }

        disposable = CompositeDisposable(subscription, Disposables.fromAction {
            vibrator.cancel()
        })

        return disposable
    }
}
