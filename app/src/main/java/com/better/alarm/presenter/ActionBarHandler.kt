package com.better.alarm.presenter

import android.app.ActionBar
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ShareActionProvider
import android.widget.TextView
import com.better.alarm.BuildConfig
import com.better.alarm.R
import com.better.alarm.interfaces.IAlarmsManager
import io.reactivex.disposables.Disposables
import org.acra.ACRA
import kotlin.jvm.internal.Intrinsics


/**
 * This class handles options menu and action bar
 *
 * @author Kate
 */
class ActionBarHandler(context: Activity, private val store: UiStore, private val alarms: IAlarmsManager) {
    private val mContext: Context
    private var sub = Disposables.disposed()

    init {
        Intrinsics.checkNotNull(context, "context")
        this.mContext = context
    }

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

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)

            // Add data to the intent, the receiving app will decide what to do with
            // it.
            putExtra(Intent.EXTRA_SUBJECT, "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
        }

        val menuItem = menu.findItem(R.id.menu_share)
        val sp = menuItem.actionProvider as ShareActionProvider
        sp.setShareIntent(intent)

        sub = store.editing().subscribe { edited ->
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
            R.id.menu_item_settings -> mContext.startActivity(Intent(mContext, SettingsActivity::class.java))
            R.id.menu_review -> showReview()
            R.id.menu_bugreport -> showBugreport()
            R.id.set_alarm_menu_delete_alarm -> deleteAlarm()
            R.id.menu_about -> showAbout()
            android.R.id.home -> store.onBackPressed().onNext("ActionBar")
        }
        return true
    }

    private fun showAbout() {
        AlertDialog.Builder(mContext).apply {
            setTitle(mContext.getString(R.string.menu_about_title))
            setView(View.inflate(mContext, R.layout.about, null).apply {
                findViewById<TextView>(R.id.about_text).run {
                    setText(R.string.menu_about_content)
                    movementMethod = LinkMovementMethod.getInstance()
                }
            })
            setPositiveButton(android.R.string.ok) { _, _ ->
            }
                    .create()
                    .show()
        }
    }

    private fun deleteAlarm() {
        AlertDialog.Builder(mContext).apply {
            setTitle(mContext.getString(R.string.delete_alarm))
            setMessage(mContext.getString(R.string.delete_alarm_confirm))
            setPositiveButton(android.R.string.ok) { _, _ ->
                alarms.getAlarm(store.editing().blockingFirst().id())?.delete()
                store.hideDetails()
            }
            setNegativeButton(android.R.string.cancel, null)
        }.show()
    }

    private fun showReview() {
        AlertDialog.Builder(mContext).apply {
            setPositiveButton(android.R.string.ok) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID))
                mContext.startActivity(intent)
            }
            setTitle(R.string.menu_review)
            setMessage(R.string.menu_review_message)
            setCancelable(true)
            setNegativeButton(android.R.string.cancel, EmptyClickListener())
        }
                .create()
                .show()
    }

    private fun showBugreport() {
        val inflator = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val dialogView = inflator.inflate(R.layout.dialog_bugreport, null)
        val report = dialogView.findViewById<TextView>(R.id.dialog_bugreport_edittext)

        dialogView.findViewById<TextView>(R.id.dialog_bugreport_textview).movementMethod = LinkMovementMethod.getInstance()

        AlertDialog.Builder(mContext).apply {
            setPositiveButton(android.R.string.ok) { _, _ ->
                ACRA.getErrorReporter().putCustomData("USER_COMMENT", report.text.toString())
                ACRA.getErrorReporter().handleSilentException(Exception(report.text.toString()))
            }
            setTitle(R.string.menu_bugreport)
            setCancelable(true)
            setNegativeButton(android.R.string.cancel, EmptyClickListener())
            setView(dialogView)
        }
                .create()
                .show()
    }

    private class EmptyClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            //this listener does not do much
        }
    }
}
