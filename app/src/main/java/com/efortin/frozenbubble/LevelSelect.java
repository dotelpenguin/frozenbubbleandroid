/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.GameView;
import org.jfedor.frozenbubble.LevelManager;
import org.jfedor.frozenbubble.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Puzzle level select: a paged grid of level bubbles, 20 to a page.
 * <ul>
 * <li>Completed levels show their bubble; tap one to see its best score.
 * <li>The highest level reached is marked in yellow.
 * <li>Levels not reached yet are frozen and can't be chosen.
 * </ul>
 * Play starts the chosen level (replacing a saved Puzzle game, like New
 * Game does).
 */
public class LevelSelect {

  /** What the home screen provides. */
  public interface Host {
    void click();
    /** Show a popup over the dimmed menu. */
    void showPopup(AlertDialog dialog);
    /** Play the chosen level (0-based). */
    void play(int level);
    /** Back: return to the previous menu. */
    void back();
  }

  private static final int COLS     = 5;
  private static final int ROWS     = 4;
  private static final int PER_PAGE = COLS * ROWS;

  private static final int[] BUBBLE_HD = {
    R.drawable.bubble_1_hd, R.drawable.bubble_2_hd, R.drawable.bubble_3_hd,
    R.drawable.bubble_4_hd, R.drawable.bubble_5_hd, R.drawable.bubble_6_hd,
    R.drawable.bubble_7_hd, R.drawable.bubble_8_hd,
  };
  private static final int[] BUBBLE = {
    R.drawable.bubble_1, R.drawable.bubble_2, R.drawable.bubble_3,
    R.drawable.bubble_4, R.drawable.bubble_5, R.drawable.bubble_6,
    R.drawable.bubble_7, R.drawable.bubble_8,
  };
  private static final int[] FROZEN_HD = {
    R.drawable.frozen_1_hd, R.drawable.frozen_2_hd, R.drawable.frozen_3_hd,
    R.drawable.frozen_4_hd, R.drawable.frozen_5_hd, R.drawable.frozen_6_hd,
    R.drawable.frozen_7_hd, R.drawable.frozen_8_hd,
  };
  private static final int[] FROZEN = {
    R.drawable.frozen_1, R.drawable.frozen_2, R.drawable.frozen_3,
    R.drawable.frozen_4, R.drawable.frozen_5, R.drawable.frozen_6,
    R.drawable.frozen_7, R.drawable.frozen_8,
  };

  private final Activity activity;
  private final Host     host;
  private final int      numLevels;
  private final int      highest;   // highest level reached (0-based)
  /** Best score of each completed level (0-based level → score). */
  private final Map<Integer, HighscoreDO> best;

  private int page;
  private int selected;

  private final TextView[] cells = new TextView[PER_PAGE];
  private TextView pageLabel;
  private TextView info;
  private Button   prev, next, play;

  public LevelSelect(Activity activity, Host host) {
    this.activity  = activity;
    this.host      = host;
    this.numLevels = countLevels(activity);
    this.best      = loadBestScores(activity);
    int bestCompleted = -1;
    for (int level : best.keySet()) {
      bestCompleted = Math.max(bestCompleted, level);
    }
    this.highest  = Math.min(numLevels - 1,
        FrozenBubble.getHighestPuzzleLevel(activity, bestCompleted));
    this.selected = Math.max(0, Math.min(highest,
                                         FrozenBubble.getPuzzleLevel(activity)));
    this.page     = selected / PER_PAGE;
  }

  /** The number of Puzzle levels (100 in the built-in set). */
  private static int countLevels(Activity activity) {
    try (InputStream is = activity.getAssets().open("levels.txt")) {
      byte[] data = new byte[is.available()];
      int read = is.read(data);
      if (read > 0) {
        return Math.max(1, new LevelManager(data, 0).getNumLevels());
      }
    } catch (IOException | RuntimeException e) {
      // Fall through.
    }
    return 100;
  }

  /** The best (fewest shots, then fastest) score of each level. */
  private static Map<Integer, HighscoreDO> loadBestScores(Activity activity) {
    Map<Integer, HighscoreDO> map = new HashMap<Integer, HighscoreDO>();
    HighscoreDB db = null;
    try {
      db = new HighscoreDB(activity, HighscoreManager.PUZZLE_DATABASE_NAME);
      List<HighscoreDO> all = db.selectAll();
      for (HighscoreDO hi : all) {
        HighscoreDO old = map.get(hi.getLevel());
        if ((old == null) || (hi.getShots() < old.getShots()) ||
            ((hi.getShots() == old.getShots()) &&
             (hi.getTime() < old.getTime()))) {
          map.put(hi.getLevel(), hi);
        }
      }
    } catch (RuntimeException e) {
      // No scores yet, or the database can't be read: show none.
    } finally {
      if (db != null) {
        db.close();
      }
    }
    return map;
  }

  public void show() {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();
    final float dp = activity.getResources().getDisplayMetrics().density;

    LinearLayout box = new LinearLayout(activity);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (8 * dp);
    box.setPadding(pad, pad, pad, pad);

    TextView title = new TextView(activity);
    OptionsMenus.styleText(title, 22);
    title.setText(R.string.level_select_title);
    box.addView(title);

    // The grid of level bubbles.
    int cell = (int) (54 * dp);
    int gap  = (int) (5 * dp);
    for (int r = 0; r < ROWS; r++) {
      LinearLayout row = new LinearLayout(activity);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.CENTER_HORIZONTAL);
      for (int c = 0; c < COLS; c++) {
        final int index = (r * COLS) + c;
        TextView t = new TextView(activity);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        t.setTypeface(null, Typeface.BOLD);
        t.setShadowLayer(4, 1, 1, Color.BLACK);
        t.setOnClickListener(v -> choose((page * PER_PAGE) + index));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cell, cell);
        lp.setMargins(gap, gap, gap, gap);
        row.addView(t, lp);
        cells[index] = t;
      }
      box.addView(row);
    }

    // Page arrows.
    LinearLayout nav = new LinearLayout(activity);
    nav.setOrientation(LinearLayout.HORIZONTAL);
    nav.setGravity(Gravity.CENTER);
    prev = MenuButtons.create(activity, "◀", v -> {
      host.click();
      setPage(page - 1);
    });
    next = MenuButtons.create(activity, "▶", v -> {
      host.click();
      setPage(page + 1);
    });
    for (Button b : new Button[] { prev, next }) {
      b.setWidth((int) (b.getTextSize() * 3.5f));
    }
    pageLabel = new TextView(activity);
    OptionsMenus.styleText(pageLabel, 16);
    pageLabel.setMinWidth((int) (110 * dp));
    nav.addView(prev);
    nav.addView(pageLabel);
    nav.addView(next);
    box.addView(nav);

    info = new TextView(activity);
    OptionsMenus.styleText(info, 15);
    info.setTypeface(null, Typeface.NORMAL);
    box.addView(info);

    play = MenuButtons.create(activity, "", v -> {
      host.click();
      dialog.dismiss();
      host.play(selected);
    });
    OptionsMenus.widen(play);
    Button back = MenuButtons.create(activity, R.string.back, v -> {
      host.click();
      dialog.dismiss();
      host.back();
    });
    OptionsMenus.widen(back);
    box.addView(MenuButtons.column(activity, play, back));

    ScrollView scroll = new ScrollView(activity);
    scroll.addView(box);
    dialog.setView(scroll);
    dialog.setOnCancelListener(d -> host.back());
    render();
    host.showPopup(dialog);
  }

  private void choose(int level) {
    if ((level < numLevels) && (level <= highest)) {
      host.click();
      selected = level;
      render();
    }
  }

  private void setPage(int newPage) {
    int last = (numLevels - 1) / PER_PAGE;
    page = Math.max(0, Math.min(last, newPage));
    render();
  }

  private void render() {
    boolean hd = !android.preference.PreferenceManager
        .getDefaultSharedPreferences(activity)
        .getBoolean(GameView.KEY_ORIGINAL_ART, false);
    for (int i = 0; i < PER_PAGE; i++) {
      int level = (page * PER_PAGE) + i;
      TextView t = cells[i];
      if (level >= numLevels) {
        t.setVisibility(android.view.View.INVISIBLE);
        continue;
      }
      t.setVisibility(android.view.View.VISIBLE);
      t.setText(Integer.toString(level + 1));
      int color = level % BUBBLE.length;
      boolean reached = level <= highest;
      t.setBackgroundResource(reached ? (hd ? BUBBLE_HD[color] : BUBBLE[color])
                                      : (hd ? FROZEN_HD[color] : FROZEN[color]));
      t.setAlpha(reached ? 1f : 0.45f);
      t.setEnabled(reached);
      // Yellow: the highest level reached (not completed yet).
      t.setTextColor((reached && !best.containsKey(level)) ? 0xFFFFE14D
                                                           : Color.WHITE);
      boolean isSelected = (level == selected);
      t.setScaleX(isSelected ? 1.22f : 1f);
      t.setScaleY(isSelected ? 1.22f : 1f);
    }
    int pages = ((numLevels - 1) / PER_PAGE) + 1;
    pageLabel.setText(activity.getString(R.string.level_select_page,
                                         page + 1, pages));
    prev.setEnabled(page > 0);
    next.setEnabled(page < pages - 1);
    prev.setAlpha(prev.isEnabled() ? 1f : 0.4f);
    next.setAlpha(next.isEnabled() ? 1f : 0.4f);

    HighscoreDO score = best.get(selected);
    if (score != null) {
      long secs = score.getTime() / 1000;
      info.setText(activity.getString(R.string.level_select_best,
          selected + 1, score.getShots(),
          String.format(java.util.Locale.ROOT, "%d:%02d", secs / 60, secs % 60)));
    }
    else {
      info.setText(activity.getString(R.string.level_select_not_done,
                                      selected + 1));
    }
    play.setText(activity.getString(R.string.level_select_play, selected + 1));
  }
}
