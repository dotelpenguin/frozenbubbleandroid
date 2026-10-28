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

import org.jfedor.frozenbubble.BubbleSprite;
import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.LevelManager;
import org.jfedor.frozenbubble.R;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity{

  private SeekBarPreference collisionSlider;
  private SeekBarPreference difficultySlider;

  private void cleanUp() {
    collisionSlider  = null;
    difficultySlider = null;
  }
  
  public static Preferences getDefaultPrefs(SharedPreferences sp) {
    Preferences prefs = new Preferences();
    prefs.bluetooth  =  sp.getInt("bluetooth", 0);
    prefs.collision  =  sp.getInt("collision_option", BubbleSprite.MIN_PIX);
    prefs.compressor =  sp.getBoolean("compressor_option", false);
    prefs.difficulty =  sp.getInt("difficulty_option", LevelManager.NORMAL);
    prefs.dontRushMe = !sp.getBoolean("rush_me_option", true);
    prefs.fullscreen =  sp.getBoolean("fullscreen_option", true);
    prefs.colorMode  =  sp.getBoolean("colorblind_option", FrozenBubble.GAME_COLORBLIND);
    prefs.musicOn    =  sp.getBoolean("play_music_option", true);
    prefs.soundOn    =  sp.getBoolean("sound_effects_option", true);
    prefs.targetMode =  Integer.valueOf(sp.getString("targeting_option",
        Integer.toString(FrozenBubble.POINT_TO_SHOOT)));

    return prefs;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     addPreferencesFromResource(R.layout.activity_preferences_screen);
     collisionSlider  = (SeekBarPreference) findPreference("collision_option" );
     difficultySlider = (SeekBarPreference) findPreference("difficulty_option");
     setupCustomMusic();
     setupCredits();
     addBackButton();
     SystemUi.apply(this, PreferenceManager.getDefaultSharedPreferences(this)
         .getBoolean("fullscreen_option", true));
  }

  /**
   * An on-screen Back button at the top of the list, in the home screen
   * menu style; phones may have no Back button, and the gesture isn't
   * obvious.  It does the same as Back: apply the options and close.
   */
  private void addBackButton() {
    // Same look as the menu buttons elsewhere (this screen's own theme
    // would draw an old-style button).
    android.content.Context themed = new android.view.ContextThemeWrapper(
        this, android.R.style.Theme_DeviceDefault_Light);
    android.widget.Button back = MenuButtons.create(themed, R.string.back,
        new android.view.View.OnClickListener() {
      @Override
      public void onClick(android.view.View v) {
        closeOptions();
      }
    });
    android.widget.LinearLayout header =
        MenuButtons.column(themed, back);
    getListView().addHeaderView(header, null, false);
  }

  /** Apply the chosen options and return to the previous screen. */
  private void closeOptions() {
    FrozenBubble.setPrefs(getDefaultPrefs(PreferenceManager.
        getDefaultSharedPreferences(this)));
    finish();
  }

  private static final String KEY_FOLDER_PICK      = "custom_music_folder_pick";
  private static final String KEY_CREDITS          = "credits_pick";

  /**
   * The "Credits" row plays the scrolling end-of-game credits, then
   * returns here.
   */
  private void setupCredits() {
    findPreference(KEY_CREDITS).setOnPreferenceClickListener(p -> {
      Intent intent = new Intent(this, ScrollingCredits.class);
      intent.putExtra(ScrollingCredits.EXTRA_FROM_OPTIONS, true);
      startActivity(intent);
      return true;
    });
  }
  private static final int    REQUEST_MUSIC_FOLDER = 2001;

  /**
   * Wire up the "Use My Music Folder" switch and the folder chooser row.
   */
  private void setupCustomMusic() {
    CheckBoxPreference toggle =
        (CheckBoxPreference) findPreference(CustomMusic.KEY_ON);
    Preference folderRow = findPreference(KEY_FOLDER_PICK);

    folderRow.setOnPreferenceClickListener(p -> {
      pickMusicFolder();
      return true;
    });
    toggle.setOnPreferenceChangeListener((p, newValue) -> {
      // Turning it on with no folder yet: go straight to the picker.
      if (Boolean.TRUE.equals(newValue) && (CustomMusic.getFolder(this) == null)) {
        pickMusicFolder();
      }
      return true;
    });
    updateMusicFolderSummary();
  }

  private void pickMusicFolder() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    try {
      startActivityForResult(intent, REQUEST_MUSIC_FOLDER);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "No folder picker is available on this device.",
                     Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != REQUEST_MUSIC_FOLDER) {
      return;
    }
    CheckBoxPreference toggle =
        (CheckBoxPreference) findPreference(CustomMusic.KEY_ON);
    if ((resultCode == RESULT_OK) && (data != null) && (data.getData() != null)) {
      Uri folder = data.getData();
      CustomMusic.setFolder(this, folder);
      toggle.setChecked(true);
      int count = CustomMusic.listSongs(this, folder).size();
      Toast.makeText(this, (count == 0) ?
          "No .mod/.xm/.it/.s3m songs found there. Built-in music will play." :
          count + ((count == 1) ? " song found." : " songs found."),
          Toast.LENGTH_LONG).show();
    }
    else if (CustomMusic.getFolder(this) == null) {
      // Picker cancelled and there is still no folder: switch back off.
      toggle.setChecked(false);
    }
    updateMusicFolderSummary();
  }

  private void updateMusicFolderSummary() {
    Preference folderRow = findPreference(KEY_FOLDER_PICK);
    Uri folder = CustomMusic.getFolder(this);
    if (folder == null) {
      folderRow.setSummary("No folder chosen");
      return;
    }
    int count = CustomMusic.listSongs(this, folder).size();
    folderRow.setSummary(CustomMusic.describeFolder(this, folder) + " - " +
        count + ((count == 1) ? " song" : " songs"));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    cleanUp();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    boolean result = false;

    if (keyCode == KeyEvent.KEYCODE_BACK) {
      closeOptions();
    }
    else {
      result  = collisionSlider .onKey(keyCode, msg);
      result |= difficultySlider.onKey(keyCode, msg);
    }

    return result || super.onKeyDown(keyCode, msg);
  }

  public static void saveDefaultPreferences(Preferences prefs,
                                            SharedPreferences sp) {
    SharedPreferences.Editor editor = sp.edit();
    editor.putInt    ("bluetooth",            prefs.bluetooth );
    editor.putInt    ("collision_option",     prefs.collision );
    editor.putBoolean("compressor_option",    prefs.compressor);
    editor.putInt    ("difficulty_option",    prefs.difficulty);
    editor.putBoolean("rush_me_option",      !prefs.dontRushMe);
    editor.putBoolean("fullscreen_option",    prefs.fullscreen);
    editor.putBoolean("colorblind_option",    prefs.colorMode );
    editor.putBoolean("play_music_option",    prefs.musicOn   );
    editor.putBoolean("sound_effects_option", prefs.soundOn   );
    editor.putString ("targeting_option",     Integer.toString(prefs.targetMode));
    editor.commit();
  }
}
