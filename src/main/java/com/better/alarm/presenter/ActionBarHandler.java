package com.better.alarm.presenter;

import org.acra.ACRA;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ShareActionProvider;

import com.better.alarm.R;
import com.google.common.base.Preconditions;

/**
 * This class handles options menu and action bar
 * 
 * @author Kate
 * 
 */
public class ActionBarHandler {

    private static final int JELLY_BEAN_MR1 = 17;
    private final Context mContext;

    public ActionBarHandler(Context context) {
        this.mContext = Preconditions.checkNotNull(context);
    }

    /**
     * Delegate {@link Activity#onCreateOptionsMenu(Menu)}
     * 
     * @param menu
     * @param inflater
     * @param actionBar
     * @return
     */
    public boolean onCreateOptionsMenu(Menu menu, MenuInflater inflater, ActionBar actionBar) {
        inflater.inflate(R.menu.settings_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_share);
        ShareActionProvider sp = (ShareActionProvider) menuItem.getActionProvider();

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide what to do with
        // it.
        intent.putExtra(Intent.EXTRA_SUBJECT, "https://play.google.com/store/apps/details?id=com.better.alarm");
        intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=com.better.alarm");

        sp.setShareIntent(intent);

        if (Build.VERSION.SDK_INT < JELLY_BEAN_MR1) {
            MenuItem menuItemDashclock = menu.findItem(R.id.menu_dashclock);
            menuItemDashclock.setVisible(false);
        }

        return true;
    }

    /**
     * Delegate {@link Activity#onOptionsItemSelected(MenuItem)}
     * 
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.menu_item_settings:
            // TODO show details
            mContext.startActivity(new Intent(mContext, SettingsActivity.class));
            return true;

        case R.id.menu_review:
            showReview();
            return true;

        case R.id.menu_bugreport:
            showBugreport();
            return true;

        case R.id.menu_dashclock:
            showDashClock();
            return true;

        case R.id.menu_mp3cutter:
            showMp3();
            return true;
        }
        return false;
    }

    private void showReview() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="
                        + mContext.getApplicationContext().getPackageName()));
                mContext.startActivity(intent);
            }
        });
        builder.setTitle(R.string.review);
        builder.setMessage(R.string.review_message);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }

    private void showDashClock() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="
                        + "net.nurik.roman.dashclock"));
                mContext.startActivity(intent);
            }
        });
        builder.setTitle(R.string.dashclock);
        builder.setMessage(R.string.dashclock_message);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }

    private void showMp3() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=mp3+cutter&c=app"));
                mContext.startActivity(intent);
            }
        });
        builder.setTitle(R.string.mp3cutter);
        builder.setMessage(R.string.mp3cutter_message);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }

    private void showBugreport() {
        final EditText report = new EditText(mContext);
        report.setHint(R.string.bugreport_hint);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ACRA.getErrorReporter().handleSilentException(new Exception(report.getText().toString()));
            }
        });
        builder.setTitle(R.string.bugreport);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setView(report);
        builder.create().show();
    }

}
