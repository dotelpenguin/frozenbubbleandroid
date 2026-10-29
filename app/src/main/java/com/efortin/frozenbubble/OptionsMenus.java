/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.BubbleSprite;
import org.jfedor.frozenbubble.Effects;
import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.GameView;
import org.jfedor.frozenbubble.LevelManager;
import org.jfedor.frozenbubble.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The bubble-style option popups (Music & Sound, Display, Controls,
 * Gameplay), shared by the Options screen and the in-game Options menu.
 * <p>Each category is a see-through popup of home-screen-style buttons
 * that show and change their setting; sliders open their own popup.
 * Settings are saved to the shared preferences and applied to the game
 * straight away.
 */
public class OptionsMenus {

  public static final String KEY_COLLISION   = "collision_option";
  public static final String KEY_COMPRESSOR  = "compressor_option";
  public static final String KEY_DIFFICULTY  = "difficulty_option";
  public static final String KEY_RUSH_ME     = "rush_me_option";
  public static final String KEY_FULLSCREEN  = "fullscreen_option";
  public static final String KEY_COLORBLIND  = "colorblind_option";
  public static final String KEY_MUSIC       = "play_music_option";
  public static final String KEY_SOUND       = "sound_effects_option";
  public static final String KEY_TARGETING   = "targeting_option";

  private static final int COLLISION_MAX  = 29;
  private static final int DIFFICULTY_MAX = 8;

  /** What the screen showing these popups provides. */
  public interface Host {
    /** Play the menu click sound. */
    void click();
    /** A setting changed; it is already saved and applied. */
    void onSettingChanged(String key);
    /**
     * Open the music folder picker, or <code>false</code> if this screen
     * can't (the in-game menu).
     */
    boolean pickMusicFolder();
    /**
     * Position a popup window, e.g. below the game's PAUSE panel, or do
     * nothing to center it.
     */
    void placePopup(Window window);
  }

  /** One button in a category popup. */
  private interface Item {
    String label();
    void   onTap();
  }

  private final Activity          activity;
  private final Host              host;
  private final SharedPreferences sp;

  /** Refreshes the button labels of the open category popup, if any. */
  private Runnable    refreshOpenMenu = null;
  /** The open category popup, hidden while a slider popup is open. */
  private AlertDialog openMenu        = null;

  public OptionsMenus(Activity activity, Host host) {
    this.activity = activity;
    this.host     = host;
    this.sp       = PreferenceManager.getDefaultSharedPreferences(activity);
  }

  /** Close the open category popup (e.g. before the screen reloads). */
  public void closeOpenMenu() {
    if (openMenu != null) {
      openMenu.setOnDismissListener(null);
      openMenu.dismiss();
      openMenu = null;
      refreshOpenMenu = null;
    }
  }

  /** Relabel the open popup's buttons, e.g. after the folder picker. */
  public void refresh() {
    if (refreshOpenMenu != null)
      refreshOpenMenu.run();
  }

  /*
   * Category popups.  onClose runs when the popup is closed, so the
   * caller can show its own menu again.
   */

  public void showMusicMenu(Runnable onClose) {
    showMenu(onClose,
        toggle(R.string.opt_music, KEY_MUSIC, true, null),
        toggle(R.string.opt_sound, KEY_SOUND, true, null),
        toggle(R.string.opt_my_music, CustomMusic.KEY_ON, false, () -> {
          // Turned on with no folder yet: go straight to the picker.
          if (sp.getBoolean(CustomMusic.KEY_ON, false) &&
              (CustomMusic.getFolder(activity) == null)) {
            if (!host.pickMusicFolder()) {
              sp.edit().putBoolean(CustomMusic.KEY_ON, false).apply();
              toast(R.string.opt_folder_from_home);
            }
          }
        }),
        action(R.string.opt_choose_folder, () -> {
          if (!host.pickMusicFolder())
            toast(R.string.opt_folder_from_home);
        }));
  }

  public void showDisplayMenu(Runnable onClose) {
    showMenu(onClose,
        toggle(R.string.opt_fullscreen, KEY_FULLSCREEN, true, null),
        toggle(R.string.opt_gpu, GameView.KEY_GPU_RENDERING, true, () ->
            toast(R.string.opt_next_game_note)),
        toggle(R.string.opt_original_art, GameView.KEY_ORIGINAL_ART, false, () ->
            toast(R.string.opt_next_game_note)),
        toggle(R.string.opt_colorblind, KEY_COLORBLIND,
               FrozenBubble.GAME_NORMAL, null));
  }

  public void showControlsMenu(Runnable onClose) {
    final String[] modes =
        activity.getResources().getStringArray(R.array.shoot_mode_array);
    showMenu(onClose,
        new Item() {
          public String label() {
            int mode = targetMode();
            String name = ((mode >= 0) && (mode < modes.length)) ?
                modes[mode] : modes[FrozenBubble.POINT_TO_SHOOT];
            return activity.getString(R.string.opt_targeting) + ": " + name;
          }
          public void onTap() {
            int mode = (targetMode() + 1) % modes.length;
            sp.edit().putString(KEY_TARGETING, Integer.toString(mode)).apply();
            changed(KEY_TARGETING);
          }
        },
        slider(R.string.opt_sensitivity, KEY_COLLISION,
               BubbleSprite.MIN_PIX, COLLISION_MAX, BubbleSprite.MIN_PIX,
               R.string.opt_sensitivity_value, R.string.opt_sensitivity_help));
  }

  public void showGameplayMenu(Runnable onClose) {
    showMenu(onClose,
        slider(R.string.opt_difficulty, KEY_DIFFICULTY,
               LevelManager.EASY, DIFFICULTY_MAX, LevelManager.NORMAL,
               R.string.opt_difficulty_value, R.string.opt_difficulty_help),
        toggle(R.string.opt_hurry, KEY_RUSH_ME, true, null),
        toggle(R.string.opt_compressor, KEY_COMPRESSOR, false, null),
        new Item() {
          public String label() {
            return activity.getString(R.string.opt_player_name) + ": " +
                PlayerName.get(activity);
          }
          public void onTap() {
            showNameEditor();
          }
        });
  }

  /**
   * Edit the player's name (recorded with high scores): a name field
   * with Done / Cancel, in place of the category popup.
   */
  private void showNameEditor() {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    LinearLayout box = new LinearLayout(activity);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (12 * activity.getResources().getDisplayMetrics().density);
    box.setPadding(pad, pad, pad, 0);
    TextView title = new TextView(activity);
    styleText(title, 22);
    title.setText(R.string.opt_player_name);
    box.addView(title);
    final android.widget.EditText edit =
        PlayerName.makeEditor(activity, PlayerName.get(activity));
    box.addView(edit);
    Button done = MenuButtons.create(activity, R.string.opt_done, v -> {
      host.click();
      PlayerName.set(activity, edit.getText().toString());
      dialog.dismiss();
    });
    Button cancel = MenuButtons.create(activity, R.string.cancel, v -> {
      host.click();
      dialog.dismiss();
    });
    box.addView(MenuButtons.column(activity, done, cancel));
    dialog.setView(scrolling(box));
    // Only this popup shows; its category popup comes back after.
    final AlertDialog under = openMenu;
    if (under != null)
      under.hide();
    dialog.setOnDismissListener(d -> {
      if ((under != null) && (openMenu == under)) {
        refresh();
        under.show();
      }
    });
    showSeeThrough(dialog);
  }

  /**
   * Options &rarr; Language: the phone's language, or one the game is
   * translated into, each named in its own language.  The current choice
   * is marked.
   */
  public void showLanguageMenu(Runnable onClose) {
    Item[] items = new Item[LocaleHelper.TAGS.length];
    for (int i = 0; i < items.length; i++) {
      final String tag  = LocaleHelper.TAGS[i];
      final String name = LocaleHelper.NAMES[i];
      items[i] = new Item() {
        public String label() {
          String text = (name != null) ? name
                                       : activity.getString(R.string.lang_system);
          return tag.equals(LocaleHelper.getLanguage(activity)) ?
              "\u25CF " + text : text;
        }
        public void onTap() {
          if (!tag.equals(LocaleHelper.getLanguage(activity))) {
            LocaleHelper.setLanguage(activity, tag);
            host.onSettingChanged(LocaleHelper.KEY_LANGUAGE);
          }
        }
      };
    }
    showMenu(onClose, items);
  }

  public void showEffectsMenu(Runnable onClose) {
    showMenu(onClose,
        toggle(R.string.fx_pop,     Effects.KEY_POP,     Effects.DEF_POP,     null),
        toggle(R.string.fx_fade,    Effects.KEY_FADE,    Effects.DEF_FADE,    null),
        toggle(R.string.fx_wobble,  Effects.KEY_WOBBLE,  Effects.DEF_WOBBLE,  null),
        toggle(R.string.fx_shake,   Effects.KEY_SHAKE,   Effects.DEF_SHAKE,   null),
        toggle(R.string.fx_trail,   Effects.KEY_TRAIL,   Effects.DEF_TRAIL,   null),
        toggle(R.string.fx_guide,   Effects.KEY_GUIDE,   Effects.DEF_GUIDE,   null),
        toggle(R.string.fx_vibrate, Effects.KEY_VIBRATE, Effects.DEF_VIBRATE, null));
  }

  private int targetMode() {
    try {
      return Integer.parseInt(sp.getString(KEY_TARGETING,
          Integer.toString(FrozenBubble.POINT_TO_SHOOT)));
    } catch (NumberFormatException e) {
      return FrozenBubble.POINT_TO_SHOOT;
    }
  }

  /** Apply the saved settings to the game, then tell the host. */
  private void changed(String key) {
    FrozenBubble.setPrefs(PreferencesActivity.getDefaultPrefs(sp));
    Effects.loadSettings(activity);
    host.onSettingChanged(key);
  }

  private void toast(int textId) {
    Toast.makeText(activity, textId, Toast.LENGTH_SHORT).show();
  }

  /**
   * A see-through popup with one bubble button per item, plus Back.
   * Tapping an item changes its setting and relabels the buttons; the
   * popup stays open until Back.
   */
  private void showMenu(final Runnable onClose, final Item... items) {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    final Button[] buttons = new Button[items.length + 1];
    final Runnable refresh = () -> {
      for (int i = 0; i < items.length; i++)
        buttons[i].setText(items[i].label());
    };
    for (int i = 0; i < items.length; i++) {
      final Item item = items[i];
      buttons[i] = MenuButtons.create(activity, item.label(), v -> {
        host.click();
        item.onTap();
        refresh.run();
      });
      widen(buttons[i]);
    }
    buttons[items.length] = MenuButtons.create(activity, R.string.back, v -> {
      host.click();
      dialog.dismiss();
    });
    widen(buttons[items.length]);
    dialog.setView(scrolling(MenuButtons.column(activity, buttons)));
    dialog.setOnDismissListener(d -> {
      if (refreshOpenMenu == refresh)
        refreshOpenMenu = null;
      if (openMenu == dialog)
        openMenu = null;
      if (onClose != null)
        onClose.run();
    });
    refreshOpenMenu = refresh;
    openMenu        = dialog;
    showSeeThrough(dialog);
  }

  /** Lets a popup scroll when it is taller than the screen (landscape). */
  private View scrolling(View content) {
    ScrollView scroll = new ScrollView(activity);
    scroll.addView(content);
    return scroll;
  }

  /** Setting buttons show a value too, so they are wider. */
  public static void widen(Button button) {
    button.setWidth((int) (button.getTextSize() * 13));
  }

  /** Show a popup with no dialog box: the buttons float over the screen. */
  public void showSeeThrough(AlertDialog dialog) {
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      host.placePopup(window);
    }
    dialog.show();
  }

  private Item toggle(final int nameId, final String key,
                      final boolean def, final Runnable after) {
    return new Item() {
      public String label() {
        return activity.getString(nameId) + ": " +
            activity.getString(sp.getBoolean(key, def) ? R.string.opt_on
                                                       : R.string.opt_off);
      }
      public void onTap() {
        sp.edit().putBoolean(key, !sp.getBoolean(key, def)).apply();
        if (after != null)
          after.run();
        changed(key);
      }
    };
  }

  private Item action(final int nameId, final Runnable run) {
    return new Item() {
      public String label() { return activity.getString(nameId); }
      public void   onTap() { run.run(); }
    };
  }

  /** A button showing a number setting, which opens a slider popup. */
  private Item slider(final int nameId, final String key, final int min,
                      final int max, final int def, final int valueFmtId,
                      final int helpId) {
    return new Item() {
      public String label() {
        return activity.getString(nameId) + ": " +
            activity.getString(valueFmtId, clamp(sp.getInt(key, def), min, max));
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
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    final float dp = activity.getResources().getDisplayMetrics().density;
    final String name = activity.getString(nameId);

    final TextView value = new TextView(activity);
    styleText(value, 22);
    final TextView help = new TextView(activity);
    styleText(help, 14);
    help.setTypeface(null, Typeface.NORMAL);
    help.setText(helpId);

    // A thick, high-contrast slider (res/layout/options_slider.xml).
    final SeekBar bar = (SeekBar) activity.getLayoutInflater()
        .inflate(R.layout.options_slider, null);
    bar.setMax(max - min);
    bar.setProgress(clamp(sp.getInt(key, def), min, max) - min);
    value.setText(name + ": " +
                  activity.getString(valueFmtId, bar.getProgress() + min));
    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar s, int progress, boolean user) {
        value.setText(name + ": " +
                      activity.getString(valueFmtId, progress + min));
      }
      public void onStartTrackingTouch(SeekBar s) {}
      public void onStopTrackingTouch(SeekBar s) {}
    });

    Button done = MenuButtons.create(activity, R.string.opt_done, v -> {
      host.click();
      sp.edit().putInt(key, bar.getProgress() + min).apply();
      changed(key);
      dialog.dismiss();
    });
    Button cancel = MenuButtons.create(activity, R.string.cancel, v -> {
      host.click();
      dialog.dismiss();
    });

    LinearLayout box = new LinearLayout(activity);
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
    box.addView(MenuButtons.column(activity, done, cancel));
    dialog.setView(scrolling(box));
    // Only the slider popup shows; its category popup comes back after.
    final AlertDialog under = openMenu;
    if (under != null)
      under.hide();
    dialog.setOnDismissListener(d -> {
      if ((under != null) && (openMenu == under)) {
        refresh();
        under.show();
      }
    });
    showSeeThrough(dialog);
  }

  /** White, bold, shadowed text, readable over the screen behind. */
  public static void styleText(TextView text, int sizeDp) {
    text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
    text.setTypeface(null, Typeface.BOLD);
    text.setTextColor(Color.WHITE);
    text.setShadowLayer(6, 2, 2, Color.BLACK);
    text.setGravity(Gravity.CENTER_HORIZONTAL);
  }
}
