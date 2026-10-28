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
import org.jfedor.frozenbubble.GameView;
import org.jfedor.frozenbubble.LevelManager;
import org.jfedor.frozenbubble.R;
import org.jfedor.frozenbubble.SoundManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Options screen, in the style of the home screen menu: a short
 * menu of categories, each opening a see-through popup of bubble
 * buttons that show and change their setting.  Sliders (difficulty,
 * bubble sensitivity) open their own popup.
 * <p>Settings are stored in the same shared preferences, under the same
 * keys, as the old settings list, so existing settings carry over.
 */
public class PreferencesActivity extends Activity {

  private static final String KEY_COLLISION   = "collision_option";
  private static final String KEY_COMPRESSOR  = "compressor_option";
  private static final String KEY_DIFFICULTY  = "difficulty_option";
  private static final String KEY_RUSH_ME     = "rush_me_option";
  private static final String KEY_FULLSCREEN  = "fullscreen_option";
  private static final String KEY_COLORBLIND  = "colorblind_option";
  private static final String KEY_MUSIC       = "play_music_option";
  private static final String KEY_SOUND       = "sound_effects_option";
  private static final String KEY_TARGETING   = "targeting_option";
  private static final String KEY_GPU         = "gpu_rendering_option";
  private static final String KEY_ORIGINAL    = "original_art_option";

  private static final int COLLISION_MAX  = 29;
  private static final int DIFFICULTY_MAX = 8;

  private static final int REQUEST_MUSIC_FOLDER = 2001;

  private SharedPreferences sp;
  private SoundManager      mSoundManager;
  /** Refreshes the button labels of the open category popup, if any. */
  private Runnable          refreshOpenMenu = null;
  /** The open category popup, hidden while a slider popup is open. */
  private AlertDialog       openMenu        = null;
  /** The main Options menu, hidden while a category popup is open. */
  private View              mainMenu        = null;

  /** One button in a category popup. */
  private interface Item {
    String label();
    void   onTap();
  }

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

    // The home screen art behind the menu, as on the home screen.
    FrameLayout root = new FrameLayout(this);
    ImageView art = new ImageView(this);
    art.setBackgroundColor(Color.BLACK);
    art.setImageResource(GameView.chooseArt(this, R.drawable.home_screen_hd,
                                            R.drawable.home_screen));
    root.addView(art, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT));

    LinearLayout menu = MenuButtons.column(this,
        MenuButtons.create(this, R.string.opt_music_sound,
                           v -> { click(); showMusicMenu(); }),
        MenuButtons.create(this, R.string.opt_display,
                           v -> { click(); showDisplayMenu(); }),
        MenuButtons.create(this, R.string.opt_controls,
                           v -> { click(); showControlsMenu(); }),
        MenuButtons.create(this, R.string.opt_gameplay,
                           v -> { click(); showGameplayMenu(); }),
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
    applyPrefs();
    finish();
  }

  /** Make the game use the current settings straight away. */
  private void applyPrefs() {
    FrozenBubble.setPrefs(getDefaultPrefs(sp));
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

  private void click() {
    if (mSoundManager != null)
      mSoundManager.playSound("stick", R.raw.stick);
  }

  /*
   * Category popups.
   */

  private void showMusicMenu() {
    showMenu(
        toggle(R.string.opt_music, KEY_MUSIC, true, null),
        toggle(R.string.opt_sound, KEY_SOUND, true, null),
        toggle(R.string.opt_my_music, CustomMusic.KEY_ON, false, () -> {
          // Turned on with no folder yet: go straight to the picker.
          if (sp.getBoolean(CustomMusic.KEY_ON, false) &&
              (CustomMusic.getFolder(this) == null)) {
            pickMusicFolder();
          }
        }),
        action(R.string.opt_choose_folder, this::pickMusicFolder));
  }

  private void showDisplayMenu() {
    showMenu(
        toggle(R.string.opt_fullscreen, KEY_FULLSCREEN, true,
               this::applyFullscreen),
        toggle(R.string.opt_gpu, KEY_GPU, true, null),
        toggle(R.string.opt_original_art, KEY_ORIGINAL, false, () ->
            Toast.makeText(this, R.string.opt_original_art_note,
                           Toast.LENGTH_SHORT).show()),
        toggle(R.string.opt_colorblind, KEY_COLORBLIND,
               FrozenBubble.GAME_COLORBLIND, null));
  }

  private void showControlsMenu() {
    final String[] modes = getResources().getStringArray(R.array.shoot_mode_array);
    showMenu(
        new Item() {
          public String label() {
            int mode = targetMode();
            String name = ((mode >= 0) && (mode < modes.length)) ?
                modes[mode] : modes[FrozenBubble.POINT_TO_SHOOT];
            return getString(R.string.opt_targeting) + ": " + name;
          }
          public void onTap() {
            int mode = (targetMode() + 1) % modes.length;
            sp.edit().putString(KEY_TARGETING, Integer.toString(mode)).apply();
          }
        },
        slider(R.string.opt_sensitivity, KEY_COLLISION,
               BubbleSprite.MIN_PIX, COLLISION_MAX, BubbleSprite.MIN_PIX,
               R.string.opt_sensitivity_value, R.string.opt_sensitivity_help));
  }

  private void showGameplayMenu() {
    showMenu(
        slider(R.string.opt_difficulty, KEY_DIFFICULTY,
               LevelManager.EASY, DIFFICULTY_MAX, LevelManager.NORMAL,
               R.string.opt_difficulty_value, R.string.opt_difficulty_help),
        toggle(R.string.opt_hurry, KEY_RUSH_ME, true, null),
        toggle(R.string.opt_compressor, KEY_COMPRESSOR, false, null));
  }

  private int targetMode() {
    try {
      return Integer.parseInt(sp.getString(KEY_TARGETING,
          Integer.toString(FrozenBubble.POINT_TO_SHOOT)));
    } catch (NumberFormatException e) {
      return FrozenBubble.POINT_TO_SHOOT;
    }
  }

  /**
   * A see-through popup with one bubble button per item, plus Back.
   * Tapping an item changes its setting and relabels the buttons; the
   * popup stays open until Back.
   */
  private void showMenu(final Item... items) {
    final AlertDialog dialog = new AlertDialog.Builder(this).create();
    final Button[] buttons = new Button[items.length + 1];
    final Runnable refresh = () -> {
      for (int i = 0; i < items.length; i++)
        buttons[i].setText(items[i].label());
    };
    for (int i = 0; i < items.length; i++) {
      final Item item = items[i];
      buttons[i] = MenuButtons.create(this, item.label(), v -> {
        click();
        item.onTap();
        applyPrefs();
        refresh.run();
      });
      widen(buttons[i]);
    }
    buttons[items.length] = MenuButtons.create(this, R.string.back, v -> {
      click();
      dialog.dismiss();
    });
    widen(buttons[items.length]);
    dialog.setView(scrolling(MenuButtons.column(this, buttons)));
    dialog.setOnDismissListener(d -> {
      if (refreshOpenMenu == refresh)
        refreshOpenMenu = null;
      if (openMenu == dialog)
        openMenu = null;
      // Back to the main Options menu.
      if (mainMenu != null)
        mainMenu.setVisibility(View.VISIBLE);
    });
    refreshOpenMenu = refresh;
    openMenu        = dialog;
    // Only the category popup shows, not the main menu behind it.
    if (mainMenu != null)
      mainMenu.setVisibility(View.INVISIBLE);
    showSeeThrough(dialog);
  }

  /** Lets a popup scroll when it is taller than the screen (landscape). */
  private View scrolling(View content) {
    ScrollView scroll = new ScrollView(this);
    scroll.addView(content);
    return scroll;
  }

  /** Setting buttons show a value too, so they are wider. */
  private static void widen(Button button) {
    button.setWidth((int) (button.getTextSize() * 13));
  }

  private static void showSeeThrough(AlertDialog dialog) {
    if (dialog.getWindow() != null) {
      // No dialog box: the buttons float over the menu.
      dialog.getWindow().setBackgroundDrawable(
          new ColorDrawable(Color.TRANSPARENT));
    }
    dialog.show();
  }

  private Item toggle(final int nameId, final String key,
                      final boolean def, final Runnable after) {
    return new Item() {
      public String label() {
        return getString(nameId) + ": " +
            getString(sp.getBoolean(key, def) ? R.string.opt_on
                                              : R.string.opt_off);
      }
      public void onTap() {
        sp.edit().putBoolean(key, !sp.getBoolean(key, def)).apply();
        if (after != null)
          after.run();
      }
    };
  }

  private Item action(final int nameId, final Runnable run) {
    return new Item() {
      public String label() { return getString(nameId); }
      public void   onTap() { run.run(); }
    };
  }

  /** A button showing a number setting, which opens a slider popup. */
  private Item slider(final int nameId, final String key, final int min,
                      final int max, final int def, final int valueFmtId,
                      final int helpId) {
    return new Item() {
      public String label() {
        return getString(nameId) + ": " +
            getString(valueFmtId, clamp(sp.getInt(key, def), min, max));
      }
      public void onTap() {
        showSlider(nameId, key, min, max, def, valueFmtId, helpId);
      }
    };
  }

  private static int clamp(int v, int min, int max) {
    return Math.max(min, Math.min(max, v));
  }

  /**
   * A see-through popup with a slider, its value above it, and Done /
   * Cancel.  The setting only changes on Done.
   */
  private void showSlider(int nameId, final String key, final int min,
                          int max, int def, final int valueFmtId,
                          int helpId) {
    final AlertDialog dialog = new AlertDialog.Builder(this).create();
    final float dp = getResources().getDisplayMetrics().density;
    final String name = getString(nameId);

    final TextView value = new TextView(this);
    styleText(value, 22);
    final TextView help = new TextView(this);
    styleText(help, 14);
    help.setTypeface(null, Typeface.NORMAL);
    help.setText(helpId);

    // A thick, high-contrast slider (res/layout/options_slider.xml).
    final SeekBar bar = (SeekBar) getLayoutInflater()
        .inflate(R.layout.options_slider, null);
    bar.setMax(max - min);
    bar.setProgress(clamp(sp.getInt(key, def), min, max) - min);
    value.setText(name + ": " + getString(valueFmtId, bar.getProgress() + min));
    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar s, int progress, boolean user) {
        value.setText(name + ": " + getString(valueFmtId, progress + min));
      }
      public void onStartTrackingTouch(SeekBar s) {}
      public void onStopTrackingTouch(SeekBar s) {}
    });

    Button done = MenuButtons.create(this, R.string.opt_done, v -> {
      click();
      sp.edit().putInt(key, bar.getProgress() + min).apply();
      applyPrefs();
      dialog.dismiss();
    });
    Button cancel = MenuButtons.create(this, R.string.cancel, v -> {
      click();
      dialog.dismiss();
    });

    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (12 * dp);
    box.setPadding(pad, pad, pad, 0);
    box.addView(value);
    LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
        (int) (300 * dp), LinearLayout.LayoutParams.WRAP_CONTENT);
    barParams.topMargin    = (int) (16 * dp);
    barParams.bottomMargin = (int) (8 * dp);
    box.addView(bar, barParams);
    box.addView(help);
    box.addView(MenuButtons.column(this, done, cancel));
    dialog.setView(scrolling(box));
    // Only the slider popup shows; its category popup comes back after.
    final AlertDialog under = openMenu;
    if (under != null)
      under.hide();
    dialog.setOnDismissListener(d -> {
      if ((under != null) && (openMenu == under)) {
        if (refreshOpenMenu != null)
          refreshOpenMenu.run();
        under.show();
      }
    });
    showSeeThrough(dialog);
  }

  /** White, bold, shadowed text, readable over the home screen art. */
  private static void styleText(TextView text, int sizeDp) {
    text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
    text.setTypeface(null, Typeface.BOLD);
    text.setTextColor(Color.WHITE);
    text.setShadowLayer(6, 2, 2, Color.BLACK);
    text.setGravity(Gravity.CENTER_HORIZONTAL);
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

  private void pickMusicFolder() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    try {
      startActivityForResult(intent, REQUEST_MUSIC_FOLDER);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, R.string.opt_no_picker, Toast.LENGTH_LONG).show();
    }
  }

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
    if (refreshOpenMenu != null)
      refreshOpenMenu.run();
  }
}
