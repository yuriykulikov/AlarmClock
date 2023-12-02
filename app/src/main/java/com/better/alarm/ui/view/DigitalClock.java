/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.better.alarm.ui.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.better.alarm.R;
import com.better.alarm.bootstrap.InjectKt;
import com.better.alarm.data.Prefs;
import io.reactivex.Single;
import java.text.DateFormatSymbols;
import java.util.Calendar;

/** Displays the time */
public class DigitalClock extends LinearLayout {
  public static final String M12 = "h:mm";
  public static final String M24 = "kk:mm";

  private Calendar mCalendar;
  private String mFormat;
  private TextView mTimeDisplay;
  private AmPm mAmPm;
  private ContentObserver mFormatChangeObserver;
  private boolean mLive = true;
  private boolean mAttached;

  private final Single<Boolean> is24HoutFormat;

  /* called by system on minute ticks */
  private final Handler mHandler = new Handler();
  private final BroadcastReceiver mIntentReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (mLive && intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            mCalendar = Calendar.getInstance();
          }
          // Post a runnable to avoid blocking the broadcast.
          mHandler.post(
              new Runnable() {
                @Override
                public void run() {
                  updateTime();
                }
              });
        }
      };

  private static class AmPm {
    private final TextView mAmPm;
    private final String mAmString, mPmString;

    public AmPm(View parent) {
      mAmPm = (TextView) parent.findViewById(R.id.digital_clock_am_pm);

      String[] ampm = new DateFormatSymbols().getAmPmStrings();
      mAmString = ampm[0];
      mPmString = ampm[1];
    }

    public void setShowAmPm(boolean show) {
      if (mAmPm != null) {
        // check for null to be able to use ADT preview
        mAmPm.setVisibility(show ? View.VISIBLE : View.GONE);
      }
    }

    public void setIsMorning(boolean isMorning) {
      if (mAmPm != null) {
        mAmPm.setText(isMorning ? mAmString : mPmString);
      }
    }
  }

  private class FormatChangeObserver extends ContentObserver {
    FormatChangeObserver() {
      super(new Handler());
    }

    @Override
    public void onChange(boolean selfChange) {
      setDateFormat();
      updateTime();
    }
  }

  public DigitalClock(Context context) {
    this(context, null);
  }

  public DigitalClock(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (isInEditMode()) {
      is24HoutFormat = Single.just(true);
    } else {
      is24HoutFormat = InjectKt.javaInject(Prefs.class).is24HourFormat();
    }
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mTimeDisplay = (TextView) findViewById(R.id.digital_clock_time);
    mAmPm = new AmPm(this);
    mCalendar = Calendar.getInstance();

    setDateFormat();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (mAttached) return;
    mAttached = true;

    if (mLive) {
      /* monitor time ticks, time changed, timezone */
      IntentFilter filter = new IntentFilter();
      filter.addAction(Intent.ACTION_TIME_TICK);
      filter.addAction(Intent.ACTION_TIME_CHANGED);
      filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
      getContext().registerReceiver(mIntentReceiver, filter);
    }

    /* monitor 12/24-hour display preference */
    mFormatChangeObserver = new FormatChangeObserver();
    getContext()
        .getContentResolver()
        .registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);

    updateTime();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (!mAttached) return;
    mAttached = false;

    if (mLive) {
      getContext().unregisterReceiver(mIntentReceiver);
    }
    getContext().getContentResolver().unregisterContentObserver(mFormatChangeObserver);
  }

  public void updateTime(Calendar c) {
    mCalendar = c;
    updateTime();
  }

  private void updateTime() {
    if (mLive) {
      mCalendar.setTimeInMillis(System.currentTimeMillis());
    }

    CharSequence newTime = DateFormat.format(mFormat, mCalendar);
    if (mTimeDisplay != null) {
      mTimeDisplay.setText(newTime);
    }
    if (mAmPm != null) {
      mAmPm.setIsMorning((int) mCalendar.get(Calendar.AM_PM) == 0 || isInEditMode());
    }
  }

  private void setDateFormat() {
    mFormat = is24HoutFormat.blockingGet() ? M24 : M12;
    mAmPm.setShowAmPm(M12.equals(mFormat) || isInEditMode());
  }

  public void setLive(boolean live) {
    mLive = live;
  }

  public void setColor(int color) {
    mAmPm.mAmPm.setTextColor(color);
    mTimeDisplay.setTextColor(color);
  }
}
