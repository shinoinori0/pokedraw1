package com.example.pokedraw;

import android.content.Context;
import android.media.MediaPlayer;

public class MusicManager {

    private static MusicManager instance;
    private MediaPlayer mediaPlayer;
    private boolean muted = false;

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) instance = new MusicManager();
        return instance;
    }

    public void start(Context context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.bg_music);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.5f, 0.5f);
        }
        if (!muted && !mediaPlayer.isPlaying()) mediaPlayer.start();
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    public void resume() {
        if (mediaPlayer != null && !muted && !mediaPlayer.isPlaying()) mediaPlayer.start();
    }

    public void setMuted(boolean mute) {
        muted = mute;
        if (mediaPlayer == null) return;
        if (muted && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else if (!muted && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public boolean isMuted() { return muted; }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
