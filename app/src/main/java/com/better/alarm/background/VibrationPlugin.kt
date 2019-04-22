package com.better.alarm.background

import android.os.Vibrator
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Function3

class VibrationPlugin(
        private val log: Logger,
        private val mVibrator: Vibrator,
        private val vibrate: Observable<Boolean>
) : AlertPlugin {
    private val sVibratePattern: LongArray = longArrayOf(500, 500)
    private var subscription = Disposables.disposed()

    override fun go(alarm: AlarmValue, inCall: Observable<Boolean>, volume: Observable<Float>): Disposable {
        subscription = Observable
                .combineLatest<Boolean, Boolean, Float, Boolean>(
                        vibrate,
                        inCall,
                        volume.doOnNext { println("Volmue: $it") },
                        Function3 { isEnabled, isInCall, currentVolume ->
                            isEnabled && !isInCall && currentVolume > 0.75f
                        })
                .distinctUntilChanged()
                .subscribe { vibrate ->
                    if (vibrate) {
                        log.d("Starting vibration")
                        mVibrator.vibrate(sVibratePattern, 0)
                    } else {
                        log.d("Canceling vibration")
                        mVibrator.cancel()
                    }
                }

        return CompositeDisposable(subscription, Disposables.fromAction {
            mVibrator.cancel()
        })
    }
}
