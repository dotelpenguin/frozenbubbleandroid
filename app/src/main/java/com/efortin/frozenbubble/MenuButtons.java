/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.jfedor.frozenbubble.R;

/**
 * Buttons in the style of the home screen menu (Arcade, Puzzle,
 * 2 Player, Options...), for use in other screens and dialogs.
 */
public final class MenuButtons {
  private MenuButtons() {}

  /** Apply the home screen menu button style to a button. */
  public static void style(Button button) {
    button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    button.setWidth((int) (button.getTextSize() * 9));
    button.setTypeface(null, Typeface.BOLD);
    button.setHorizontalFadingEdgeEnabled(true);
    button.setFadingEdgeLength(5);
    bubble(button);
  }

  /**
   * The bubble look: a glossy blue bubble background (like the round
   * in-game buttons), with light text and a dark outline-like shadow so
   * it reads on the blue.
   */
  public static void bubble(Button button) {
    float dp = button.getResources().getDisplayMetrics().density;
    button.setBackground(new BubbleButtonDrawable(dp));
    button.setTextColor(new android.content.res.ColorStateList(
        new int[][] { { -android.R.attr.state_enabled }, {} },
        new int[]   { 0xFFC8D2DC, 0xFFF0FAFF }));
    button.setShadowLayer(2.5f * dp, 0f, 0.8f * dp, 0xFF08183C);
    button.setMinHeight((int) (46 * dp));
    button.setMinimumHeight((int) (46 * dp));
  }

  /** A home-screen-style menu button. */
  public static Button create(Context context, int textId,
                              View.OnClickListener listener) {
    Button button = new Button(context);
    button.setText(textId);
    style(button);
    button.setOnClickListener(listener);
    return button;
  }

  /** A home-screen-style menu button with a text label. */
  public static Button create(Context context, CharSequence text,
                              View.OnClickListener listener) {
    Button button = new Button(context);
    button.setText(text);
    style(button);
    button.setOnClickListener(listener);
    return button;
  }

  /**
   * A centered column of home-screen-style menu buttons, spaced like the
   * home screen menu.
   */
  public static LinearLayout column(Context context, Button... buttons) {
    LinearLayout column = new LinearLayout(context);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setGravity(Gravity.CENTER_HORIZONTAL);
    int pad = (int) (8 * context.getResources().getDisplayMetrics().density);
    column.setPadding(pad, pad, pad, pad);
    for (Button button : buttons) {
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT);
      params.gravity      = Gravity.CENTER_HORIZONTAL;
      params.topMargin    = 15;
      params.bottomMargin = 15;
      column.addView(button, params);
    }
    return column;
  }
}
