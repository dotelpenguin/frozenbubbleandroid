/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.preference.PreferenceManager;

/**
 * The language chosen in Options &rarr; Language, or the phone's own
 * language ("System Default").
 * <p>Each screen applies it in <code>attachBaseContext()</code>, so its
 * text comes from that language's strings (falling back to English for
 * anything not translated).
 */
public final class LocaleHelper {
  private LocaleHelper() {}

  public static final String KEY_LANGUAGE = "app_language";

  /*
   * The languages the game is translated into: a language tag ("" for
   * the phone's language) and the language's own name, so each one can
   * be found whatever language is showing.
   */
  public static final String[] TAGS  = {
    "", "en", "de", "es", "fr", "is", "pt-BR", "zh-CN",
  };
  public static final String[] NAMES = {
    null, "English", "Deutsch", "Español", "Français", "Íslenska",
    "Português (Brasil)", "中文（简体）",
  };

  /** The chosen language tag, or "" for the phone's language. */
  public static String getLanguage(Context context) {
    String tag = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(KEY_LANGUAGE, "");
    return (tag == null) ? "" : tag;
  }

  public static void setLanguage(Context context, String tag) {
    PreferenceManager.getDefaultSharedPreferences(context).edit()
        .putString(KEY_LANGUAGE, (tag == null) ? "" : tag).commit();
  }

  /**
   * A context showing the chosen language, for a screen's
   * <code>attachBaseContext()</code>.
   */
  public static Context wrap(Context base) {
    String tag = getLanguage(base);
    if (tag.isEmpty()) {
      // The phone's language (undoing an earlier choice in this run).
      Locale.setDefault(base.getResources().getConfiguration()
                            .getLocales().get(0));
      return base;
    }
    Locale locale = Locale.forLanguageTag(tag);
    Locale.setDefault(locale);
    Configuration config =
        new Configuration(base.getResources().getConfiguration());
    config.setLocale(locale);
    config.setLocales(new LocaleList(locale));
    return base.createConfigurationContext(config);
  }
}
