/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.BuildConfig;
import org.jfedor.frozenbubble.GameView;
import org.jfedor.frozenbubble.R;
import org.jfedor.frozenbubble.SoundManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Help: how to play, each mode, Arcade scoring, the options, My Music,
 * saving, and About (with the Credits).  A scrolling column of panels
 * over a blurred copy of the home screen picture.  Opened from the home
 * screen and from the in-game Options.
 */
public class HelpActivity extends Activity {
  private static final int PANEL_COLOUR  = 0xC0102448;  // deep blue, 75%
  private static final int PANEL_EDGE    = 0x60C8E6FF;
  private static final int TITLE_COLOUR  = 0xFFFFF6E1;
  private static final int HEAD_COLOUR   = 0xFFFFE14D;
  private static final int BODY_COLOUR   = 0xFFEFF4FF;
  private static final int SHADOW_COLOUR = 0xFF0A1E5A;
  private static final int MAX_WIDTH_DP  = 560;

  private SoundManager sound;
  private float        dp;
  private Typeface     titleFont;

  @Override
  protected void attachBaseContext(Context base) {
    // Show the language chosen in Options (or the phone's).
    super.attachBaseContext(LocaleHelper.wrap(base));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    BackKey.handle(this, this::finish);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    SystemUi.apply(this, Prefs.get(this).getBoolean(OptionsMenus.KEY_FULLSCREEN, true));
    sound = new SoundManager(this);
    sound.loadSound("stick", R.raw.stick);
    dp        = getResources().getDisplayMetrics().density;
    titleFont = ArcadeSummary.titleFont(this);

    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.BLACK);
    root.addView(new BlurredPicture(this), new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT));

    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    int pad = px(16);
    column.setPadding(pad, pad, pad, pad);

    column.addView(text(getString(R.string.help), 40, titleFont, TITLE_COLOUR,
                        Gravity.CENTER));
    column.addView(text(getString(R.string.help_version, BuildConfig.VERSION_NAME),
                        14, Typeface.DEFAULT_BOLD, BODY_COLOUR, Gravity.CENTER));

    LinearLayout play = section(R.drawable.bubble_3_hd, R.string.help_play_t,
                                R.string.help_play);
    play.addView(buttonGuide());
    column.addView(play);
    column.addView(section(R.drawable.bubble_4_hd, R.string.help_puzzle_t,
                           R.string.help_puzzle));
    column.addView(section(R.drawable.bubble_6_hd, R.string.help_arcade_t,
                           R.string.help_arcade));
    column.addView(section(R.drawable.bubble_5_hd, R.string.help_scoring_t,
                           R.string.help_scoring));
    column.addView(section(R.drawable.bubble_2_hd, R.string.help_2p_t,
                           R.string.help_2p));
    column.addView(section(R.drawable.bubble_7_hd, R.string.help_scores_t,
                           R.string.help_scores));
    column.addView(section(R.drawable.bubble_8_hd, R.string.help_options_t,
                           R.string.help_options));
    column.addView(section(R.drawable.next_song_button_hd, R.string.help_music_t,
                           R.string.help_music));
    column.addView(section(R.drawable.bubble_1_hd, R.string.help_save_t,
                           R.string.help_save));
    LinearLayout about = section(R.drawable.bubble_3_hd, R.string.help_about_t,
                                 R.string.help_about);
    column.addView(about);
    // The source code address opens in the browser.
    TextView aboutBody = (TextView) about.getChildAt(about.getChildCount() - 1);
    Linkify.addLinks(aboutBody, Linkify.WEB_URLS);
    aboutBody.setLinkTextColor(0xFF9FD8FF);
    aboutBody.setMovementMethod(LinkMovementMethod.getInstance());

    LinearLayout buttons = MenuButtons.column(this,
        MenuButtons.create(this, R.string.credits, v -> {
          click();
          Intent intent = new Intent(this, ScrollingCredits.class);
          intent.putExtra(ScrollingCredits.EXTRA_FROM_OPTIONS, true);
          startActivity(intent);
        }),
        MenuButtons.create(this, R.string.back, v -> {
          click();
          finish();
        }));
    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    blp.topMargin = px(12);
    buttons.setGravity(Gravity.CENTER_HORIZONTAL);
    column.addView(buttons, blp);

    // At most MAX_WIDTH_DP wide (tablets, landscape), centred.
    FrameLayout centre = new FrameLayout(this);
    int width = Math.min(getResources().getDisplayMetrics().widthPixels,
                         px(MAX_WIDTH_DP));
    centre.addView(column, new FrameLayout.LayoutParams(
        width, FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER_HORIZONTAL));
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.addView(centre, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT));
    root.addView(scroll, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT));
    setContentView(root);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (BackKey.isLegacy(keyCode)) {
      finish();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (sound != null) {
      sound.cleanUp();
      sound = null;
    }
  }

  private void click() {
    if (sound != null) {
      sound.playSound("stick", R.raw.stick);
    }
  }

  private int px(float dips) {
    return Math.round(dips * dp);
  }

  /** A panel: an icon and a heading, then the text. */
  private LinearLayout section(int iconId, int titleId, int bodyId) {
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    int pad = px(14);
    panel.setPadding(pad, px(10), pad, pad);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(PANEL_COLOUR);
    bg.setCornerRadius(px(16));
    bg.setStroke(Math.max(1, px(1)), PANEL_EDGE);
    panel.setBackground(bg);

    LinearLayout head = new LinearLayout(this);
    head.setOrientation(LinearLayout.HORIZONTAL);
    head.setGravity(Gravity.CENTER_VERTICAL);
    ImageView icon = new ImageView(this);
    icon.setImageResource(iconId);
    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int size = px(28);
    LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(size, size);
    ilp.rightMargin = px(10);
    head.addView(icon, ilp);
    head.addView(text(getString(titleId), 22, titleFont, HEAD_COLOUR,
                      Gravity.START));
    panel.addView(head);

    TextView body = text(getString(bodyId), 15, Typeface.DEFAULT, BODY_COLOUR,
                         Gravity.START);
    body.setLineSpacing(0f, 1.15f);
    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    tlp.topMargin = px(6);
    panel.addView(body, tlp);

    LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    plp.topMargin = px(12);
    panel.setLayoutParams(plp);
    return panel;
  }

  /** The game's round buttons, with what they do. */
  private View buttonGuide() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_HORIZONTAL);
    row.addView(guide(R.drawable.next_song_button_hd, R.string.help_btn_next));
    row.addView(guide(R.drawable.back_button_hd, R.string.help_btn_menu));
    row.addView(guide(R.drawable.pause_button_hd, R.string.help_btn_pause));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin = px(10);
    row.setLayoutParams(lp);
    return row;
  }

  private View guide(int iconId, int labelId) {
    LinearLayout cell = new LinearLayout(this);
    cell.setOrientation(LinearLayout.VERTICAL);
    cell.setGravity(Gravity.CENTER_HORIZONTAL);
    ImageView icon = new ImageView(this);
    icon.setImageResource(iconId);
    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int size = px(40);
    cell.addView(icon, new LinearLayout.LayoutParams(size, size));
    TextView label = text(getString(labelId), 13, Typeface.DEFAULT_BOLD,
                          BODY_COLOUR, Gravity.CENTER);
    cell.addView(label);
    cell.setLayoutParams(new LinearLayout.LayoutParams(0,
        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    return cell;
  }

  private TextView text(String s, int sizeSp, Typeface face, int colour,
                        int gravity) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    t.setTypeface(face);
    t.setTextColor(colour);
    t.setShadowLayer(3 * dp, 0, 1.5f * dp, SHADOW_COLOUR);
    t.setGravity(gravity);
    return t;
  }

  /**
   * The home screen picture, blurred and darkened toward deep blue (as
   * behind the game's letterbox bars), filling the screen.
   */
  private static final class BlurredPicture extends View {
    private Bitmap source;
    private Bitmap blurred;

    BlurredPicture(Activity activity) {
      super(activity);
      try {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = 4;   // it is blurred anyway
        source = BitmapFactory.decodeResource(activity.getResources(),
            GameView.chooseArt(activity, R.drawable.home_screen_hd,
                               R.drawable.home_screen), o);
      } catch (OutOfMemoryError e) {
        source = null;
      }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);
      if (blurred != null) {
        blurred.recycle();
        blurred = null;
      }
      if ((source != null) && (w > 0) && (h > 0)) {
        try {
          blurred = Backdrop.build(source, w, h);
        } catch (OutOfMemoryError e) {
          blurred = null;
        }
      }
    }

    @Override
    protected void onDraw(Canvas canvas) {
      if (blurred != null) {
        canvas.drawBitmap(blurred, 0, 0, null);
      }
    }
  }
}
