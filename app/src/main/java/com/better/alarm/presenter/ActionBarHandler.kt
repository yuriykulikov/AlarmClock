package com.better.alarm.presenter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import com.better.alarm.BuildConfig
import com.better.alarm.R
import com.better.alarm.bugreports.BugReporter
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.lollipop
import io.reactivex.disposables.Disposables

/**
 * This class handles options menu and action bar
 *
 * @author Kate
 */
class ActionBarHandler(
    private val mContext: AppCompatActivity,
    private val store: UiStore,
    private val alarms: IAlarmsManager,
    private val reporter: BugReporter
) {
  private var sub = Disposables.disposed()

  /**
   * Delegate [Activity.onCreateOptionsMenu]
   *
   * @param menu
   * @param inflater
   * @param actionBar
   * @return
   */
  fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater, actionBar: ActionBar): Boolean {
    inflater.inflate(R.menu.menu_action_bar, menu)

    val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"

          addFlags(
              when {
                lollipop() -> Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                else -> @Suppress("DEPRECATION") Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
              })

          // Add data to the intent, the receiving app will decide what to do with
          // it.
          putExtra(
              Intent.EXTRA_SUBJECT,
              "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
          putExtra(
              Intent.EXTRA_TEXT,
              "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
        }

    val menuItem = menu.findItem(R.id.menu_share)
    val sp =
        MenuItemCompat.getActionProvider(menuItem) as androidx.appcompat.widget.ShareActionProvider
    sp.setShareIntent(intent)

    sub =
        store.editing().subscribe { edited ->
          val showDelete = edited.isEdited && !edited.isNew

          menu.findItem(R.id.set_alarm_menu_delete_alarm).isVisible = showDelete

          actionBar.setDisplayHomeAsUpEnabled(edited.isEdited)
        }

    return true
  }

  fun onDestroy() {
    sub.dispose()
  }

  /**
   * Delegate [Activity.onOptionsItemSelected]
   *
   * @param item
   * @return
   */
  fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_settings ->
          mContext.startActivity(Intent(mContext, SettingsActivity::class.java))
      R.id.menu_review -> showSayThanks()
      R.id.menu_bugreport -> showBugreport()
      R.id.set_alarm_menu_delete_alarm -> deleteAlarm()
      R.id.menu_about -> showAbout()
      android.R.id.home -> store.onBackPressed().onNext("ActionBar")
    }
    return true
  }

  private fun showAbout() {
    AlertDialog.Builder(mContext)
        .apply {
          setTitle(mContext.getString(R.string.menu_about_title))
          setView(
              View.inflate(mContext, R.layout.dialog_about, null).apply {
                findViewById<TextView>(R.id.dialog_about_text).run {
                  setText(R.string.dialog_about_content)
                  movementMethod = LinkMovementMethod.getInstance()
                }
              })
          setPositiveButton(android.R.string.ok) { _, _ -> }
        }
        .create()
        .show()
  }

  private fun deleteAlarm() {
    AlertDialog.Builder(mContext)
        .apply {
          setTitle(mContext.getString(R.string.delete_alarm))
          setMessage(mContext.getString(R.string.delete_alarm_confirm))
          setPositiveButton(android.R.string.ok) { _, _ ->
            alarms.getAlarm(store.editing().blockingFirst().id())?.delete()
            store.hideDetails()
          }
          setNegativeButton(android.R.string.cancel, null)
        }
        .show()
  }

  private fun showSayThanks() {
    val inflator = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    val dialogView =
        inflator.inflate(R.layout.dialog_say_thanks, null).apply {
          findViewById<Button>(R.id.dialog_say_thanks_button_review).setOnClickListener {
            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID))
            mContext.startActivity(intent)
          }

          findViewById<TextView>(R.id.dialog_say_thanks_text_as_button_donate_premium)
              .movementMethod = LinkMovementMethod.getInstance()
        }

    AlertDialog.Builder(mContext)
        .apply {
          setPositiveButton(android.R.string.ok) { _, _ -> }
          setTitle(R.string.dialog_say_thanks_title)
          setView(dialogView)
          setCancelable(true)
        }
        .create()
        .show()
  }

  private fun showBugreport() {
    val inflator = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    val dialogView = inflator.inflate(R.layout.dialog_bugreport, null)

    dialogView.findViewById<TextView>(R.id.dialog_bugreport_textview).movementMethod =
        LinkMovementMethod.getInstance()

    AlertDialog.Builder(mContext)
        .apply {
          setPositiveButton(android.R.string.ok) { _, _ -> reporter.sendUserReport() }
          setTitle(R.string.menu_bugreport)
          setCancelable(true)
          setNegativeButton(android.R.string.cancel) { _, _ -> }
          setView(dialogView)
        }
        .create()
        .show()
  }
}
