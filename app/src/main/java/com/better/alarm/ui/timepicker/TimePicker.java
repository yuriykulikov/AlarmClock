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

package com.better.alarm.ui.timepicker;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.better.alarm.R;
import com.better.alarm.bootstrap.InjectKt;
import com.better.alarm.data.Prefs;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import java.text.DateFormatSymbols;

public class TimePicker extends LinearLayout implements Button.OnClickListener {
  private final Context mContext;

  private final boolean mIs24HoursMode;
  protected int mInputSize = 5;

  private TextView mAmPmLabel;
  protected final Button mNumbers[] = new Button[10];
  protected Button mLeft, mRight;
  protected ImageButton mDelete;
  protected TextView timePickerTime;
  private final TimePickerPresenter presenter;
  private int hours;
  private int minutes;

  private final SparseArray<View> views = new SparseArray<>();
  public static final String[] AM_PM_STRINGS = new DateFormatSymbols().getAmPmStrings();

  public TimePicker(Context context) {
    this(context, null);
  }

  public TimePicker(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
    LayoutInflater layoutInflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    layoutInflater.inflate(R.layout.time_picker_view, this);
    mInputSize = 4;
    mIs24HoursMode =
        isInEditMode() ? true : InjectKt.javaInject(Prefs.class).is24HourFormat().blockingGet();
    presenter = new TimePickerPresenter(mIs24HoursMode);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    View v1 = findViewById(R.id.first);
    View v2 = findViewById(R.id.second);
    View v3 = findViewById(R.id.third);
    View v4 = findViewById(R.id.fourth);
    timePickerTime = (TextView) findViewById(R.id.time_picker_time);
    mDelete = (ImageButton) findViewById(R.id.delete);
    mDelete.setOnClickListener(this);
    mDelete.setOnLongClickListener(
        new OnLongClickListener() {
          @Override
          public boolean onLongClick(View v) {
            presenter.reset();
            return true;
          }
        });

    mNumbers[1] = (Button) v1.findViewById(R.id.key_left);
    mNumbers[2] = (Button) v1.findViewById(R.id.key_middle);
    mNumbers[3] = (Button) v1.findViewById(R.id.key_right);

    mNumbers[4] = (Button) v2.findViewById(R.id.key_left);
    mNumbers[5] = (Button) v2.findViewById(R.id.key_middle);
    mNumbers[6] = (Button) v2.findViewById(R.id.key_right);

    mNumbers[7] = (Button) v3.findViewById(R.id.key_left);
    mNumbers[8] = (Button) v3.findViewById(R.id.key_middle);
    mNumbers[9] = (Button) v3.findViewById(R.id.key_right);

    mLeft = (Button) v4.findViewById(R.id.key_left);
    mNumbers[0] = (Button) v4.findViewById(R.id.key_middle);
    mRight = (Button) v4.findViewById(R.id.key_right);

    for (int i = 0; i < 10; i++) {
      mNumbers[i].setOnClickListener(this);
      mNumbers[i].setText(String.format("%d", i));
    }

    mNumbers[0].setTag(TimePickerPresenter.Key.ZERO);
    mNumbers[1].setTag(TimePickerPresenter.Key.ONE);
    mNumbers[2].setTag(TimePickerPresenter.Key.TWO);
    mNumbers[3].setTag(TimePickerPresenter.Key.THREE);
    mNumbers[4].setTag(TimePickerPresenter.Key.FOUR);
    mNumbers[5].setTag(TimePickerPresenter.Key.FIVE);
    mNumbers[6].setTag(TimePickerPresenter.Key.SIX);
    mNumbers[7].setTag(TimePickerPresenter.Key.SEVEN);
    mNumbers[8].setTag(TimePickerPresenter.Key.EIGHT);
    mNumbers[9].setTag(TimePickerPresenter.Key.NINE);
    mLeft.setTag(TimePickerPresenter.Key.LEFT);
    mRight.setTag(TimePickerPresenter.Key.RIGHT);
    mDelete.setTag(TimePickerPresenter.Key.DELETE);

    mLeft.setOnClickListener(this);
    mRight.setOnClickListener(this);

    Resources res = mContext.getResources();
    mAmPmLabel = (TextView) findViewById(R.id.time_picker_ampm_label);

    if (mIs24HoursMode) {
      mLeft.setText(res.getString(R.string.time_picker_00_label));
      mRight.setText(res.getString(R.string.time_picker_30_label));
      mAmPmLabel.setVisibility(INVISIBLE);
    } else {
      mLeft.setText(AM_PM_STRINGS[0]);
      mRight.setText(AM_PM_STRINGS[1]);
    }

    mLeft.setContentDescription(null);
    mRight.setContentDescription(null);
    views.append(TimePickerPresenter.Key.ONE.ordinal(), mNumbers[1]);
    views.append(TimePickerPresenter.Key.TWO.ordinal(), mNumbers[2]);
    views.append(TimePickerPresenter.Key.THREE.ordinal(), mNumbers[3]);
    views.append(TimePickerPresenter.Key.FOUR.ordinal(), mNumbers[4]);
    views.append(TimePickerPresenter.Key.FIVE.ordinal(), mNumbers[5]);
    views.append(TimePickerPresenter.Key.SIX.ordinal(), mNumbers[6]);
    views.append(TimePickerPresenter.Key.SEVEN.ordinal(), mNumbers[7]);
    views.append(TimePickerPresenter.Key.EIGHT.ordinal(), mNumbers[8]);
    views.append(TimePickerPresenter.Key.NINE.ordinal(), mNumbers[9]);
    views.append(TimePickerPresenter.Key.ZERO.ordinal(), mNumbers[0]);
    views.append(TimePickerPresenter.Key.LEFT.ordinal(), mLeft);
    views.append(TimePickerPresenter.Key.RIGHT.ordinal(), mRight);
    views.append(TimePickerPresenter.Key.DELETE.ordinal(), mDelete);
  }

  @Override
  public void onClick(View v) {
    presenter.onClick((TimePickerPresenter.Key) v.getTag());
  }

  @CheckReturnValue
  public Disposable setSetButton(Button setButton) {
    setButton.setTag(TimePickerPresenter.Key.OK);
    views.append(TimePickerPresenter.Key.OK.ordinal(), setButton);

    final String noAmPm = getContext().getResources().getString(R.string.time_picker_ampm_label);

    return presenter
        .getState()
        .subscribe(
            new Consumer<TimePickerPresenter.State>() {
              @Override
              public void accept(@NonNull TimePickerPresenter.State state) throws Exception {
                timePickerTime.setText(state.getAsText());
                hours = state.getHours();
                minutes = state.getMinutes();

                for (int i = 0; i < views.size(); i++) {
                  views.valueAt(i).setEnabled(false);
                  views.get(TimePickerPresenter.Key.DELETE.ordinal()).setAlpha(0.5f);
                }

                if (state.getAmPm().equals(TimePickerPresenter.AmPm.AM)) {
                  mAmPmLabel.setText(AM_PM_STRINGS[0]);
                } else if (state.getAmPm().equals(TimePickerPresenter.AmPm.PM)) {
                  mAmPmLabel.setText(AM_PM_STRINGS[1]);
                } else {
                  mAmPmLabel.setText(noAmPm);
                }

                for (TimePickerPresenter.Key key : state.getEnabled()) {
                  views.get(key.ordinal()).setEnabled(true);
                  if (key.equals(TimePickerPresenter.Key.DELETE)) {
                    views.get(TimePickerPresenter.Key.DELETE.ordinal()).setAlpha(1f);
                  }
                }
              }
            });
  }

  public int getHours() {
    return hours;
  }

  public int getMinutes() {
    return minutes;
  }
}
