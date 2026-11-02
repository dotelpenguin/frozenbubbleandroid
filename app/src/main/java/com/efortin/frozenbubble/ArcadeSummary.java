/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import java.util.Locale;
import java.util.Random;

import org.jfedor.frozenbubble.R;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The end of an Arcade game: "GAME OVER", the score in a big bubble
 * (counting up), "NEW BEST!" with a burst of sparkles and applause, or
 * the place on the leaderboard, the difficulty, and the stats in small
 * bubbles, with Play Again and Quit.
 */
public final class ArcadeSummary {
  private ArcadeSummary() {}

  /** What the game provides. */
  public interface Host {
    void playAgain();
    void quit();
    /** Play the applause (a new best score). */
    void applause();
  }

  private static Typeface sTitleFont = null;

  /** Luckiest Guy (assets/fonts), the lettering of the "HD" logo. */
  public static Typeface titleFont(Context context) {
    if (sTitleFont == null) {
      try {
        sTitleFont = Typeface.createFromAsset(context.getAssets(),
                                              "fonts/LuckiestGuy.ttf");
      } catch (RuntimeException e) {
        sTitleFont = Typeface.DEFAULT_BOLD;
      }
    }
    return sTitleFont;
  }

  /**
   * The summary's view.
   * @param s - from GameThread.arcadeSummary(): score, popped, dropped,
   * bestCombo, shots, time, difficulty, rank.
   */
  public static View build(final Activity activity, Bundle s,
                           final Host host) {
    final float dp = activity.getResources().getDisplayMetrics().density;
    Typeface lg = titleFont(activity);

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (8 * dp);
    root.setPadding(pad, pad, pad, pad);

    TextView title = text(activity, activity.getString(R.string.game_over),
                          40, lg, 0xFFFFF6E1, 0xFF142864);
    root.addView(title);

    // The big bubble with the score, and room around it for sparkles.
    final long score = s.getLong("score");
    final int  rank  = s.getInt("rank");
    FrameLayout big = new FrameLayout(activity);
    int bigSize = (int) (250 * dp);
    final SparkleView sparkles = new SparkleView(activity);
    big.addView(sparkles, new FrameLayout.LayoutParams(bigSize, bigSize));
    ImageView bubble = new ImageView(activity);
    bubble.setImageResource(R.drawable.bubble_3_hd);
    bubble.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int bubbleSize = (int) (190 * dp);
    big.addView(bubble, new FrameLayout.LayoutParams(bubbleSize, bubbleSize,
                                                     Gravity.CENTER));
    LinearLayout inBubble = new LinearLayout(activity);
    inBubble.setOrientation(LinearLayout.VERTICAL);
    inBubble.setGravity(Gravity.CENTER);
    // The score in a clean bold font (easier to read than the logo's).
    final TextView scoreText = text(activity, format(0), 38,
                                    Typeface.DEFAULT_BOLD, Color.WHITE,
                                    0xFF0A1E5A);
    inBubble.addView(scoreText);
    final TextView bestText;
    if (rank == 1) {
      bestText = text(activity, activity.getString(R.string.sum_new_best),
                      18, lg, 0xFFFFE14D, 0xFF5A2800);
    }
    else if (rank > 1) {
      bestText = text(activity, activity.getString(R.string.sum_rank, rank),
                      14, Typeface.DEFAULT_BOLD, Color.WHITE, 0xFF0A1E5A);
    }
    else {
      bestText = null;
    }
    if (bestText != null) {
      bestText.setVisibility(View.INVISIBLE);
      bestText.setMaxWidth((int) (150 * dp));
      inBubble.addView(bestText);
    }
    big.addView(inBubble, new FrameLayout.LayoutParams(bubbleSize, bubbleSize,
                                                       Gravity.CENTER));
    root.addView(big, new LinearLayout.LayoutParams(bigSize, bigSize));

    // The difficulty and its multiplier.
    int d = s.getInt("difficulty", org.jfedor.frozenbubble.LevelManager.NORMAL);
    root.addView(text(activity, activity.getString(R.string.opt_difficulty) +
        ": " + OptionsMenus.difficultyName(activity, d) +
        "  ·  ×" + half(d - 2), 16, Typeface.DEFAULT_BOLD, Color.WHITE,
        Color.BLACK));

    // The stats, in small bubbles: three, then two.
    int combo = s.getInt("bestCombo");
    LinearLayout row1 = row(activity);
    row1.addView(stat(activity, R.drawable.bubble_7_hd,
        Integer.toString(s.getInt("popped")), R.string.sum_popped, lg));
    row1.addView(stat(activity, R.drawable.bubble_4_hd,
        Integer.toString(s.getInt("dropped")), R.string.sum_dropped, lg));
    row1.addView(stat(activity, R.drawable.bubble_6_hd,
        (combo > 0) ? "×" + half(combo + 1) : "–", R.string.sum_best_combo, lg));
    root.addView(row1);
    LinearLayout row2 = row(activity);
    row2.addView(stat(activity, R.drawable.bubble_2_hd,
        Integer.toString(s.getInt("shots")), R.string.sum_shots, lg));
    row2.addView(stat(activity, R.drawable.bubble_8_hd,
        HighScores.time(s.getLong("time")), R.string.sum_time, lg));
    root.addView(row2);

    Button again = MenuButtons.create(activity, R.string.play_again,
                                      v -> host.playAgain());
    Button quit  = MenuButtons.create(activity, R.string.quit, v -> host.quit());
    OptionsMenus.widen(again);
    OptionsMenus.widen(quit);
    root.addView(MenuButtons.column(activity, again, quit));

    // The score counts up; then "NEW BEST!" pops in, with sparkles and
    // applause (or the place on the leaderboard shows).
    ValueAnimator count = ValueAnimator.ofFloat(0f, 1f);
    count.setDuration((score > 0) ? 1400 : 1);
    count.setStartDelay(250);
    count.setInterpolator(new DecelerateInterpolator(1.6f));
    count.addUpdateListener(a ->
        scoreText.setText(format(Math.round(score * (float) a.getAnimatedValue()))));
    count.addListener(new android.animation.AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(android.animation.Animator a) {
        scoreText.setText(format(score));
        if (bestText != null) {
          bestText.setVisibility(View.VISIBLE);
          bestText.setScaleX(0f);
          bestText.setScaleY(0f);
          bestText.animate().scaleX(1f).scaleY(1f).setDuration(350)
              .setInterpolator(new OvershootInterpolator(2.5f)).start();
        }
        if (rank == 1) {
          sparkles.burst();
          host.applause();
        }
      }
    });
    count.start();
    return root;
  }

  private static String format(long n) {
    return HighScores.points(n);
  }

  /** n / 2 as "1", "1.5", "2"... */
  private static String half(int n) {
    return ((n % 2) == 0) ? Integer.toString(n / 2) : (n / 2) + ".5";
  }

  private static TextView text(Context c, String s, int sizeDp, Typeface face,
                               int colour, int shadow) {
    TextView t = new TextView(c);
    float dp = c.getResources().getDisplayMetrics().density;
    t.setText(s);
    t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
    t.setTypeface(face);
    t.setTextColor(colour);
    t.setShadowLayer(3 * dp, 0, 1.5f * dp, shadow);
    t.setGravity(Gravity.CENTER);
    return t;
  }

  private static LinearLayout row(Context c) {
    LinearLayout r = new LinearLayout(c);
    r.setOrientation(LinearLayout.HORIZONTAL);
    r.setGravity(Gravity.CENTER_HORIZONTAL);
    return r;
  }

  /** A stat: its value in a small bubble, its name under it. */
  private static View stat(Context c, int bubbleId, String value, int labelId,
                           Typeface lg) {
    float dp = c.getResources().getDisplayMetrics().density;
    LinearLayout cell = new LinearLayout(c);
    cell.setOrientation(LinearLayout.VERTICAL);
    cell.setGravity(Gravity.CENTER_HORIZONTAL);
    int size = (int) (66 * dp);
    FrameLayout f = new FrameLayout(c);
    ImageView b = new ImageView(c);
    b.setImageResource(bubbleId);
    b.setScaleType(ImageView.ScaleType.FIT_CENTER);
    f.addView(b, new FrameLayout.LayoutParams(size, size));
    TextView v = text(c, value, 17, Typeface.DEFAULT_BOLD, Color.WHITE,
                      Color.BLACK);
    f.addView(v, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
    cell.addView(f, new LinearLayout.LayoutParams(size, size));
    TextView label = text(c, c.getString(labelId), 12, Typeface.DEFAULT_BOLD,
                          Color.WHITE, Color.BLACK);
    label.setMaxLines(2);
    label.setEllipsize(TextUtils.TruncateAt.END);
    cell.addView(label);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        (int) (96 * dp), LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.setMargins((int) (2 * dp), (int) (4 * dp), (int) (2 * dp), (int) (2 * dp));
    cell.setLayoutParams(lp);
    return cell;
  }

  /** A burst of sparkles flying out from the big bubble. */
  private static final class SparkleView extends View {
    private static final int COUNT = 32;
    private static final int[] COLOURS = {
      0xFFFFFFFF, 0xFFFFE14D, 0xFFB8F0FF, 0xFFFFF6C8,
    };
    private final float[] angle = new float[COUNT];
    private final float[] speed = new float[COUNT];
    private final float[] delay = new float[COUNT];
    private final float[] size  = new float[COUNT];
    private final int[]   colour = new int[COUNT];
    private final Paint   paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float t = -1f;   // -1: nothing to draw

    SparkleView(Context c) {
      super(c);
      float dp = c.getResources().getDisplayMetrics().density;
      Random r = new Random();
      for (int i = 0; i < COUNT; i++) {
        angle[i]  = (float) (r.nextFloat() * Math.PI * 2);
        speed[i]  = 0.6f + (0.6f * r.nextFloat());
        delay[i]  = 0.35f * r.nextFloat();
        size[i]   = (3 + (4 * r.nextFloat())) * dp;
        colour[i] = COLOURS[r.nextInt(COLOURS.length)];
      }
      paint.setStrokeCap(Paint.Cap.ROUND);
    }

    void burst() {
      ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
      a.setDuration(1600);
      a.addUpdateListener(v -> {
        t = (float) v.getAnimatedValue();
        invalidate();
      });
      a.addListener(new android.animation.AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(android.animation.Animator x) {
          t = -1f;
          invalidate();
        }
      });
      a.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      if (t < 0) {
        return;
      }
      float cx = getWidth() / 2f;
      float cy = getHeight() / 2f;
      float r0 = Math.min(cx, cy) * 0.62f;   // the bubble's edge
      float r1 = Math.min(cx, cy) * 0.98f;
      for (int i = 0; i < COUNT; i++) {
        float p = (t - delay[i]) / (1f - delay[i]);
        if (p <= 0) {
          continue;
        }
        float dist = r0 + ((r1 - r0) * Math.min(1f, p * speed[i]));
        float x = cx + (float) (Math.cos(angle[i]) * dist);
        float y = cy + (float) (Math.sin(angle[i]) * dist);
        int alpha = (int) (255 * Math.max(0f, 1f - p));
        paint.setColor(colour[i]);
        paint.setAlpha(alpha);
        float s = size[i] * (1f - (0.4f * p));
        // A four-pointed star: a cross and a dot.
        paint.setStrokeWidth(s * 0.35f);
        canvas.drawLine(x - s, y, x + s, y, paint);
        canvas.drawLine(x, y - s, x, y + s, paint);
        canvas.drawCircle(x, y, s * 0.35f, paint);
      }
    }
  }
}
