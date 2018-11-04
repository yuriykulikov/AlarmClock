/*
 * Copyright (C) 2017 Yuriy Kulikov yuriy.kulikov.87@gmail.com
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

package com.better.alarm.presenter

import android.annotation.TargetApi
import android.app.Activity
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.RingtonePreference
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import com.better.alarm.R
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.configuration.Store
import com.better.alarm.interfaces.AlarmEditor
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.view.RepeatPreference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.better.alarm.util.Optional
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.*

/**
 * Details activity allowing for fine-grained alarm modification
 */
class AlarmDetailsFragment : PreferenceFragment {
    private val IS_LOLLI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    private val alarms: IAlarmsManager
    private val logger: Logger
    private val rxSharedPreferences: RxSharedPreferences

    private val disposables = CompositeDisposable()

    private var backButtonSub: Disposable = Disposables.disposed()
    private var disposableDialog = Disposables.disposed()

    private val store: UiStore by lazy { AlarmsListActivity.uiStore(this) }
    private val mLabel: EditText by lazy { fragmentView.findViewById(R.id.details_label) as EditText }
    private val rowHolder: RowHolder by lazy { RowHolder(fragmentView, alarmId) }
    private val mAlarmPref: RingtonePreference by lazy { findPreference("alarm_ringtone") as RingtonePreference }
    private val mRepeatPref: RepeatPreference by lazy { findPreference("setRepeat") as RepeatPreference }
    private val mPreAlarmPref: CheckBoxPreference by lazy { findPreference("prealarm") as CheckBoxPreference }

    private val editor: Subject<AlarmEditor> = BehaviorSubject.create()

    private val isNewAlarm: Boolean by lazy { arguments.getBoolean(Store.IS_NEW_ALARM) }
    private val alarmId: Int by lazy { arguments.getInt(Intents.EXTRA_ID) }

    lateinit var fragmentView: View

    constructor() : super() {
        alarms = container().alarms()
        logger = container().logger()
        rxSharedPreferences = container().rxPrefs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.alarm_details)
        editor.onNext(alarms.getAlarm(alarmId).edit())
    }

    @TargetApi(21)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        logger.d("Inflating layout")
        val view = inflater.inflate(R.layout.details_activity, container, false)
        (view.findViewById(android.R.id.list) as ListView).addFooterView(inflater.inflate(R.layout.details_label, null))

        this.fragmentView = view

        rowHolder.onOff().setOnClickListener {
            modify("onOff") { editor ->
                editor.withIsEnabled(!editor.isEnabled)
            }
        }

        rowHolder.detailsButton().visibility = View.INVISIBLE
        rowHolder.daysOfWeek().visibility = View.INVISIBLE
        rowHolder.label().visibility = View.INVISIBLE

        setTransitionNames()

        editor.subscribe { logger.d("---- $it") }

        rowHolder.digitalClock().setLive(false)
        rowHolder.digitalClock().setOnClickListener {
            disposableDialog = TimePickerDialogFragment.showTimePicker(fragmentManager).subscribe(pickerConsumer)
        }

        editor.firstOrError().subscribe { editor -> mLabel.setText(editor.label) }

        editor.distinctUntilChanged()
                .subscribe { editor ->
                    rowHolder.digitalClock().updateTime(Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, editor.hour)
                        set(Calendar.MINUTE, editor.minutes)
                    })

                    rowHolder.onOff().isChecked = editor.isEnabled
                }

        view.findViewById(R.id.details_activity_button_save).setOnClickListener { saveAlarm() }
        view.findViewById(R.id.details_activity_button_revert).setOnClickListener { revert() }

        if (isNewAlarm) {
            disposableDialog = TimePickerDialogFragment.showTimePicker(fragmentManager)
                    .subscribe(pickerConsumer)
        }

        rowHolder.rowView().setOnClickListener { saveAlarm() }

        bindToPreferences()

        return view
    }

    @TargetApi(21)
    private fun setTransitionNames() {
        if (IS_LOLLI) {
            rowHolder.digitalClock().transitionName = "clock" + alarmId
            rowHolder.container().transitionName = "onOff" + alarmId
        }
    }

    fun bindToPreferences() {
        //init preferences with editor$ values
        editor.firstOrError()
                .subscribe { editor ->
                    mRepeatPref.daysOfWeek = editor.daysOfWeek
                    mPreAlarmPref.isChecked = editor.isPrealarm
                    rxSharedPreferences.getString(mAlarmPref.key).set(editor.alertString)
                }

        //pre-alarm duration
        disposables.add(rxSharedPreferences.getString("prealarm_duration", "-1")
                .asObservable()
                .subscribe { value ->
                    val duration = Integer.parseInt(value)
                    if (duration == -1) {
                        preferenceScreen.removePreference(mPreAlarmPref)
                    } else {
                        preferenceScreen.addPreference(mPreAlarmPref)
                        mPreAlarmPref.setSummaryOff(R.string.prealarm_off_summary)
                        mPreAlarmPref.summaryOn = resources.getString(R.string.prealarm_summary, duration)
                    }
                })
        //pre-alarm
        disposables.add(rxSharedPreferences.getBoolean(mPreAlarmPref.key)
                .asObservable()
                .skip(1)
                .subscribe { preAlarmEnabled ->
                    modify("Pre-alarm") { editor -> editor.with(isPrealarm = preAlarmEnabled, enabled = true) }
                })

        //Alert summary
        mAlarmPref.bindPreferenceSummary().let { disposables.add(it) }
        //Alert
        mAlarmPref.setOnPreferenceChangeListener { _, alert ->
            modify("Alert") { editor -> editor.with(alertString = alert as String, enabled = true) }
            true
        }

        //repeat
        mRepeatPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, daysOfWeek ->
            modify("Repeat") { editor -> editor.with(daysOfWeek = daysOfWeek as DaysOfWeek, enabled = true) }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        backButtonSub = store.onBackPressed().subscribe { saveAlarm() }
        store.transitioningToNewAlarmDetails().onNext(false)
    }

    override fun onPause() {
        super.onPause()
        disposableDialog.dispose()
        backButtonSub.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    private fun saveAlarm() {
        editor.firstOrError().subscribe { editorToSave ->
            editorToSave
                    .withLabel(mLabel.text.toString())
                    .commit()
            store.hideDetails(rowHolder)
        }
    }

    private fun revert() {
        editor.firstOrError().subscribe { edited ->
            // "Revert" on a newly created alarm should delete it.
            if (isNewAlarm) {
                alarms.delete(edited)
            }
            // else do not save changes
            store.hideDetails(rowHolder)
        }
    }

    val pickerConsumer = { picked: Optional<PickedTime> ->
        if (picked.isPresent()) {
            modify("Picker") { editor: AlarmEditor ->
                editor.with(hour = picked.get().hour,
                        minutes = picked.get().minute,
                        enabled = true)
            }
        }
    }

    private fun modify(reason: String, function: (AlarmEditor) -> AlarmEditor) {
        logger.d("Performing modification because of $reason")
        editor.firstOrError().subscribe { ed -> editor.onNext(function.invoke(ed)) }
    }

    private fun RingtonePreference.bindPreferenceSummary(): Disposable {
        return this.bindPreferenceSummary(rxSharedPreferences, activity)
    }
}

fun RingtonePreference.bindPreferenceSummary(rxSharedPreferences: RxSharedPreferences, activity: Activity): Disposable {
    return rxSharedPreferences.getString(this.key, "")
            .asObservable()
            .subscribe { uriString ->
                val uri: Uri = Uri.parse(uriString)
                val ringtone: Ringtone? = RingtoneManager.getRingtone(activity, uri)
                this.summary = ringtone?.getTitle(activity) ?: "..."
            }
}
