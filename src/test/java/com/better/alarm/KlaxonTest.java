package com.better.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

import com.better.alarm.background.KlaxonServiceCallback;
import com.better.alarm.background.KlaxonServiceDelegate;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.wakelock.WakeLockManager;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Yuriy on 20.08.2017.
 */

public class KlaxonTest {
    @Test
    public void smoke() {
        PowerManager powerManager = mock(PowerManager.class);
        when(powerManager.newWakeLock(anyInt(), anyString())).thenReturn(mock(PowerManager.WakeLock.class));

        IAlarmsManager alarmsManager = mock(IAlarmsManager.class);
        when(alarmsManager.getAlarm(anyInt())).thenReturn(mock(Alarm.class));

        KlaxonServiceCallback klaxonServiceCallback = mock(KlaxonServiceCallback.class);
        when(klaxonServiceCallback.getDefaultUri(anyInt())).thenReturn(mock(Uri.class));
        when(klaxonServiceCallback.createMediaPlayer()).thenReturn(mock(MediaPlayer.class));

        TestScheduler scheduler = new TestScheduler();
        KlaxonServiceDelegate delegate = new KlaxonServiceDelegate(
                new Logger(),
                powerManager,
                mock(AudioManager.class),
                mock(WakeLockManager.class),
                alarmsManager,
                mock(Context.class),
                mock(Resources.class),
                Observable.just(TelephonyManager.CALL_STATE_IDLE),
                Observable.just(2),
                Observable.just(30000),
                klaxonServiceCallback,
                scheduler
        );

        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(Intents.ALARM_ALERT_ACTION);
        delegate.onStartCommand(intent);
        scheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        delegate.onDestroy();
    }
}
