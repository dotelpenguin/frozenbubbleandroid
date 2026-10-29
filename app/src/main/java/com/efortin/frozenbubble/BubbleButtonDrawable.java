/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * The menu button background: a glossy blue bubble, like the round
 * in-game buttons.  A dark navy rim, a light-to-deep blue body with a
 * soft glow at the bottom, and a white gloss across the top.
 * <ul>
 * <li>Pressed: a deeper blue, with less gloss.
 * <li>Focused (keys or a gamepad): a bright rim.
 * <li>Disabled: grey.
 * </ul>
 * Drawn in code, so it is sharp at any size.  Tall (two line) buttons
 * become rounded rectangles rather than very round pills.
 */
public class BubbleButtonDrawable extends Drawable {

  private static final int RIM         = 0xFF0C285F;
  private static final int RIM_FOCUSED = 0xFFC8F4FF;
  private static final int TOP         = 0xFF8CD7FF;
  private static final int BOTTOM      = 0xFF1478D7;
  private static final int TOP_PRESSED    = 0xFF55AEE8;
  private static final int BOTTOM_PRESSED = 0xFF0A55A6;
  private static final int GLOW        = 0xAA78E6FF;
  private static final int SHADOW      = 0x50000A28;

  private final float dp;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect  = new RectF();

  private boolean pressed;
  private boolean focused;
  private boolean enabled = true;

  private static final ColorFilter GREY;
  static {
    ColorMatrix m = new ColorMatrix();
    m.setSaturation(0.15f);
    GREY = new ColorMatrixColorFilter(m);
  }

  public BubbleButtonDrawable(float density) {
    dp = density;
  }

  @Override
  public void draw(Canvas canvas) {
    Rect b = getBounds();
    if (b.isEmpty()) {
      return;
    }
    float shadow = 2 * dp;
    float left   = b.left + dp;
    float top    = b.top + dp;
    float right  = b.right - dp;
    float bottom = b.bottom - shadow;
    float h      = bottom - top;
    float radius = Math.min(h / 2, 26 * dp);
    paint.setColorFilter(enabled ? null : GREY);
    paint.setShader(null);

    // Shadow under the bubble.
    paint.setColor(SHADOW);
    rect.set(left, top + shadow, right, bottom + shadow);
    canvas.drawRoundRect(rect, radius, radius, paint);

    // Rim.
    paint.setColor((focused && enabled) ? RIM_FOCUSED : RIM);
    rect.set(left, top, right, bottom);
    canvas.drawRoundRect(rect, radius, radius, paint);

    // Body.
    float rim = 1.6f * dp;
    rect.set(left + rim, top + rim, right - rim, bottom - rim);
    float r = Math.max(0, radius - rim);
    paint.setColor(0xFFFFFFFF);
    paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
        pressed ? TOP_PRESSED : TOP, pressed ? BOTTOM_PRESSED : BOTTOM,
        Shader.TileMode.CLAMP));
    canvas.drawRoundRect(rect, r, r, paint);

    // Glow at the bottom.
    paint.setShader(new LinearGradient(0, rect.top + (rect.height() * 0.45f),
        0, rect.bottom, 0x0078E6FF, GLOW, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(rect, r, r, paint);

    // Gloss across the top.
    float glossH = Math.min(rect.height() * 0.46f, 22 * dp);
    float inset  = Math.min(radius * 0.5f, rect.width() * 0.1f);
    RectF gloss  = new RectF(rect.left + inset, rect.top + dp,
                             rect.right - inset, rect.top + dp + glossH);
    float gr = Math.min(glossH / 2, r);
    paint.setShader(new LinearGradient(0, gloss.top, 0, gloss.bottom,
        pressed ? 0x80FFFFFF : 0xD2FFFFFF, pressed ? 0x10FFFFFF : 0x28FFFFFF,
        Shader.TileMode.CLAMP));
    canvas.drawRoundRect(gloss, gr, gr, paint);
    paint.setShader(null);
  }

  /** Room for the text inside the rim and gloss. */
  @Override
  public boolean getPadding(Rect padding) {
    int h = (int) (18 * dp);
    int v = (int) (9 * dp);
    padding.set(h, v, h, v + (int) (2 * dp));
    return true;
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    boolean p = false, f = false, e = false;
    for (int s : state) {
      if (s == android.R.attr.state_pressed)  p = true;
      if (s == android.R.attr.state_focused)  f = true;
      if (s == android.R.attr.state_enabled)  e = true;
    }
    boolean changed = (p != pressed) || (f != focused) || (e != enabled);
    pressed = p;
    focused = f;
    enabled = e;
    if (changed) {
      invalidateSelf();
    }
    return changed;
  }

  @Override
  public void setAlpha(int alpha) {
    // Not used: a faded button fades the whole view (View.setAlpha).
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    // The disabled state sets its own filter.
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
