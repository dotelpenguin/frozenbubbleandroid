/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.jfedor.frozenbubble.LevelManager;
import org.jfedor.frozenbubble.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * The High Scores popup, with a tab for each mode.  (Puzzle scores are
 * shown in Choose Level.)
 * <ul>
 * <li>Arcade: the top 3 games for each number of colours (most shots,
 *     then longest: Arcade is about lasting).
 * <li>Vs CPU: wins for each number of colours, and the quickest win.
 * </ul>
 * Also score helpers used by the game (the "Best" line).
 */
public class HighScores {

  private static final int TAB_ARCADE = 0;
  private static final int TAB_CPU    = 1;
  private static final int ARCADE_TOP = 3;

  /** Fewest shots, then fastest (the quickest Vs CPU win). */
  public static final Comparator<HighscoreDO> QUICKEST_ORDER = (a, b) ->
      (a.getShots() != b.getShots()) ? Integer.compare(a.getShots(), b.getShots())
                                     : Long.compare(a.getTime(), b.getTime());
  /** Arcade order: most shots, then longest. */
  public static final Comparator<HighscoreDO> ARCADE_ORDER = (a, b) ->
      (a.getShots() != b.getShots()) ? Integer.compare(b.getShots(), a.getShots())
                                     : Long.compare(b.getTime(), a.getTime());

  private final Activity     activity;
  private final Runnable     click;
  /** Shows the popup (see-through, over the dimmed screen). */
  private final java.util.function.Consumer<AlertDialog> showPopup;
  private final float        dp;

  private int tab = TAB_ARCADE;
  private Button[]     tabs;
  private LinearLayout content;

  public HighScores(Activity activity, Runnable click,
                    java.util.function.Consumer<AlertDialog> showPopup) {
    this.activity  = activity;
    this.click     = click;
    this.showPopup = showPopup;
    this.dp       = activity.getResources().getDisplayMetrics().density;
  }

  /*
   * Score helpers.
   */

  /** Every score in a database (an empty list if it can't be read). */
  public static List<HighscoreDO> loadAll(Context context, String dbName) {
    HighscoreDB db = null;
    try {
      db = new HighscoreDB(context, dbName);
      return db.selectAll();
    } catch (RuntimeException e) {
      return new ArrayList<HighscoreDO>();
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  /** The scores grouped by level (or number of colours), each sorted. */
  public static Map<Integer, List<HighscoreDO>> byLevel(
      List<HighscoreDO> all, Comparator<HighscoreDO> order) {
    Map<Integer, List<HighscoreDO>> map = new TreeMap<Integer, List<HighscoreDO>>();
    for (HighscoreDO hi : all) {
      List<HighscoreDO> list = map.get(hi.getLevel());
      if (list == null) {
        list = new ArrayList<HighscoreDO>();
        map.put(hi.getLevel(), list);
      }
      list.add(hi);
    }
    for (List<HighscoreDO> list : map.values()) {
      Collections.sort(list, order);
    }
    return map;
  }

  /** "1:32" */
  public static String time(long millis) {
    long secs = Math.max(0, millis) / 1000;
    return String.format(Locale.ROOT, "%d:%02d", secs / 60, secs % 60);
  }

  /*
   * The popup.
   */

  public void show(final Runnable onClose) {
    final AlertDialog dialog = new AlertDialog.Builder(activity).create();

    LinearLayout box = new LinearLayout(activity);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (8 * dp);
    box.setPadding(pad, pad, pad, pad);

    TextView title = new TextView(activity);
    OptionsMenus.styleText(title, 22);
    title.setText(R.string.high_scores);
    box.addView(title);

    // The mode tabs: the chosen one bright, the others faded.
    LinearLayout tabRow = new LinearLayout(activity);
    tabRow.setOrientation(LinearLayout.HORIZONTAL);
    tabRow.setGravity(Gravity.CENTER);
    int[] names = { R.string.home_arcade, R.string.hs_tab_cpu };
    tabs = new Button[names.length];
    for (int i = 0; i < names.length; i++) {
      final int which = i;
      tabs[i] = MenuButtons.create(activity, names[i], v -> {
        click.run();
        tab = which;
        render();
      });
      tabs[i].setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
      tabs[i].setSingleLine(true);
      tabs[i].setWidth((int) (tabs[i].getTextSize() * 8.5f));
      tabRow.addView(tabs[i]);
    }
    box.addView(tabRow);

    // The scores, scrolling when long.
    content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER_HORIZONTAL);
    final int maxHeight = (int) (activity.getResources().getDisplayMetrics()
                                 .heightPixels * 0.5f);
    ScrollView scroll = new ScrollView(activity) {
      @Override
      protected void onMeasure(int widthSpec, int heightSpec) {
        // As tall as the scores, up to half the screen.
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(
            maxHeight, MeasureSpec.AT_MOST));
      }
    };
    scroll.addView(content);
    box.addView(scroll, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));

    Button back = MenuButtons.create(activity, R.string.back, v -> {
      click.run();
      dialog.dismiss();
    });
    OptionsMenus.widen(back);
    box.addView(MenuButtons.column(activity, back));

    dialog.setView(box);
    dialog.setOnDismissListener(d -> {
      if (onClose != null) {
        onClose.run();
      }
    });
    render();
    showPopup.accept(dialog);
  }

  private void render() {
    for (int i = 0; i < tabs.length; i++) {
      tabs[i].setAlpha((i == tab) ? 1f : 0.5f);
    }
    content.removeAllViews();
    TableLayout table = new TableLayout(activity);
    table.setStretchAllColumns(false);
    table.setColumnShrinkable(3, true);
    boolean any;
    switch (tab) {
      case TAB_CPU:
        any = fillCpu(table);
        break;
      default:
        any = fillArcade(table);
        break;
    }
    if (any) {
      content.addView(table);
    }
    else {
      TextView none = new TextView(activity);
      OptionsMenus.styleText(none, 16);
      none.setTypeface(null, Typeface.NORMAL);
      none.setText(R.string.hs_none);
      int pad = (int) (24 * dp);
      none.setPadding(pad, pad, pad, pad);
      content.addView(none);
    }
  }

  private boolean fillArcade(TableLayout table) {
    Map<Integer, List<HighscoreDO>> byColours = byLevel(
        loadAll(activity, HighscoreManager.ARCADE_DATABASE_NAME), ARCADE_ORDER);
    if (byColours.isEmpty()) {
      return false;
    }
    header(table, 0, R.string.hs_col_shots, R.string.hs_col_time,
           R.string.hs_col_player);
    for (Map.Entry<Integer, List<HighscoreDO>> e : byColours.entrySet()) {
      group(table, colours(e.getKey()));
      List<HighscoreDO> list = e.getValue();
      for (int i = 0; (i < list.size()) && (i < ARCADE_TOP); i++) {
        HighscoreDO hi = list.get(i);
        row(table, "#" + (i + 1), Integer.toString(hi.getShots()),
            time(hi.getTime()), hi.getName());
      }
    }
    return true;
  }

  private boolean fillCpu(TableLayout table) {
    Map<Integer, List<HighscoreDO>> byColours = byLevel(
        loadAll(activity, HighscoreManager.MULTIPLAYER_DATABASE_NAME),
        QUICKEST_ORDER);
    if (byColours.isEmpty()) {
      return false;
    }
    header(table, R.string.opt_difficulty, R.string.hs_col_wins,
           R.string.hs_col_shots, R.string.hs_col_time);
    for (Map.Entry<Integer, List<HighscoreDO>> e : byColours.entrySet()) {
      HighscoreDO quickest = e.getValue().get(0);
      row(table, colours(e.getKey()),
          Integer.toString(e.getValue().size()),
          Integer.toString(quickest.getShots()), time(quickest.getTime()));
    }
    return true;
  }

  private String colours(int difficulty) {
    return OptionsMenus.difficultyName(activity, difficulty);
  }

  /*
   * Table building.
   */

  private void header(TableLayout table, int... ids) {
    String[] texts = new String[ids.length];
    for (int i = 0; i < ids.length; i++) {
      texts[i] = (ids[i] == 0) ? "" : activity.getString(ids[i]);
    }
    TableRow row = rowOf(texts, 13);
    for (int i = 0; i < row.getChildCount(); i++) {
      ((TextView) row.getChildAt(i)).setAlpha(0.75f);
    }
    table.addView(row);
  }

  private void group(TableLayout table, String text) {
    TextView t = cell(text, 16, true);
    t.setGravity(Gravity.CENTER_HORIZONTAL);
    t.setPadding(0, (int) (8 * dp), 0, (int) (2 * dp));
    TableRow row = new TableRow(activity);
    TableRow.LayoutParams lp = new TableRow.LayoutParams();
    lp.span = 4;
    row.addView(t, lp);
    table.addView(row);
  }

  private void row(TableLayout table, String... texts) {
    table.addView(rowOf(texts, 15));
  }

  private TableRow rowOf(String[] texts, int sizeDp) {
    TableRow row = new TableRow(activity);
    for (int i = 0; i < texts.length; i++) {
      TextView t = cell(texts[i], sizeDp, i == 0);
      // Numbers line up on the right, the name on the left.
      t.setGravity((i == texts.length - 1) && !isNumber(texts[i])
                   ? Gravity.START : Gravity.END);
      row.addView(t);
    }
    return row;
  }

  private static boolean isNumber(String s) {
    return !TextUtils.isEmpty(s) && s.matches("[#0-9:]+");
  }

  private TextView cell(String text, int sizeDp, boolean bold) {
    TextView t = new TextView(activity);
    OptionsMenus.styleText(t, sizeDp);
    t.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
    t.setText(text);
    t.setSingleLine(true);
    t.setEllipsize(TextUtils.TruncateAt.END);
    t.setMaxWidth((int) (130 * dp));
    int h = (int) (9 * dp);
    int v = (int) (2 * dp);
    t.setPadding(h, v, h, v);
    t.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
    return t;
  }
}
