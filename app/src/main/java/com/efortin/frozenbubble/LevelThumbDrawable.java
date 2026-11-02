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
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A level's preview in a Choose Level bubble: a dark blue bubble with a
 * light rim, and the level's bubbles as small coloured dots in their
 * honeycomb inside it.  Drawn in code from the level data, so it is
 * sharp at any size.
 */
public class LevelThumbDrawable extends Drawable {

  /** The bubble colours (0 black ... 7 orange), as dots. */
  private static final int[] COLOURS = {
    0xFF463E3C, 0xFFE1E1E4, 0xFF465AD7, 0xFF3CBE5F,
    0xFFEBD23C, 0xFFAA50C8, 0xFFD73C46, 0xFFF08C32,
  };
  private static final int BACKGROUND = 0xEB1E3C78;
  private static final int RIM        = 0xFFC8E6FF;
  /** The level area: 8 bubbles wide, 10 rows (in game pixels). */
  private static final float LEVEL_W = 8 * 32;
  private static final float LEVEL_H = (9 * 28) + 32;

  private final byte[][] level;   // [x][y], -1 = empty
  private final float    dp;
  private final Paint    paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public LevelThumbDrawable(byte[][] level, float density) {
    this.level = level;
    this.dp    = density;
  }

  @Override
  public void draw(Canvas canvas) {
    Rect b = getBounds();
    float size = Math.min(b.width(), b.height());
    float cx = b.exactCenterX();
    float cy = b.exactCenterY();
    float r  = size / 2;

    paint.setStyle(Paint.Style.FILL);
    paint.setColor(BACKGROUND);
    canvas.drawCircle(cx, cy, r - dp, paint);

    // The level, scaled to fit well inside the circle.
    float scale = (size * 0.62f) / LEVEL_W;
    float ox = cx - ((LEVEL_W * scale) / 2);
    float oy = cy - ((LEVEL_H * scale) / 2);
    float dot = 16 * scale * 0.92f;
    if (level != null) {
      for (int x = 0; x < level.length; x++) {
        for (int y = 0; (y < level[x].length) && (y < 10); y++) {
          int c = level[x][y];
          if ((c < 0) || (c >= COLOURS.length)) {
            continue;
          }
          // As in the game: odd rows are shifted half a bubble.
          float px = ox + (((x * 32) - ((y % 2) * 16) + 16) * scale);
          float py = oy + (((y * 28) + 16) * scale);
          paint.setColor(COLOURS[c]);
          canvas.drawCircle(px, py, dot, paint);
        }
      }
    }

    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2 * dp);
    paint.setColor(RIM);
    canvas.drawCircle(cx, cy, r - (2 * dp), paint);
  }

  @Override
  public void setAlpha(int alpha) {
    // Not used: locked levels fade the whole view.
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    paint.setColorFilter(cf);
  }

  @Override
  @SuppressWarnings("deprecation")   // Still required by Drawable.
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
