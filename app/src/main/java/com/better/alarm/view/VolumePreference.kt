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

package com.better.alarm.view

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.provider.Settings
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.better.alarm.R
import com.better.alarm.background.KlaxonPlugin
import com.better.alarm.background.PluginAlarmData
import com.better.alarm.background.TargetVolume
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.Prefs.Companion.MAX_PREALARM_VOLUME
import com.better.alarm.configuration.globalInject
import com.better.alarm.configuration.globalLogger
import com.better.alarm.model.Alarmtone
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.PublishSubject
import org.koin.core.context.GlobalContext.get
import org.koin.core.qualifier.named
import java.util.concurrent.TimeUnit

class VolumePreference(mContext: Context, attrs: AttributeSet) : Preference(mContext, attrs) {
    private var ringtone: Ringtone? = null
    private var ringtoneSummary: TextView? = null
    private val disposable = CompositeDisposable()

    private val klaxon: KlaxonPlugin by lazy {
        get().koin.rootScope.get<KlaxonPlugin>(named("volumePreferenceDemo"))
    }

    init {
        layoutResource = R.layout.seekbar_dialog
        // this actually can return null
        this.ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        ringtone?.streamType = AudioManager.STREAM_ALARM
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.let { view ->
            bindPrealarmSeekBar(view.findViewById(R.id.seekbar_dialog_seekbar_prealarm_volume) as SeekBar)
            bindAudioManagerVolume(view.findViewById(R.id.seekbar_dialog_seekbar_master_volume) as SeekBar)

            view.findViewById(R.id.settings_ringtone).setOnClickListener {
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
            }
            ringtoneSummary = view.findViewById(R.id.settings_ringtone_summary) as TextView
            onResume()
        }
    }

    override fun onDetached() {
        super.onDetached()
        disposable.dispose()
    }
    fun onResume() {
        ringtoneSummary?.text = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                ?.getTitle(context)
                ?: context.getText(R.string.silent_alarm_summary)
    }

    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
        ringtone?.stop()
    }

    fun onPause() {

    }

    /**
     * This class is controls playback using AudioManager
     */
    private fun bindAudioManagerVolume(seekBar: SeekBar) {
        val am: AudioManager by globalInject()
        val masterListener = SeekBarListener()
        seekBar.setOnSeekBarChangeListener(masterListener)

        seekBar.progress = am.getStreamVolume(AudioManager.STREAM_ALARM)
        seekBar.max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        masterListener
                .progressObservable()
                .subscribe { progress ->
                    //stop prealarm sample if there is one
                    stopPrealarmSample()
                    am.setStreamVolume(AudioManager.STREAM_ALARM, progress!!, 0)
                    if (ringtone?.isPlaying == false) {
                        ringtone?.play()
                    }
                }
                .let { disposable.add(it) }

        masterListener
                .progressObservable()
                .debounce(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe { ringtone?.stop() }
                .let { disposable.add(it) }
    }

    private fun bindPrealarmSeekBar(preAlarmSeekBar: SeekBar) {
        val prealarmListener = SeekBarListener()
        preAlarmSeekBar.setOnSeekBarChangeListener(prealarmListener)
        val rxPrefs: Prefs by globalInject()
        val log by globalLogger("VolumePreference")
        val prealarmPreference = rxPrefs.preAlarmVolume
        preAlarmSeekBar.max = MAX_PREALARM_VOLUME

        prealarmPreference.observe().subscribe { integer -> preAlarmSeekBar.progress = integer }.let { disposable.add(it) }

        prealarmListener
                .progressObservable()
                .doOnNext { integer ->
                    log.debug { "Pre-alarm $integer" }
                    prealarmPreference.value = integer
                    ringtone?.stop()
                }
                .subscribe {
                    prealarmSampleDisposable.dispose()
                    prealarmSampleDisposable = klaxon.go(PluginAlarmData(
                            id = -1,
                            label = "",
                            alarmtone = Alarmtone.Default()
                    ), prealarm = true, targetVolume = Observable.just(TargetVolume.FADED_IN))
                }
                .let { disposable.add(it) }

        prealarmListener
                .progressObservable()
                .debounce(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe { stopPrealarmSample() }
                .let { disposable.add(it) }
    }

    private var prealarmSampleDisposable = Disposables.empty()

    private fun stopPrealarmSample() {
        prealarmSampleDisposable.dispose()
    }

    /**
     * Turns a [SeekBar] into a volume control.
     */
    private class SeekBarListener : OnSeekBarChangeListener {
        private val progressChanged = PublishSubject.create<Int>()

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromTouch: Boolean) {
            if (!fromTouch) return
            progressChanged.onNext(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            //empty
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            //empty
        }

        fun progressObservable(): Observable<Int> {
            return progressChanged
        }
    }
}