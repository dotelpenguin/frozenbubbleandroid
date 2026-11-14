/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 *
 * MusicFocus.java - asks Android for "audio focus" before the music
 * plays, so the game's music makes way for phone calls, other apps'
 * music and videos, navigation prompts and alarms, and stops when
 * headphones are unplugged.
 */

package com.efortin.frozenbubble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/**
 * Audio focus for one music player.  The music only plays while it
 * holds focus:
 * <ul>
 * <li>another app plays music or a video: the music pauses, and plays
 * again the next time the game or menu resumes;
 * <li>a phone call, alarm or voice assistant: the music pauses, and
 * carries on by itself when it ends;
 * <li>a navigation prompt or notification sound: the music is turned
 * down until it ends (Android 8+ does this itself);
 * <li>headphones unplugged or disconnected: the music pauses, and
 * {@link Listener#onNoisy()} is called.
 * </ul>
 */
final class MusicFocus implements AudioManager.OnAudioFocusChangeListener {

  /** What the player does when focus changes.  Called on the UI thread. */
  interface Listener {
    /** Focus came back (or arrived late): play if wanted, full volume. */
    void onFocusGain();
    /** Paused for a while (a call); onFocusGain() follows. */
    void onFocusPause();
    /** Another app took over: stop until asked to play again. */
    void onFocusLoss();
    /** Play quietly for a moment. */
    void onDuck();
    /** Headphones were unplugged: stop until asked to play again. */
    void onNoisy();
  }

  private final AudioManager audio;
  private final Context      context;
  private final Listener     listener;
  private AudioFocusRequest  request;   // Android 8+
  private boolean            noisyRegistered = false;

  private final BroadcastReceiver noisy = new BroadcastReceiver() {
    @Override
    public void onReceive(Context c, Intent intent) {
      if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
        listener.onNoisy();
      }
    }
  };

  MusicFocus(Context context, Listener listener) {
    this.context  = context.getApplicationContext();
    this.listener = listener;
    this.audio    = (AudioManager) this.context.getSystemService(
        Context.AUDIO_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build())
          .setAcceptsDelayedFocusGain(true)   // asked during a call
          .setOnAudioFocusChangeListener(this,
              new Handler(Looper.getMainLooper()))
          .build();
    }
  }

  /**
   * Ask for focus.
   * @return true if the music may play now.  If not, it may still get
   * focus later (asked during a call), and onFocusGain() is called then.
   */
  synchronized boolean request() {
    if (audio == null) {
      return true;
    }
    int result;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      result = audio.requestAudioFocus(request);
    }
    else {
      result = requestOld();
    }
    watchNoisy(true);
    return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
  }

  /** Give focus back (the music was paused or stopped). */
  synchronized void abandon() {
    watchNoisy(false);
    if (audio == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audio.abandonAudioFocusRequest(request);
    }
    else {
      abandonOld();
    }
  }

  @SuppressWarnings("deprecation")   // the only way before Android 8
  private int requestOld() {
    return audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                                   AudioManager.AUDIOFOCUS_GAIN);
  }

  @SuppressWarnings("deprecation")   // the only way before Android 8
  private void abandonOld() {
    audio.abandonAudioFocus(this);
  }

  private void watchNoisy(boolean on) {
    if (on == noisyRegistered) {
      return;
    }
    try {
      if (on) {
        IntentFilter filter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.registerReceiver(noisy, filter,
                                   Context.RECEIVER_NOT_EXPORTED);
        }
        else {
          context.registerReceiver(noisy, filter);
        }
      }
      else {
        context.unregisterReceiver(noisy);
      }
      noisyRegistered = on;
    } catch (IllegalArgumentException e) {
      noisyRegistered = false;   // was not registered
    }
  }

  @Override
  public void onAudioFocusChange(int change) {
    switch (change) {
      case AudioManager.AUDIOFOCUS_GAIN:
        listener.onFocusGain();
        break;
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
        listener.onFocusPause();
        break;
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
        listener.onDuck();
        break;
      case AudioManager.AUDIOFOCUS_LOSS:
        abandon();
        listener.onFocusLoss();
        break;
      default:
        break;
    }
  }
}
