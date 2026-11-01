/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/**
 * Modern system bar handling.
 *
 * <p>Apps targeting Android 15+ are always drawn edge-to-edge, behind the
 * status and navigation bars. The old FLAG_FULLSCREEN window flag no
 * longer hides the navigation bar, so on Android 11+ this uses
 * WindowInsetsController for true immersive fullscreen, and pads the
 * content view by whatever system bars or display cutout remain visible
 * so the game is never drawn underneath them.
 */
public final class SystemUi {
  private SystemUi() {}

  public static void apply(Activity activity, boolean fullscreen) {
    Window window = activity.getWindow();
    legacyFlags(window, fullscreen);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      return; // Android 7-10: the window flags above do it.
    }
    View content = window.findViewById(android.R.id.content);
    if (content != null) {
      content.setOnApplyWindowInsetsListener((v, insets) -> {
        android.graphics.Insets bars = insets.getInsets(
            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
        return WindowInsets.CONSUMED;
      });
      content.requestApplyInsets();
    }

    WindowInsetsController controller = window.getInsetsController();
    if (controller == null) {
      return;
    }
    if (fullscreen) {
      controller.setSystemBarsBehavior(
          WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
      controller.hide(WindowInsets.Type.systemBars());
    }
    else {
      controller.show(WindowInsets.Type.systemBars());
    }
  }

  /**
   * The window flags that hide the status bar on Android 7-10 (and that
   * the screens set on every version before this class did it).  They
   * are deprecated from Android 11, where the insets controller above
   * does the work; they are kept here, in one place, for the older
   * versions.
   */
  @SuppressWarnings("deprecation")
  private static void legacyFlags(Window window, boolean fullscreen) {
    final int flagFs   = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    final int flagNoFs = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    if (fullscreen) {
      window.addFlags(flagFs);
      window.clearFlags(flagNoFs);
    }
    else {
      window.clearFlags(flagFs);
      window.addFlags(flagNoFs);
    }
  }
}
