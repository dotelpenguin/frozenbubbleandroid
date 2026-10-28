/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 * Additional source - Copyright (c) 2013 Eric Fortin.
 *
 * This code is distributed under the GNU General Public License
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to:
 * Free Software Foundation, Inc.
 * 675 Mass Ave
 * Cambridge, MA 02139, USA
 *
 * Artwork:
 *    Alexis Younes <73lab at free.fr>
 *      (everything but the bubbles)
 *    Amaury Amblard-Ladurantie <amaury at linuxfr.org>
 *      (the bubbles)
 *
 * Soundtrack:
 *    Matthias Le Bidan <matthias.le_bidan at caramail.com>
 *      (the three musics and all the sound effects)
 *
 * Design & Programming:
 *    Guillaume Cottenceau <guillaume.cottenceau at free.fr>
 *      (design and manage the project, whole Perl sourcecode)
 *
 * Java version:
 *    Glenn Sanson <glenn.sanson at free.fr>
 *      (whole Java sourcecode, including JIGA classes
 *             http://glenn.sanson.free.fr/jiga/)
 *
 * Android port:
 *    Pawel Aleksander Fedorynski <pfedor@fuw.edu.pl>
 *    Eric Fortin <videogameboy76 at yahoo.com>
 *    Copyright (c) Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 */

/*
 *  This file is derived from the LunarLander.java file which is part of
 *  the Lunar Lander game included with Android documentation.  The
 *  copyright notice for the Lunar Lander game is reproduced below.
 */

/*
 * Copyright (c) 2007 Google Inc.
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

package org.jfedor.frozenbubble;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.jfedor.frozenbubble.GameScreen.eventEnum;
import org.jfedor.frozenbubble.GameScreen.stateEnum;
import org.jfedor.frozenbubble.GameView.GameListener;
import org.jfedor.frozenbubble.GameView.GameThread;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.efortin.frozenbubble.AccelerometerManager;
import com.efortin.frozenbubble.AccelerometerManager.AccelerometerListener;
import com.efortin.frozenbubble.CustomMusic;
import com.efortin.frozenbubble.HomeScreen;
import com.efortin.frozenbubble.ModPlayer;
import com.efortin.frozenbubble.Preferences;
import com.efortin.frozenbubble.PreferencesActivity;
import com.efortin.frozenbubble.ScrollingCredits;
import com.efortin.frozenbubble.VirtualInput;
import com.peculiargames.andmodplug.PlayerThread;
import com.peculiargames.andmodplug.PlayerThread.PlayerListener;

public class FrozenBubble extends Activity
  implements GameListener,
             AccelerometerListener,
             PlayerListener{
  public final static boolean GAME_NORMAL     = false;
  public final static boolean GAME_COLORBLIND = true;

  public final static int MENU_COLORBLIND_ON  = 1;
  public final static int MENU_COLORBLIND_OFF = 2;
  public final static int MENU_FULLSCREEN_ON  = 3;
  public final static int MENU_FULLSCREEN_OFF = 4;
  public final static int MENU_SOUND_OPTIONS  = 5;
  public final static int MENU_DONT_RUSH_ME   = 6;
  public final static int MENU_RUSH_ME        = 7;
  public final static int MENU_NEW_GAME       = 8;
  public final static int MENU_ABOUT          = 9;
  public final static int MENU_EDITOR         = 10;
  public final static int MENU_TARGET_MODE    = 11;

  public final static int AIM_TO_SHOOT    = 0;
  public final static int POINT_TO_SHOOT  = 1;
  public final static int ROTATE_TO_SHOOT = 2;

  public final static int LOCALE_LOCAL     = 0;
  public final static int LOCALE_WIFI      = 1;
  public final static int LOCALE_INTERNET  = 2;
  public final static int LOCALE_BLUETOOTH = 3;

  public final static int CPU   = 0;
  public final static int HUMAN = 1;

  public static boolean arcadeGame = false;
  public static int     gameLocale = LOCALE_LOCAL;
  public static int     myPlayerId = VirtualInput.PLAYER1; 
  public static int     numPlayers = 0;
  public static int     opponentId = CPU;
  public static boolean playerSave = false;

  private static Preferences prefs = new Preferences();

  public final static String EDITORACTION = "org.jfedor.frozenbubble.GAME";
  public final static String PREFS_NAME   = "frozenbubble";
  public final static String SAVE_GAME    = "FrozenBubble.save";
  /* Preference recording which kind of game the save file holds. */
  public final static String SAVED_GAME_MODE    = "savedGameMode";
  public final static String SAVED_MODE_ARCADE  = "arcade";
  public final static String SAVED_MODE_PUZZLE  = "puzzle";
  public final static String SAVED_MODE_2PLAYER = "2player";

  /*
   * Saved games: one per mode (Arcade, Puzzle, 2 Player), so starting
   * one kind of game never replaces another's save.  SAVED_GAME_MODE
   * remembers the mode saved most recently.
   */
  private final static String SAVED_DETAIL = "savedGameDetail_";

  /** The file holding a mode's saved game. */
  public static String saveFileName(String slot) {
    return "FrozenBubble-" + slot + ".save";
  }

  /** <code>true</code> if the mode (a SAVED_MODE_ value) has a save. */
  public static boolean hasSavedGame(Context context, String slot) {
    return context.getFileStreamPath(saveFileName(slot)).exists();
  }

  public static boolean hasAnySavedGame(Context context) {
    return hasSavedGame(context, SAVED_MODE_ARCADE) ||
           hasSavedGame(context, SAVED_MODE_PUZZLE) ||
           hasSavedGame(context, SAVED_MODE_2PLAYER);
  }

  /**
   * Where a saved game left off, for the Continue popup, e.g.
   * "Puzzle - Level 23".
   */
  public static String savedGameTitle(Context context, String slot) {
    int detail = PreferenceManager.getDefaultSharedPreferences(context)
        .getInt(SAVED_DETAIL + slot, -1);
    if (SAVED_MODE_PUZZLE.equals(slot)) {
      return (detail > 0) ?
          context.getString(R.string.save_title_puzzle_level, detail) :
          context.getString(R.string.save_title_puzzle);
    }
    if (SAVED_MODE_ARCADE.equals(slot)) {
      return (detail > 0) ?
          context.getString(R.string.save_title_arcade_colors, detail) :
          context.getString(R.string.save_title_arcade);
    }
    return context.getString((detail == CPU) ? R.string.save_title_2p_cpu :
                                               R.string.save_title_2p);
  }

  /**
   * Older versions kept one save for every mode: move it to its mode's
   * slot, once.
   */
  public static void migrateSavedGame(Context context) {
    java.io.File old = context.getFileStreamPath(SAVE_GAME);
    if (!old.exists()) {
      return;
    }
    String slot = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(SAVED_GAME_MODE, SAVED_MODE_PUZZLE);
    if (!SAVED_MODE_ARCADE.equals(slot) && !SAVED_MODE_2PLAYER.equals(slot)) {
      slot = SAVED_MODE_PUZZLE;
    }
    java.io.File slotFile = context.getFileStreamPath(saveFileName(slot));
    if (slotFile.exists() || !old.renameTo(slotFile)) {
      old.delete();
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit()
        .putString(SAVED_GAME_MODE, slot)
        .putBoolean("playerSave", hasAnySavedGame(context))
        .commit();
  }

  /** The save slot of the game being played. */
  private static String currentSlot() {
    return (numPlayers > 1) ? SAVED_MODE_2PLAYER :
           (arcadeGame ? SAVED_MODE_ARCADE : SAVED_MODE_PUZZLE);
  }
  public final static String TAG          = "FrozenBubble.java";

  private boolean    activityCustomStarted = false;
  private boolean    allowUnpause          = true;
  private int        currentOrientation;
  private GameThread mGameThread           = null;
  private GameView   mGameView             = null;
  private ModPlayer  myModPlayer           = null;
  private int        lastSongIndex         = -1;  // index of the song playing
  private int        songSkip              = 0;   // songs skipped this game
  private OrientationEventListener myOrientationEventListener = null;

  private final int[] MODlist = {
    R.raw.ambientpower,
    R.raw.ambientlight,
    R.raw.androidrupture,
    R.raw.artificial,
    R.raw.aftertherain,
    R.raw.bluestars,
    R.raw.chungababe,
    R.raw.crystalhammer,
    R.raw.dreamscope,
    R.raw.freefall,
    R.raw.gaeasawakening,
    R.raw.homesick,
    R.raw.ifcrystals,
    R.raw.popcorn,
    R.raw.stardustmemories,
    R.raw.sunshineofthemorningsun,
    R.raw.technostyleiii
  };

  /*
   * Following are ancestor superclass overrides.
   */

  /*
   * (non-Javadoc)
   * @see android.app.Activity#onCreate(android.os.Bundle)
   * This method is called when the activity is started.  The activity
   * may have been reconfigured or the system may have killed the
   * process, after which it regained focus to invoke this method.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    //if (savedInstanceState != null)
    //{
    //  Log.i(TAG, "FrozenBubble.onCreate(...)");
    //}
    //else
    //{
    //  Log.i(TAG, "FrozenBubble.onCreate(null)");
    //}
    super.onCreate(savedInstanceState);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    /*
     * Allow editor functionalities.
     */
    Intent intent = getIntent();
    try {
      if ((null == intent) || (null == intent.getExtras()) ||
          !intent.getExtras().containsKey("levels"))
        startDefaultGame(intent, savedInstanceState);
      else
        startCustomGame(intent);
    } catch (RuntimeException e) {
      /*
       * This screen can be opened by other apps (level editors), so a
       * malformed request must not crash it: start a normal game.
       */
      startDefaultGame(null, savedInstanceState);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_COLORBLIND_ON,  0, R.string.menu_colorblind_on);
    menu.add(0, MENU_COLORBLIND_OFF, 0, R.string.menu_colorblind_off);
    menu.add(0, MENU_FULLSCREEN_ON,  0, R.string.menu_fullscreen_on);
    menu.add(0, MENU_FULLSCREEN_OFF, 0, R.string.menu_fullscreen_off);
    menu.add(0, MENU_SOUND_OPTIONS,  0, R.string.menu_sound_options);
    menu.add(0, MENU_TARGET_MODE,    0, R.string.menu_target_mode);
    menu.add(0, MENU_DONT_RUSH_ME,   0, R.string.menu_dont_rush_me);
    menu.add(0, MENU_RUSH_ME,        0, R.string.menu_rush_me);
    menu.add(0, MENU_ABOUT,          0, R.string.menu_about);
    menu.add(0, MENU_NEW_GAME,       0, R.string.menu_new_game);
    menu.add(0, MENU_EDITOR,         0, R.string.menu_editor);
    return true;
  }

  /**
   * Invoked when the Activity is finishing or being destroyed by the
   * system.
   */
  @Override
  protected void onDestroy() {
    //Log.i(TAG, "FrozenBubble.onDestroy()");
    super.onDestroy();
    cleanUp();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    boolean handled = false;
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      /*
       * Only show the game exit dialog while a game is in progress,
       * otherwise simply exit.
       */
      if ((mGameThread != null) && mGameThread.gameInProgress()) {
        exitGameDialog();
      }
      else {
        exit(true);
      }
      handled = true;
    }
    return handled || super.onKeyDown(keyCode, event);
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onNewIntent(android.content.Intent)
   */
  @Override
  protected void onNewIntent(Intent intent) {
    if (null != intent) {
      if (EDITORACTION.equals(intent.getAction())) {
        cleanUpGameView();
        try {
          startCustomGame(intent);
        } catch (RuntimeException e) {
          startDefaultGame(null, null);
        }
      }
      else if ((numPlayers != 0) && intent.hasExtra("numPlayers")) {
        int newNumPlayers = intent.getIntExtra("numPlayers", 1);

        if (newNumPlayers != numPlayers) {
          cleanUpGameView();
          startDefaultGame(intent, null);
        }
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return handleMenuAction(item.getItemId());
  }

  /**
   * Carry out an in-game menu action.  Shared by the options menu (the
   * hardware Menu key, on devices that still have one) and the Game
   * Options list reached from the exit dialog.
   */
  private boolean handleMenuAction(int itemId) {
    boolean prefsUpdated = false;

    switch (itemId) {
      case MENU_NEW_GAME:
        newGameDialog(null);
        return true;
      case MENU_COLORBLIND_ON:
        prefs.colorMode = GAME_COLORBLIND;
        prefsUpdated = true;
        break;
      case MENU_COLORBLIND_OFF:
        prefs.colorMode = GAME_NORMAL;
        prefsUpdated = true;
        break;
      case MENU_FULLSCREEN_ON:
        prefs.fullscreen = true;
        setFullscreen();
        prefsUpdated = true;
        break;
      case MENU_FULLSCREEN_OFF:
        prefs.fullscreen = false;
        setFullscreen();
        prefsUpdated = true;
        break;
      case MENU_SOUND_OPTIONS:
        soundOptionsDialog();
        return true;
      case MENU_ABOUT:
        if (mGameThread != null)
          mGameThread.setState(stateEnum.ABOUT);
        return true;
      case MENU_TARGET_MODE:
        targetOptionsDialog();
        return true;
      case MENU_DONT_RUSH_ME:
        prefs.dontRushMe = true;
        prefsUpdated = true;
        break;
      case MENU_RUSH_ME:
        prefs.dontRushMe = false;
        prefsUpdated = true;
        break;
      case MENU_EDITOR:
        startEditor();
        return true;
    }

    if (prefsUpdated) {
      SharedPreferences sp =
          PreferenceManager.getDefaultSharedPreferences(this);
      PreferencesActivity.saveDefaultPreferences(prefs, sp);
    }

    return prefsUpdated;
  }

  @Override
  public void onOptionsMenuClosed(Menu menu) {
    super.onOptionsMenuClosed(menu);
    allowUnpause = true;
  }

  /**
   * Invoked when the Activity loses user focus.
   */
  @Override
  protected void onPause() {
    //Log.i(TAG, "FrozenBubble.onPause()");
    super.onPause();
    pause();
    /*
     * Leaving the game any way (Home, switching apps, screen off) saves
     * it, so it survives the app being closed; Continue resumes it.
     * When finishing, exit() has already saved and reset the game.
     */
    if (!isFinishing()) {
      autoSave();
    }
  }

  /*
   * Set when an arcade game ends (its save is deleted then), and cleared
   * when the next game or level starts.
   */
  private boolean gameOver = false;

  /**
   * Save the current game for Continue, unless there is nothing worth
   * resuming: a finished arcade game, a score screen, a level-editor
   * game, or a network game (which would come back as a local one).
   */
  private void autoSave() {
    if ((mGameThread == null) || (numPlayers == 0) || gameOver ||
        activityCustomStarted || (gameLocale != LOCALE_LOCAL) ||
        mGameThread.showingScores()) {
      return;
    }
    saveGame(null);
  }

  /** Delete this mode's saved game (the others are kept). */
  private void clearSavedGame() {
    String slot = currentSlot();
    deleteFile(saveFileName(slot));
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    Editor editor = sp.edit()
        .putBoolean("playerSave", hasAnySavedGame(this))
        .remove(SAVED_DETAIL + slot);
    if (slot.equals(sp.getString(SAVED_GAME_MODE, null))) {
      editor.remove(SAVED_GAME_MODE);
    }
    editor.commit();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    allowUnpause = false;
    menu.findItem(MENU_COLORBLIND_ON ).setVisible(prefs.colorMode == GAME_NORMAL);
    menu.findItem(MENU_COLORBLIND_OFF).setVisible(prefs.colorMode != GAME_NORMAL);
    menu.findItem(MENU_FULLSCREEN_ON ).setVisible(!prefs.fullscreen);
    menu.findItem(MENU_FULLSCREEN_OFF).setVisible(prefs.fullscreen);
    menu.findItem(MENU_SOUND_OPTIONS ).setVisible(true);
    menu.findItem(MENU_TARGET_MODE   ).setVisible(true);
    menu.findItem(MENU_DONT_RUSH_ME  ).setVisible(!prefs.dontRushMe);
    menu.findItem(MENU_RUSH_ME       ).setVisible(prefs.dontRushMe);
    return true;
  }

  /**
   * Notification that something is about to happen, to give the
   * Activity a chance to save state.
   * @param outState - A Bundle into which this Activity should save its
   * state.
   */
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    //Log.i(TAG, "FrozenBubble.onSaveInstanceState()");
    /*
     * Just have the View's thread save its state into our Bundle.
     */
    super.onSaveInstanceState(outState);
    saveState(outState);

    /*
     * Set a flag indicating that the system passed us a bundle to save
     * the game state in.
     */
    SharedPreferences.Editor editor =
        PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putBoolean("systemSave", true);
    editor.commit();
  }

  @Override
  public void onWindowFocusChanged (boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    allowUnpause = hasFocus;
  }

  /*
   * The following methods are used to retrieve and set the volatile
   * memory game options variables.  They are all static, so are
   * available to all activities within this application while the
   * application is running.
   */

  public static boolean getAimThenShoot() {
    return (prefs.targetMode == AIM_TO_SHOOT) || (prefs.targetMode == ROTATE_TO_SHOOT);
  }

  public static int getBluetooth() {
    return prefs.bluetooth;
  }

  public static void setBluetooth(int newBluetooth) {
    prefs.bluetooth = newBluetooth;
  }

  public static int getCollision() {
    return prefs.collision;
  }

  public static void setCollision(int newCollision) {
    prefs.collision = newCollision;
    BubbleSprite.setCollisionThreshold(prefs.collision);
  }

  public static boolean getColorMode() {
    return prefs.colorMode;
  }

  public static void setColorMode(boolean newMode) {
    prefs.colorMode = newMode;
  }

  public static boolean getCompressor() {
    return prefs.compressor;
  }

  public static void setCompressor(boolean newCompressor) {
    prefs.compressor = newCompressor;
  }

  public static int getDifficulty() {
    return prefs.difficulty;
  }

  public static void setDifficulty(int newDifficulty) {
    prefs.difficulty = newDifficulty;
  }

  public static boolean getDontRushMe() {
    return prefs.dontRushMe;
  }

  public static void setDontRushMe(boolean dont) {
    prefs.dontRushMe = dont;
  }

  public static boolean getFullscreen() {
    return prefs.fullscreen;
  }

  public static void setFullscreen(boolean newFullscreen) {
    prefs.fullscreen = newFullscreen;
  }

  public static boolean getMusicOn() {
    return prefs.musicOn;
  }

  public static void setMusicOn(boolean mo) {
    prefs.musicOn = mo;
  }

  public static boolean getSoundOn() {
    return prefs.soundOn;
  }

  public static void setSoundOn(boolean so) {
    prefs.soundOn = so;
  }

  public static int getTargetMode() {
    return prefs.targetMode;
  }

  public static void setTargetMode(int tm) {
    prefs.targetMode = tm;
  }

  /*
   * Following are general utility functions.
   */

  /**
   * Perform activity cleanup.  <b>This must only be called when the
   * activity is being destroyed.</b>
   */
  public void cleanUp() {
    /*
     * The current game is being destroyed, so reset the static game
     * state variables.
     */
    arcadeGame = false;
    gameLocale = LOCALE_LOCAL;
    myPlayerId = VirtualInput.PLAYER1; 
    numPlayers = 0;
    opponentId = CPU;

    if (AccelerometerManager.isListening())
      AccelerometerManager.stopListening();

    if (myOrientationEventListener != null) {
      myOrientationEventListener.disable();
    }
    myOrientationEventListener = null;

    cleanUpGameView();

    if (myModPlayer != null) {
      myModPlayer.destroyMusicPlayer();
    }
    myModPlayer = null;
  }

  private void cleanUpGameView() {
    mGameThread = null;
    if (mGameView != null) {
      mGameView.cleanUp();
    }
    mGameView = null;
  }

  private void exit(boolean goToHomeScreen) {
    /*
     * Preserve game information and perform activity cleanup.
     */
    pause();
    if (mGameThread != null)
      mGameThread.setRunning(false);
    cleanUp();
    /*
     * Clear the flag indicating that the system passed us a bundle to
     * save the game state.
     */
    SharedPreferences.Editor editor =
        PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putBoolean("systemSave", false);
    editor.commit();
    /*
     * Create an intent to launch the home screen.
     */
    if (goToHomeScreen) {
      Intent intent = new Intent(this, HomeScreen.class);
      intent.putExtra("startHomeScreen", true);
      startActivity(intent);
    }
    /*
     * Finish the current activity.
     */
    finish();
  }

  /** The shared bubble-style option popups, created on first use. */
  private com.efortin.frozenbubble.OptionsMenus optionsMenus = null;

  private com.efortin.frozenbubble.OptionsMenus getOptionsMenus() {
    if (optionsMenus == null) {
      optionsMenus = new com.efortin.frozenbubble.OptionsMenus(this,
          new com.efortin.frozenbubble.OptionsMenus.Host() {
        @Override
        public void click() {
          // The in-game menus are silent, like the Exit menu.
        }
        @Override
        public void onSettingChanged(String key) {
          // The settings are already applied; update what is on screen.
          if (com.efortin.frozenbubble.OptionsMenus.KEY_FULLSCREEN.equals(key)) {
            setFullscreen();
          }
          else if (com.efortin.frozenbubble.OptionsMenus.KEY_TARGETING.equals(key)) {
            setTargetModeOrientation();
          }
          else if (com.efortin.frozenbubble.OptionsMenus.KEY_MUSIC.equals(key)) {
            if (myModPlayer != null) {
              myModPlayer.setMusicOn(getMusicOn());
            }
          }
        }
        @Override
        public boolean pickMusicFolder() {
          // The folder is chosen in Options on the home screen.
          return false;
        }
        @Override
        public void placePopup(android.view.Window window) {
          centerOverDimmedGame(window);
        }
      });
    }
    return optionsMenus;
  }

  /**
   * The in-game Options menu, in the home screen menu style: the same
   * option categories as the Options screen, plus the game actions.
   * Back returns to the Exit menu.
   */
  private void gameOptionsDialog() {
    final com.efortin.frozenbubble.OptionsMenus menus = getOptionsMenus();
    final AlertDialog menu = new AlertDialog.Builder(FrozenBubble.this).create();
    // Category popups hide this menu, and show it again when closed.
    final Runnable backHere = () -> menu.show();
    Button[] buttons = {
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.opt_music_sound,
          v -> { menu.hide(); menus.showMusicMenu(backHere); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.opt_display,
          v -> { menu.hide(); menus.showDisplayMenu(backHere); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.opt_controls,
          v -> { menu.hide(); menus.showControlsMenu(backHere); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.opt_gameplay,
          v -> { menu.hide(); menus.showGameplayMenu(backHere); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.opt_effects,
          v -> { menu.hide(); menus.showEffectsMenu(backHere); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.menu_new_game,
          v -> { menu.hide(); newGameDialog(backHere); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.menu_about,
          v -> { menu.dismiss(); handleMenuAction(MENU_ABOUT); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.menu_editor,
          v -> { menu.dismiss(); handleMenuAction(MENU_EDITOR); }),
      com.efortin.frozenbubble.MenuButtons.create(this, R.string.back,
          v -> { menu.dismiss(); exitGameDialog(); }),
    };
    for (Button b : buttons) {
      com.efortin.frozenbubble.OptionsMenus.widen(b);
    }
    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    scroll.addView(com.efortin.frozenbubble.MenuButtons.column(this, buttons));
    menu.setView(scroll);
    // The system Back gesture or a tap outside also goes back.
    menu.setOnCancelListener(d -> exitGameDialog());
    menus.showSeeThrough(menu);
  }

  /**
   * Center an in-game menu on the screen, like the main menus.  The game
   * dims itself behind the menus (see GameThread.setMenuDim()): the
   * window's own dimming doesn't reach the game's drawing surface on
   * every phone, so it is switched off to avoid dimming twice.
   */
  private void centerOverDimmedGame(android.view.Window window) {
    android.view.WindowManager.LayoutParams lp = window.getAttributes();
    lp.gravity   = android.view.Gravity.CENTER;
    lp.y         = 0;
    lp.dimAmount = 0f;
    window.setAttributes(lp);
    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
  }

  /** Dim the game while an in-game menu is open. */
  private void setMenuDim(boolean on) {
    if (mGameThread != null) {
      mGameThread.setMenuDim(on);
    }
  }

  private void exitGameDialog() {
    /*
     * Exit (which always saves), Options and Cancel as a column of buttons in
     * the home screen menu style.  Options opens the in-game menu, since
     * modern phones have no Menu key to reach it.
     */
    setMenuDim(true);
    final AlertDialog alert = new AlertDialog.Builder(FrozenBubble.this)
    .setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        /*
         * User canceled.  Do nothing.
         */
        setMenuDim(false);
        if (mGameThread != null) {
          mGameThread.resumeGame();
        }
      }
    })
    .create();

    Button exitButton = com.efortin.frozenbubble.MenuButtons.create(this,
        R.string.exit, new android.view.View.OnClickListener() {
      @Override
      public void onClick(android.view.View v) {
        alert.dismiss();
        // The game is always saved; Continue on the home screen resumes it.
        autoSave();
        exit(true);
      }
    });
    Button optionsButton = com.efortin.frozenbubble.MenuButtons.create(this,
        R.string.options, new android.view.View.OnClickListener() {
      @Override
      public void onClick(android.view.View v) {
        alert.dismiss();
        gameOptionsDialog();
      }
    });
    Button cancelButton = com.efortin.frozenbubble.MenuButtons.create(this,
        R.string.cancel, new android.view.View.OnClickListener() {
      @Override
      public void onClick(android.view.View v) {
        alert.dismiss();
        setMenuDim(false);
        if (mGameThread != null) {
          mGameThread.resumeGame();
        }
      }
    });
    alert.setView(com.efortin.frozenbubble.MenuButtons.column(this,
        exitButton, optionsButton, cancelButton));
    /*
     * No dialog box: the buttons float over the (dimmed) game.
     */
    if (alert.getWindow() != null) {
      alert.getWindow().setBackgroundDrawable(
          new android.graphics.drawable.ColorDrawable(
              android.graphics.Color.TRANSPARENT));
      centerOverDimmedGame(alert.getWindow());
    }
    alert.show();
  }

  /**
   * Obtain the screen orientation.
   * @param windowManager - used to get a reference to the display to
   * obtain display information.
   * @return The screen orientation, which can be among the following
   * values:<br><code>
   * ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE<br>
   * ActivityInfo.SCREEN_ORIENTATION_PORTRAIT<br>
   * FrozenBubble.SCREEN_ORIENTATION_REVERSE_LANDSCAPE<br>
   * FrozenBubble.SCREEN_ORIENTATION_REVERSE_PORTRAIT</code>
   */
  public static int getScreenOrientation(WindowManager windowManager) {
    /*
     * The method getOrientation() was deprecated in API level 8.
     *
     * For API level 8 or greater, use getRotation().
     */
    int rotation = windowManager.getDefaultDisplay().getRotation();
    DisplayMetrics dm = new DisplayMetrics();
    windowManager.getDefaultDisplay().getMetrics(dm);
    int width  = dm.widthPixels;
    int height = dm.heightPixels;
    int orientation;
    /*
     * The orientation determination is based on the natural orienation
     * mode of the device, which can be either portrait, landscape, or
     * square.
     *
     * After the natural orientation is determined, convert the device
     * rotation into a fully qualified orientation.
     */
    if ((((rotation == Surface.ROTATION_0  ) ||
          (rotation == Surface.ROTATION_180)) && (height > width)) ||
        (((rotation == Surface.ROTATION_90 ) ||
          (rotation == Surface.ROTATION_270)) && (width  > height))) {
      /*
       * Natural orientation is portrait.
       */
      switch(rotation) {
        case Surface.ROTATION_0:
          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;
        case Surface.ROTATION_90:
          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;
        case Surface.ROTATION_180:
          orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          break;
        case Surface.ROTATION_270:
          orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          break;
        default:
          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;              
      }
    }
    else {
      /*
       * Natural orientation is landscape or square.
       */
      switch(rotation) {
        case Surface.ROTATION_0:
          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;
        case Surface.ROTATION_90:
          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;
        case Surface.ROTATION_180:
          orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          break;
        case Surface.ROTATION_270:
          orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          break;
        default:
          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;              
      }
    }

    return orientation;
  }

  /**
   * Restore the game options from the saved preferences, and register
   * the device orientation listener to detect orientation changes.
   */
  private void initGameOptions() {
    restoreGamePrefs();

    currentOrientation = getScreenOrientation(getWindowManager());
    myOrientationEventListener =
      new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
        @Override
        public void onOrientationChanged(int arg0) {
          currentOrientation = getScreenOrientation(getWindowManager());
        }
      };
    if (myOrientationEventListener.canDetectOrientation())
      myOrientationEventListener.enable();
  }

  /**
   * Start a new game and music player.
   */
  public void newGame() {
    setMenuDim(false);
    if (mGameThread != null) {
      mGameThread.newGame(true);
    }

    playMusic(false);
  }

  /**
   * Ask before starting a new game, in the home screen menu style.
   * @param onCancel - run if the player decides not to (back to the
   * menu that asked).
   */
  private void newGameDialog(final Runnable onCancel) {
    final AlertDialog dialog = new AlertDialog.Builder(FrozenBubble.this).create();
    final boolean[] starting = { false };
    android.widget.TextView question = new android.widget.TextView(this);
    com.efortin.frozenbubble.OptionsMenus.styleText(question, 22);
    question.setText(R.string.opt_new_game_q);
    Button yes = com.efortin.frozenbubble.MenuButtons.create(this,
        R.string.menu_new_game, v -> {
      starting[0] = true;
      dialog.dismiss();
      newGame();
    });
    Button no = com.efortin.frozenbubble.MenuButtons.create(this,
        R.string.cancel, v -> dialog.dismiss());
    com.efortin.frozenbubble.OptionsMenus.widen(yes);
    com.efortin.frozenbubble.OptionsMenus.widen(no);
    android.widget.LinearLayout box = new android.widget.LinearLayout(this);
    box.setOrientation(android.widget.LinearLayout.VERTICAL);
    box.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
    box.addView(question);
    box.addView(com.efortin.frozenbubble.MenuButtons.column(this, yes, no));
    dialog.setView(box);
    dialog.setOnDismissListener(d -> {
      if (!starting[0] && (onCancel != null))
        onCancel.run();
    });
    getOptionsMenus().showSeeThrough(dialog);
  }

  private void newGameView(Bundle map) {
    if (map != null) {
      /*
       * Force the game locale to be confined to the local device when
       * restoring a saved game.  Restoring a network game is of little
       * to no value as opposed to simply starting a new game.
       */
      arcadeGame = map.getBoolean("arcadeGame", false               );
      gameLocale = LOCALE_LOCAL;
      myPlayerId = map.getInt    ("myPlayerId", VirtualInput.PLAYER1);
      numPlayers = map.getInt    ("numPlayers", 0                   );
      opponentId = map.getInt    ("opponentId", CPU                 );
    }
    mGameView = new GameView(this,
                             numPlayers,
                             myPlayerId,
                             opponentId,
                             gameLocale,
                             arcadeGame);
    setContentView(mGameView);
    mGameView.setGameListener(this);
    mGameThread = mGameView.getThread();
    mGameThread.restoreState(map);
    mGameView.requestFocus();
    /*
     * A restored game may be a different mode (e.g. Continue 2 Player),
     * so apply its screen orientation now that it is known.
     */
    setTargetModeOrientation();
  }

  @Override
  public void onAccelerationChanged(float x, float y, float z) {
    if (mGameThread != null) {
      if (numPlayers > 1) {
        if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
          x = -y;
        else if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
          x = y;
        else if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
          x = -x;
      }
      else if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
        x = -x;
      }
      mGameThread.setPosition(20.0f+x*2.0f);
    }
  }

  @Override
  public void onGameEvent(eventEnum event) {
    switch (event) {
      case GAME_EXIT:
        /*
         * Only show the game exit dialog while a game is in progress,
         * otherwise simply exit.
         */
        if ((mGameThread != null) && mGameThread.gameInProgress()) {
          exitGameDialog();
        }
        else {
          exit(true);
        }
        break;

      case GAME_WON:
      case GAME_LOST:
        /*
         * An arcade game is over when it is won or lost: nothing left to
         * continue.  (Puzzle and 2 player games carry on with the next
         * level or round.)
         */
        if (arcadeGame) {
          gameOver = true;
          clearSavedGame();
        }
        break;

      case NEXT_SONG:
        nextSong();
        break;

      case GAME_PAUSED:
        if (myModPlayer != null)
          myModPlayer.pausePlay();
        break;

      case GAME_RESUME:
        if (myModPlayer == null)
          playMusic(true);
        else if (allowUnpause)
          myModPlayer.unPausePlay();
        break;

      case GAME_START:
        gameOver = false;
        if (!arcadeGame && (mGameView != null) && (mGameThread != null) &&
            (numPlayers == 1)) {
          if (mGameThread.getCurrentLevelIndex() == 0) {
            /*
             * Destroy the current music player, which will free audio
             * stream resources and allow the system to use them.
             */
            if (myModPlayer != null)
              myModPlayer.destroyMusicPlayer();
            myModPlayer = null;
            /*
             * Clear the game screen and suspend input processing for
             * three seconds.
             *
             * Afterwards,  the "About" screen will be displayed as a
             * backup just in case anything goes awry with displaying
             * the end-of-game credits.  It will be displayed after the
             * user touches the screen when the credits are finished.
             */
            mGameView.clearGameScreen(true, 3000);
            /*
             * Create an intent to launch the activity to display the
             * credits screen.
             */
            Intent intent = new Intent(this, ScrollingCredits.class);
            startActivity(intent);
            break;
          }
        }
        playMusic(true);
        break;

      default:
        break;
    }
  }

  @Override
  public void onPlayerEvent(
      com.peculiargames.andmodplug.PlayerThread.eventEnum event) {
      switch (event) {
      case SONG_COMPLETED:
        if (myModPlayer != null) {
          if (!songsSequential()) {
            playMusic(true);
          }
        }
        break;
      case PATTERN_CHANGE:
      case PLAYER_STARTED:
      default:
        break;
    }
  }

  /**
   * Pause the game.
   */
  private void pause() {
    if (mGameThread != null)
      mGameThread.pause();

    /*
     * Pause the MOD player.
     */
    if (myModPlayer != null)
      myModPlayer.pausePlay();
  }

  /**
   * This function determines whether a music player instance needs to
   * be created or if one already exists.  Then, based on the current
   * level, the song to play is calculated and loaded.  If desired, the
   * song will start playing immediately, or it can remain paused.
   * @param startPlaying - If <code>true</code>, the song starts playing
   * immediately.  Otherwise it is paused and must be unpaused to start
   * playing.
   */
  private void playMusic(boolean startPlaying) {
    playSong(startPlaying, false);
  }

  /**
   * Skip to the next song in whichever list is playing (the player's own
   * music folder or the built-in songs).  Later levels continue on from
   * the new song.
   */
  private void nextSong() {
    if (myModPlayer == null) {
      return;
    }
    if (songsSequential()) {
      songSkip++;
    }
    playSong(true, true);
  }

  /**
   * Pick and load a song.
   * @param startPlaying - start playing immediately, or stay paused.
   * @param skip - <code>true</code> to play the song after the current
   * one instead of choosing by level (or at random).
   */
  private void playSong(boolean startPlaying, boolean skip)
  {
    int loopCount = PlayerThread.LOOP_SONG_FOREVER;
    /*
     * Songs from the player's own music folder, if that option is on.
     * When present they replace the built-in level music.
     */
    List<Uri> customSongs = CustomMusic.getActiveSongs(this);
    boolean   useCustom   = !customSongs.isEmpty();
    int       count       = useCustom ? customSongs.size() : MODlist.length;
    /*
     * Ascertain which song to play.  For a single player game, the song
     * is based on the current level.  For an arcade game, a two player
     * game, or if the game thread has been destroyed, the song is
     * selected at random.  A skip moves on to the next song in the list.
     */
    boolean sequential = songsSequential();
    int index;
    if (skip && (lastSongIndex >= 0)) {
      index = (lastSongIndex + 1) % count;
    }
    else if (sequential) {
      index = (mGameThread.getCurrentLevelIndex() + songSkip) % count;
    }
    else {
      index = new Random().nextInt(count);
    }
    int modNow = index % MODlist.length;
    /*
     * Determine whether to create a music player or load the song.
     * With custom songs, the player starts paused on a built-in song
     * and the custom song is swapped in below.
     */
    if (myModPlayer == null) {
      myModPlayer = new ModPlayer(this,
                                  MODlist[modNow],
                                  loopCount,
                                  getMusicOn(),
                                  useCustom || !startPlaying);
      myModPlayer.setPlayerListener(this);
    }
    else if (!useCustom) {
      myModPlayer.loadNewSong(MODlist[modNow], startPlaying);
    }
    lastSongIndex = index;
    if (useCustom) {
      boolean loaded = false;
      /*
       * Skip files that fail to load; after a few failures in a row,
       * fall back to the built-in song.
       */
      for (int tries = 0; (tries < Math.min(count, 5)) && !loaded; tries++) {
        int i = (index + tries) % count;
        loaded = myModPlayer.loadNewSong(customSongs.get(i), startPlaying);
        if (loaded) {
          lastSongIndex = i;
        }
      }
      if (!loaded) {
        myModPlayer.loadNewSong(MODlist[modNow], startPlaying);
      }
    }
    allowUnpause = true;
  }

  /**
   * Restore the game from a player saved bundle.
   * <p>Note that this implementation uses a practice that is not
   * recommended by the API documentation, as serializing a
   * <code>Parcel</code> object is not guaranteed to be compatible
   * amongst disparate versions of the Android OS.
   */
  private Bundle restoreSavedGame(String slot) {
    Bundle inState = null;
    Parcel parcel  = Parcel.obtain();

    try {
      FileInputStream fis = openFileInput(saveFileName(slot));
      byte[] array = new byte[(int) fis.getChannel().size()];
      fis.read(array, 0, array.length);
      fis.close();
      parcel.unmarshall(array, 0, array.length);
      parcel.setDataPosition(0);
      inState = parcel.readBundle();
      inState.putAll(inState);
    } catch (FileNotFoundException fnfe) {
      /*
       * Cannot open file, so the game could not be restored.
       */
    } catch (IOException ioe) {
      /*
       * Error occurred while reading from file.
       */
      ioe.printStackTrace();
    } finally {
      parcel.recycle();
    }

    return inState;
  }

  /**
   * Load the game options from the saved shared preferences.
   */
  private void restoreGamePrefs() {
    SharedPreferences sp =
        PreferenceManager.getDefaultSharedPreferences(this);
    prefs = PreferencesActivity.getDefaultPrefs(sp);

    /*
     * Some game options require additional handling to fully implement
     * changes to game play.
     */
    BubbleSprite.setCollisionThreshold(prefs.collision);
    setTargetModeOrientation();
  }

  /**
   * Save the game to a player saved bundle.
   * <p>Note that this implementation uses a practice that is not
   * recommended by the API documentation, as serializing a
   * <code>Parcel</code> object is not guaranteed to be compatible
   * amongst disparate versions of the Android OS.
   */
  private void saveGame(Bundle map) {
    Bundle outState = map;
    Parcel parcel   = Parcel.obtain();

    if (outState == null) {
      outState = new Bundle();
      saveState(outState);
    }

    try {
      String slot = currentSlot();
      FileOutputStream fos = openFileOutput(saveFileName(slot),
                                            Context.MODE_PRIVATE);
      outState.writeToParcel(parcel, 0);
      fos.write(parcel.marshall());
      fos.flush();
      fos.close();
      /*
       * Set a flag indicating that the player saved the game.
       */
      Editor editor =
          PreferenceManager.getDefaultSharedPreferences(this).edit();
      editor.putBoolean("playerSave", true);
      editor.putString(SAVED_GAME_MODE, slot);
      // Where it left off, for the home screen's Continue popup.
      int detail;
      if (SAVED_MODE_PUZZLE.equals(slot)) {
        detail = (mGameThread != null) ?
            mGameThread.getCurrentLevelIndex() + 1 : -1;
      }
      else if (SAVED_MODE_ARCADE.equals(slot)) {
        detail = getDifficulty();
      }
      else {
        detail = opponentId;
      }
      editor.putInt(SAVED_DETAIL + slot, detail);
      editor.commit();
    } catch (FileNotFoundException fnfe) {
      /*
       * Cannot create file, so the game could not be saved.
       */
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      /*
       * Error occurred while writing to file.
       */
      ioe.printStackTrace();
    } finally {
      parcel.recycle();
    }
  }

  /**
   * Save critically important game information.
   */
  public void saveState(Bundle map) {
    if (!arcadeGame && (mGameThread != null) && (numPlayers == 1)) {
      /*
       * Allow level editor functionalities.
       */
      SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                  Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = sp.edit();
      /*
       * If the game wasn't launched from the level editor, save the
       * last level played.
       */
      Intent i = getIntent();
      if ((null == i) || !activityCustomStarted) {
        editor.putInt("level", mGameThread.getCurrentLevelIndex());
      }
      else {
        /*
         * The level editor's intent is running.
         */
        editor.putInt("levelCustom", mGameThread.getCurrentLevelIndex());
      }
      editor.commit();
    }
    if (map != null) {
      map.putBoolean("arcadeGame", arcadeGame);
      map.putInt    ("gameLocale", gameLocale);
      map.putInt    ("myPlayerId", myPlayerId);
      map.putInt    ("numPlayers", numPlayers);
      map.putInt    ("opponentId", opponentId);
  
      if (mGameThread != null) {
        mGameThread.saveState(map);
      }
    }
  }

  private void setFullscreen() {
    final int flagFs   = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    final int flagNoFs = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;

    if (prefs.fullscreen) {
      getWindow().addFlags(flagFs);
      getWindow().clearFlags(flagNoFs);
    }
    else {
      getWindow().clearFlags(flagFs);
      getWindow().addFlags(flagNoFs);
    }
    com.efortin.frozenbubble.SystemUi.apply(this, prefs.fullscreen);

    if (mGameView != null)
      mGameView.requestLayout();
  }

  private void setTargetModeOrientation() {
    boolean rotateToShoot = (prefs.targetMode == ROTATE_TO_SHOOT) &&
        AccelerometerManager.isSupported(getApplicationContext());
    /*
     * The accelerometer is only needed for "rotate to shoot" aiming.
     */
    if (rotateToShoot) {
      AccelerometerManager.startListening(getApplicationContext(),this);
    }
    else if (AccelerometerManager.isListening()) {
      AccelerometerManager.stopListening();
    }
    /*
     * Screen orientation:
     * - Local 2 player games (vs CPU or two players on one phone) always
     *   run in landscape (either way up), even if the system rotation is
     *   locked to portrait, so both playfields are shown side by side.
     * - Network 2 player games (Wi-Fi, Bluetooth) follow the system
     *   setting: in portrait each phone shows just its own playfield.
     *   With "rotate to shoot" they use landscape, as before.
     * - 1 player "rotate to shoot" stays in portrait (either way up),
     *   since tilting the phone aims.
     * - Otherwise follow the system rotation setting.  (This used to be
     *   "sensor", which ignored the user's rotation lock.)
     * This only affects the game screen: the menus follow the system
     * setting, so leaving a 2 player game returns to portrait when
     * rotation is locked.
     */
    int orientation;
    if ((numPlayers > 1) &&
        ((gameLocale == LOCALE_LOCAL) || rotateToShoot)) {
      orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    }
    else if ((numPlayers <= 1) && rotateToShoot) {
      orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
    }
    else {
      orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }
    if (getRequestedOrientation() != orientation) {
      setRequestedOrientation(orientation);
    }
  }

  /**
   * For a Puzzle game, there are consecutive levels.  Each level will
   * have a corresponding song.
   * @return <code>true</code> if the song index should be selected
   * based on the current game level.
   */
  private boolean songsSequential() {
    return(!arcadeGame && (mGameThread != null) && (numPlayers == 1));
  }

  private void soundOptionsDialog() {
    boolean isCheckedItem[] = {getSoundOn(), getMusicOn()};

    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    /*
     * Set the dialog title.
     */
    builder.setTitle(R.string.menu_sound_options)
    /*
     * Specify the list array, the items to be selected by default (null
     * for none), and the listener through which to receive callbacks
     * when items are selected.
     */
    .setMultiChoiceItems(R.array.sound_options_array, isCheckedItem,
                         new DialogInterface.OnMultiChoiceClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        boolean prefsUpdated = false;

        switch (which) {
          case 0:
            setSoundOn(isChecked);
            prefsUpdated = true;
            break;
          case 1:
            setMusicOn(isChecked);
            if (myModPlayer != null) {
              myModPlayer.setMusicOn(isChecked);
            }
            prefsUpdated = true;
            break;
        }
        
        if (prefsUpdated) {
          SharedPreferences sp =
              PreferenceManager.getDefaultSharedPreferences(getApplication());
          PreferencesActivity.saveDefaultPreferences(prefs, sp);
        }
      }
    })
    /*
     * Set the action buttons.
     */
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        // User clicked OK.
      }
    });
    builder.create();
    builder.show();
  }

  /**
   * Update the game preferences to the desired values.
   * @param prefs - the desired game preferences.
   */
  public static void setPrefs(Preferences prefs) {
    FrozenBubble.setBluetooth (prefs.bluetooth );
    FrozenBubble.setCollision (prefs.collision );
    FrozenBubble.setColorMode (prefs.colorMode );
    FrozenBubble.setCompressor(prefs.compressor);
    FrozenBubble.setDifficulty(prefs.difficulty);
    FrozenBubble.setDontRushMe(prefs.dontRushMe);
    FrozenBubble.setFullscreen(prefs.fullscreen);
    FrozenBubble.setMusicOn   (prefs.musicOn   );
    FrozenBubble.setSoundOn   (prefs.soundOn   );
    FrozenBubble.setTargetMode(prefs.targetMode);
  }

  /**
   * Method to start a game using levels from the level editor.
   * <p>If the level isn't specified from the editor, then the player
   * selected the option to continue playing from the last level
   * played, so use the last level played instead.
   * @param intent - The intent from the level editor used to start this
   * activity, which contains the custom level data.
   */
  private void startCustomGame(Intent intent) {
    /*
     * The levels come from another app (a level editor): check them, and
     * play the normal game instead if they are missing or malformed.
     */
    byte[] levels = intent.getByteArrayExtra("levels");
    if (!LevelManager.isValidLevelData(levels)) {
      startDefaultGame(null, null);
      return;
    }
    activityCustomStarted = true;
    numPlayers = 1;
    initGameOptions();
    /*
     * Get custom level last played.
     */
    SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                Context.MODE_PRIVATE);
    int startingLevel       = sp    .getInt     ("levelCustom",    0);
    int startingLevelIntent = intent.getIntExtra("startingLevel", -2);
    startingLevel =
      (startingLevelIntent == -2) ? startingLevel : startingLevelIntent;
    // An out-of-range level starts at the first one (LevelManager).
    mGameView = new GameView(this, levels, startingLevel);
    setContentView(mGameView);
    mGameView.setGameListener(this);
    mGameThread = mGameView.getThread();
    mGameView.requestFocus();
    setFullscreen();
    playMusic(false);
  }

  /**
   * Method to start a game using default levels, if single player game
   * mode was selected.
   * <p>This method is also used to start a multiplayer game.
   * @param intent - The intent used to start this activity.
   * @param savedInstanceState - the bundle of saved state information.
   */
  private void startDefaultGame(Intent intent, Bundle savedInstanceState) {
    /*
     * Initialize the flag to denote this game uses default levels.
     */
    activityCustomStarted = false;

    /*
     * Initialize the game state variables to safe defaults.
     */
    arcadeGame = false;
    gameLocale = LOCALE_LOCAL;
    myPlayerId = VirtualInput.PLAYER1;
    numPlayers = 1;
    opponentId = CPU;
    playerSave = false;

    /*
     * Load the game configuration from the intent data, if it exists.
     * These values may be superseded if a player or system save bundle
     * is loaded.
     */
    if (intent != null) {
      if (intent.hasExtra("myPlayerId"))
        myPlayerId = intent.getIntExtra("myPlayerId", VirtualInput.PLAYER1);
      if (intent.hasExtra("numPlayers"))
        numPlayers = intent.getIntExtra("numPlayers", 1);
      if (intent.hasExtra("opponentId"))
        opponentId = intent.getIntExtra("opponentId", CPU);
      if (intent.hasExtra("gameLocale"))
        gameLocale = intent.getIntExtra("gameLocale", LOCALE_LOCAL);
      if (intent.hasExtra("arcadeGame"))
        arcadeGame = intent.getBooleanExtra("arcadeGame", false);
      if (intent.hasExtra("playerSave"))
        playerSave = intent.getBooleanExtra("playerSave", false);
    }
    /*
     * This screen can be opened by other apps, so only accept values the
     * game supports.
     */
    if ((numPlayers != 1) && (numPlayers != 2))
      numPlayers = 1;
    if ((myPlayerId != VirtualInput.PLAYER1) &&
        (myPlayerId != VirtualInput.PLAYER2))
      myPlayerId = VirtualInput.PLAYER1;
    if ((opponentId != CPU) && (opponentId != HUMAN))
      opponentId = CPU;
    if ((numPlayers == 1) ||
        ((gameLocale != LOCALE_LOCAL) && (gameLocale != LOCALE_WIFI) &&
         (gameLocale != LOCALE_BLUETOOTH)))
      gameLocale = LOCALE_LOCAL;

    initGameOptions();

    if (playerSave) {
      // Which mode to continue: chosen on the home screen, else the
      // most recently saved one.
      String slot = (intent != null) ? intent.getStringExtra("saveSlot") : null;
      if (slot == null) {
        slot = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(SAVED_GAME_MODE, SAVED_MODE_PUZZLE);
      }
      newGameView(restoreSavedGame(slot));
    }
    else {
      newGameView(savedInstanceState);
    }

    setFullscreen();
    playMusic(false);
  }

  /**
   * Starts editor / market with editor's download.
   */
  private void startEditor() {
    Intent i = new Intent();
    /*
     * First try to run the plus version of the level editor.
     */
    i.setClassName("sk.halmi.fbeditplus", 
                   "sk.halmi.fbeditplus.EditorActivity");
    try {
      startActivity(i);
      finish();
    } catch (ActivityNotFoundException e) {
      /*
       * If not found, try to run the normal version.
       */
      i.setClassName("sk.halmi.fbedit", 
                     "sk.halmi.fbedit.EditorActivity");
      try {
        startActivity(i);
        finish();
      } catch (ActivityNotFoundException ex) {
        /*
         * If the user doesn't have the Frozen Bubble Level Editor, take
         * him to the application market.
         */
        try {
          Toast.makeText(getApplicationContext(), 
                         R.string.install_editor, Toast.LENGTH_SHORT).show();
          i = new Intent(Intent.ACTION_VIEW,
                         Uri.parse(
                         "market://search?q=frozen bubble level editor"));
          startActivity(i);
        } catch (Exception exc) {
          /*
           * Damn, you don't have market?
           */
          Toast.makeText(getApplicationContext(), 
                         R.string.market_missing, Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  private void targetOptionsDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    /*
     * Set the dialog title.
     */
    builder.setTitle(R.string.menu_target_mode)
    /*
     * Specify the list array, the item to be selected by default, and
     * the listener through which to receive callbacks when the item is
     * selected.
     */
    .setSingleChoiceItems(R.array.shoot_mode_array, prefs.targetMode,
                          new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface builder, int which) {
        switch (which) {
          case 0:
            setTargetMode(AIM_TO_SHOOT);
            break;
          case 1:
            setTargetMode(POINT_TO_SHOOT);
            break;
          case 2:
            setTargetMode(ROTATE_TO_SHOOT);
            break;
        }
        setTargetModeOrientation();
      }
    })
    /*
     * Set the action buttons.
     */
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface builder, int id) {
        // User clicked OK.
        SharedPreferences sp =
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PreferencesActivity.saveDefaultPreferences(prefs, sp);
      }
    });

    builder.create();
    builder.show();
  }
}
