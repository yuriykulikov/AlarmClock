package com.better.alarm.ui.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.fragment.app.Fragment
import com.better.alarm.R
import com.better.alarm.data.Prefs
import com.better.alarm.domain.Store
import com.better.alarm.util.Optional
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import org.koin.android.ext.android.inject

/** @author Yuriy */
class InfoFragment : Fragment() {
  private val prefs: Prefs by inject()
  private val store: Store by inject()
  private var disposable: Disposable? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    val view = inflater.inflate(R.layout.info_fragment, container, false)
    val fadeIn = AnimationUtils.loadAnimation(requireActivity(), android.R.anim.fade_in)
    val fadeOut = AnimationUtils.loadAnimation(requireActivity(), android.R.anim.fade_out)
    val viewFactory =
        ViewSwitcher.ViewFactory {
          requireActivity().layoutInflater.inflate(R.layout.info_fragment_text, container, false)
              as TextView
        }

    val remainingTime: TextSwitcher =
        view.findViewById<TextSwitcher>(R.id.info_fragment_text_view_remaining_time).apply {
          setFactory(viewFactory)
          inAnimation = fadeIn
          outAnimation = fadeOut
        }

    disposable =
        Observable.combineLatest<Optional<Store.Next>, Long, String>(
                store.next(),
                now(),
                BiFunction { next, now ->
                  next.getOrNull()?.let {
                    computeTexts(
                        resources,
                        it,
                        now,
                        prefs.preAlarmDuration.value,
                    )
                  }
                      ?: ""
                })
            .distinctUntilChanged()
            .subscribe { remainingText -> remainingTime.setText(remainingText) }

    return view
  }

  override fun onDestroy() {
    super.onDestroy()
    disposable?.dispose()
  }

  private fun now(): Observable<Long> {
    return Observable.create<Long> { emitter ->
          val tickReceiver =
              object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                  emitter.onNext(System.currentTimeMillis())
                }
              }
          requireActivity().registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
          emitter.setCancellable { requireActivity().unregisterReceiver(tickReceiver) }
        }
        .startWith(System.currentTimeMillis())
  }
}

fun computeTexts(
    res: Resources,
    alarm: Store.Next,
    now: Long,
    prealarmDuration: Int,
): String {
  val nextMillis = alarm.nextNonPrealarmTime()

  val formatRemainingTimeString = formatRemainingTimeString(res, nextMillis, now)

  return if (alarm.isPrealarm) {
    val prealarmSummary = res.getString(R.string.info_fragment_prealarm_summary, prealarmDuration)
    """$formatRemainingTimeString
           $prealarmSummary"""
        .trimIndent()
    formatRemainingTimeString.plus("\n$prealarmSummary")
  } else {
    formatRemainingTimeString
  }
}

/** format "Alarm set for 2 days 7 hours and 53 minutes from now" */
private fun formatRemainingTimeString(res: Resources, timeInMillis: Long, now: Long): String {
  val delta = timeInMillis - now
  val days = delta / (1000 * 60 * 60) / 24
  val hours = delta / (1000 * 60 * 60) % 24
  val minutes = delta / (1000 * 60) % 60
  val daySeq =
      when (days) {
        0L -> ""
        1L -> res.getString(R.string.day)
        else -> res.getString(R.string.days, days.toString())
      }
  val minSeq =
      when (minutes) {
        0L -> ""
        1L -> res.getString(R.string.minute)
        else -> res.getString(R.string.minutes, minutes.toString())
      }
  val hourSeq =
      when (hours) {
        0L -> ""
        1L -> res.getString(R.string.hour)
        else -> res.getString(R.string.hours, hours.toString())
      }

  // bitmask
  val index = (if (days > 0) 1 else 0) or (if (hours > 0) 2 else 0) or (if (minutes > 0) 4 else 0)

  val formats = res.getStringArray(R.array.alarm_set_short)
  return String.format(formats[index], daySeq, hourSeq, minSeq)
}
