/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * A full-screen picture (the splash and home screens), shown whole and
 * centered, with the bars around it filled by the blurred {@link
 * Backdrop} instead of black, as in the game.
 */
public class BackdropImageView extends ImageView {
  private Bitmap backdrop;
  private Bitmap backdropSource;

  public BackdropImageView(Context context) {
    super(context);
    setScaleType(ScaleType.FIT_CENTER);
    setBackgroundColor(0xFF000000);
  }

  /**
   * Where the picture is drawn, in this view's coordinates (empty before
   * layout).
   */
  public Rect imageRect() {
    Rect r = new Rect();
    Drawable d = getDrawable();
    int vw = getWidth()  - getPaddingLeft() - getPaddingRight();
    int vh = getHeight() - getPaddingTop()  - getPaddingBottom();
    if ((d == null) || (vw <= 0) || (vh <= 0) ||
        (d.getIntrinsicWidth() <= 0) || (d.getIntrinsicHeight() <= 0)) {
      return r;
    }
    float s  = Math.min((float) vw / d.getIntrinsicWidth(),
                        (float) vh / d.getIntrinsicHeight());
    int   iw = Math.round(d.getIntrinsicWidth()  * s);
    int   ih = Math.round(d.getIntrinsicHeight() * s);
    int   l  = getPaddingLeft() + ((vw - iw) / 2);
    int   t  = getPaddingTop()  + ((vh - ih) / 2);
    r.set(l, t, l + iw, t + ih);
    return r;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    Bitmap bars = backdrop(getWidth(), getHeight());
    if (bars != null) {
      canvas.drawBitmap(bars, 0, 0, null);
    }
    super.onDraw(canvas);
  }

  private Bitmap backdrop(int w, int h) {
    Drawable d = getDrawable();
    if (!(d instanceof BitmapDrawable) || (w <= 0) || (h <= 0)) {
      return null;
    }
    Bitmap src = ((BitmapDrawable) d).getBitmap();
    if ((src == null) || src.isRecycled()) {
      return null;
    }
    if ((backdrop != null) && (backdropSource == src) &&
        (backdrop.getWidth() == w) && (backdrop.getHeight() == h)) {
      return backdrop;
    }
    if (backdrop != null) {
      backdrop.recycle();
      backdrop = null;
    }
    try {
      backdrop       = Backdrop.build(src, w, h);
      backdropSource = src;
    } catch (OutOfMemoryError e) {
      backdrop = null;   // Black bars, as before.
    }
    return backdrop;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (backdrop != null) {
      backdrop.recycle();
      backdrop       = null;
      backdropSource = null;
    }
  }
}
