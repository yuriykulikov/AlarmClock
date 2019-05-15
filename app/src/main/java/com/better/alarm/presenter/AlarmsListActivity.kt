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

import android.annotation.TargetApi
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import android.transition.*
import android.view.Menu
import android.view.MenuItem
import com.better.alarm.R
import com.better.alarm.checkPermissions
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.configuration.AlarmApplication.themeHandler
import com.better.alarm.configuration.EditedAlarm
import com.better.alarm.configuration.Store
import com.better.alarm.interfaces.Intents
import com.better.alarm.lollipop
import com.better.alarm.util.Optional
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
class AlarmsListActivity : FragmentActivity() {
    private var mActionBarHandler: ActionBarHandler? = null
    private val logger = container().logger()
    private val alarms = container().alarms()

    private var sub = Disposables.disposed()

    private val store = object : UiStore {
        var onBackPressed = PublishSubject.create<String>()
        var editing: Subject<EditedAlarm> = BehaviorSubject.createDefault(EditedAlarm())
        var transitioningToNewAlarmDetails: Subject<Boolean> = BehaviorSubject.createDefault(false)

        override fun editing(): Subject<EditedAlarm> {
            return editing
        }

        override fun onBackPressed(): PublishSubject<String> {
            return onBackPressed
        }

        override fun createNewAlarm() {
            transitioningToNewAlarmDetails.onNext(true)
            val newAlarm = alarms.createNewAlarm().edit()
            editing().onNext(EditedAlarm(
                    /* new */ true,
                    /* edited */true,
                    /* id */ newAlarm.id,
                    Optional.absent()))
        }

        override fun transitioningToNewAlarmDetails(): Subject<Boolean> {
            return transitioningToNewAlarmDetails
        }

        override fun edit(id: Int) {
            editing().onNext(EditedAlarm(
                    /* new */ false,
                    /* edited */true,
                    /* id */ id,
                    Optional.absent()))
        }

        override fun edit(id: Int, holder: RowHolder) {
            editing().onNext(EditedAlarm(
                    /* new */ false,
                    /* edited */true,
                    /* id */ id,
                    Optional.of(holder)))
        }

        override fun hideDetails() {
            editing().onNext(EditedAlarm())
        }

        override fun hideDetails(holder: RowHolder) {
            editing().onNext(EditedAlarm(
                    /* new */ false,
                    /* edited */false,
                    /* id */ holder.alarmId(),
                    Optional.of(holder)))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(themeHandler().getIdForName(AlarmsListActivity::class.java.name))
        super.onCreate(savedInstanceState)
        logger.d(this@AlarmsListActivity)
        this.mActionBarHandler = ActionBarHandler(this, store, alarms)

        val isTablet = !resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContentView(R.layout.list_activity)

        if (intent != null && intent.hasExtra(Intents.EXTRA_ID)) {
            //jump directly to editor
            store.edit(intent.getIntExtra(Intents.EXTRA_ID, -1))
        }

        container()
                .store
                .alarms()
                .take(1)
                .subscribe { alarms ->
                    checkPermissions(this, alarms.map { it.alarmtone })
                }.apply { }
    }

    override fun onStart() {
        logger.d(this@AlarmsListActivity)
        super.onStart()
        configureTransactions()
    }

    override fun onStop() {
        logger.d(this@AlarmsListActivity)
        super.onStop()
        this.sub.dispose()
    }

    override fun onDestroy() {
        logger.d(this@AlarmsListActivity)
        super.onDestroy()
        this.mActionBarHandler!!.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return mActionBarHandler!!.onCreateOptionsMenu(menu, menuInflater, actionBar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return mActionBarHandler!!.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        store.onBackPressed().onNext(AlarmsListActivity::class.java.simpleName)
    }

    private fun configureTransactions() {
        sub = store.editing()
                .distinctUntilChanged { (_, isEdited) -> isEdited }
                .subscribe(Consumer { edited ->
                    when {
                        lollipop() && isDestroyed -> return@Consumer
                        edited.isEdited -> showDetails(edited)
                        else -> showList(edited)
                    }
                })
    }

    private fun showList(@NonNull edited: EditedAlarm) {
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
                .commit()
    }

    private fun showDetails(@NonNull edited: EditedAlarm) {
        supportFragmentManager.findFragmentById(R.id.main_fragment_container)?.apply {
            lollipop {
                exitTransition = Fade()
            }
        }

        val detailsFragment = AlarmDetailsFragment().apply {
            arguments = Bundle()
            arguments.putInt(Intents.EXTRA_ID, edited.id())
            arguments.putBoolean(Store.IS_NEW_ALARM, edited.isNew)
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
                .commit()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun moveTransition(): TransitionSet {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
        }
    }

    private fun RowHolder.addSharedElementsToTransition(fragmentTransaction: FragmentTransaction) {
        fragmentTransaction.addSharedElement(digitalClock(), "clock" + alarmId())
        fragmentTransaction.addSharedElement(container(), "onOff" + alarmId())
        fragmentTransaction.addSharedElement(detailsButton(), "detailsButton" + alarmId())
    }

    companion object {
        fun uiStore(activity: AlarmsListActivity): UiStore {
            return activity.store
        }
    }
}

