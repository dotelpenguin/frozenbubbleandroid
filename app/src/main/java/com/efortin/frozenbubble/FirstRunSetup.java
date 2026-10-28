/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.R;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * First start: a few bubble-style popups over the home screen.
 * <ol>
 * <li>Language (System Default first, and preselected).
 * <li>The player's name, suggested from the phone's name.
 * <li>Bluetooth permission, for 2-player games with a nearby phone
 *     (Android 12+, only on phones with Bluetooth).  "Not Now" is fine:
 *     the game asks again when Bluetooth play is chosen.
 * </ol>
 * Changing the language reloads the home screen in it; setup then carries
 * on where it was.
 */
public class FirstRunSetup {

  /** What the home screen provides. */
  public interface Host {
    void click();
    /** Show a popup over the dimmed menu. */
    void showPopup(AlertDialog dialog);
    /** Reload the home screen in the newly chosen language. */
    void reloadForLanguage();
    /** Ask Android for the Bluetooth permission. */
    void requestBluetoothPermission();
  }

  private static final String KEY_DONE = "first_run_done";
  private static final String KEY_STEP = "first_run_step";
  private static final int STEP_LANGUAGE   = 0;
  private static final int STEP_NAME       = 1;
  private static final int STEP_BLUETOOTH  = 2;

  private final Activity activity;
  private final Host     host;
  private final SharedPreferences sp;

  public FirstRunSetup(Activity activity, Host host) {
    this.activity = activity;
    this.host     = host;
    this.sp       = PreferenceManager.getDefaultSharedPreferences(activity);
  }

  public static boolean isNeeded(Context context) {
    return !PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(KEY_DONE, false);
  }

  /** Start (or carry on) setup. */
  public void start() {
    showStep(sp.getInt(KEY_STEP, STEP_LANGUAGE));
  }

  private void showStep(int step) {
    sp.edit().putInt(KEY_STEP, step).apply();
    switch (step) {
      case STEP_LANGUAGE:
        showLanguage();
        break;
      case STEP_NAME:
        showName();
        break;
      case STEP_BLUETOOTH:
        if (bluetoothPermissionNeeded()) {
          showBluetooth();
        }
        else {
          finish();
        }
        break;
      default:
        finish();
        break;
    }
  }

  private void finish() {
    sp.edit().putBoolean(KEY_DONE, true).remove(KEY_STEP).apply();
  }

  /*
   * Step 1: language.
   */
  private void showLanguage() {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    LinearLayout box = column();
    box.addView(title(R.string.setup_welcome, 24));
    box.addView(title(R.string.setup_choose_language, 17));
    String current = LocaleHelper.getLanguage(activity);
    Button[] buttons = new Button[LocaleHelper.TAGS.length];
    for (int i = 0; i < buttons.length; i++) {
      final String tag  = LocaleHelper.TAGS[i];
      String name = (LocaleHelper.NAMES[i] != null) ? LocaleHelper.NAMES[i]
          : activity.getString(R.string.lang_system);
      if (tag.equals(current)) {
        name = "● " + name;
      }
      buttons[i] = MenuButtons.create(activity, name, v -> {
        host.click();
        dialog.dismiss();
        boolean changed = !tag.equals(LocaleHelper.getLanguage(activity));
        LocaleHelper.setLanguage(activity, tag);
        sp.edit().putInt(KEY_STEP, STEP_NAME).apply();
        if (changed) {
          // Setup carries on (at the name) once the menu shows in the
          // new language.
          host.reloadForLanguage();
        }
        else {
          showStep(STEP_NAME);
        }
      });
      OptionsMenus.widen(buttons[i]);
    }
    box.addView(MenuButtons.column(activity, buttons));
    show(dialog, box);
  }

  /*
   * Step 2: the player's name.
   */
  private void showName() {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    LinearLayout box = column();
    box.addView(title(R.string.setup_name_question, 22));
    TextView note = title(R.string.setup_name_note, 14);
    note.setTypeface(null, Typeface.NORMAL);
    box.addView(note);
    final EditText edit = PlayerName.makeEditor(activity,
                                                PlayerName.get(activity));
    edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
    box.addView(edit);
    final Runnable next = () -> {
      host.click();
      PlayerName.set(activity, edit.getText().toString());
      dialog.dismiss();
      showStep(STEP_BLUETOOTH);
    };
    edit.setOnEditorActionListener((v, action, event) -> {
      next.run();
      return true;
    });
    Button ok = MenuButtons.create(activity, R.string.setup_next, v -> next.run());
    box.addView(MenuButtons.column(activity, ok));
    show(dialog, box);
  }

  /*
   * Step 3: Bluetooth permission.
   */
  private boolean bluetoothPermissionNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      return false; // Granted at install on older Android versions.
    }
    try {
      if (BluetoothAdapter.getDefaultAdapter() == null) {
        return false; // No Bluetooth on this phone.
      }
    } catch (RuntimeException e) {
      return false;
    }
    return activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
        != PackageManager.PERMISSION_GRANTED;
  }

  private void showBluetooth() {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    LinearLayout box = column();
    box.addView(title(R.string.setup_bt_title, 22));
    TextView msg = title(R.string.setup_bt_message, 15);
    msg.setTypeface(null, Typeface.NORMAL);
    box.addView(msg);
    Button allow = MenuButtons.create(activity, R.string.setup_allow, v -> {
      host.click();
      dialog.dismiss();
      finish();
      host.requestBluetoothPermission();
    });
    Button later = MenuButtons.create(activity, R.string.setup_not_now, v -> {
      host.click();
      dialog.dismiss();
      finish();
    });
    box.addView(MenuButtons.column(activity, allow, later));
    show(dialog, box);
  }

  /*
   * Popup building.
   */
  private LinearLayout column() {
    LinearLayout box = new LinearLayout(activity);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (12 * activity.getResources().getDisplayMetrics().density);
    box.setPadding(pad, pad, pad, pad);
    return box;
  }

  private TextView title(int textId, int sizeDp) {
    TextView t = new TextView(activity);
    OptionsMenus.styleText(t, sizeDp);
    t.setText(textId);
    int pad = (int) (4 * activity.getResources().getDisplayMetrics().density);
    t.setPadding(pad, pad, pad, pad);
    return t;
  }

  private void show(AlertDialog dialog, LinearLayout box) {
    ScrollView scroll = new ScrollView(activity);
    scroll.addView(box);
    dialog.setView(scroll);
    // Setup must be answered: Back or a tap outside don't skip a step.
    dialog.setCancelable(false);
    host.showPopup(dialog);
  }
}
