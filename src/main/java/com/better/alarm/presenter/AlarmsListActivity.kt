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
import android.app.Activity
import android.app.Fragment
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.transition.*
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import com.better.alarm.R
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
class AlarmsListActivity : Activity() {
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
        fragmentManager.findFragmentById(R.id.main_fragment_container)?.lollipop {
            exitTransition = Fade()
        }

        val listFragment = AlarmsListFragment().lollipop {
            val move = moveTransition()
            sharedElementEnterTransition = move
            sharedElementReturnTransition = move
            enterTransition = Fade()
        }

        fragmentManager.beginTransaction()
                .lollipop {
                    if (edited.holder.isPresent()) {
                        val viewHolder = edited.holder.get()
                        addSharedElement(viewHolder.digitalClock(), "clock" + viewHolder.alarmId())
                        addSharedElement(viewHolder.container(), "onOff" + viewHolder.alarmId())
                    }
                }
                .replace(R.id.main_fragment_container, listFragment)
                .commitAllowingStateLoss()
    }

    private fun showDetails(@NonNull edited: EditedAlarm) {
        fragmentManager.findFragmentById(R.id.main_fragment_container)?.lollipop {
            exitTransition = Fade()
        }

        val detailsFragment = AlarmDetailsFragment().apply {
            arguments = Bundle()
            arguments.putInt(Intents.EXTRA_ID, edited.id())
            arguments.putBoolean(Store.IS_NEW_ALARM, edited.isNew)
        }.lollipop {
            val enterSlide = Slide()

            if (edited.holder.isPresent()) {
                val viewHolder = edited.holder.get()
                enterSlide.epicenterCallback = viewHolder.epicenter()
                enterSlide.slideEdge = Gravity.TOP
            }

            enterTransition = TransitionSet().addTransition(enterSlide).addTransition(Fade())
            reenterTransition = TransitionSet().addTransition(enterSlide).addTransition(Fade())

            val move = moveTransition()

            sharedElementEnterTransition = move
            sharedElementReturnTransition = move
            allowEnterTransitionOverlap = true
        }

        fragmentManager.beginTransaction()
                .lollipop {
                    val viewHolder = edited.holder.get()
                    addSharedElement(viewHolder.digitalClock(), viewHolder.digitalClock.transitionName)
                    addSharedElement(viewHolder.container(), viewHolder.container().transitionName)
                }
                .replace(R.id.main_fragment_container, detailsFragment)
                .commitAllowingStateLoss()
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun moveTransition(): TransitionSet {
        return TransitionSet().lollipop {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
            duration = 1500
        }
    }

    companion object {
        fun uiStore(fragment: Fragment): UiStore {
            return (fragment.activity as AlarmsListActivity).store
        }
    }
}
