/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The app's settings.
 * <p>This replaces <code>android.preference.PreferenceManager
 * .getDefaultSharedPreferences()</code> (deprecated since Android 10,
 * along with the rest of the old <code>android.preference</code>
 * package).  It opens exactly the same file, named the same way
 * ("&lt;package&gt;_preferences", private), so every setting, the player's
 * name and the saved-game details carry over unchanged.
 */
public final class Prefs {
  private Prefs() {}

  /** The settings file's name, as Android's default settings name it. */
  public static String name(Context context) {
    return context.getPackageName() + "_preferences";
  }

  /** The app's settings. */
  public static SharedPreferences get(Context context) {
    return context.getSharedPreferences(name(context), Context.MODE_PRIVATE);
  }
}
