/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The player's game statistics (High Scores > Stats): games won and
 * lost per mode, shots, bubbles cleared, the biggest drop, the best
 * combo and the time played.
 * <p>Counted by the game as it plays (from the game thread), kept in
 * memory and written to their own settings file now and then (see
 * {@link #save()}): at the end of a level or game and when the game
 * pauses.  Only the local, human player's play is counted.
 */
public final class Stats {
  private Stats() {}

  private static final String FILE = "stats";

  /* Counters. */
  public static final String PUZZLE_WON   = "puzzle_won";
  public static final String PUZZLE_LOST  = "puzzle_lost";
  public static final String ARCADE_GAMES = "arcade_games";
  public static final String CPU_WON      = "cpu_won";
  public static final String CPU_LOST     = "cpu_lost";
  public static final String LOCAL_WON    = "local_won";
  public static final String LOCAL_LOST   = "local_lost";
  public static final String NET_WON      = "net_won";
  public static final String NET_LOST     = "net_lost";
  public static final String SHOTS        = "shots";
  public static final String CLEARS       = "clears";   // shots that cleared
  public static final String POPPED       = "popped";
  public static final String DROPPED      = "dropped";
  public static final String TIME_MS      = "time_ms";
  /* Bests (the highest value seen). */
  public static final String BIGGEST_DROP = "biggest_drop";
  public static final String BEST_COMBO   = "best_combo"; // combo step, 1-5

  private static final String[] KEYS = {
    PUZZLE_WON, PUZZLE_LOST, ARCADE_GAMES, CPU_WON, CPU_LOST, LOCAL_WON,
    LOCAL_LOST, NET_WON, NET_LOST, SHOTS, CLEARS, POPPED, DROPPED, TIME_MS,
    BIGGEST_DROP, BEST_COMBO,
  };

  private static Context           sContext = null;
  private static Map<String, Long> sValues  = null;
  private static boolean           sDirty   = false;

  /** Call once from each screen before use (keeps the app context). */
  public static synchronized void init(Context context) {
    if (sContext == null) {
      sContext = context.getApplicationContext();
    }
    load();
  }

  private static SharedPreferences file() {
    return sContext.getSharedPreferences(FILE, Context.MODE_PRIVATE);
  }

  private static void load() {
    if ((sValues != null) || (sContext == null)) {
      return;
    }
    sValues = new HashMap<String, Long>();
    SharedPreferences sp = file();
    for (String k : KEYS) {
      sValues.put(k, sp.getLong(k, 0));
    }
  }

  /** Add to a counter. */
  public static synchronized void add(String key, long n) {
    load();
    if ((sValues == null) || (n == 0)) {
      return;
    }
    Long v = sValues.get(key);
    sValues.put(key, ((v == null) ? 0 : v) + n);
    sDirty = true;
  }

  /** Keep the larger of a best and a new value. */
  public static synchronized void best(String key, long n) {
    load();
    if (sValues == null) {
      return;
    }
    Long v = sValues.get(key);
    if ((v == null) || (n > v)) {
      sValues.put(key, n);
      sDirty = true;
    }
  }

  public static synchronized long get(String key) {
    load();
    Long v = (sValues == null) ? null : sValues.get(key);
    return (v == null) ? 0 : v;
  }

  /** A clearing shot by the player: bubbles popped and dropped. */
  public static void clear(int popped, int dropped) {
    add(CLEARS, 1);
    add(POPPED, popped);
    add(DROPPED, dropped);
    best(BIGGEST_DROP, dropped);
  }

  /** Write the counters to storage, if they changed. */
  public static synchronized void save() {
    if (!sDirty || (sValues == null) || (sContext == null)) {
      return;
    }
    SharedPreferences.Editor e = file().edit();
    for (Map.Entry<String, Long> entry : sValues.entrySet()) {
      e.putLong(entry.getKey(), entry.getValue());
    }
    e.apply();
    sDirty = false;
  }

  /** Start again from zero (High Scores > Stats > Reset Stats). */
  public static synchronized void reset() {
    if (sContext == null) {
      return;
    }
    file().edit().clear().apply();
    sValues = null;
    sDirty  = false;
    load();
  }

  /** All games played, in every mode. */
  public static long gamesPlayed() {
    return get(PUZZLE_WON) + get(PUZZLE_LOST) + get(ARCADE_GAMES) +
           get(CPU_WON) + get(CPU_LOST) + get(LOCAL_WON) + get(LOCAL_LOST) +
           get(NET_WON) + get(NET_LOST);
  }
}
