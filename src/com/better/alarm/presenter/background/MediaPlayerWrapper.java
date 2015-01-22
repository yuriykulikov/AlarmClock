package com.better.alarm.presenter.background;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.media.MediaPlayer;
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

public class MediaPlayerWrapper implements IMediaPlayer {
    private final MediaPlayer player;

    MediaPlayerWrapper(MediaPlayer player) {
        this.player = player;
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        player.setDisplay(sh);
    }

    @Override
    public void setSurface(Surface surface) {
        player.setSurface(surface);
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException,
            SecurityException, IllegalStateException {
        player.setDataSource(context, uri);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException,
            IllegalArgumentException, SecurityException, IllegalStateException {
        player.setDataSource(context, uri, headers);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException,
            IllegalStateException {
        player.setDataSource(path);
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        player.setDataSource(fd);
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException,
            IllegalArgumentException, IllegalStateException {
        player.setDataSource(fd, offset, length);
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        player.prepare();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        player.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        player.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        player.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        player.pause();
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        player.setWakeMode(context, mode);
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        player.setScreenOnWhilePlaying(screenOn);
    }

    @Override
    public int getVideoWidth() {
        return player.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return player.getVideoHeight();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        player.seekTo(msec);
    }

    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return player.getDuration();
    }

    @Override
    public void release() {
        player.release();
    }

    @Override
    public void reset() {
        player.reset();
    }

    @Override
    public void setAudioStreamType(int streamtype) {
        player.setAudioStreamType(streamtype);
    }

    @Override
    public void setLooping(boolean looping) {
        player.setLooping(looping);
    }

    @Override
    public boolean isLooping() {
        return player.isLooping();
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        player.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setVolume(float volume) {
        player.setVolume(volume, volume);
    }

    @Override
    public void setAudioSessionId(int sessionId) throws IllegalArgumentException, IllegalStateException {
        player.setAudioSessionId(sessionId);
    }

    @Override
    public int getAudioSessionId() {
        return player.getAudioSessionId();
    }

    @Override
    public void attachAuxEffect(int effectId) {
        player.attachAuxEffect(effectId);
    }

    @Override
    public void setAuxEffectSendLevel(float level) {
        player.setAuxEffectSendLevel(level);
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        player.setOnPreparedListener(listener);
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        player.setOnCompletionListener(listener);
    }

    @Override
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        player.setOnBufferingUpdateListener(listener);
    }

    @Override
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        player.setOnSeekCompleteListener(listener);
    }

    @Override
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        player.setOnVideoSizeChangedListener(listener);
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        player.setOnErrorListener(listener);
    }

    @Override
    public void setOnInfoListener(OnInfoListener listener) {
        player.setOnInfoListener(listener);
    }
}
