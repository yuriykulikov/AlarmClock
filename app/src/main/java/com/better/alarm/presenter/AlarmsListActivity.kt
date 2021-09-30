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
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.better.alarm.BuildConfig
import com.better.alarm.NotificationSettings
import com.better.alarm.R
import com.better.alarm.checkPermissions
import com.better.alarm.configuration.EditedAlarm
import com.better.alarm.configuration.Store
import com.better.alarm.configuration.globalGet
import com.better.alarm.configuration.globalInject
import com.better.alarm.configuration.globalLogger
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.lollipop
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.util.Optional
import com.better.alarm.util.formatToast
import com.google.android.material.snackbar.Snackbar
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.Calendar
import org.koin.core.module.Module
import org.koin.dsl.module

/** This activity displays a list of alarms and optionally a details fragment. */
class AlarmsListActivity : AppCompatActivity() {
  private lateinit var mActionBarHandler: ActionBarHandler

  private val logger: Logger by globalLogger("AlarmsListActivity")
  private val alarms: IAlarmsManager by globalInject()
  private val store: Store by globalInject()

  private var sub = Disposables.disposed()
  private var snackbarDisposable = Disposables.disposed()

  private val uiStore: UiStore by globalInject()
  private val dynamicThemeHandler: DynamicThemeHandler by globalInject()

  companion object {
    val uiStoreModule: Module = module { single<UiStore> { createStore(EditedAlarm(), get()) } }

    private fun createStore(edited: EditedAlarm, alarms: IAlarmsManager): UiStore {
      class UiStoreIR : UiStore {
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
          editing.onNext(
              EditedAlarm(
                  isNew = true,
                  value = Optional.of(newAlarm.data),
                  id = newAlarm.id,
                  holder = Optional.absent()))
        }

        override fun transitioningToNewAlarmDetails(): Subject<Boolean> {
          return transitioningToNewAlarmDetails
        }

        override fun edit(id: Int, holder: RowHolder?) {
          alarms.getAlarm(id)?.let { alarm ->
            editing.onNext(
                EditedAlarm(
                    isNew = false,
                    value = Optional.of(alarm.data),
                    id = id,
                    holder = Optional.fromNullable(holder)))
          }
        }

        override fun hideDetails(holder: RowHolder?) {
          editing.onNext(
              EditedAlarm(
                  isNew = false,
                  value = Optional.absent(),
                  id = holder?.alarmId ?: -1,
                  holder = Optional.fromNullable(holder)))
        }

        override var openDrawerOnCreate: Boolean = false
      }

      return UiStoreIR()
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    println("new $intent, ${intent?.extras}")
    if (intent?.getStringExtra("reason") == SettingsFragment.themeChangeReason) {
      finish()
      startActivity(
          Intent(this, AlarmsListActivity::class.java).apply {
            putExtra("openDrawerOnCreate", true)
          })
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt("version", BuildConfig.VERSION_CODE)
    uiStore.editing().value?.writeInto(outState)
  }

  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(dynamicThemeHandler.defaultTheme())
    super.onCreate(savedInstanceState)
    uiStore.openDrawerOnCreate = intent?.getBooleanExtra("openDrawerOnCreate", false) ?: false
    val prevVersion = savedInstanceState?.getInt("version", BuildConfig.VERSION_CODE)
    if (prevVersion == BuildConfig.VERSION_CODE) {
      val restored = editedAlarmFromSavedInstanceState(savedInstanceState)
      logger.trace { "Restored $this with $restored" }
      uiStore.editing().onNext(restored)
    }

    this.mActionBarHandler = ActionBarHandler(this, uiStore, alarms, globalGet())

    if (!resources.getBoolean(R.bool.isTablet)) {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    setContentView(R.layout.list_activity)

    store
        .alarms()
        .take(1)
        .subscribe { alarms -> checkPermissions(this, alarms.map { it.alarmtone }) }
        .apply {}
  }

  override fun onStart() {
    super.onStart()
    configureTransactions()
    configureSnackbar()
  }

  private fun configureSnackbar() {
    snackbarDisposable =
        store
            .sets()
            .withLatestFrom(store.uiVisible, { set, uiVisible -> set to uiVisible })
            .subscribe { (set: Store.AlarmSet, uiVisible: Boolean) ->
              if (uiVisible) {
                showSnackbar(set)
              }
            }
  }

  /**
   * using the main container instead of a root view here fixes
   * https://github.com/yuriykulikov/AlarmClock/issues/372
   */
  private fun showSnackbar(set: Store.AlarmSet) {
    val toastText = formatToast(applicationContext, set.millis)
    Snackbar.make(findViewById(R.id.main_fragment_container), toastText, Snackbar.LENGTH_LONG)
        .apply {
          val text = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
          text.gravity = Gravity.CENTER_HORIZONTAL
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            text.textAlignment = View.TEXT_ALIGNMENT_CENTER
          }
        }
        .show()
  }

  override fun onResume() {
    super.onResume()
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    NotificationSettings().checkSettings(this)
    store.uiVisible.onNext(true)
  }

  override fun onPause() {
    super.onPause()
    store.uiVisible.onNext(false)
  }

  override fun onStop() {
    super.onStop()
    this.sub.dispose()
    snackbarDisposable.dispose()
  }

  override fun onDestroy() {
    logger.debug { "$this" }
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
    sub =
        uiStore
            .editing()
            .distinctUntilChanged { edited -> edited.isEdited }
            .subscribe(
                Consumer { edited ->
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
      // "skipping fragment transition, because already showing $currentFragment"
    } else {
      supportFragmentManager.findFragmentById(R.id.main_fragment_container)?.apply {
        lollipop { exitTransition = Fade() }
      }

      val listFragment =
          AlarmsListFragment().apply {
            lollipop {
              sharedElementEnterTransition = moveTransition()
              enterTransition = Fade()
              allowEnterTransitionOverlap = true
            }
          }

      supportFragmentManager
          .beginTransaction()
          .apply { lollipop { edited.holder.getOrNull()?.addSharedElementsToTransition(this) } }
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
      logger.trace { "skipping fragment transition, because already showing $currentFragment" }
    } else {
      logger.trace { "transition from: $currentFragment to show details, edited: $edited" }
      currentFragment?.apply { lollipop { exitTransition = Fade() } }

      val detailsFragment =
          AlarmDetailsFragment().apply { arguments = Bundle() }.apply {
            lollipop {
              enterTransition = TransitionSet().addTransition(Slide()).addTransition(Fade())
              sharedElementEnterTransition = moveTransition()
              allowEnterTransitionOverlap = true
            }
          }

      supportFragmentManager
          .beginTransaction()
          .apply {
            if (!lollipop()) {
              this.setCustomAnimations(R.anim.push_down_in, android.R.anim.fade_out)
            }
          }
          .apply { lollipop { edited.holder.getOrNull()?.addSharedElementsToTransition(this) } }
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

  private fun RowHolder.addSharedElementsToTransition(
      fragmentTransaction: androidx.fragment.app.FragmentTransaction
  ) {
    fragmentTransaction.addSharedElement(digitalClock, "clock$alarmId")
    fragmentTransaction.addSharedElement(container, "onOff$alarmId")
    fragmentTransaction.addSharedElement(detailsButton, "detailsButton$alarmId")
  }

  /** restores an [EditedAlarm] from SavedInstanceState. Counterpart of [EditedAlarm.writeInto]. */
  private fun editedAlarmFromSavedInstanceState(savedInstanceState: Bundle): EditedAlarm {
    return EditedAlarm(
        isNew = savedInstanceState.getBoolean("isNew"),
        id = savedInstanceState.getInt("id"),
        value =
            if (savedInstanceState.getBoolean("isEdited")) {
              Optional.of(
                  AlarmValue(
                      id = savedInstanceState.getInt("id"),
                      isEnabled = savedInstanceState.getBoolean("isEnabled"),
                      hour = savedInstanceState.getInt("hour"),
                      minutes = savedInstanceState.getInt("minutes"),
                      daysOfWeek = DaysOfWeek(savedInstanceState.getInt("daysOfWeek")),
                      isPrealarm = savedInstanceState.getBoolean("isPrealarm"),
                      alarmtone = Alarmtone.fromString(savedInstanceState.getString("alarmtone")),
                      label = savedInstanceState.getString("label") ?: "",
                      isVibrate = true,
                      state = savedInstanceState.getString("state") ?: "",
                      nextTime = Calendar.getInstance()))
            } else {
              Optional.absent()
            })
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
        putString("state", edited.state)
      }

      logger.trace { "Saved state $toWrite" }
    }
  }
}
