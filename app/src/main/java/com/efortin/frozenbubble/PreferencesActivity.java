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

package com.efortin.frozenbubble;

import static com.efortin.frozenbubble.OptionsMenus.*;

import org.jfedor.frozenbubble.BubbleSprite;
import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.GameView;
import org.jfedor.frozenbubble.LevelManager;
import org.jfedor.frozenbubble.R;
import org.jfedor.frozenbubble.SoundManager;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

/**
 * The Options screen, in the style of the home screen menu: a short
 * menu of categories, each opening a see-through popup of bubble
 * buttons that show and change their setting (see {@link OptionsMenus},
 * shared with the in-game Options menu).
 * <p>Settings are stored in the same shared preferences, under the same
 * keys, as the old settings list, so existing settings carry over.
 */
public class PreferencesActivity extends Activity
    implements OptionsMenus.Host {

  private static final int REQUEST_MUSIC_FOLDER = 2001;

  private SharedPreferences sp;
  private SoundManager      mSoundManager;
  private OptionsMenus      menus;
  /** The main Options menu, hidden while a category popup is open. */
  private View              mainMenu = null;

  public static Preferences getDefaultPrefs(SharedPreferences sp) {
    Preferences prefs = new Preferences();
    prefs.bluetooth  =  sp.getInt("bluetooth", 0);
    prefs.collision  =  sp.getInt(KEY_COLLISION, BubbleSprite.MIN_PIX);
    prefs.compressor =  sp.getBoolean(KEY_COMPRESSOR, false);
    prefs.difficulty =  sp.getInt(KEY_DIFFICULTY, LevelManager.NORMAL);
    prefs.dontRushMe = !sp.getBoolean(KEY_RUSH_ME, true);
    prefs.fullscreen =  sp.getBoolean(KEY_FULLSCREEN, true);
    prefs.colorMode  =  sp.getBoolean(KEY_COLORBLIND, FrozenBubble.GAME_COLORBLIND);
    prefs.musicOn    =  sp.getBoolean(KEY_MUSIC, true);
    prefs.soundOn    =  sp.getBoolean(KEY_SOUND, true);
    prefs.targetMode =  Integer.valueOf(sp.getString(KEY_TARGETING,
        Integer.toString(FrozenBubble.POINT_TO_SHOOT)));

    return prefs;
  }

  public static void saveDefaultPreferences(Preferences prefs,
                                            SharedPreferences sp) {
    SharedPreferences.Editor editor = sp.edit();
    editor.putInt    ("bluetooth",       prefs.bluetooth );
    editor.putInt    (KEY_COLLISION,     prefs.collision );
    editor.putBoolean(KEY_COMPRESSOR,    prefs.compressor);
    editor.putInt    (KEY_DIFFICULTY,    prefs.difficulty);
    editor.putBoolean(KEY_RUSH_ME,      !prefs.dontRushMe);
    editor.putBoolean(KEY_FULLSCREEN,    prefs.fullscreen);
    editor.putBoolean(KEY_COLORBLIND,    prefs.colorMode );
    editor.putBoolean(KEY_MUSIC,         prefs.musicOn   );
    editor.putBoolean(KEY_SOUND,         prefs.soundOn   );
    editor.putString (KEY_TARGETING,     Integer.toString(prefs.targetMode));
    editor.commit();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    sp = PreferenceManager.getDefaultSharedPreferences(this);
    applyFullscreen();

    mSoundManager = new SoundManager(this);
    mSoundManager.loadSound("stick", R.raw.stick);
    menus = new OptionsMenus(this, this);

    // The home screen art behind the menu, as on the home screen.
    FrameLayout root = new FrameLayout(this);
    ImageView art = new ImageView(this);
    art.setBackgroundColor(Color.BLACK);
    art.setImageResource(GameView.chooseArt(this, R.drawable.home_screen_hd,
                                            R.drawable.home_screen));
    root.addView(art, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT));

    final Runnable backToMain = () -> mainMenu.setVisibility(View.VISIBLE);
    LinearLayout menu = MenuButtons.column(this,
        MenuButtons.create(this, R.string.opt_music_sound,
                           v -> { openCategory(); menus.showMusicMenu(backToMain); }),
        MenuButtons.create(this, R.string.opt_display,
                           v -> { openCategory(); menus.showDisplayMenu(backToMain); }),
        MenuButtons.create(this, R.string.opt_controls,
                           v -> { openCategory(); menus.showControlsMenu(backToMain); }),
        MenuButtons.create(this, R.string.opt_gameplay,
                           v -> { openCategory(); menus.showGameplayMenu(backToMain); }),
        MenuButtons.create(this, R.string.opt_effects,
                           v -> { openCategory(); menus.showEffectsMenu(backToMain); }),
        MenuButtons.create(this, R.string.credits,
                           v -> { click(); showCredits(); }),
        MenuButtons.create(this, R.string.back,
                           v -> { click(); closeOptions(); }));
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    FrameLayout center = new FrameLayout(this);
    center.addView(menu, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    scroll.addView(center, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT));
    mainMenu = scroll;
    root.addView(scroll, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT));
    setContentView(root);
  }

  /** Only the category popup shows, not the main menu behind it. */
  private void openCategory() {
    click();
    mainMenu.setVisibility(View.INVISIBLE);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mSoundManager != null)
      mSoundManager.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mSoundManager != null)
      mSoundManager.onResume();
    applyFullscreen();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mSoundManager != null) {
      mSoundManager.cleanUp();
      mSoundManager = null;
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      closeOptions();
      return true;
    }
    return super.onKeyDown(keyCode, msg);
  }

  /** Apply the chosen options and return to the previous screen. */
  private void closeOptions() {
    FrozenBubble.setPrefs(getDefaultPrefs(sp));
    finish();
  }

  private void applyFullscreen() {
    boolean fullscreen = sp.getBoolean(KEY_FULLSCREEN, true);
    if (fullscreen) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
    else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
    SystemUi.apply(this, fullscreen);
  }

  /*
   * OptionsMenus.Host
   */

  @Override
  public void click() {
    if (mSoundManager != null)
      mSoundManager.playSound("stick", R.raw.stick);
  }

  @Override
  public void onSettingChanged(String key) {
    if (KEY_FULLSCREEN.equals(key))
      applyFullscreen();
  }

  @Override
  public boolean pickMusicFolder() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    try {
      startActivityForResult(intent, REQUEST_MUSIC_FOLDER);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, R.string.opt_no_picker, Toast.LENGTH_LONG).show();
    }
    return true;
  }

  @Override
  public void placePopup(Window window) {
    // Centered on the screen.
  }

  /*
   * Credits.
   */

  private void showCredits() {
    Intent intent = new Intent(this, ScrollingCredits.class);
    intent.putExtra(ScrollingCredits.EXTRA_FROM_OPTIONS, true);
    startActivity(intent);
  }

  /*
   * Custom music folder.
   */

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != REQUEST_MUSIC_FOLDER) {
      return;
    }
    if ((resultCode == RESULT_OK) && (data != null) && (data.getData() != null)) {
      Uri folder = data.getData();
      CustomMusic.setFolder(this, folder);
      sp.edit().putBoolean(CustomMusic.KEY_ON, true).apply();
      int count = CustomMusic.listSongs(this, folder).size();
      Toast.makeText(this, CustomMusic.describeFolder(this, folder) + ": " +
          ((count == 0) ?
           "no .mod/.xm/.it/.s3m songs found. Built-in music will play." :
           count + ((count == 1) ? " song found." : " songs found.")),
          Toast.LENGTH_LONG).show();
    }
    else if (CustomMusic.getFolder(this) == null) {
      // Picker cancelled and there is still no folder: switch back off.
      sp.edit().putBoolean(CustomMusic.KEY_ON, false).apply();
    }
    menus.refresh();
  }
}
