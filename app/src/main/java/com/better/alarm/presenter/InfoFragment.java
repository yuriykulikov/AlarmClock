package com.better.alarm.presenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.better.alarm.R;
import com.better.alarm.configuration.InjectKt;
import com.better.alarm.configuration.Prefs;
import com.better.alarm.configuration.Store;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.util.Optional;

import java.util.Calendar;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * @author Yuriy
 */
public class InfoFragment extends Fragment implements ViewFactory {
    private static final String DM12 = "E h:mm aa";
    private static final String DM24 = "E kk:mm";

    private TextSwitcher textView;
    private TextSwitcher remainingTime;

    private AlarmValue alarm;

    private TickReceiver mTickReceiver;

    private long milliseconds;

    private boolean isPrealarm;

    private final Prefs prefs = InjectKt.globalInject(Prefs.class).getValue();
    private final Store store = InjectKt.globalInject(Store.class).getValue();

    private Disposable nextDisposable;

    private final class TickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (alarm != null) {
                formatString();
            }
        }
    }

    private class AlarmChangedReceiver implements Consumer<Optional<Store.Next>> {
        @Override
        public void accept(@NonNull Optional<Store.Next> nextOptional) throws Exception {
            if (nextOptional.isPresent()) {
                alarm = nextOptional.get().alarm();
                String format = prefs.is24HourFormat().blockingGet() ? DM24 : DM12;

                milliseconds = nextOptional.get().nextNonPrealarmTime();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(milliseconds);

                String timeString = (String) DateFormat.format(format, calendar);
                textView.setText(timeString);
                isPrealarm = nextOptional.get().isPrealarm();
                formatString();
            } else {
                textView.setText("");
                remainingTime.setText("");
                alarm = null;
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTickReceiver = new TickReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.info_fragment, container, false);
        textView = (TextSwitcher) view.findViewById(R.id.info_fragment_text_view);
        remainingTime = (TextSwitcher) view.findViewById(R.id.info_fragment_text_view_remaining_time);

        textView.setFactory(this);
        remainingTime.setFactory(this);

        Animation in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        textView.setInAnimation(in);
        textView.setOutAnimation(out);
        remainingTime.setInAnimation(in);
        remainingTime.setOutAnimation(out);
        getActivity().registerReceiver(mTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        nextDisposable = store.next().subscribe(new AlarmChangedReceiver());
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mTickReceiver);
        nextDisposable.dispose();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from now"
     */
    private String formatRemainingTimeString(long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = days == 0 ? "" : days == 1 ? getActivity().getString(R.string.day) : getActivity().getString(
                R.string.days, Long.toString(days));

        String minSeq = minutes == 0 ? "" : minutes == 1 ? getActivity().getString(R.string.minute) : getActivity()
                .getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = hours == 0 ? "" : hours == 1 ? getActivity().getString(R.string.hour) : getActivity()
                .getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) | (dispHour ? 2 : 0) | (dispMinute ? 4 : 0);

        String[] formats = getActivity().getResources().getStringArray(R.array.alarm_set_short);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    private void formatString() {
        if (isPrealarm) {
            int duration = prefs.getPreAlarmDuration().getValue();
            remainingTime.setText(formatRemainingTimeString(milliseconds) + "\n"
                    + getResources().getString(R.string.info_fragment_prealarm_summary, duration));
        } else {
            remainingTime.setText(formatRemainingTimeString(milliseconds));
        }
    }

    @Override
    public View makeView() {
        TextView t = (TextView) getActivity().getLayoutInflater().inflate(R.layout.info_fragment_text, null);
        return t;
    }
}