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

package com.better.alarm.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
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
import android.view.View.GONE
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.better.alarm.BuildConfig
import com.better.alarm.R
import com.better.alarm.bootstrap.AlarmApplication
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.AlarmValue
import com.better.alarm.domain.Store
import com.better.alarm.logger.Logger
import com.better.alarm.notifications.NotificationSettings
import com.better.alarm.platform.checkPermissions
import com.better.alarm.ui.details.AlarmDetailsFragment
import com.better.alarm.ui.list.AlarmsListFragment
import com.better.alarm.ui.list.configureBottomDrawer
import com.better.alarm.ui.settings.SettingsFragment
import com.better.alarm.ui.state.BackPresses
import com.better.alarm.ui.state.EditedAlarm
import com.better.alarm.ui.themes.DynamicThemeHandler
import com.better.alarm.ui.toast.formatToast
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.Disposables
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/** This activity displays a list of alarms and optionally a details fragment. */
class AlarmsListActivity() : AppCompatActivity() {
  private val mActionBarHandler: ActionBarHandler by lazy {
    ActionBarHandler(this, viewModel, backPresses)
  }
  private val logger: Logger by globalLogger("AlarmsListActivity")
  private val store: Store by inject()

  private var snackbarDisposable = Disposables.disposed()

  private val viewModel: MainViewModel by viewModel()
  private val backPresses: BackPresses by inject()
  private val dynamicThemeHandler: DynamicThemeHandler by inject()

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    logger.debug { "new $intent, ${intent?.extras}" }
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
    viewModel.editing().value?.writeInto(outState)
  }

  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?) {
    AlarmApplication.startOnce(application)
    setTheme(dynamicThemeHandler.defaultTheme())
    super.onCreate(savedInstanceState)
    viewModel.drawerExpanded.value = intent?.getBooleanExtra("openDrawerOnCreate", false) ?: false
    val prevVersion = savedInstanceState?.getInt("version", BuildConfig.VERSION_CODE)
    if (prevVersion == BuildConfig.VERSION_CODE) {
      val restored = editedAlarmFromSavedInstanceState(savedInstanceState)
      logger.trace { "Restored $this with $restored" }
      restored?.let { viewModel.edit(it) }
    } else {
      viewModel.hideDetails()
    }

    if (!resources.getBoolean(R.bool.isTablet)) {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    setContentView(R.layout.list_activity)
    configureBottomDrawer(
        this, findViewById(R.id.bottom_drawer_container), logger, viewModel, lifecycleScope)
    store
        .alarms()
        .take(1)
        .subscribe { alarms -> checkPermissions(this, alarms.map { it.alarmtone }) }
        .apply {}

    backPresses.onBackPressed(lifecycle) { finish() }
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
            .withLatestFrom(store.uiVisible) { set, uiVisible -> set to uiVisible }
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
          text.textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        .show()
  }

  override fun onResume() {
    super.onResume()
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    NotificationSettings.checkNotificationPermissionsAndSettings(this)
    store.uiVisible.onNext(true)
  }

  override fun onPause() {
    super.onPause()
    store.uiVisible.onNext(false)
    viewModel.awaitStored()
  }

  override fun onStop() {
    super.onStop()
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
    backPresses.backPressed("AlarmsListActivity.onBackPressed")
  }

  private fun configureTransactions() {
    combine(viewModel.editing(), viewModel.layout()) { l, r -> l to r }
        .distinctUntilChanged()
        .onEach { (edited, _) ->
          when {
            isDestroyed -> return@onEach
            edited != null -> showDetails(edited)
            else -> showList()
          }
        }
        .launchIn(lifecycleScope)
  }

  private fun showList() {
    val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)
    val details: AlarmDetailsFragment? = currentFragment as? AlarmDetailsFragment
    logger.trace { "transition from: $details to AlarmsListFragment" }
    supportFragmentManager.commit(allowStateLoss = true) {
      replace(
          R.id.main_fragment_container,
          AlarmsListFragment().apply {
            arguments = Bundle()
            enterTransition = TransitionSet().addTransition(Fade())
            sharedElementEnterTransition = moveTransition()
            allowEnterTransitionOverlap = true
          })
      details?.exitTransition = Fade()
      details?.rowHolder?.run {
        addSharedElement(digitalClock, "clock${details.editedAlarmId}")
        addSharedElement(container, "onOff${details.editedAlarmId}")
        addSharedElement(detailsButton, "detailsButton${details.editedAlarmId}")
      }
    }
    // fade out the bottom drawer using animation
    findViewById<View>(R.id.bottom_drawer_container)
        .apply { visibility = View.VISIBLE }
        .animate()
        .alpha(1f)
        .setDuration(1000)
  }

  private fun showDetails(edited: EditedAlarm) {
    val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)
    if (currentFragment is AlarmDetailsFragment) {
      logger.trace { "skipping fragment transition, because already showing $currentFragment" }
    } else {
      val listFragment = currentFragment as? AlarmsListFragment
      logger.trace { "transition from: $currentFragment to AlarmDetailsFragment" }

      supportFragmentManager.commit(allowStateLoss = true) {
        replace(
            R.id.main_fragment_container,
            AlarmDetailsFragment().apply {
              arguments = Bundle()
              enterTransition = TransitionSet().addTransition(Slide()).addTransition(Fade())
              sharedElementEnterTransition = moveTransition()
              allowEnterTransitionOverlap = true
            })
        listFragment?.exitTransition = Fade()
        listFragment?.transitionRowHolder?.run {
          addSharedElement(digitalClock, "clock")
          addSharedElement(container, "onOff")
          addSharedElement(detailsButton, "detailsButton")
        }
      }
      // fade out the bottom drawer using animation
      findViewById<View>(R.id.bottom_drawer_container).run {
        animate().alpha(0f).setDuration(200).withEndAction {
          visibility = GONE
          viewModel.drawerExpanded.value = false
        }
      }
    }
  }

  private fun moveTransition(): TransitionSet {
    return TransitionSet().apply {
      ordering = TransitionSet.ORDERING_TOGETHER
      addTransition(ChangeBounds())
      addTransition(ChangeTransform())
    }
  }

  /** restores an [EditedAlarm] from SavedInstanceState. Counterpart of [EditedAlarm.writeInto]. */
  @OptIn(ExperimentalSerializationApi::class)
  private fun editedAlarmFromSavedInstanceState(savedInstanceState: Bundle): EditedAlarm? {
    return if (savedInstanceState.getBoolean("isEdited")) {
      val restored =
          ProtoBuf.decodeFromByteArray(
              AlarmValue.serializer(), savedInstanceState.getByteArray("edited") ?: ByteArray(0))
      EditedAlarm(savedInstanceState.getBoolean("isNew"), restored)
    } else {
      null
    }
  }

  /**
   * Saves EditedAlarm into SavedInstanceState. Counterpart of [editedAlarmFromSavedInstanceState]
   */
  @OptIn(ExperimentalSerializationApi::class)
  private fun EditedAlarm.writeInto(outState: Bundle?) {
    val toWrite: EditedAlarm = this
    outState?.run {
      putBoolean("isNew", isNew)
      putBoolean("isEdited", true)
      putByteArray("edited", ProtoBuf.encodeToByteArray(AlarmValue.serializer(), value))
      logger.trace { "Saved state $toWrite" }
    }
  }
}
