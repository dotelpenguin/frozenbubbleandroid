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

import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.GameView;
import org.jfedor.frozenbubble.R;
import org.jfedor.frozenbubble.SoundManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.peculiargames.andmodplug.PlayerThread;

public class HomeScreen extends Activity {
  /*
   * Provide unique IDs for the views associated with the relative
   * layout.  These are used to define relative view layout positions
   * with respect to other views in the layout.
   *
   * These IDs are generated automatically if using an XML layout, but
   * this object implements a RelativeLayout that is constructed purely
   * programmatically.
   */
  private final static int SCREEN_ID = 100;
  private final static int BACK_ID   = 101;
  private final static int BTN1_ID   = 102;
  private final static int BTN2_ID   = 103;
  private final static int BTN3_ID   = 104;
  private final static int BTN4_ID   = 105;
  private final static int BTN5_ID   = 106;
  private final static int BTN6_ID   = 107;
  private final static int BTN7_ID   = 108;
  private final static int BTN8_ID   = 109;
  private final static int BTN9_ID   = 110;
  private final static int BTN10_ID  = 111;
  private final static int BTN11_ID  = 112;
  private final static int BTN12_ID  = 113;
  private final static int BTN13_ID  = 114;

  private static int buttonSelPage1 = BTN1_ID;
  private static int buttonSelPage2 = BTN5_ID;
  private static int buttonSelPage3 = BTN9_ID;
  private static int buttonSelPage4 = BTN11_ID;
  private static int pageSelected   = 1;

  private boolean        finished      = false;
  private boolean        homeShown     = false;
  private boolean        playerSave    = false;
  private ImageView      myImageView   = null;
  private RelativeLayout myLayout      = null;
  private ModPlayer      myModPlayer   = null;
  private Preferences    myPreferences = null;
  private SoundManager   mSoundManager = null;
  private Thread         splashThread  = null;

  /**
   * Given that we are using a relative layout for the home screen in
   * order to display the background image and various buttons, this
   * function programmatically adds an on-screen back button to the
   * layout.
   */
  private void addBackButton() {
    /*
     * Construct the back button.
     */
    Button backButton = new Button(this);
    backButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        mSoundManager.playSound("stick", R.raw.stick);
        backKeyPress();
      }
    });
    backButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    /*
     * Set the back button text to the following Unicode character:
     * Anticlockwise Top Semicircle Arrow
     * http://en.wikipedia.org/wiki/Arrow_(symbol)
     */
    backButton.setText("\u21B6");
    backButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
    backButton.setMinimumHeight(0);
    backButton.setMinimumWidth(0);
    backButton.setHeight((int) backButton.getTextSize() * 2);
    backButton.setWidth((int) backButton.getTextSize() * 2);
    backButton.setTypeface(null, Typeface.BOLD);
    backButton.setBackgroundResource(R.drawable.round_button);
    backButton.setId(BACK_ID);
    backButton.setFocusable(true);
    backButton.setFocusableInTouchMode(true);
    LayoutParams myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                             LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    myParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    myParams.topMargin   = 0;
    myParams.rightMargin = 0;
    /*
     * Add view to layout.
     */
    myLayout.addView(backButton, myParams);
  }

  /**
   * Given that we are using a relative layout for the home screen in
   * order to display the background image and various buttons, this
   * function adds the buttons to the layout to provide game options to
   * the player.
   * <p>The buttons are defined in relation to one another so that when
   * using keys to navigate the buttons, the appropriate button will be
   * highlighted.
   */
  private void addGameExtrasButtons() {
    /*
     * Construct the options button.
     */
    Button optionsButton = new Button(this);
    optionsButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage4 = BTN12_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Process the button tap and start the preferences activity.
         */
        startPreferencesScreen();
      }
    });
    optionsButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        boolean result = false;
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            v.requestFocus();
            break;
          case MotionEvent.ACTION_UP:
            result = v.performClick();
            break;
          default:
            break;
        }
        return result;
      }
    });
    optionsButton.setText("Options");
    optionsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    optionsButton.setWidth((int) (optionsButton.getTextSize() * 9));
    optionsButton.setTypeface(null, Typeface.BOLD);
    optionsButton.setHorizontalFadingEdgeEnabled(true);
    optionsButton.setFadingEdgeLength(5);
    optionsButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    optionsButton.setId(BTN12_ID);
    optionsButton.setFocusable(true);
    optionsButton.setFocusableInTouchMode(true);
    LayoutParams myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                             LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.topMargin    = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(optionsButton, myParams);
    /*
     * Construct the continue button.
     */
    Button continueButton = new Button(this);
    continueButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage4 = BTN11_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Process the button tap and load the saved game.
         */
        startFrozenBubble(VirtualInput.PLAYER1, 1,
                          FrozenBubble.HUMAN,
                          FrozenBubble.LOCALE_LOCAL, false, true);
      }
    });
    continueButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        boolean result = false;
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            v.requestFocus();
            break;
          case MotionEvent.ACTION_UP:
            result = v.performClick();
            break;
          default:
            break;
        }
        return result;
      }
    });
    continueButton.setText("Saved Game");
    continueButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    continueButton.setWidth((int) (continueButton.getTextSize() * 9));
    continueButton.setTypeface(null, Typeface.BOLD);
    continueButton.setHorizontalFadingEdgeEnabled(true);
    continueButton.setFadingEdgeLength(5);
    continueButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    continueButton.setId(BTN11_ID);
    continueButton.setFocusable(true);
    continueButton.setFocusableInTouchMode(true);
    continueButton.setEnabled(playerSave);
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.ABOVE, optionsButton.getId());
    myParams.topMargin    = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(continueButton, myParams);
  }

  /**
   * Given that we are using a relative layout for the home screen in
   * order to display the background image and various buttons, this
   * function adds the buttons to the layout to provide game options to
   * the player.
   * <p>The buttons are defined in relation to one another so that when
   * using keys to navigate the buttons, the appropriate button will be
   * highlighted.
   */
  private void addHomeButtons() {
    /*
     * Construct the 2 player game button.
     */
    Button start2pGameButton = new Button(this);
    start2pGameButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage1 = BTN3_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Display the 2 player mode buttons page.
         */
        startOrAskToContinue(FrozenBubble.SAVED_MODE_2PLAYER, () ->
            displayPage(2));
      }
    });
    start2pGameButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    start2pGameButton.setText("2 Player");
    start2pGameButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    start2pGameButton.setWidth((int) (start2pGameButton.getTextSize() * 9));
    start2pGameButton.setTypeface(null, Typeface.BOLD);
    start2pGameButton.setHorizontalFadingEdgeEnabled(true);
    start2pGameButton.setFadingEdgeLength(5);
    start2pGameButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    start2pGameButton.setId(BTN3_ID);
    start2pGameButton.setFocusable(true);
    start2pGameButton.setFocusableInTouchMode(true);
    LayoutParams myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                             LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(start2pGameButton, myParams);
    /*
     * Construct the 1 player game button.
     */
    Button start1pGameButton = new Button(this);
    start1pGameButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage1 = BTN2_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Start a Puzzle game, or ask whether to continue the saved one.
         */
        startOrAskToContinue(FrozenBubble.SAVED_MODE_PUZZLE, () ->
            startFrozenBubble(VirtualInput.PLAYER1, 1,
                              FrozenBubble.HUMAN,
                              FrozenBubble.LOCALE_LOCAL, false, false));
      }
    });
    start1pGameButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    start1pGameButton.setText("Puzzle");
    start1pGameButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    start1pGameButton.setWidth((int) (start1pGameButton.getTextSize() * 9));
    start1pGameButton.setTypeface(null, Typeface.BOLD);
    start1pGameButton.setHorizontalFadingEdgeEnabled(true);
    start1pGameButton.setFadingEdgeLength(5);
    start1pGameButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    start1pGameButton.setId(BTN2_ID);
    start1pGameButton.setFocusable(true);
    start1pGameButton.setFocusableInTouchMode(true);
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.ABOVE, start2pGameButton.getId());
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(start1pGameButton, myParams);
    /*
     * Construct the 1 player arcade game button.
     */
    Button startArcadeGameButton = new Button(this);
    startArcadeGameButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage1 = BTN1_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Start an Arcade game, or ask whether to continue the saved one.
         */
        startOrAskToContinue(FrozenBubble.SAVED_MODE_ARCADE, () ->
            startFrozenBubble(VirtualInput.PLAYER1, 1,
                              FrozenBubble.HUMAN,
                              FrozenBubble.LOCALE_LOCAL, true, false));
      }
    });
    startArcadeGameButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    startArcadeGameButton.setText("Arcade");
    startArcadeGameButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    startArcadeGameButton.setWidth((int) (startArcadeGameButton.getTextSize() * 9));
    startArcadeGameButton.setTypeface(null, Typeface.BOLD);
    startArcadeGameButton.setHorizontalFadingEdgeEnabled(true);
    startArcadeGameButton.setFadingEdgeLength(5);
    startArcadeGameButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    startArcadeGameButton.setId(BTN1_ID);
    startArcadeGameButton.setFocusable(true);
    startArcadeGameButton.setFocusableInTouchMode(true);
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.ABOVE, start1pGameButton.getId());
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(startArcadeGameButton, myParams);
    /*
     * Construct the game extras button.
     */
    Button gameExtrasButton = new Button(this);
    gameExtrasButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage1 = BTN4_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Open the options screen.  (This used to open an Extras page
         * holding Options and Saved Game; saved games are now resumed
         * with Continue at the top of this menu.)
         */
        startPreferencesScreen();
      }
    });
    gameExtrasButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    gameExtrasButton.setText("Options");
    gameExtrasButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    gameExtrasButton.setWidth((int) (gameExtrasButton.getTextSize() * 9));
    gameExtrasButton.setTypeface(null, Typeface.BOLD);
    gameExtrasButton.setHorizontalFadingEdgeEnabled(true);
    gameExtrasButton.setFadingEdgeLength(5);
    gameExtrasButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    gameExtrasButton.setId(BTN4_ID);
    gameExtrasButton.setFocusable(true);
    gameExtrasButton.setFocusableInTouchMode(true);
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.BELOW, start2pGameButton.getId());
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(gameExtrasButton, myParams);
    /*
     * With a saved game, offer to continue it at the top of the menu.
     */
    // Saved games are offered by each mode's button (see
    // startOrAskToContinue()), not by a separate Continue button.
  }

  /**
   * Start a new game of a mode, or, if that mode has a saved game, ask
   * first: Continue / New Game / Back.
   * @param slot - the mode (FrozenBubble.SAVED_MODE_...).
   * @param startNew - starts a new game of the mode.
   */
  private void startOrAskToContinue(final String slot,
                                    final Runnable startNew) {
    if (!FrozenBubble.hasSavedGame(this, slot)) {
      startNew.run();
      return;
    }
    final AlertDialog dialog = new AlertDialog.Builder(HomeScreen.this).create();
    TextView title = new TextView(this);
    OptionsMenus.styleText(title, 22);
    title.setText(FrozenBubble.savedGameTitle(this, slot));
    Button cont = MenuButtons.create(this, R.string.save_continue, v -> {
      dialog.dismiss();
      mSoundManager.playSound("stick", R.raw.stick);
      startSavedGame(slot);
    });
    Button fresh = MenuButtons.create(this, R.string.save_new_game, v -> {
      dialog.dismiss();
      mSoundManager.playSound("stick", R.raw.stick);
      startNew.run();
    });
    Button back = MenuButtons.create(this, R.string.back, v -> {
      dialog.dismiss();
      mSoundManager.playSound("stick", R.raw.stick);
    });
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    box.addView(title);
    box.addView(MenuButtons.column(this, cont, fresh, back));
    dialog.setView(box);
    showOverDimmedMenu(dialog);
  }

  /*
   * Popups over the home screen (Continue / New Game, CPU skill): like
   * the in-game menus, the menu behind is hidden and the screen dimmed,
   * so only the popup's buttons show.
   */
  private static final int MENU_DIM_COLOR = 0xA6000000; // 65% black
  private View dimOverlay = null;
  private final java.util.List<View> hiddenForPopup =
      new java.util.ArrayList<View>();

  private void showOverDimmedMenu(AlertDialog dialog) {
    android.view.Window window = dialog.getWindow();
    if (window != null) {
      // No dialog box: the buttons float over the dimmed screen.
      window.setBackgroundDrawable(
          new android.graphics.drawable.ColorDrawable(
              android.graphics.Color.TRANSPARENT));
      // We dim the screen ourselves; don't let the window dim it twice.
      window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }
    setMenuDimmed(true);
    dialog.setOnDismissListener(d -> setMenuDimmed(false));
    dialog.show();
  }

  private void setMenuDimmed(boolean dimmed) {
    if (myLayout == null) {
      return;
    }
    if (dimmed) {
      // Hide the menu buttons (not the background art).
      for (int i = 0; i < myLayout.getChildCount(); i++) {
        View child = myLayout.getChildAt(i);
        if ((child != myImageView) && (child != dimOverlay) &&
            (child.getVisibility() == View.VISIBLE)) {
          child.setVisibility(View.INVISIBLE);
          hiddenForPopup.add(child);
        }
      }
      if (dimOverlay == null) {
        dimOverlay = new View(this);
        dimOverlay.setBackgroundColor(MENU_DIM_COLOR);
      }
      if (dimOverlay.getParent() == null) {
        myLayout.addView(dimOverlay, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      }
    }
    else {
      for (View child : hiddenForPopup) {
        child.setVisibility(View.VISIBLE);
      }
      hiddenForPopup.clear();
      if ((dimOverlay != null) && (dimOverlay.getParent() != null)) {
        myLayout.removeView(dimOverlay);
      }
    }
  }

  /** The mode whose saved game to continue (passed to the game). */
  private String continueSlot = null;

  private void startSavedGame(String slot) {
    continueSlot = slot;
    startFrozenBubble(VirtualInput.PLAYER1, 1,
                      FrozenBubble.HUMAN,
                      FrozenBubble.LOCALE_LOCAL, false, true);
  }


  /**
   * Given that we are using a relative layout for the home screen in
   * order to display the background image and various buttons, this
   * function adds the buttons to the layout to provide multiplayer game
   * options to the player.
   * <p>The buttons are defined in relation to one another so that when
   * using keys to navigate the buttons, the appropriate button will be
   * highlighted.
   */
  private void addMultiplayerButtons() {
    /*
     * Construct the WiFi game button.
     */
    Button startWiFiGameButton = new Button(this);
    startWiFiGameButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage2 = BTN7_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Display the player ID buttons page.
         */
        displayPage(3);
      }
    });
    startWiFiGameButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    startWiFiGameButton.setText("WiFi");
    startWiFiGameButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    startWiFiGameButton.setWidth((int) (startWiFiGameButton.getTextSize() * 9));
    startWiFiGameButton.setTypeface(null, Typeface.BOLD);
    startWiFiGameButton.setHorizontalFadingEdgeEnabled(true);
    startWiFiGameButton.setFadingEdgeLength(5);
    startWiFiGameButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    startWiFiGameButton.setId(BTN7_ID);
    startWiFiGameButton.setFocusable(true);
    startWiFiGameButton.setFocusableInTouchMode(true);
    LayoutParams myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                             LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(startWiFiGameButton, myParams);
    /*
     * Construct the Bluetooth network game button.
     */
    Button startBluetoothGameButton = new Button(this);
    startBluetoothGameButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage2 = BTN6_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        requestBluetoothThenShowDevices();
      }
    });
    startBluetoothGameButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    startBluetoothGameButton.setText("Bluetooth");
    startBluetoothGameButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    startBluetoothGameButton.setWidth((int) (startBluetoothGameButton.getTextSize() * 9));
    startBluetoothGameButton.setTypeface(null, Typeface.BOLD);
    startBluetoothGameButton.setHorizontalFadingEdgeEnabled(true);
    startBluetoothGameButton.setFadingEdgeLength(5);
    startBluetoothGameButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    startBluetoothGameButton.setId(BTN6_ID);
    startBluetoothGameButton.setFocusable(true);
    startBluetoothGameButton.setFocusableInTouchMode(true);
    /*
     * Only disable Bluetooth play on phones with no Bluetooth.  (The
     * paired device list can't be read until the permission is granted
     * on Android 12+, and the permission is requested from this button,
     * so checking the list here disabled the button for good.)
     */
    if (android.bluetooth.BluetoothAdapter.getDefaultAdapter() == null) {
      startBluetoothGameButton.setEnabled(false);
    }
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.ABOVE, startWiFiGameButton.getId());
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(startBluetoothGameButton, myParams);
    /*
     * Construct the Player vs. CPU game button.
     */
    Button startCPUGameButton = new Button(this);
    startCPUGameButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage2 = BTN5_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Ask for the CPU skill, then start the 2 player game.
         */
        showCpuSkillMenu();
      }
    });
    startCPUGameButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    startCPUGameButton.setText("Player vs. CPU");
    startCPUGameButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    startCPUGameButton.setWidth((int) (startCPUGameButton.getTextSize() * 9));
    startCPUGameButton.setTypeface(null, Typeface.BOLD);
    startCPUGameButton.setHorizontalFadingEdgeEnabled(true);
    startCPUGameButton.setFadingEdgeLength(5);
    startCPUGameButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    startCPUGameButton.setId(BTN5_ID);
    startCPUGameButton.setFocusable(true);
    startCPUGameButton.setFocusableInTouchMode(true);
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.ABOVE, startBluetoothGameButton.getId());
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(startCPUGameButton, myParams);
    /*
     * Construct the local 2 player game button.
     */
    Button startLocal2PlayerGameButton = new Button(this);
    startLocal2PlayerGameButton.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        int numGamepads = numGamepadsConnected();
        buttonSelPage2 = BTN8_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Only start a local 2 player game if at least one gamepad is
         * connected.
         */
        if (numGamepads > 0) {
          /*
           * Process the button tap and start a 2 player game.
           */
          startFrozenBubble(VirtualInput.PLAYER1, 2,
                            FrozenBubble.HUMAN,
                            FrozenBubble.LOCALE_LOCAL, false, false);
        }
        else {
          numGamepadsDialog(numGamepads);
        }
      }
    });
    startLocal2PlayerGameButton.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    startLocal2PlayerGameButton.setText("Local");
    startLocal2PlayerGameButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    startLocal2PlayerGameButton.setWidth((int) (startLocal2PlayerGameButton.getTextSize() * 9));
    startLocal2PlayerGameButton.setTypeface(null, Typeface.BOLD);
    startLocal2PlayerGameButton.setHorizontalFadingEdgeEnabled(true);
    startLocal2PlayerGameButton.setFadingEdgeLength(5);
    startLocal2PlayerGameButton.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    startLocal2PlayerGameButton.setId(BTN8_ID);
    startLocal2PlayerGameButton.setFocusable(true);
    startLocal2PlayerGameButton.setFocusableInTouchMode(true);
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.BELOW, startWiFiGameButton.getId());
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(startLocal2PlayerGameButton, myParams);
  }

  /**
   * Given that we are using a relative layout for the home screen in
   * order to display the background image and various buttons, this
   * function adds the buttons to the layout to provide player ID
   * selection options to the player.
   * <p>The buttons are defined in relation to one another so that when
   * using keys to navigate the buttons, the appropriate button will be
   * highlighted.
   */
  private void addPlayerSelectButtons() {
    /*
     * Construct the player 2 button.
     */
    Button player2Button = new Button(this);
    player2Button.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage3 = BTN10_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Process the button tap and start a 2 player game.
         */
        if (buttonSelPage2 == BTN6_ID) {
          startFrozenBubble(VirtualInput.PLAYER2, 2,
                            FrozenBubble.HUMAN,
                            FrozenBubble.LOCALE_BLUETOOTH, false, false);
        }
        else if (buttonSelPage2 == BTN7_ID) {
          startFrozenBubble(VirtualInput.PLAYER2, 2,
                            FrozenBubble.HUMAN,
                            FrozenBubble.LOCALE_WIFI, false, false);
        }
      }
    });
    player2Button.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    player2Button.setText("Player 2");
    player2Button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    player2Button.setWidth((int) (player2Button.getTextSize() * 9));
    player2Button.setTypeface(null, Typeface.BOLD);
    player2Button.setHorizontalFadingEdgeEnabled(true);
    player2Button.setFadingEdgeLength(5);
    player2Button.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    player2Button.setId(BTN10_ID);
    player2Button.setFocusable(true);
    player2Button.setFocusableInTouchMode(true);
    LayoutParams myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                             LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(player2Button, myParams);
    /*
     * Construct the player 1 button.
     */
    Button player1Button = new Button(this);
    player1Button.setOnClickListener(new Button.OnClickListener(){
      public void onClick(View v){
        buttonSelPage3 = BTN9_ID;
        mSoundManager.playSound("stick", R.raw.stick);
        /*
         * Process the button tap and start a 2 player game.
         */
        if (buttonSelPage2 == BTN6_ID) {
          startFrozenBubble(VirtualInput.PLAYER1, 2,
                            FrozenBubble.HUMAN,
                            FrozenBubble.LOCALE_BLUETOOTH, false, false);
        }
        else if (buttonSelPage2 == BTN7_ID) {
          startFrozenBubble(VirtualInput.PLAYER1, 2,
                            FrozenBubble.HUMAN,
                            FrozenBubble.LOCALE_WIFI, false, false);
        }
      }
    });
    player1Button.setOnTouchListener(new Button.OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
          v.requestFocus();
        return false;
      }
    });
    player1Button.setText("Player 1");
    player1Button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
    player1Button.setWidth((int) (player1Button.getTextSize() * 9));
    player1Button.setTypeface(null, Typeface.BOLD);
    player1Button.setHorizontalFadingEdgeEnabled(true);
    player1Button.setFadingEdgeLength(5);
    player1Button.setShadowLayer(5, 5, 5, android.graphics.Color.BLACK);
    player1Button.setId(BTN9_ID);
    player1Button.setFocusable(true);
    player1Button.setFocusableInTouchMode(true);
    myParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
    myParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    myParams.addRule(RelativeLayout.ABOVE, player2Button.getId());
    myParams.topMargin = 15;
    myParams.bottomMargin = 15;
    /*
     * Add view to layout.
     */
    myLayout.addView(player1Button, myParams);
  }

  private void backKeyPress() {
    /*
     * If the multiplayer player select page is active, go back to the
     * multiplayer game page.
     * 
     * If any other page is displayed other than the home page, go back
     * to the home page.
     *
     * Otherwise if the home page is currently displayed, exit the home
     * screen activity.
     */
    if (pageSelected == 3) {
      displayPage(2);
    }
    else if (pageSelected > 1) {
      displayPage(1);
    }
    else {
      finished = true;
      cleanUp();
      finish();
    }
  }

  private void cleanUp() {
    if (mSoundManager != null) {
      mSoundManager.cleanUp();
    }
    mSoundManager = null;
    if (myModPlayer != null) {
      myModPlayer.destroyMusicPlayer();
    }
    myModPlayer = null;
  }

  /**
   * Easy / Normal / Hard / Expert / Cancel, as bubble-style menu buttons, shown
   * when Player vs. CPU is chosen.  The choice is remembered and used by
   * the CPU (including when a saved game is continued).
   */
  private void showCpuSkillMenu() {
    final AlertDialog dialog = new AlertDialog.Builder(HomeScreen.this).create();
    final int[] labels = { R.string.cpu_easy, R.string.cpu_normal,
                           R.string.cpu_hard, R.string.cpu_expert };
    final int[] skills = { org.gsanson.frozenbubble.Freile.SKILL_EASY,
                           org.gsanson.frozenbubble.Freile.SKILL_NORMAL,
                           org.gsanson.frozenbubble.Freile.SKILL_HARD,
                           org.gsanson.frozenbubble.Freile.SKILL_EXPERT };
    Button[] buttons = new Button[labels.length + 1];
    for (int i = 0; i < labels.length; i++) {
      final int skill = skills[i];
      buttons[i] = MenuButtons.create(this, labels[i], v -> {
        dialog.dismiss();
        mSoundManager.playSound("stick", R.raw.stick);
        ComputerAI.setSkill(HomeScreen.this, skill);
        startFrozenBubble(VirtualInput.PLAYER1, 2,
                          FrozenBubble.CPU,
                          FrozenBubble.LOCALE_LOCAL, false, false);
      });
    }
    buttons[labels.length] = MenuButtons.create(this, R.string.cancel,
                                                v -> dialog.dismiss());
    dialog.setView(MenuButtons.column(this, buttons));
    showOverDimmedMenu(dialog);
  }

  private static final int REQUEST_BLUETOOTH_CONNECT = 1001;
  private static final int REQUEST_ENABLE_BLUETOOTH  = 1002;

  /**
   * Android 12+ requires the BLUETOOTH_CONNECT runtime permission before
   * paired devices can be listed or sockets opened.
   */
  private void requestBluetoothThenShowDevices() {
    if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) &&
        (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) !=
         android.content.pm.PackageManager.PERMISSION_GRANTED)) {
      requestPermissions(
          new String[] { android.Manifest.permission.BLUETOOTH_CONNECT },
          REQUEST_BLUETOOTH_CONNECT);
      return;
    }
    enableBluetoothThenShowDevices();
  }

  /**
   * If Bluetooth is switched off, ask the player to turn it on (apps
   * can no longer switch it on themselves), then list paired devices.
   */
  private void enableBluetoothThenShowDevices() {
    android.bluetooth.BluetoothAdapter adapter =
        android.bluetooth.BluetoothAdapter.getDefaultAdapter();
    boolean enabled;
    try {
      enabled = (adapter != null) && adapter.isEnabled();
    } catch (SecurityException se) {
      enabled = false;
    }
    if ((adapter != null) && !enabled) {
      try {
        startActivityForResult(
            new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE),
            REQUEST_ENABLE_BLUETOOTH);
        return;
      } catch (RuntimeException e) {
        // No system prompt available: fall through to the device list.
      }
    }
    displayBluetoothDevicesList();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if ((requestCode == REQUEST_ENABLE_BLUETOOTH) && (resultCode == RESULT_OK)) {
      displayBluetoothDevicesList();
    }
  }

  /**
   * Explain that the other phone must be paired first, with a shortcut
   * to Android's Bluetooth settings.
   */
  private void showNoPairedDevices() {
    new AlertDialog.Builder(HomeScreen.this)
    .setIcon(R.drawable.bluetooth)
    .setTitle(R.string.menu_bluetooth_device)
    .setMessage("No paired devices found. Pair the other phone in " +
                "Android's Bluetooth settings first, then try again.")
    .setPositiveButton("Bluetooth Settings",
                       new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        try {
          startActivity(new Intent(
              android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (RuntimeException e) {
          // No settings screen available.
        }
      }
    })
    .setNegativeButton(R.string.cancel, null)
    .show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
      if ((grantResults.length > 0) &&
          (grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
        enableBluetoothThenShowDevices();
      }
      else {
        android.widget.Toast.makeText(this,
            "Bluetooth permission is needed for Bluetooth play.",
            android.widget.Toast.LENGTH_LONG).show();
      }
    }
  }

  private void displayBluetoothDevicesList() {
    AlertDialog.Builder builderSingle =
        new AlertDialog.Builder(HomeScreen.this);
    builderSingle.setIcon(R.drawable.bluetooth);
    builderSingle.setTitle(R.string.menu_bluetooth_device);

    final ArrayAdapter<String> arrayAdapter =
        new ArrayAdapter<String>(HomeScreen.this,
                                 android.R.layout.select_dialog_singlechoice);

    BluetoothDevice[] devices = BluetoothManager.getPairedDevices();
    if ((devices == null) || (devices.length == 0)) {
      showNoPairedDevices();
      return;
    }
    for (int index = 0; index < devices.length; index++) {
      String name;
      try {
        name = devices[index].getName();
      } catch (SecurityException se) {
        name = null;
      }
      arrayAdapter.add((name != null) ? name : devices[index].getAddress());
    }

    builderSingle.setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });

    builderSingle.setAdapter(arrayAdapter,
                             new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        SharedPreferences sp =
            PreferenceManager.getDefaultSharedPreferences(HomeScreen.this);
        myPreferences.bluetooth = which;
        PreferencesActivity.saveDefaultPreferences(myPreferences, sp);
        /*
         * Display the player ID buttons page.
         */
        displayPage(3);
      }
    });

    builderSingle.show();
  }

  /**
   * Manage a set of button "pages", where each page displays buttons.
   * The pages are indexed by a unique identifier.  When a valid page
   * identifier is provided, all buttons corresponding to other pages
   * are removed and the buttons for the requested page ID are added.
   * @param pageID - the requested page identifier (1-based).
   */
  private void displayPage(int pageID) {
    pageSelected = pageID;
    removeDynamicViews();
    switch (pageID) {
      default:
      case 1:
        addHomeButtons();
        selectInitialButton(buttonSelPage1);
        break;
      case 2:
        addMultiplayerButtons();
        selectInitialButton(buttonSelPage2);
        break;
      case 3:
        addPlayerSelectButtons();
        selectInitialButton(buttonSelPage3);
        break;
      case 4:
        addGameExtrasButtons();
        selectInitialButton(buttonSelPage4);
        break;
    }
  }

  private void numGamepadsDialog(int numGamepads) {
    AlertDialog.Builder builder = new AlertDialog.Builder(HomeScreen.this);
    /*
     * Set the dialog title.
     */
    builder.setTitle("Input Not Found");
    /*
     * Set the dialog message.
     */
    builder.setMessage(numGamepads + " gamepads found, 1 or more needed.")
    /*
     * Set the action buttons.
     */
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        // User clicked OK.  Do nothing - the dialog will be closed.
      }
    });
    builder.create();
    builder.show();
  }

  /**
   * NOTE: The InputDevice SOURCE_GAMEPAD input type was added in API
   * 12.  It is a derivative of the input source class
   * SOURCE_CLASS_BUTTON.
   * @return the total number of gamepad input devices.
   */
  private int numGamepadsConnected() {
    int[] deviceIds   = InputDevice.getDeviceIds();
    int   numGamepads = 0;

    for (int id : deviceIds) {
      InputDevice device = InputDevice.getDevice(id);
      if (((device.getSources() & InputDevice.SOURCE_GAMEPAD) ==
          InputDevice.SOURCE_GAMEPAD) ||
          ((device.getSources() & InputDevice.SOURCE_JOYSTICK) ==
          InputDevice.SOURCE_JOYSTICK)) {
        numGamepads++;
      }
    }

    return numGamepads;
  }

  private void removeDynamicViews() {
    removeViewByID(BTN1_ID);
    removeViewByID(BTN2_ID);
    removeViewByID(BTN3_ID);
    removeViewByID(BTN4_ID);
    removeViewByID(BTN5_ID);
    removeViewByID(BTN6_ID);
    removeViewByID(BTN7_ID);
    removeViewByID(BTN8_ID);
    removeViewByID(BTN9_ID);
    removeViewByID(BTN10_ID);
    removeViewByID(BTN11_ID);
    removeViewByID(BTN12_ID);
    removeViewByID(BTN13_ID);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    boolean handled = false;
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      backKeyPress();
      handled = true;
    }
    return handled || super.onKeyDown(keyCode, event);
  }

  /*
   * (non-Javadoc)
   * @see android.app.Activity#onCreate(android.os.Bundle)
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    finished = false;
    restoreGamePrefs();
    /*
     * Remove the title bar and configure the window layout.
     */
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    myLayout = new RelativeLayout(this);
    myLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                              LayoutParams.MATCH_PARENT));
    myImageView = new ImageView(this);

    /*
     * Restore the player saved and system save flags.
     */
    FrozenBubble.migrateSavedGame(this);
    SharedPreferences mConfig =
        PreferenceManager.getDefaultSharedPreferences(this);
    playerSave         = FrozenBubble.hasAnySavedGame(this);
    boolean systemSave = mConfig.getBoolean("systemSave", false );

    if (FrozenBubble.numPlayers != 0)
      startFrozenBubble();
    else if (getIntent().hasExtra("startHomeScreen")) {
      setBackgroundImage(GameView.chooseArt(this, R.drawable.home_screen_hd, R.drawable.home_screen));
      setContentView(myLayout);
      setWindowLayout();
      startHomeScreen();
    }
    else {
      /*
       * A game left mid-play is now saved automatically and offered as
       * Continue on the menu, so a fresh launch always shows the splash
       * and menu instead of jumping into a (new) game.
       */
      if (systemSave) {
        Editor editor = mConfig.edit();
        editor.putBoolean("systemSave", false);
        editor.commit();
      }
      {
        setBackgroundImage(GameView.chooseArt(this, R.drawable.splash_hd, R.drawable.splash));
        setContentView(myLayout);
        setWindowLayout();
        /*
         * Thread for managing the splash screen.
         */
        splashThread = new Thread() {
          @Override
          public void run() {
            try {
              synchronized(this) {
                /*
                 * Display the splash screen image for 3 seconds.
                 */
                wait(3000);
              }
            } catch (InterruptedException e) {
            } finally {
              if (!finished) {
                runOnUiThread(new Runnable() {
                  public void run() {
                    startHomeScreen();
                  }
                });
              }
            }
          }
        };
        splashThread.start();
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (myModPlayer != null) {
      myModPlayer.pausePlay();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (myModPlayer != null) {
      restoreGamePrefs();
      // Apply a change made in Options, then resume (if music is on).
      myModPlayer.setMusicOn(myPreferences.musicOn);
      myModPlayer.unPausePlay();
    }
  }

  /*
   * (non-Javadoc)
   * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
   * Invoked when the screen is touched.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (splashThread != null) {
        synchronized(splashThread) {
          splashThread.notifyAll();
        }
      }
    }
    return true;
  }

  @Override
  public void onWindowFocusChanged (boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      restoreGamePrefs();
      setWindowLayout ();
    }
  }

  private void removeViewByID(int id) {
    if (myLayout != null) {
      myLayout.removeView(myLayout.findViewById(id));
    }
  }

  private void restoreGamePrefs() {
    SharedPreferences sp =
        PreferenceManager.getDefaultSharedPreferences(this);
    myPreferences = PreferencesActivity.getDefaultPrefs(sp);
    /*
     * Also apply them app-wide right away.  Sound effects check the
     * app-wide settings, which otherwise keep their defaults (sound on)
     * until a game or the Options screen loads them, so the first
     * button tap after launch played a sound even with sound off (#61).
     */
    FrozenBubble.setPrefs(myPreferences);
  }

  private void selectInitialButton(int initialButton) {
    /*
     * On a touch screen every menu button looks the same: buttons don't
     * take focus from a tap (a focused button is drawn in a different
     * colour), and there is no initial highlight.  With a keyboard,
     * controller or remote, Android leaves touch mode and the focused
     * button is highlighted as usual.
     */
    for (int i = 0; i < myLayout.getChildCount(); i++) {
      View child = myLayout.getChildAt(i);
      if (child instanceof Button) {
        child.setFocusableInTouchMode(false);
      }
    }
    if (myLayout.isInTouchMode()) {
      return;
    }
    /*
     * Select the last button that was pressed.
     */
    Button selectedButton = (Button) myLayout.findViewById(initialButton);
    if (selectedButton == null) {
      /*
       * The remembered button isn't on this page now (e.g. Continue
       * after the saved game is gone): use the page's first button.
       */
      selectedButton = (Button) myLayout.findViewById(BTN1_ID);
      if (selectedButton == null) {
        return;
      }
    }
    selectedButton.requestFocus();
    selectedButton.setSelected(true);
  }

  private void setBackgroundImage(int resId) {
    if (myImageView.getParent() != null)
      myLayout.removeView(myImageView);

    myImageView.setBackgroundColor(getResources().getColor(R.color.black));
    myImageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                                 LayoutParams.MATCH_PARENT));
    myImageView.setImageResource(resId);
    myImageView.setId(SCREEN_ID);
    myLayout.addView(myImageView);
  }

  /**
   * Set the window layout according to the game preferences.
   * <p>Requesting that the title bar be removed <b>must</b> be
   * performed before setting the view content by applying the XML
   * layout, or it will generate an exception.
   */
  private void setWindowLayout() {
    final int flagFs   = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    final int flagNoFs = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    /*
     * Set full screen mode based on the game preferences.
     */
    if (myPreferences.fullscreen) {
      getWindow().addFlags(flagFs);
      getWindow().clearFlags(flagNoFs);
    }
    else {
      getWindow().clearFlags(flagFs);
      getWindow().addFlags(flagNoFs);
    }
    SystemUi.apply(this, myPreferences.fullscreen);

    if (myLayout != null) {
      myLayout.requestLayout();
    }
  }

  /**
   * Start the game with the specified number of players in the
   * specified locale.  A 1 player game can only be played locally.
   * @param myPlayerId - the local player ID.
   * @param numPlayers - the number of players (1 or 2).
   * @param opponentId - the opponent type ID, human or CPU.
   * @param gameLocale - the location of the opponent.  A local opponent
   * will be played by the CPU.  A LAN opponent will be played over the
   * network using multicasting, and an internet opponent will be played
   * using TCP.
   * @param arcadeGame - endless arcade game that scrolls new bubbles.
   * @param playerSave - load saved game information.
   */
  private void startFrozenBubble(int     myPlayerId,
                                 int     numPlayers,
                                 int     opponentId,
                                 int     gameLocale,
                                 boolean arcadeGame,
                                 boolean playerSave) {
    finished = true;
    /*
     * Since the default game activity creates its own player,
     * destroy the current player.
     */
    cleanUp();
    /*
     * Create an intent to launch the activity to play the game.
     */
    Intent intent = new Intent(this, FrozenBubble.class);
    intent.putExtra("myPlayerId", (int)     myPlayerId);
    intent.putExtra("numPlayers", (int)     numPlayers);
    intent.putExtra("opponentId", (int)     opponentId);
    intent.putExtra("gameLocale", (int)     gameLocale);
    intent.putExtra("arcadeGame", (boolean) arcadeGame);
    intent.putExtra("playerSave", (boolean) playerSave);
    if (playerSave && (continueSlot != null)) {
      intent.putExtra("saveSlot", continueSlot);
    }
    startActivity(intent);
    /*
     * Terminate the splash screen activity.
     */
    finish();
  }

  /**
   * Start the game without setting any intent data.  The game will use
   * default, existing, or saved values depending on whether the
   * application is already running or is passed a bundle.
   */
  private void startFrozenBubble() {
    finished = true;
    /*
     * Since the default game activity creates its own player,
     * destroy the current player.
     */
    cleanUp();
    /*
     * Create an intent to launch the activity to play the game.
     */
    Intent intent = new Intent(this, FrozenBubble.class);
    startActivity(intent);
    /*
     * Terminate the splash screen activity.
     */
    finish();
  }

  private void startHomeScreen() {
    if (!homeShown) {
      homeShown = true;
      setBackgroundImage(GameView.chooseArt(this, R.drawable.home_screen_hd, R.drawable.home_screen));
      setContentView(myLayout);
      myLayout.setFocusable(true);
      myLayout.setFocusableInTouchMode(true);
      myLayout.requestFocus();
      addBackButton();
      displayPage(pageSelected);

      /*
       * Create a sound manager to play sound effects.
       */
      mSoundManager = new SoundManager(this);

      /*
       * Load game sound effects.
       */
      mSoundManager.loadSound("stick", R.raw.stick );

      /*
       * Create a new music player to play the home screen music.
       */
      myModPlayer =
          new ModPlayer(this,
                        R.raw.introzik,
                        PlayerThread.LOOP_SONG_FOREVER,
                        myPreferences.musicOn,
                        false);
    }
  }

  private void startPreferencesScreen() {
    Intent intent = new Intent(this, PreferencesActivity.class);
    startActivity(intent);
  }
}
