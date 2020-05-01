/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.transition.*
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.better.alarm.*
import com.better.alarm.configuration.EditedAlarm
import com.better.alarm.configuration.Store
import com.better.alarm.configuration.globalGet
import com.better.alarm.configuration.globalInject
import com.better.alarm.configuration.globalLogger
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmData
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.util.Optional
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
class AlarmsListActivity : AppCompatActivity() {
    private lateinit var mActionBarHandler: ActionBarHandler

    // lazy because it seems that AlarmsListActivity.<init> can be called before Application.onCreate()
    private val logger: Logger by globalLogger("AlarmsListActivity")
    private val alarms: IAlarmsManager by globalInject()
    private val store: Store by globalInject()

    private var sub = Disposables.disposed()

    private val uiStore: UiStore by globalInject()
    private val dynamicThemeHandler: DynamicThemeHandler by globalInject()

    companion object {
        val uiStoreModule: Module = module {
            single<UiStore> { createStore(EditedAlarm(), get()) }
        }

        private fun createStore(edited: EditedAlarm, alarms: IAlarmsManager): UiStore {
            return object : UiStore {
                var onBackPressed = PublishSubject.create<String>()
                var editing: BehaviorSubject<EditedAlarm> = BehaviorSubject.createDefault(edited)
                var transitioningToNewAlarmDetails: Subject<Boolean> = BehaviorSubject.createDefault(false)

                override fun editing(): BehaviorSubject<EditedAlarm> {
                    return editing
                }

                override fun onBackPressed(): PublishSubject<String> {
                    return onBackPressed
                }

                override fun createNewAlarm() {
                    transitioningToNewAlarmDetails.onNext(true)
                    val newAlarm = alarms.createNewAlarm()
                    editing.onNext(EditedAlarm(
                            isNew = true,
                            value = Optional.of(AlarmData.from(newAlarm.edit())),
                            id = newAlarm.id,
                            holder = Optional.absent()))
                }

                override fun transitioningToNewAlarmDetails(): Subject<Boolean> {
                    return transitioningToNewAlarmDetails
                }

                override fun edit(id: Int) {
                    alarms.getAlarm(id)?.let { alarm ->
                        editing.onNext(EditedAlarm(
                                isNew = false,
                                value = Optional.of(AlarmData.from(alarm.edit())),
                                id = id,
                                holder = Optional.absent()))
                    }
                }

                override fun edit(id: Int, holder: RowHolder) {
                    alarms.getAlarm(id)?.let { alarm ->
                        editing.onNext(EditedAlarm(
                                isNew = false,
                                value = Optional.of(AlarmData.from(alarm.edit())),
                                id = id,
                                holder = Optional.of(holder)))
                    }
                }

                override fun hideDetails() {
                    editing.onNext(EditedAlarm())
                }

                override fun hideDetails(holder: RowHolder) {
                    editing.onNext(EditedAlarm(
                            isNew = false,
                            value = Optional.absent(),
                            id = holder.alarmId,
                            holder = Optional.of(holder)))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getStringExtra("reason") == SettingsFragment.themeChangeReason) {
            finish()
            startActivity(Intent(this, AlarmsListActivity::class.java))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("version", BuildConfig.VERSION_CODE)
        uiStore.editing().value?.writeInto(outState)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(dynamicThemeHandler.getIdForName(AlarmsListActivity::class.java.name))
        super.onCreate(savedInstanceState)

        when {
            savedInstanceState != null && savedInstanceState.getInt("version", BuildConfig.VERSION_CODE) == BuildConfig.VERSION_CODE -> {
                val restored = editedAlarmFromSavedInstanceState(savedInstanceState)
                logger.d("Restored ${this@AlarmsListActivity} with $restored")
                uiStore.editing().onNext(restored)
            }
            else -> {
                val initialState = EditedAlarm()
                logger.d("Created ${this@AlarmsListActivity} with $initialState")
            }
            // if (intent != null && intent.hasExtra(Intents.EXTRA_ID)) {
            //     //jump directly to editor
            //     uiStore.edit(intent.getIntExtra(Intents.EXTRA_ID, -1))
            // }
        }

        this.mActionBarHandler = ActionBarHandler(this, uiStore, alarms, globalGet())

        val isTablet = !resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContentView(R.layout.list_activity)

        store
                .alarms()
                .take(1)
                .subscribe { alarms ->
                    checkPermissions(this, alarms.map { it.alarmtone })
                }.apply { }
    }

    override fun onStart() {
        super.onStart()
        configureTransactions()
    }

    override fun onResume() {
        super.onResume()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        NotificationSettings().checkSettings(this)
    }

    override fun onStop() {
        super.onStop()
        this.sub.dispose()
    }

    override fun onDestroy() {
        logger.d(this@AlarmsListActivity)
        super.onDestroy()
        this.mActionBarHandler.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return supportActionBar?.let { mActionBarHandler.onCreateOptionsMenu(menu, menuInflater, it) }
                ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return mActionBarHandler.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        uiStore.onBackPressed().onNext(AlarmsListActivity::class.java.simpleName)
    }

    private fun configureTransactions() {
        sub = uiStore.editing()
                .distinctUntilChanged { edited -> edited.isEdited }
                .subscribe(Consumer { edited ->
                    when {
                        lollipop() && isDestroyed -> return@Consumer
                        edited.isEdited -> showDetails(edited)
                        else -> showList(edited)
                    }
                })
    }

    private fun showList(@NonNull edited: EditedAlarm) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

        if (currentFragment is AlarmsListFragment) {
            logger.d("skipping fragment transition, because already showing $currentFragment")
        } else {
            logger.d("transition from: $currentFragment to show list, edited: $edited")
            supportFragmentManager.findFragmentById(R.id.main_fragment_container)?.apply {
                lollipop {
                    exitTransition = Fade()
                }
            }

            val listFragment = AlarmsListFragment().apply {
                lollipop {
                    sharedElementEnterTransition = moveTransition()
                    enterTransition = Fade()
                    allowEnterTransitionOverlap = true
                }
            }

            supportFragmentManager.beginTransaction()
                    .apply {
                        lollipop {
                            edited.holder.getOrNull()?.addSharedElementsToTransition(this)
                        }
                    }
                    .apply {
                        if (!lollipop()) {
                            this.setCustomAnimations(R.anim.push_down_in, android.R.anim.fade_out)
                        }
                    }
                    .replace(R.id.main_fragment_container, listFragment)
                    .commitAllowingStateLoss()
        }
    }

    private fun showDetails(@NonNull edited: EditedAlarm) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

        if (currentFragment is AlarmDetailsFragment) {
            logger.d("skipping fragment transition, because already showing $currentFragment")
        } else {
            logger.d("transition from: $currentFragment to show details, edited: $edited")
            currentFragment?.apply {
                lollipop {
                    exitTransition = Fade()
                }
            }

            val detailsFragment = AlarmDetailsFragment().apply {
                arguments = Bundle()
            }.apply {
                lollipop {
                    enterTransition = TransitionSet().addTransition(Slide()).addTransition(Fade())
                    sharedElementEnterTransition = moveTransition()
                    allowEnterTransitionOverlap = true
                }
            }

            supportFragmentManager.beginTransaction()
                    .apply {
                        if (!lollipop()) {
                            this.setCustomAnimations(R.anim.push_down_in, android.R.anim.fade_out)
                        }
                    }
                    .apply {
                        lollipop {
                            edited.holder.getOrNull()?.addSharedElementsToTransition(this)
                        }
                    }
                    .replace(R.id.main_fragment_container, detailsFragment)
                    .commitAllowingStateLoss()
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun moveTransition(): TransitionSet {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
        }
    }

    private fun RowHolder.addSharedElementsToTransition(fragmentTransaction: androidx.fragment.app.FragmentTransaction) {
        fragmentTransaction.addSharedElement(digitalClock, "clock" + alarmId)
        fragmentTransaction.addSharedElement(container, "onOff" + alarmId)
        fragmentTransaction.addSharedElement(detailsButton, "detailsButton" + alarmId)
    }

    /**
     * restores an [EditedAlarm] from SavedInstanceState. Counterpart of [EditedAlarm.writeInto].
     */
    private fun editedAlarmFromSavedInstanceState(savedInstanceState: Bundle): EditedAlarm {
        return EditedAlarm(
                isNew = savedInstanceState.getBoolean("isNew"),
                id = savedInstanceState.getInt("id"),
                value = if (savedInstanceState.getBoolean("isEdited")) {
                    Optional.of(
                            AlarmData(
                                    id = savedInstanceState.getInt("id"),
                                    isEnabled = savedInstanceState.getBoolean("isEnabled"),
                                    hour = savedInstanceState.getInt("hour"),
                                    minutes = savedInstanceState.getInt("minutes"),
                                    daysOfWeek = DaysOfWeek(savedInstanceState.getInt("daysOfWeek")),
                                    isPrealarm = savedInstanceState.getBoolean("isPrealarm"),
                                    alarmtone = Alarmtone.fromString(savedInstanceState.getString("alarmtone")),
                                    label = savedInstanceState.getString("label"),
                                    isVibrate = true,
                                    skipping = savedInstanceState.getBoolean("skipping")
                            )
                    )
                } else {
                    Optional.absent()
                }
        )
    }

    /**
     * Saves EditedAlarm into SavedInstanceState. Counterpart of [editedAlarmFromSavedInstanceState]
     */
    private fun EditedAlarm.writeInto(outState: Bundle?) {
        val toWrite: EditedAlarm = this
        outState?.run {
            putBoolean("isNew", isNew)
            putInt("id", id)
            putBoolean("isEdited", isEdited)

            value.getOrNull()?.let { edited ->
                putInt("id", edited.id)
                putBoolean("isEnabled", edited.isEnabled)
                putInt("hour", edited.hour)
                putInt("minutes", edited.minutes)
                putInt("daysOfWeek", edited.daysOfWeek.coded)
                putString("label", edited.label)
                putBoolean("isPrealarm", edited.isPrealarm)
                putBoolean("isVibrate", edited.isVibrate)
                putString("alarmtone", edited.alarmtone.persistedString)
                putBoolean("skipping", edited.skipping)
            }

            logger.d("Saved state $toWrite")
        }
    }
}
