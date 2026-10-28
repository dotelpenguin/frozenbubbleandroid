/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.R;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;

/**
 * The player's name, recorded with their high scores.  Asked for on
 * first start, and changeable in Options &rarr; Gameplay.
 */
public final class PlayerName {
  private PlayerName() {}

  /* Stored where the original game kept its high score name. */
  private static final String KEY = "highscorename";
  public  static final int    MAX_LENGTH = 16;

  /** The player's name, or a suggested one if none has been set. */
  public static String get(Context context) {
    String name = context.getSharedPreferences(FrozenBubble.PREFS_NAME,
        Context.MODE_PRIVATE).getString(KEY, null);
    if ((name == null) || name.trim().isEmpty() || "anon".equals(name)) {
      return suggest(context);
    }
    return name;
  }

  public static void set(Context context, String name) {
    String clean = (name == null) ? "" : name.trim();
    if (clean.length() > MAX_LENGTH) {
      clean = clean.substring(0, MAX_LENGTH).trim();
    }
    context.getSharedPreferences(FrozenBubble.PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY, clean.isEmpty() ? suggest(context) : clean)
        .apply();
  }

  /**
   * A name suggested from the phone: many phones are named after their
   * owner, like "Drew's Pixel", so the part before "'s" is used.
   * Otherwise "Player".  (Reading the owner's real name would need access
   * to their contacts or accounts, which this game doesn't ask for.)
   */
  public static String suggest(Context context) {
    String device = null;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      try {
        device = Settings.Global.getString(context.getContentResolver(),
                                           Settings.Global.DEVICE_NAME);
      } catch (RuntimeException e) {
        device = null;
      }
    }
    if (device != null) {
      String d = device.replace('’', '\'');
      int i = d.indexOf("'s ");
      if (i < 0 && d.endsWith("'s")) {
        i = d.length() - 2;
      }
      if (i > 0) {
        String owner = d.substring(0, i).trim();
        if (!owner.isEmpty() && (owner.length() <= MAX_LENGTH)) {
          return owner;
        }
      }
    }
    return context.getString(R.string.name_default);
  }

  /** A name field in the menus' white, bold style. */
  public static EditText makeEditor(Context context, String name) {
    EditText edit = new EditText(context);
    edit.setText(name);
    edit.setSelection(edit.getText().length());
    edit.setSingleLine(true);
    edit.setInputType(InputType.TYPE_CLASS_TEXT |
                      InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    edit.setFilters(new InputFilter[] {
        new InputFilter.LengthFilter(MAX_LENGTH) });
    edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
    edit.setTextColor(Color.WHITE);
    edit.setHintTextColor(0xB0FFFFFF);
    edit.setHint(R.string.name_hint);
    edit.setGravity(Gravity.CENTER);
    edit.setShadowLayer(6, 2, 2, Color.BLACK);
    edit.setMinWidth((int) (240 * context.getResources()
        .getDisplayMetrics().density));
    return edit;
  }
}
