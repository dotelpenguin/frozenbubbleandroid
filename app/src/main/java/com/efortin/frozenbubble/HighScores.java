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
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The High Scores popup, with a tab for each mode.  (Puzzle scores are
 * shown in Choose Level.)
 * <ul>
 * <li>Arcade: the top 10 games of every difficulty, by points (the
 *     points include the difficulty multiplier).
 * <li>Vs CPU: the top 10 wins: the harder difficulty first, then the
 *     fewest shots, then the quickest; each shows its time.
 * </ul>
 * Both show the top 3 on a podium of medal bubbles, then places 4 to
 * 10 as rows.
 * Also score helpers used by the game (the "Best" line).
 */
public class HighScores {

  private static final int TAB_ARCADE = 0;
  private static final int TAB_CPU    = 1;
  private static final int ARCADE_TOP = 10;

  /**
   * Vs CPU order (wins): the harder difficulty first, then the fewest
   * shots, then the quickest.
   */
  public static final Comparator<HighscoreDO> CPU_ORDER = (a, b) ->
      (a.getLevel() != b.getLevel()) ? Integer.compare(b.getLevel(), a.getLevel())
      : (a.getShots() != b.getShots()) ? Integer.compare(a.getShots(), b.getShots())
      : Long.compare(a.getTime(), b.getTime());
  /** Arcade order: most points, then the quicker game. */
  public static final Comparator<HighscoreDO> ARCADE_ORDER = (a, b) ->
      (a.getScore() != b.getScore()) ? Long.compare(b.getScore(), a.getScore())
                                     : Long.compare(a.getTime(), b.getTime());

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
    // Both tabs: the top 3 on a podium, then places 4 to 10.
    View board = (tab == TAB_CPU)
        ? leaderboard(HighscoreManager.MULTIPLAYER_DATABASE_NAME, CPU_ORDER,
                      hi -> time(hi.getTime()))
        : leaderboard(HighscoreManager.ARCADE_DATABASE_NAME, ARCADE_ORDER,
                      hi -> points(hi.getScore()));
    if (board != null) {
      content.addView(board);
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

  /*
   * The Arcade leaderboard: the top 3 on a podium of gold, silver and
   * bronze bubbles (first in the middle, biggest), then places 4 to 10
   * as rows.  One leaderboard for every difficulty: the points include
   * the difficulty multiplier.
   */

  /** Medal bubbles: gold (yellow), silver (white), bronze (orange). */
  private static final int[] MEDALS = {
    R.drawable.bubble_5_hd, R.drawable.bubble_2_hd, R.drawable.bubble_8_hd,
  };
  /** A dot colour for each difficulty, Easy (4 colours) ... Insane (8). */
  private static final int[] DIFFICULTY_COLOURS = {
    0xFF3CBE5F, 0xFF465AD7, 0xFFEBD23C, 0xFFF08C32, 0xFFD73C46,
  };

  /** Points as "48 210": a thin space between thousands, any language. */
  public static String points(long n) {
    String digits = Long.toString(Math.abs(n));
    StringBuilder out = new StringBuilder();
    int lead = digits.length() % 3;
    for (int i = 0; i < digits.length(); i++) {
      if ((i > 0) && (((i - lead) % 3) == 0)) {
        out.append('\u2009');
      }
      out.append(digits.charAt(i));
    }
    return ((n < 0) ? "-" : "") + out;
  }

  /**
   * A leaderboard: the top 3 on the podium, then places 4 to 10.
   * @param value - what each entry shows: the points (Arcade) or the
   * time of the win (Vs CPU).
   */
  private View leaderboard(String dbName, Comparator<HighscoreDO> order,
                           java.util.function.Function<HighscoreDO, String> value) {
    List<HighscoreDO> all = loadAll(activity, dbName);
    Collections.sort(all, order);
    if (all.isEmpty()) {
      return null;
    }
    LinearLayout board = new LinearLayout(activity);
    board.setOrientation(LinearLayout.VERTICAL);
    board.setGravity(Gravity.CENTER_HORIZONTAL);

    // The podium, in the order 2, 1, 3, bottoms lined up.
    LinearLayout podium = new LinearLayout(activity);
    podium.setOrientation(LinearLayout.HORIZONTAL);
    podium.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
    int[] podiumOrder = { 1, 0, 2 };
    int[] sizes = { 100, 82, 76 };   // dp, by place
    for (int place : podiumOrder) {
      if (place < all.size()) {
        podium.addView(podiumPlace(place, all.get(place), sizes[place], value));
      }
    }
    board.addView(podium);

    // Places 4 to 10.
    for (int i = 3; (i < all.size()) && (i < ARCADE_TOP); i++) {
      board.addView(listRow(i, all.get(i), value));
    }
    return board;
  }

  private View podiumPlace(int place, HighscoreDO hi, int sizeDp,
                           java.util.function.Function<HighscoreDO, String> value) {
    LinearLayout col = new LinearLayout(activity);
    col.setOrientation(LinearLayout.VERTICAL);
    col.setGravity(Gravity.CENTER_HORIZONTAL);
    int size = (int) (sizeDp * dp);
    FrameLayout medal = new FrameLayout(activity);
    ImageView bubble = new ImageView(activity);
    bubble.setImageResource(MEDALS[place]);
    bubble.setScaleType(ImageView.ScaleType.FIT_CENTER);
    medal.addView(bubble, new FrameLayout.LayoutParams(size, size));
    TextView rank = label(Integer.toString(place + 1), (int) (sizeDp * 0.42f),
                          ArcadeSummary.titleFont(activity), Color.WHITE);
    rank.setShadowLayer(4 * dp, 0, 1.5f * dp, 0xFF3C2800);
    medal.addView(rank, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
    col.addView(medal, new LinearLayout.LayoutParams(size, size));
    col.addView(label(value.apply(hi), (place == 0) ? 20 : 17,
                      Typeface.DEFAULT_BOLD, Color.WHITE));
    TextView who = label(hi.getName() + " · " + colours(hi.getLevel()), 12,
                         Typeface.DEFAULT, 0xFFDCE6F5);
    who.setMaxLines(2);
    who.setEllipsize(TextUtils.TruncateAt.END);
    col.addView(who);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        (int) (108 * dp), LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.setMargins((int) (2 * dp), 0, (int) (2 * dp), (int) (8 * dp));
    col.setLayoutParams(lp);
    return col;
  }

  private View listRow(int index, HighscoreDO hi,
                       java.util.function.Function<HighscoreDO, String> value) {
    LinearLayout row = new LinearLayout(activity);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    android.graphics.drawable.GradientDrawable bg =
        new android.graphics.drawable.GradientDrawable();
    bg.setCornerRadius(22 * dp);
    bg.setColor(0x28FFFFFF);
    bg.setStroke((int) (1.5f * dp), 0x46FFFFFF);
    row.setBackground(bg);
    int h = (int) (12 * dp);
    row.setPadding(h, (int) (6 * dp), (int) (16 * dp), (int) (6 * dp));

    TextView rank = label(Integer.toString(index + 1), 18,
                          ArcadeSummary.titleFont(activity), 0xFFFFF6E1);
    rank.setMinWidth((int) (28 * dp));
    row.addView(rank);
    TextView name = label(hi.getName(), 15, Typeface.DEFAULT_BOLD, Color.WHITE);
    name.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    name.setSingleLine(true);
    name.setEllipsize(TextUtils.TruncateAt.END);
    row.addView(name, new LinearLayout.LayoutParams(0,
        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    // The difficulty: a coloured dot and its name.
    int d = Math.max(org.jfedor.frozenbubble.LevelManager.EASY,
                     Math.min(org.jfedor.frozenbubble.LevelManager.INSANE,
                              hi.getLevel()));
    android.graphics.drawable.GradientDrawable dot =
        new android.graphics.drawable.GradientDrawable();
    dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
    dot.setColor(DIFFICULTY_COLOURS[d - org.jfedor.frozenbubble.LevelManager.EASY]);
    View dotView = new View(activity);
    dotView.setBackground(dot);
    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
        (int) (10 * dp), (int) (10 * dp));
    dlp.setMargins((int) (6 * dp), 0, (int) (4 * dp), 0);
    row.addView(dotView, dlp);
    TextView diff = label(colours(hi.getLevel()), 12, Typeface.DEFAULT, 0xFFD2DCEB);
    diff.setMinWidth((int) (56 * dp));
    diff.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    row.addView(diff);
    TextView score = label(value.apply(hi), 16, Typeface.DEFAULT_BOLD,
                           0xFFFFE14D);
    score.setMinWidth((int) (70 * dp));
    score.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    row.addView(score);

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        (int) (316 * dp), LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.setMargins(0, (int) (3 * dp), 0, (int) (3 * dp));
    row.setLayoutParams(lp);
    return row;
  }

  private TextView label(String text, int sizeDp, Typeface face, int colour) {
    TextView t = new TextView(activity);
    t.setText(text);
    t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, sizeDp);
    t.setTypeface(face);
    t.setTextColor(colour);
    t.setShadowLayer(3 * dp, 0, 1 * dp, Color.BLACK);
    t.setGravity(Gravity.CENTER);
    return t;
  }

  private String colours(int difficulty) {
    return OptionsMenus.difficultyName(activity, difficulty);
  }

}
