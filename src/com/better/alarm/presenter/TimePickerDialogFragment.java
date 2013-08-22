/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License
 */

package com.better.alarm.presenter;

import org.acra.ACRA;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.view.TimePicker;
import com.github.androidutils.logger.Logger;

/**
 * Dialog to set alarm time.
 */
public class TimePickerDialogFragment extends DialogFragment {

    private static final String KEY_ALARM = "alarm";

    private Button mSet, mCancel;
    private TimePicker mPicker;
    private final Logger log = Logger.getDefaultLogger();

    private Alarm alarm;

    /**
     * 
     * @param id
     * @param handler
     * @return
     */
    public static TimePickerDialogFragment newInstance(int id) {
        final TimePickerDialogFragment frag = new TimePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_ALARM, id);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE,
                DynamicThemeHandler.getInstance().getIdForName(TimePickerDialogFragment.class.getName()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.time_picker_dialog, null);

        try {
            int id = getArguments().getInt(KEY_ALARM);
            alarm = AlarmsManager.getAlarmsManager().getAlarm(id);

            mSet = (Button) v.findViewById(R.id.set_button);
            mCancel = (Button) v.findViewById(R.id.cancel_button);
            mCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    notifyOnCancelListener();
                    dismiss();
                }
            });
            mPicker = (TimePicker) v.findViewById(R.id.time_picker);
            mPicker.setSetButton(mSet);
            mSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Activity activity = getActivity();
                    if (activity instanceof AlarmTimePickerDialogHandler) {
                        final AlarmTimePickerDialogHandler act = (AlarmTimePickerDialogHandler) activity;
                        act.onDialogTimeSet(alarm, mPicker.getHours(), mPicker.getMinutes());
                    } else {
                        log.e("Error! Activities that use TimePickerDialogFragment must implement "
                                + "AlarmTimePickerDialogHandler");
                    }
                    dismiss();
                }
            });
        } catch (AlarmNotFoundException e) {
            Logger.getDefaultLogger().e("oops", e);
            ACRA.getErrorReporter().handleSilentException(e);
            dismiss();
        }
        return v;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        notifyOnCancelListener();
        super.onCancel(dialog);
    }

    private void notifyOnCancelListener() {
        Activity activity = getActivity();
        if (activity instanceof OnAlarmTimePickerCanceledListener) {
            final OnAlarmTimePickerCanceledListener act = (OnAlarmTimePickerCanceledListener) getActivity();
            act.onTimePickerCanceled(alarm);
        }
    }

    public interface AlarmTimePickerDialogHandler {
        void onDialogTimeSet(Alarm alarm, int hourOfDay, int minute);
    }

    public interface OnAlarmTimePickerCanceledListener {
        void onTimePickerCanceled(Alarm alarm);
    }

    public static void showTimePicker(Alarm alarm, FragmentManager fragmentManager) {
        final FragmentTransaction ft = fragmentManager.beginTransaction();
        final Fragment prev = fragmentManager.findFragmentByTag("time_dialog");
        if (prev != null) {
            ft.remove(prev);
        }

        final TimePickerDialogFragment fragment = TimePickerDialogFragment.newInstance(alarm.getId());
        fragment.show(ft, "time_dialog");
    }
}
