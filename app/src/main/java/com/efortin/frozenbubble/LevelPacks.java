/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfedor.frozenbubble.LevelManager;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Puzzle level packs.
 * <ul>
 * <li>"original": the 100 classic levels (assets/levels.txt).
 * <li>Every assets/packs/NN_id.txt: a pack in the same text format, with
 *     optional "#" comment lines at the top (NN sets the order).  Its
 *     name comes from the string resource "pack_" + id, or from a
 *     "# name: ..." line.
 * </ul>
 * Each pack keeps its own progress (current and highest level) and its
 * own best scores, so packs never mix.  The Original pack uses the keys
 * and database it always used, so existing progress carries over.
 */
public final class LevelPacks {
  private LevelPacks() {}

  public static final String ORIGINAL = "original";
  /** Settings key: the pack Puzzle plays. */
  public static final String KEY_PACK = "level_pack";
  private static final String DIR     = "packs";

  /** The packs, Original first. */
  public static List<String> ids(Context context) {
    List<String> ids = new ArrayList<String>();
    ids.add(ORIGINAL);
    try {
      String[] files = context.getAssets().list(DIR);
      if (files != null) {
        Arrays.sort(files);
        for (String f : files) {
          if (f.endsWith(".txt")) {
            ids.add(idOf(f));
          }
        }
      }
    } catch (IOException e) {
      // Only the Original pack.
    }
    return ids;
  }

  /** "02_pictures.txt" -> "pictures" */
  private static String idOf(String file) {
    String id = file.substring(0, file.length() - 4);
    int us = id.indexOf('_');
    return ((us > 0) && id.substring(0, us).matches("[0-9]+"))
           ? id.substring(us + 1) : id;
  }

  private static String fileOf(Context context, String id) throws IOException {
    if (ORIGINAL.equals(id)) {
      return "levels.txt";
    }
    String[] files = context.getAssets().list(DIR);
    if (files != null) {
      for (String f : files) {
        if (f.endsWith(".txt") && idOf(f).equals(id)) {
          return DIR + "/" + f;
        }
      }
    }
    throw new IOException("No level pack " + id);
  }

  /** The pack Puzzle plays (Original if it is missing). */
  public static String current(Context context) {
    String id = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(KEY_PACK, ORIGINAL);
    return ids(context).contains(id) ? id : ORIGINAL;
  }

  public static void setCurrent(Context context, String id) {
    PreferenceManager.getDefaultSharedPreferences(context).edit()
        .putString(KEY_PACK, id).commit();
  }

  public static boolean isOriginal(String id) {
    return ORIGINAL.equals(id);
  }

  /** The pack's name for menus: "Original", "Pictures"... */
  public static String name(Context context, String id) {
    int res = context.getResources().getIdentifier(
        "pack_" + id, "string", context.getPackageName());
    if (res != 0) {
      return context.getString(res);
    }
    try {
      for (String line : new String(read(context, fileOf(context, id)))
                             .split("\n")) {
        line = line.trim();
        if (line.startsWith("#") && line.substring(1).trim().startsWith("name:")) {
          return line.substring(line.indexOf(':') + 1).trim();
        }
      }
    } catch (IOException e) {
      // Fall through.
    }
    return id;
  }

  /** The pack's levels, without its comment lines, for LevelManager. */
  public static byte[] load(Context context, String id) throws IOException {
    StringBuilder out = new StringBuilder();
    for (String line : new String(read(context, fileOf(context, id)))
                           .replace("\r", "").split("\n", -1)) {
      if (!line.trim().startsWith("#")) {
        out.append(line).append('\n');
      }
    }
    return out.toString().trim().concat("\n").getBytes();
  }

  /** A pack's levels (null if it can't be read). */
  public static LevelManager levels(Context context, String id) {
    try {
      return new LevelManager(load(context, id), 0);
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }

  /** The number of levels in a pack. */
  public static int countLevels(Context context, String id) {
    try {
      return Math.max(1, new LevelManager(load(context, id), 0).getNumLevels());
    } catch (IOException | RuntimeException e) {
      return 1;
    }
  }

  /** The best scores database of a pack. */
  public static String dbName(String id) {
    return isOriginal(id) ? HighscoreManager.PUZZLE_DATABASE_NAME
                          : "puzzle_" + id;
  }

  /** A progress setting's key for a pack ("level", "level_pictures"...). */
  public static String key(String base, String id) {
    return isOriginal(id) ? base : base + "_" + id;
  }

  private static byte[] read(Context context, String path) throws IOException {
    try (InputStream is = context.getAssets().open(path)) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = is.read(buf)) > 0) {
        bytes.write(buf, 0, n);
      }
      return bytes.toByteArray();
    }
  }
}
