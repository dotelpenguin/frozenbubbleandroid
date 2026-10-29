/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package org.jfedor.frozenbubble;

import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;

/**
 * Optional bubble and screen effects, each switched on or off in
 * Options &rarr; Effects.
 * <p>Effects only change what is drawn (and felt), never the game: they
 * keep their own random numbers, so network games stay in step, and
 * nothing here is saved with the game.
 * <p>One instance belongs to each player's {@link FrozenGame}; the
 * settings, screen shake and vibration are shared.
 */
public class Effects {

  /*
   * Settings keys and defaults.
   */
  public static final String KEY_POP     = "effect_pop_sparkles";
  public static final String KEY_FADE    = "effect_falling_fade";
  public static final String KEY_WOBBLE  = "effect_impact_wobble";
  public static final String KEY_SHAKE   = "effect_screen_shake";
  public static final String KEY_GUIDE   = "effect_aiming_guide";
  public static final String KEY_TRAIL   = "effect_shot_trail";
  public static final String KEY_VIBRATE = "effect_vibration";

  public static final boolean DEF_POP     = true;
  public static final boolean DEF_FADE    = true;
  public static final boolean DEF_WOBBLE  = true;
  public static final boolean DEF_SHAKE   = false;
  public static final boolean DEF_GUIDE   = false;
  public static final boolean DEF_TRAIL   = false;
  public static final boolean DEF_VIBRATE = false;

  static volatile boolean popSparkles  = DEF_POP;
  static volatile boolean fallingFade  = DEF_FADE;
  static volatile boolean impactWobble = DEF_WOBBLE;
  static volatile boolean screenShake  = DEF_SHAKE;
  static volatile boolean aimingGuide  = DEF_GUIDE;
  static volatile boolean shotTrail    = DEF_TRAIL;
  static volatile boolean vibration    = DEF_VIBRATE;

  private static Vibrator sVibrator = null;

  /** Read the effect settings and get the vibrator. */
  public static void init(Context context) {
    if (sVibrator == null) {
      try {
        sVibrator = (Vibrator) context.getApplicationContext()
            .getSystemService(Context.VIBRATOR_SERVICE);
      } catch (RuntimeException e) {
        sVibrator = null;
      }
    }
    loadSettings(context);
  }

  /** <code>true</code> when GPU Rendering is on (and available). */
  public static boolean gpuRendering(Context context) {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(GameView.KEY_GPU_RENDERING, true);
  }

  /** <code>false</code> with Point To Shoot, where there's no aiming. */
  public static boolean aimingUsed(Context context) {
    String mode = PreferenceManager.getDefaultSharedPreferences(context)
        .getString("targeting_option",
                   Integer.toString(FrozenBubble.POINT_TO_SHOOT));
    return !Integer.toString(FrozenBubble.POINT_TO_SHOOT).equals(mode);
  }

  /** Re-read the effect settings (after a change in Options). */
  public static void loadSettings(Context context) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    /*
     * The advanced effects need GPU Rendering (they rely on its smooth,
     * scaled drawing); the aiming guide is no use with Point To Shoot.
     * Their switches keep their setting for when those come back.
     */
    boolean gpu = gpuRendering(context);
    popSparkles  = gpu && sp.getBoolean(KEY_POP,     DEF_POP);
    fallingFade  = gpu && sp.getBoolean(KEY_FADE,    DEF_FADE);
    impactWobble = gpu && sp.getBoolean(KEY_WOBBLE,  DEF_WOBBLE);
    screenShake  = gpu && sp.getBoolean(KEY_SHAKE,   DEF_SHAKE);
    aimingGuide  = aimingUsed(context) && sp.getBoolean(KEY_GUIDE, DEF_GUIDE);
    shotTrail    = gpu && sp.getBoolean(KEY_TRAIL,   DEF_TRAIL);
    vibration    = sp.getBoolean(KEY_VIBRATE, DEF_VIBRATE);
    if (!screenShake) {
      sShakeTicks = 0;
    }
  }

  /*
   * Bubble colours for the pop sparkles, in bubble order (sampled from
   * the bubble art; black and white are lightened so they show).
   */
  private static final int[] BUBBLE_COLORS = {
    0xFF8A8078, 0xFFF2F2F2, 0xFF5A6CFF, 0xFF40E870,
    0xFFF7E036, 0xFFC940EA, 0xFFE0303A, 0xFFF07A20,
  };

  /*
   * Screen shake (shared: the whole screen shakes).
   */
  private static final int SHAKE_TICKS = 8;
  private static volatile int    sShakeTicks = 0;
  private static volatile double sShakeAmp   = 0;
  private static int sShakeClock = 0;

  /** Shake the screen; a bigger shake replaces a smaller one. */
  static void shake(double amplitude) {
    if (!screenShake) {
      return;
    }
    double current = (sShakeTicks > 0) ?
        sShakeAmp * sShakeTicks / SHAKE_TICKS : 0;
    if (amplitude >= current) {
      sShakeAmp   = amplitude;
      sShakeTicks = SHAKE_TICKS;
    }
  }

  /** Advance the screen shake by one game step. */
  static void tickShake() {
    sShakeClock++;
    if (sShakeTicks > 0) {
      sShakeTicks--;
    }
  }

  /**
   * The current shake offset in game pixels, or 0.  <code>axis</code> is
   * 0 for x and 1 for y.
   */
  static float shakeOffset(int axis) {
    int ticks = sShakeTicks;
    if (!screenShake || (ticks <= 0)) {
      return 0f;
    }
    double t     = sShakeClock + Sprite.interpolationAlpha();
    double decay = (ticks - Sprite.interpolationAlpha()) / SHAKE_TICKS;
    if (decay < 0) {
      decay = 0;
    }
    double wave = (axis == 0) ? Math.sin(t * 2.9) : Math.cos(t * 2.3);
    return (float) (sShakeAmp * decay * wave);
  }

  /** The largest shake, in game pixels (for covering the edges). */
  static final float MAX_SHAKE = 5f;

  /*
   * Vibration (shared).
   */
  static void vibrate(int ms, int amplitude) {
    if (!vibration || (sVibrator == null)) {
      return;
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        sVibrator.vibrate(VibrationEffect.createOneShot(ms,
            Math.max(1, Math.min(255, amplitude))));
      }
      else {
        sVibrator.vibrate(ms);
      }
    } catch (RuntimeException e) {
      // No vibrator, or not allowed: just skip it.
    }
  }

  /*
   * Per-player state.
   */
  private final FrozenGame game;
  /** Only the player on this phone gets the guide, shake and vibration. */
  private final boolean    local;
  private final Random     random = new Random();

  /* Pop sparkles. */
  private static final int MAX_PARTICLES  = 160;
  private static final int PARTICLE_TICKS = 14;
  private final float[] px  = new float[MAX_PARTICLES];
  private final float[] py  = new float[MAX_PARTICLES];
  private final float[] ppx = new float[MAX_PARTICLES];
  private final float[] ppy = new float[MAX_PARTICLES];
  private final float[] vx  = new float[MAX_PARTICLES];
  private final float[] vy  = new float[MAX_PARTICLES];
  private final int[]   life  = new int[MAX_PARTICLES];
  private final int[]   color = new int[MAX_PARTICLES];

  /* Pop rings: a short white ring where each bubble burst. */
  private static final int MAX_RINGS  = 48;
  private static final int RING_TICKS = 6;
  private final float[] rx    = new float[MAX_RINGS];
  private final float[] ry    = new float[MAX_RINGS];
  private final int[]   rlife = new int[MAX_RINGS];

  /* Shot trail: where the moving bubble was on the last few steps. */
  private static final int TRAIL = 5;
  private final int[] trailX = new int[TRAIL];
  private final int[] trailY = new int[TRAIL];
  private int trailCount = 0;
  private BubbleSprite trailBubble = null;

  private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  Effects(FrozenGame game, boolean local) {
    this.game  = game;
    this.local = local;
  }

  /*
   * Game events.
   */

  /** A group of bubbles popped, and some others were cut loose. */
  void onPop(java.util.Vector<Sprite> popped, int detached) {
    if (popSparkles) {
      for (int i = 0; i < popped.size(); i++) {
        BubbleSprite b = (BubbleSprite) popped.elementAt(i);
        float cx = b.getSpriteArea().left + 16f;
        float cy = b.getSpriteArea().top  + 16f;
        addRing(cx, cy);
        int c = BUBBLE_COLORS[Math.max(0, Math.min(BUBBLE_COLORS.length - 1,
                                                   b.getColor()))];
        for (int k = 0; k < 7; k++) {
          addParticle(cx, cy, c);
        }
      }
    }
    if (local) {
      int total = popped.size() + detached;
      if (total >= 5) {
        shake(Math.min(MAX_SHAKE, 1.5 + (total - 5) * 0.5));
      }
      if (total >= 6) {
        vibrate(35, 200);
      }
      else {
        vibrate(15, 90);
      }
    }
  }

  /** The moving bubble stuck to the others. */
  void onImpact(BubbleSprite landed, java.util.Vector<BubbleSprite> neighbors,
                double moveX, double moveY) {
    if (!impactWobble) {
      return;
    }
    double len = Math.sqrt((moveX * moveX) + (moveY * moveY));
    if (len > 0) {
      landed.nudge(moveX / len * 3.0, moveY / len * 3.0);
    }
    int lx = landed.getSpriteArea().left;
    int ly = landed.getSpriteArea().top;
    for (int i = 0; i < neighbors.size(); i++) {
      BubbleSprite n = neighbors.elementAt(i);
      if ((n == null) || (n == landed)) {
        continue;
      }
      double dx = n.getSpriteArea().left - lx;
      double dy = n.getSpriteArea().top  - ly;
      double d  = Math.sqrt((dx * dx) + (dy * dy));
      if (d > 0) {
        n.nudge(dx / d * 2.0, dy / d * 2.0);
      }
    }
  }

  /** The ceiling (compressor) came down a row. */
  void onCeilingDown() {
    if (local) {
      shake(3.0);
      vibrate(25, 150);
    }
  }

  /** This player lost. */
  void onLost() {
    if (local) {
      shake(MAX_SHAKE);
      vibrate(120, 255);
    }
  }

  private void addParticle(float x, float y, int c) {
    for (int i = 0; i < MAX_PARTICLES; i++) {
      if (life[i] <= 0) {
        double angle = random.nextDouble() * Math.PI * 2;
        double speed = 2.0 + random.nextDouble() * 4.0;
        px[i] = ppx[i] = x;
        py[i] = ppy[i] = y;
        vx[i] = (float) (Math.cos(angle) * speed);
        vy[i] = (float) (Math.sin(angle) * speed) - 2f;
        life[i]  = PARTICLE_TICKS - random.nextInt(4);
        color[i] = c;
        return;
      }
    }
  }

  private void addRing(float x, float y) {
    for (int i = 0; i < MAX_RINGS; i++) {
      if (rlife[i] <= 0) {
        rx[i] = x;
        ry[i] = y;
        rlife[i] = RING_TICKS;
        return;
      }
    }
  }

  /** Advance the effects by one game step (every 40 ms). */
  void tick() {
    for (int i = 0; i < MAX_PARTICLES; i++) {
      if (life[i] > 0) {
        ppx[i] = px[i];
        ppy[i] = py[i];
        px[i] += vx[i];
        py[i] += vy[i];
        vy[i] += 0.6f;   // gravity
        vx[i] *= 0.92f;  // air
        life[i]--;
      }
    }
    for (int i = 0; i < MAX_RINGS; i++) {
      if (rlife[i] > 0) {
        rlife[i]--;
      }
    }
    // Shot trail: remember where the moving bubble has been.
    BubbleSprite moving = game.getMovingBubble();
    if (!shotTrail || (moving == null) || (moving != trailBubble)) {
      trailCount  = 0;
      trailBubble = moving;
    }
    if (shotTrail && (moving != null)) {
      for (int i = TRAIL - 1; i > 0; i--) {
        trailX[i] = trailX[i - 1];
        trailY[i] = trailY[i - 1];
      }
      trailX[0] = moving.getSpriteArea().left;
      trailY[0] = moving.getSpriteArea().top;
      if (trailCount < TRAIL) {
        trailCount++;
      }
    }
  }

  /*
   * Drawing.  Game coordinates map to the screen as x * scale + dx.
   */

  /** Effects drawn under the bubbles: the aiming guide and shot trail. */
  void paintUnder(Canvas c, double scale, int dx, int dy) {
    if (aimingGuide && local) {
      paintGuide(c, scale, dx, dy);
    }
    if (shotTrail && (trailCount > 1) && (trailBubble != null) &&
        (trailBubble == game.getMovingBubble())) {
      BmpWrap face = trailBubble.currentFace();
      // Skip the newest point: the bubble itself is drawn there.
      for (int i = 1; i < trailCount; i++) {
        int alpha = 90 - (i * 18);
        if (alpha > 0) {
          Sprite.drawImageAlpha(face, trailX[i], trailY[i], c, scale, dx, dy,
                                alpha);
        }
      }
    }
  }

  /** Effects drawn over the bubbles: pop sparkles and rings. */
  void paintOver(Canvas c, double scale, int dx, int dy) {
    for (int i = 0; i < MAX_RINGS; i++) {
      if (rlife[i] > 0) {
        float t = (RING_TICKS - rlife[i] + Sprite.interpolationAlpha()) /
                  RING_TICKS;
        if (t > 1f) {
          t = 1f;
        }
        sPaint.setStyle(Paint.Style.STROKE);
        sPaint.setStrokeWidth((float) (2.5 * scale * (1f - t) + 0.5));
        sPaint.setColor(0xFFFFFFFF);
        sPaint.setAlpha((int) (200 * (1f - t)));
        c.drawCircle((float) (rx[i] * scale + dx), (float) (ry[i] * scale + dy),
                     (float) ((14 + 12 * t) * scale), sPaint);
      }
    }
    sPaint.setStyle(Paint.Style.FILL);
    for (int i = 0; i < MAX_PARTICLES; i++) {
      if (life[i] > 0) {
        float x = (float) Sprite.lerp(ppx[i], px[i]);
        float y = (float) Sprite.lerp(ppy[i], py[i]);
        float f = (float) life[i] / PARTICLE_TICKS;
        sPaint.setColor(color[i]);
        sPaint.setAlpha((int) (255 * Math.min(1f, f * 1.5f)));
        c.drawCircle((float) (x * scale + dx), (float) (y * scale + dy),
                     (float) ((1.2 + 2.3 * f) * scale), sPaint);
      }
    }
  }

  /*
   * Aiming guide: dots along the path the next bubble would take,
   * bouncing off the walls, up to where it would first touch a bubble
   * or the ceiling.  It follows the game's own movement rules.
   */
  private static final double GUIDE_STEP    = 2.0;
  private static final double GUIDE_SPACING = 18.0;
  private static final int    GUIDE_MAX_LEN = 900;

  private void paintGuide(Canvas c, double scale, int dx, int dy) {
    if (!game.canAim()) {
      return;
    }
    double dir = game.getPosition();
    double mx  = -Math.cos(dir * Math.PI / 40.);
    double my  = -Math.sin(dir * Math.PI / 40.);
    double x = 302, y = 390;          // the launch position (left, top)
    double ceiling = 44. + game.getMoveDown();
    BubbleSprite[][] grid = game.getGrid();
    double travelled = 0, nextDot = GUIDE_SPACING * 1.5;
    sPaint.setStyle(Paint.Style.FILL);
    while (travelled < GUIDE_MAX_LEN) {
      x += mx * GUIDE_STEP;
      y += my * GUIDE_STEP;
      if (x >= 414.) {
        x  = 414. - (x - 414.);
        mx = -mx;
      }
      else if (x <= 190.) {
        x  = 190. + (190. - x);
        mx = -mx;
      }
      travelled += GUIDE_STEP;
      if ((y < ceiling) || touchesBubble(grid, x, y)) {
        break;
      }
      if (travelled >= nextDot) {
        nextDot += GUIDE_SPACING;
        float fade = (float) (1.0 - (travelled / GUIDE_MAX_LEN) * 0.6);
        float cx = (float) ((x + 16) * scale + dx);
        float cy = (float) ((y + 16) * scale + dy);
        sPaint.setColor(0xFF000000);
        sPaint.setAlpha((int) (110 * fade));
        c.drawCircle(cx, cy, (float) (3.4 * scale), sPaint);
        sPaint.setColor(0xFFFFFFFF);
        sPaint.setAlpha((int) (230 * fade));
        c.drawCircle(cx, cy, (float) (2.2 * scale), sPaint);
      }
    }
  }

  private static boolean touchesBubble(BubbleSprite[][] grid, double x,
                                       double y) {
    for (int i = 0; i < grid.length; i++) {
      for (int j = 0; j < grid[i].length; j++) {
        BubbleSprite b = grid[i][j];
        if (b != null) {
          double ddx = b.getSpriteArea().left - x;
          double ddy = b.getSpriteArea().top  - y;
          if (((ddx * ddx) + (ddy * ddy)) < BubbleSprite.minDistance) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
