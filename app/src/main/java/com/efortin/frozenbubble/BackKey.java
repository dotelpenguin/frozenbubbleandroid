/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.KeyEvent;
import android.window.OnBackInvokedDispatcher;

/**
 * The Back key (and back gesture).
 * <p>From Android 13 the system sends Back to an
 * <code>OnBackInvokedCallback</code> instead of
 * <code>onKeyDown(KEYCODE_BACK)</code>; apps targeting Android 16 can
 * only keep the old route with a manifest opt-out Google is removing.
 * So each screen registers its Back action here, and its
 * <code>onKeyDown</code> handles <code>KEYCODE_BACK</code> only on
 * older Android versions (see {@link #isLegacy(int)}), so Back is
 * never handled twice.
 */
public final class BackKey {
  private BackKey() {}

  /** Run <code>onBack</code> when Back is pressed (Android 13 and later). */
  public static void handle(Activity activity, Runnable onBack) {
    if (Build.VERSION.SDK_INT >= 33) {
      register(activity, onBack);
    }
  }

  @TargetApi(33)
  private static void register(Activity activity, final Runnable onBack) {
    activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
        OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBack::run);
  }

  /**
   * Whether this key is Back delivered the old way, to
   * <code>onKeyDown</code> (Android 12 and earlier).
   */
  public static boolean isLegacy(int keyCode) {
    return (keyCode == KeyEvent.KEYCODE_BACK) && (Build.VERSION.SDK_INT < 33);
  }
}
