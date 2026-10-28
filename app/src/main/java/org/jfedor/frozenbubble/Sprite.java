/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 * Additional source - Copyright (c) 2013 Eric Fortin.
 *
 * This code is distributed under the GNU General Public License
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to:
 * Free Software Foundation, Inc.
 * 675 Mass Ave
 * Cambridge, MA 02139, USA
 *
 * Artwork:
 *    Alexis Younes <73lab at free.fr>
 *      (everything but the bubbles)
 *    Amaury Amblard-Ladurantie <amaury at linuxfr.org>
 *      (the bubbles)
 *
 * Soundtrack:
 *    Matthias Le Bidan <matthias.le_bidan at caramail.com>
 *      (the three musics and all the sound effects)
 *
 * Design & Programming:
 *    Guillaume Cottenceau <guillaume.cottenceau at free.fr>
 *      (design and manage the project, whole Perl sourcecode)
 *
 * Java version:
 *    Glenn Sanson <glenn.sanson at free.fr>
 *      (whole Java sourcecode, including JIGA classes
 *             http://glenn.sanson.free.fr/jiga/)
 *
 * Android port:
 *    Pawel Aleksander Fedorynski <pfedor@fuw.edu.pl>
 *    Eric Fortin <videogameboy76 at yahoo.com>
 *    Copyright (c) Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 */

package org.jfedor.frozenbubble;

import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Bundle;

public abstract class Sprite {
  public static int TYPE_BUBBLE = 1;
  public static int TYPE_IMAGE = 2;
  public static int TYPE_LAUNCH_BUBBLE = 3;
  public static int TYPE_PENGUIN = 4;

  private Rect spriteArea;
  private int saved_id;

  /*
   * Smooth motion.  The game logic runs in fixed 40 ms steps, but on
   * the GPU path the screen is redrawn at its refresh rate, with each
   * sprite drawn between where it was at the start of the last step
   * and where it is now.  sAlpha is that position: 0 = previous, 1 =
   * current.  The CPU path always draws with sAlpha = 1.
   */
  private static float sAlpha = 1f;
  /*
   * Scale applied while drawing bitmaps.  1 when bitmaps are pre-scaled
   * to the screen (CPU path); the display scale when they are the
   * original small images and the GPU scales them as they are drawn.
   */
  private static double sBitmapScale = 1.0;
  private static final Paint FILTER_PAINT = new Paint(Paint.FILTER_BITMAP_FLAG);
  private static final RectF sDst = new RectF(); // drawing is single-threaded
  /* Moves larger than this in one step (game units) are drawn as jumps. */
  protected static final double MAX_INTERP_JUMP = 48;

  private int     prevLeft;
  private int     prevTop;
  private boolean hasPrev = false;

  /** How far through the current game step this frame is (0 to 1). */
  public static float interpolationAlpha() {
    return sAlpha;
  }

  public static void setInterpolation(float alpha) {
    sAlpha = (alpha < 0f) ? 0f : ((alpha > 1f) ? 1f : alpha);
  }

  public static void setBitmapScale(double scale) {
    sBitmapScale = scale;
  }

  /**
   * Called at the start of every logic step, before the game logic
   * runs.  Remembers the position to interpolate from; subclasses also
   * advance per-step animations here.
   */
  public void onTick() {
    prevLeft = spriteArea.left;
    prevTop  = spriteArea.top;
    hasPrev  = true;
  }

  /**
   * @return a value between <code>from</code> and <code>to</code> for
   * the current interpolation position.
   */
  protected static double lerp(double from, double to) {
    return from + ((to - from) * sAlpha);
  }

  /**
   * @return <code>true</code> if a move by (dx, dy) during one step
   * should be drawn as smooth motion rather than a jump.
   */
  protected static boolean smoothMove(double dx, double dy) {
    return (sAlpha < 1f) &&
        (Math.abs(dx) <= MAX_INTERP_JUMP) && (Math.abs(dy) <= MAX_INTERP_JUMP);
  }

  /** @return the X position to draw at this frame (game units). */
  public float getDrawX() {
    if (hasPrev && smoothMove(spriteArea.left - prevLeft,
                              spriteArea.top  - prevTop)) {
      return (float) lerp(prevLeft, spriteArea.left);
    }
    return spriteArea.left;
  }

  /** @return the Y position to draw at this frame (game units). */
  public float getDrawY() {
    if (hasPrev && smoothMove(spriteArea.left - prevLeft,
                              spriteArea.top  - prevTop)) {
      return (float) lerp(prevTop, spriteArea.top);
    }
    return spriteArea.top;
  }

  /**
   * Draw a bitmap with its top-left corner at a screen position,
   * scaling it on the GPU when bitmaps are not pre-scaled.
   */
  public static void blit(Canvas c, Bitmap bmp, float px, float py) {
    blit(c, bmp, px, py, 1f);
  }

  /**
   * @param assetScale - how much larger the bitmap is than its size in
   * game units (4 for the HD bubbles), so it is drawn that much smaller.
   */
  public static void blit(Canvas c, Bitmap bmp, float px, float py,
                          float assetScale) {
    if ((sBitmapScale == 1.0) && (assetScale == 1f)) {
      c.drawBitmap(bmp, px, py, null);
      return;
    }
    float s = (float) (sBitmapScale / assetScale);
    sDst.set(px, py, px + (bmp.getWidth() * s), py + (bmp.getHeight() * s));
    c.drawBitmap(bmp, null, sDst, FILTER_PAINT);
  }

  public Sprite(Rect spriteArea) {
    this.spriteArea = spriteArea;
    saved_id = -1;
  }

  public void saveState(Bundle map, Vector<Sprite> saved_sprites, int id) {
    if (saved_id != -1) {
      return;
    }
    saved_id = saved_sprites.size();
    saved_sprites.addElement(this);
    map.putInt(String.format("%d-%d-left", id, saved_id), spriteArea.left);
    map.putInt(String.format("%d-%d-right", id, saved_id), spriteArea.right);
    map.putInt(String.format("%d-%d-top", id, saved_id), spriteArea.top);
    map.putInt(String.format("%d-%d-bottom", id, saved_id), spriteArea.bottom);
    map.putInt(String.format("%d-%d-type", id, saved_id), getTypeId());
  }

  public final int getSavedId() {
    return saved_id;
  }

  public final void clearSavedId() {
    saved_id = -1;
  }

  public abstract int getTypeId();

  public void changeSpriteArea(Rect newArea) {
    spriteArea = newArea;
  }

  public final void relativeMove(Point p) {
    spriteArea = new Rect(spriteArea);
    spriteArea.offset(p.x, p.y);
  }

  public final void relativeMove(int x, int y) {
    spriteArea = new Rect(spriteArea);
    spriteArea.offset(x, y);
  }

  public final void absoluteMove(Point p) {
    spriteArea = new Rect(spriteArea);
    spriteArea.offsetTo(p.x, p.y);
  }

  public final Point getSpritePosition() {
    return new Point(spriteArea.left, spriteArea.top);
  }

  public final Rect getSpriteArea() {
    return spriteArea;
  }

  public static void drawImage(BmpWrap image, int x, int y,
                               Canvas c, double scale, int dx, int dy) {
    drawImage(image, (float) x, (float) y, c, scale, dx, dy);
  }

  public static void drawImage(BmpWrap image, float x, float y,
                               Canvas c, double scale, int dx, int dy) {
    blit(c, image.bmp, (float)(x * scale + dx), (float)(y * scale + dy),
         image.assetScale);
  }

  private static final Paint ALPHA_PAINT = new Paint(Paint.FILTER_BITMAP_FLAG);

  /**
   * Draw an image partly transparent.
   * @param alpha - 0 (invisible) to 255 (solid).
   */
  public static void drawImageAlpha(BmpWrap image, float x, float y,
                                    Canvas c, double scale, int dx, int dy,
                                    int alpha) {
    if (alpha >= 255) {
      drawImage(image, x, y, c, scale, dx, dy);
      return;
    }
    if (alpha <= 0) {
      return;
    }
    Bitmap bmp = image.bmp;
    float px = (float)(x * scale + dx);
    float py = (float)(y * scale + dy);
    float s  = (float)(sBitmapScale / image.assetScale);
    ALPHA_PAINT.setAlpha(alpha);
    sDst.set(px, py, px + (bmp.getWidth() * s), py + (bmp.getHeight() * s));
    c.drawBitmap(bmp, null, sDst, ALPHA_PAINT);
  }

  public static void drawImageClipped(BmpWrap image, int x, int y, Rect clipr,
                                      Canvas c, double scale, int dx, int dy) {
    // Android 9+: save flags and Region.Op.REPLACE are no longer allowed.
    // Intersecting inside save()/restore() gives the same result.
    c.save();
    c.clipRect((float)(clipr.left * scale + dx),
               (float)(clipr.top * scale + dy),
               (float)(clipr.right * scale + dx),
               (float)(clipr.bottom * scale + dy));
    blit(c, image.bmp, (float)(x * scale + dx), (float)(y * scale + dy),
         image.assetScale);
    c.restore();
  }

  public abstract void paint(Canvas c, double scale, int dx, int dy);
}
