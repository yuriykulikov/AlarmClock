package com.better.alarm.presenter.background;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;

public interface IMediaPlayer {

    public void setDisplay(SurfaceHolder sh);

    public void setSurface(Surface surface);

    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException,
            SecurityException, IllegalStateException;

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException,
            IllegalArgumentException, SecurityException, IllegalStateException;

    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException,
            IllegalStateException;

    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException;

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException,
            IllegalArgumentException, IllegalStateException;

    public void prepare() throws IOException, IllegalStateException;

    public void prepareAsync() throws IllegalStateException;

    public void start() throws IllegalStateException;

    public void stop() throws IllegalStateException;

    public void pause() throws IllegalStateException;

    public void setWakeMode(Context context, int mode);

    public void setScreenOnWhilePlaying(boolean screenOn);

    public int getVideoWidth();

    public int getVideoHeight();

    public boolean isPlaying();

    public void seekTo(int msec) throws IllegalStateException;

    public int getCurrentPosition();

    public int getDuration();

    public void release();

    public void reset();

    public void setAudioStreamType(int streamtype);

    public void setLooping(boolean looping);

    public boolean isLooping();

    public void setVolume(float leftVolume, float rightVolume);

    public void setAudioSessionId(int sessionId) throws IllegalArgumentException, IllegalStateException;

    public int getAudioSessionId();

    public void attachAuxEffect(int effectId);

    public void setAuxEffectSendLevel(float level);

    public void setOnPreparedListener(OnPreparedListener listener);

    public void setOnCompletionListener(OnCompletionListener listener);

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener);

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener);

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener);

    public void setOnErrorListener(OnErrorListener listener);

    public void setOnInfoListener(OnInfoListener listener);

}