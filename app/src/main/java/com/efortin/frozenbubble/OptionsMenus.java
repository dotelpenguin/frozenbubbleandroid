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
    /**
     * <code>true</code> for the in-game menu, where some options (like
     * resetting level progress) are left out.
     */
    default boolean inGame() {
      return false;
    }
    /** <code>true</code> during a network (Wi-Fi / Bluetooth) game. */
    default boolean networkGame() {
      return false;
    }
    /** Restart the current game with the changed settings. */
    default void restartGame() {
    }
    /**
     * <code>false</code> if the option doesn't affect the game being
     * played (e.g. Difficulty during a Puzzle game), so no restart.
     */
    default boolean affectsCurrentGame(String key) {
      return true;
    }
  }

  /*
   * Options that only take effect in a new game.  Changed in game, they
   * ask first and then restart the game.
   */
  private static final java.util.Set<String> RESTART_KEYS =
      new java.util.HashSet<String>(java.util.Arrays.asList(
          KEY_DIFFICULTY, KEY_COMPRESSOR,
          GameView.KEY_GPU_RENDERING, GameView.KEY_ORIGINAL_ART));

  /**
   * Apply a change: straight away, or, for an option that needs a new
   * game while one is being played, after asking, followed by a restart.
   * During a network game such options can't be changed (restarting one
   * phone would put the two out of step).
   */
  private boolean needsRestart(String key) {
    return host.inGame() && RESTART_KEYS.contains(key) &&
           host.affectsCurrentGame(key);
  }

  private void applyChange(String key, Runnable apply) {
    if (!needsRestart(key)) {
      apply.run();
      return;
    }
    if (host.networkGame()) {
      toast(R.string.restart_network);
      return;
    }
    showConfirm(R.string.restart_title, R.string.restart_message,
                R.string.restart_button, () -> {
      apply.run();
      closeOpenMenu();
      host.restartGame();
    });
  }

  /** One button in a category popup. */
  private interface Item {
    CharSequence label();
    void         onTap();
    /** <code>false</code> to grey the button out. */
    default boolean enabled() {
      return true;
    }
  }

  /** Grey out a button whose setting doesn't apply right now. */
  private static void applyEnabled(Button button, Item item) {
    boolean on = item.enabled();
    button.setEnabled(on);
    button.setAlpha(on ? 1f : 0.45f);
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
    folderSummary = null;   // the music folder may have changed
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
        new Item() {
          public CharSequence label() {
            // "Choose Music Folder" with the folder and its song count
            // below it in smaller text.
            return withNote(activity.getString(R.string.opt_choose_folder),
                            folderSummary());
          }
          public void onTap() {
            if (!host.pickMusicFolder())
              toast(R.string.opt_folder_from_home);
          }
        });
  }

  /**
   * "Applies from the next game" after changing GPU Rendering or Original
   * Art on the home screen's Options (in game, the game restarts).
   */
  private Runnable nextGameNote() {
    return host.inGame() ? null : () -> toast(R.string.opt_next_game_note);
  }

  /** A button label with a second, smaller line under it. */
  private static CharSequence withNote(String label, String note) {
    android.text.SpannableStringBuilder text =
        new android.text.SpannableStringBuilder(label + "\n" + note);
    int start = label.length() + 1;
    text.setSpan(new android.text.style.RelativeSizeSpan(0.72f),
                 start, text.length(),
                 android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.setSpan(new android.text.style.StyleSpan(Typeface.NORMAL),
                 start, text.length(),
                 android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return text;
  }

  /** Cached "Mods – 23 songs" (counting songs reads the folder). */
  private String folderSummary = null;

  private String folderSummary() {
    if (folderSummary == null) {
      android.net.Uri folder = CustomMusic.getFolder(activity);
      if (folder == null) {
        folderSummary = activity.getString(R.string.folder_none);
      }
      else {
        int count = CustomMusic.listSongs(activity, folder).size();
        folderSummary = activity.getResources().getQuantityString(
            R.plurals.music_folder_songs, count,
            CustomMusic.describeFolder(activity, folder), count);
      }
    }
    return folderSummary;
  }

  public void showDisplayMenu(Runnable onClose) {
    showMenu(onClose,
        toggle(R.string.opt_fullscreen, KEY_FULLSCREEN, true, null),
        toggle(R.string.opt_gpu, GameView.KEY_GPU_RENDERING, true, nextGameNote()),
        toggle(R.string.opt_original_art, GameView.KEY_ORIGINAL_ART, false,
               nextGameNote()),
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
    java.util.List<Item> items = new java.util.ArrayList<Item>();
    items.add(new Item() {
      public String label() {
        return activity.getString(R.string.opt_player_name) + ": " +
            PlayerName.get(activity);
      }
      public void onTap() {
        showNameEditor();
      }
    });
    items.add(slider(R.string.opt_difficulty, KEY_DIFFICULTY,
               LevelManager.EASY, DIFFICULTY_MAX, LevelManager.NORMAL,
               R.string.opt_difficulty_value, R.string.opt_difficulty_help));
    items.add(toggle(R.string.opt_hurry, KEY_RUSH_ME, true, null));
    items.add(toggle(R.string.opt_compressor, KEY_COMPRESSOR, false, null));
    if (!host.inGame()) {
      // Not offered mid-game: the game being played would save its
      // level back over the reset.
      items.add(action(R.string.opt_reset_progress, () -> showConfirm(
          R.string.reset_confirm_title, R.string.reset_confirm_message,
          R.string.reset_confirm_button, () -> {
            FrozenBubble.resetPuzzleProgress(activity);
            toast(R.string.reset_done);
          })));
    }
    showMenu(onClose, items.toArray(new Item[0]));
  }

  /**
   * Ask before doing something that can't be undone: a title, a message,
   * and the action's button with Cancel, in place of the category popup.
   */
  private void showConfirm(int titleId, int messageId, int okId,
                           final Runnable onOk) {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    LinearLayout box = new LinearLayout(activity);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (12 * activity.getResources().getDisplayMetrics().density);
    box.setPadding(pad, pad, pad, 0);
    TextView title = new TextView(activity);
    styleText(title, 22);
    title.setText(titleId);
    box.addView(title);
    TextView message = new TextView(activity);
    styleText(message, 15);
    message.setTypeface(null, Typeface.NORMAL);
    message.setText(messageId);
    message.setPadding(pad, pad, pad, 0);
    box.addView(message);
    Button ok = MenuButtons.create(activity, okId, v -> {
      host.click();
      dialog.dismiss();
      onOk.run();
    });
    Button cancel = MenuButtons.create(activity, R.string.cancel, v -> {
      host.click();
      dialog.dismiss();
    });
    box.addView(MenuButtons.column(activity, ok, cancel));
    dialog.setView(scrolling(box));
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
        // Greyed out (and off) without GPU Rendering, which draws them
        // smoothly.
        toggle(R.string.fx_pop,     Effects.KEY_POP,     Effects.DEF_POP,     null,
               () -> Effects.gpuRendering(activity), R.string.fx_needs_gpu),
        toggle(R.string.fx_fade,    Effects.KEY_FADE,    Effects.DEF_FADE,    null,
               () -> Effects.gpuRendering(activity), R.string.fx_needs_gpu),
        toggle(R.string.fx_wobble,  Effects.KEY_WOBBLE,  Effects.DEF_WOBBLE,  null,
               () -> Effects.gpuRendering(activity), R.string.fx_needs_gpu),
        toggle(R.string.fx_shake,   Effects.KEY_SHAKE,   Effects.DEF_SHAKE,   null,
               () -> Effects.gpuRendering(activity), R.string.fx_needs_gpu),
        toggle(R.string.fx_trail,   Effects.KEY_TRAIL,   Effects.DEF_TRAIL,   null,
               () -> Effects.gpuRendering(activity), R.string.fx_needs_gpu),
        // No guide with Point To Shoot: you tap where the shot goes.
        toggle(R.string.fx_guide,   Effects.KEY_GUIDE,   Effects.DEF_GUIDE,   null,
               () -> Effects.aimingUsed(activity), R.string.fx_needs_aiming),
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
      for (int i = 0; i < items.length; i++) {
        buttons[i].setText(items[i].label());
        applyEnabled(buttons[i], items[i]);
      }
    };
    for (int i = 0; i < items.length; i++) {
      final Item item = items[i];
      buttons[i] = MenuButtons.create(activity, item.label(), v -> {
        host.click();
        item.onTap();
        refresh.run();
      });
      widen(buttons[i]);
      applyEnabled(buttons[i], item);
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
    return toggle(nameId, key, def, after, null, 0);
  }

  /**
   * A toggle that is greyed out while <code>available</code> is false,
   * with a small note under it saying why (<code>whyId</code>).
   */
  private Item toggle(final int nameId, final String key,
                      final boolean def, final Runnable after,
                      final java.util.function.BooleanSupplier available,
                      final int whyId) {
    return new Item() {
      public boolean enabled() {
        return (available == null) || available.getAsBoolean();
      }
      public CharSequence label() {
        String text = activity.getString(nameId) + ": " +
            activity.getString(sp.getBoolean(key, def) ? R.string.opt_on
                                                       : R.string.opt_off);
        return (enabled() || (whyId == 0)) ? text
            : withNote(text, activity.getString(whyId));
      }
      public void onTap() {
        applyChange(key, () -> {
          sp.edit().putBoolean(key, !sp.getBoolean(key, def)).apply();
          if (after != null)
            after.run();
          changed(key);
        });
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
      final int chosen = bar.getProgress() + min;
      if (chosen == clamp(sp.getInt(key, def), min, max)) {
        dialog.dismiss();     // unchanged: nothing to apply or restart
        return;
      }
      if (needsRestart(key) && !host.networkGame()) {
        // The restart question replaces this popup.
        dialog.setOnDismissListener(null);
        dialog.dismiss();
      }
      else {
        dialog.dismiss();
      }
      applyChange(key, () -> {
        sp.edit().putInt(key, chosen).apply();
        changed(key);
      });
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
