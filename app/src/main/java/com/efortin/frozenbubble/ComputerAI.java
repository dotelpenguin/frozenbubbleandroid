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

package com.efortin.frozenbubble;

import org.gsanson.frozenbubble.Freile;
import org.gsanson.frozenbubble.Freile.eventEnum;
import org.jfedor.frozenbubble.FrozenGame;
import org.jfedor.frozenbubble.GameScreen.gameEnum;

import android.os.Looper;
import android.view.KeyEvent;

public class ComputerAI extends Thread implements Freile.OpponentListener {
  private int action;
  private boolean running;
  private FrozenGame myFrozenGame;
  private Freile cpuOpponent;
  private VirtualInput myPlayerInput;
  /* CPU skill level: Freile.SKILL_EASY, SKILL_NORMAL or SKILL_HARD. */
  private final int skill;
  private final java.util.Random random = new java.util.Random();

  /** Options key holding the CPU skill chosen for Player vs. CPU. */
  public static final String KEY_CPU_SKILL = "cpu_skill_option";

  /** @return the CPU skill last chosen (Normal by default). */
  public static int getSkill(android.content.Context context) {
    return android.preference.PreferenceManager
        .getDefaultSharedPreferences(context)
        .getInt(KEY_CPU_SKILL, Freile.SKILL_NORMAL);
  }

  public static void setSkill(android.content.Context context, int skill) {
    android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        .edit().putInt(KEY_CPU_SKILL, skill).apply();
  }

  /**
   * Pause before the CPU starts lining up each shot, in milliseconds.
   * Easy reacts slowly and unevenly, Normal a little slower than Hard;
   * Hard and Expert react immediately.
   */
  private long reactionDelay() {
    switch (skill) {
      case Freile.SKILL_EASY:
        return 900 + random.nextInt(700);
      case Freile.SKILL_NORMAL:
        return 300 + random.nextInt(250);
      default:
        return 0;
    }
  }

  /**
   * Game AI thread class constructor.
   * @param gameRef - reference used to access game information for
   * this player.
   */
  public ComputerAI(FrozenGame gameRef, VirtualInput inputRef) {
    this(gameRef, inputRef, Freile.SKILL_HARD);
  }

  public ComputerAI(FrozenGame gameRef, VirtualInput inputRef, int skill) {
    this.skill = skill;
    myFrozenGame = gameRef;
    myPlayerInput = inputRef;
    cpuOpponent = new Freile(myFrozenGame.getGrid(), skill);
    cpuOpponent.setOpponentListener(this);
    action = 0;
    running = true;
  }

  public void cleanUp() {
    cpuOpponent.stopThread();
    cpuOpponent = null;
    myFrozenGame = null;
  }

  /**
   * The parent must use this method to clear the bubble launch action,
   * because this class does not know when to clear it.
   */
  public void clearAction() {
    if ((action == KeyEvent.KEYCODE_DPAD_UP) ||
        (action == KeyEvent.KEYCODE_DPAD_DOWN))
      action = 0;

    synchronized(this) {
      this.notify();
    }
  }

  /**
   * Convert the supplied value to a normalized launcher position or
   * launcher angle as desired.
   * @param isAngle - if <code>true</code>, convert to position from
   * angle.  Otherwise convert to angle from position.
   * @param value - the value to convert.
   * @return The converted value.
   */
  private double convert(boolean isAngle, double value) {
    double result;

    if (isAngle) {
        result = (value - Freile.MIN_LAUNCHER) /
                 (Freile.MAX_LAUNCHER - Freile.MIN_LAUNCHER);
        result = FrozenGame.MIN_LAUNCH_DIRECTION +
                 (result * (FrozenGame.MAX_LAUNCH_DIRECTION -
                            FrozenGame.MIN_LAUNCH_DIRECTION));
    }
    else {
      result = ((double)(value - FrozenGame.MIN_LAUNCH_DIRECTION)) /
               ((double)(FrozenGame.MAX_LAUNCH_DIRECTION -
                         FrozenGame.MIN_LAUNCH_DIRECTION));
      result = Freile.MIN_LAUNCHER +
               (result * (Freile.MAX_LAUNCHER - Freile.MIN_LAUNCHER));
    }
    return result;
  }

  /**
   * Return the current state of the opponent action.  When the AI has
   * generated the next action, the action is set to a non-zero value.
   * @return returns the value of the CPU opponent action.
   */
  public int getAction() {
    return action;
  }

  public void onOpponentEvent(eventEnum event) {
    switch (event) {
      case DONE_COMPUTING:
        synchronized(this) {
          this.notify();
        }
        break;

      default:
        break;
    }
  }

  @Override
  public void run() {
    Looper.prepare();
    while(running) {
      try {
        /*
         * Compute the next CPU action.
         */
        if (running && (myFrozenGame != null) && !cpuOpponent.isComputing() &&
            (myFrozenGame.getGameResult() == gameEnum.PLAYING))
          cpuOpponent.compute(myFrozenGame.getCurrentColor(),
                              myFrozenGame.getNextColor(),
                              myFrozenGame.getCompressorSteps());

        /*
         * Only fire if the game state permits, and the last virtual
         * opponent action has been processed.
         */
        if (running && (myFrozenGame != null) && myFrozenGame.getOkToFire() &&
            (action != KeyEvent.KEYCODE_DPAD_UP)) {
          while (running && cpuOpponent.isComputing()) {
            synchronized(this) {
              wait();
            }
          }

          /*
           * Initialize a timeout interval to force a bubble launch if
           * the CPU opponent takes too long to compute an action.
           */
          long delay = reactionDelay();
          if (running && (delay > 0)) {
            sleep(delay);
          }
          long timeout = System.currentTimeMillis() + 10000;

          /*
           * While the current action is to aim the launcher, keep
           * pushing the directional aim command.
           */
          int actionNew = 0;
          while (running && (myFrozenGame != null) &&
                 (actionNew != KeyEvent.KEYCODE_DPAD_UP) &&
                 (System.currentTimeMillis() < timeout)) {
            actionNew = cpuOpponent.
                        getAction(convert(false, myFrozenGame.getPosition()));

            if (actionNew != KeyEvent.KEYCODE_DPAD_UP) {
              action = actionNew;
              myPlayerInput.setAction(action, false);
            }

            synchronized(this) {
              wait();
            }
          }

          /*
           * Set the launch direction to be as accurate as possible.
           */
          if (running && (myFrozenGame != null) &&
              myFrozenGame.getOkToFire()) {
            myFrozenGame.
            setPosition(convert(true, cpuOpponent.getExactDirection(0)));
            action = actionNew;
            myPlayerInput.setAction(action, false);
          }
        }

        if (running) {
          synchronized(this) {
            wait();
          }
        }
      } catch (InterruptedException e) {
      } finally {
      }
    }
    cleanUp();
  }

  /**
   * Stop the thread <code>run()</code> execution.
   * <p>Interrupt the thread when it is suspended via
   * <code>wait()</code>.
   */
  public void stopThread() {
    running = false;

    synchronized(this) {
      this.notify();
    }
  }
}
