/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The backdrop that fills the letterbox bars around a screen's art: a
 * blurred copy of the art, darkened toward deep blue and stretched to
 * cover the whole screen.  Used by the game (GameView) and the splash
 * and home screens (BackdropImageView), so they all look alike.
 */
public final class Backdrop {
  private Backdrop() {}

  /**
   * Shrink the art, blur it, darken it toward deep blue, then stretch
   * it to cover a w x h screen (centered, cropped).
   */
  public static Bitmap build(Bitmap src, int w, int h) {
    final int sw = 80;
    final int sh = 60;
    Bitmap small = Bitmap.createScaledBitmap(src, sw, sh, true);
    int[] px = new int[sw * sh];
    small.getPixels(px, 0, sw, 0, 0, sw, sh);
    if (small != src) {
      small.recycle();
    }
    for (int pass = 0; pass < 3; pass++) {
      boxBlur(px, sw, sh, 3, true);
      boxBlur(px, sw, sh, 3, false);
    }
    for (int i = 0; i < px.length; i++) {
      int c  = px[i];
      int r  = ((((c >> 16) & 0xff) * 62) + (20 * 38)) / 100;
      int g  = ((((c >>  8) & 0xff) * 62) + (26 * 38)) / 100;
      int bl = ((( c        & 0xff) * 62) + (48 * 38)) / 100;
      px[i] = 0xff000000 | (r << 16) | (g << 8) | bl;
    }
    Bitmap blurred = Bitmap.createBitmap(px, sw, sh, Bitmap.Config.ARGB_8888);
    Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
    Canvas c = new Canvas(out);
    float s  = Math.max((float) w / sw, (float) h / sh);
    float dw = sw * s;
    float dh = sh * s;
    RectF dst = new RectF((w - dw) / 2f, (h - dh) / 2f,
                          (w + dw) / 2f, (h + dh) / 2f);
    c.drawBitmap(blurred, null, dst,
                 new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG));
    blurred.recycle();
    return out;
  }

  /** One horizontal or vertical box blur pass over packed RGB pixels. */
  private static void boxBlur(int[] px, int w, int h, int radius,
                              boolean horizontal) {
    int[] out = new int[px.length];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int sr = 0, sg = 0, sb = 0, n = 0;
        for (int k = -radius; k <= radius; k++) {
          int xx = horizontal ? Math.min(w - 1, Math.max(0, x + k)) : x;
          int yy = horizontal ? y : Math.min(h - 1, Math.max(0, y + k));
          int c  = px[(yy * w) + xx];
          sr += (c >> 16) & 0xff;
          sg += (c >>  8) & 0xff;
          sb +=  c        & 0xff;
          n++;
        }
        out[(y * w) + x] = 0xff000000 | ((sr / n) << 16) |
                           ((sg / n) << 8) | (sb / n);
      }
    }
    System.arraycopy(out, 0, px, 0, px.length);
  }
}
