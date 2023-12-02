/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.better.alarm.ui.settings

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.better.alarm.R
import com.better.alarm.bootstrap.globalInject
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.Prefs
import com.better.alarm.data.Prefs.Companion.MAX_PREALARM_VOLUME
import com.better.alarm.services.KlaxonPlugin
import com.better.alarm.services.PluginAlarmData
import com.better.alarm.services.TargetVolume
import com.better.alarm.util.subscribeIn
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import org.koin.core.qualifier.named

class VolumePreference(mContext: Context, attrs: AttributeSet) : Preference(mContext, attrs) {
  private val log by globalLogger("VolumePreference")
  private val rxPrefs: Prefs by globalInject()
  private val am: AudioManager by globalInject()
  private val alarmTypeUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

  private var ringtoneSummary: TextView? = null
  private var fragmentScope = CompositeDisposable()
  private var prealarmSampleDisposable = Disposables.empty()
  private var sampleDisposable = Disposables.empty()

  private val klaxon: KlaxonPlugin by globalInject(named("volumePreferenceDemo"))

  var showPicker: (() -> Unit)? = null
  var ringtoneTitle: CharSequence? = null
    set(value) {
      field = value
      ringtoneSummary?.text = value
    }

  init {
    layoutResource = R.layout.seekbar_dialog
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    bindPrealarmSeekBar(holder.findById(R.id.seekbar_dialog_seekbar_prealarm_volume))
    bindAudioManagerVolume(holder.findById(R.id.seekbar_dialog_seekbar_master_volume))
    ringtoneSummary = holder.findById(R.id.settings_ringtone_summary)
    ringtoneTitle?.let { ringtoneSummary?.text = it }
    holder.findById<View>(R.id.settings_ringtone).setOnClickListener { showPicker?.invoke() }
  }

  /** Extension function to avoid crashes cause by an incorrect cast (see history) */
  private fun <T> PreferenceViewHolder.findById(id: Int): T = findViewById(id) as T

  override fun onDetached() {
    super.onDetached()
    fragmentScope.dispose()
  }

  /** Called from [com.better.alarm.presenter.SettingsFragment.onPause] */
  fun onPause() {
    stopMasterSample()
    stopPrealarmSample()
  }

  /** This function controls playback using AudioManager */
  private fun bindAudioManagerVolume(seekBar: SeekBar) {
    seekBar.progress = am.getStreamVolume(AudioManager.STREAM_ALARM)
    seekBar.max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)

    val masterVolumeProgress = seekBar.attachProgressChangedListener()

    masterVolumeProgress.subscribeIn(fragmentScope) { progress ->
      stopPrealarmSample()
      stopMasterSample()
      am.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0)
      sampleDisposable =
          klaxon.go(
              PluginAlarmData(
                  id = -1,
                  label = "",
                  alarmtone = rxPrefs.defaultRingtone(),
              ),
              prealarm = false,
              targetVolume = Observable.just(TargetVolume.FADED_IN))
    }

    masterVolumeProgress
        // stop after 3 seconds of inactivity
        .debounce(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
        .subscribeIn(fragmentScope) { stopMasterSample() }
  }

  private fun bindPrealarmSeekBar(preAlarmSeekBar: SeekBar) {
    preAlarmSeekBar.max = MAX_PREALARM_VOLUME

    rxPrefs.preAlarmVolume.observe().subscribeIn(fragmentScope) { preAlarmSeekBar.progress = it }

    val preAlarmSeekBarProgress = preAlarmSeekBar.attachProgressChangedListener()

    preAlarmSeekBarProgress.subscribeIn(fragmentScope) { progress ->
      log.debug { "Pre-alarm $progress" }
      rxPrefs.preAlarmVolume.value = progress
      stopMasterSample()
      stopPrealarmSample()
      prealarmSampleDisposable =
          klaxon.go(
              PluginAlarmData(
                  id = -1,
                  label = "",
                  alarmtone = rxPrefs.defaultRingtone(),
              ),
              prealarm = true,
              targetVolume = Observable.just(TargetVolume.FADED_IN))
    }

    preAlarmSeekBarProgress
        // stop after 3 seconds of inactivity
        .debounce(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
        .subscribeIn(fragmentScope) { stopPrealarmSample() }
  }

  private fun stopPrealarmSample() {
    prealarmSampleDisposable.dispose()
  }

  private fun stopMasterSample() {
    sampleDisposable.dispose()
  }

  /** Turns a [SeekBar] into a volume control. */
  private fun SeekBar.attachProgressChangedListener(): Observable<Int> {
    val progressChanged = PublishSubject.create<Int>()
    setOnSeekBarChangeListener(
        object : OnSeekBarChangeListener {
          override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromTouch: Boolean) {
            if (!fromTouch) return
            progressChanged.onNext(progress)
          }

          override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

          override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    return progressChanged
  }
}
