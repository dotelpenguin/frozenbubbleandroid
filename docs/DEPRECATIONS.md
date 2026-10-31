# Deprecated and soon-to-change Android APIs

Working list for the `deprecations` branch.  The app targets SDK 36
(Android 16), minSdk 24 (Android 7).  Google Play will require target
SDK 37 (Android 17) for new apps and updates from August 2027.

The build now prints every deprecated call (`-Xlint:deprecation` in
`app/build.gradle.kts`): in Android Studio, Build > Rebuild Project, then
the Build window lists them with file and line.  Android Studio's
Analyze > Inspect Code (or `./gradlew lintDebug`) adds Android lint's
view.

Legend: **[B]** changes behaviour soon (do first), **[D]** deprecated but
still works, **[X]** probably dead code: delete rather than migrate.

## 1. Behaviour changes

- [ ] **[B] Back key** - `KEYCODE_BACK` in `onKeyDown`: FrozenBubble,
  HomeScreen, PreferencesActivity, ScrollingCredits.  Only works because
  the manifest sets `enableOnBackInvokedCallback="false"`; Google warns a
  future release may stop honouring that.  Fix: `OnBackInvokedCallback`
  (Android 13+, no AndroidX needed), keeping `onKeyDown` for 7-12.
- [ ] **[B] Orientation on tablets / foldables** - `setRequestedOrientation()`
  (FrozenBubble).  Ignored on screens >= 600 dp for apps targeting 37.
  Fix: `android:appCategory="game"` on `<application>` (games are exempt).

## 2. Deprecated, still working

| | API | Where | Replace with |
|---|---|---|---|
| [ ] [D] | `android.preference.PreferenceManager.getDefaultSharedPreferences` (API 29) | 41 calls, 13 files | A small `Prefs.get(context)` helper opening the same file (`<package>_preferences`), so no data moves |
| [ ] [D] | `WindowManager.getDefaultDisplay()`, `Display.getMetrics()` (API 30) | FrozenBubble | `WindowMetrics` (API 30+) / `context.getDisplay()`, old calls below 30 |
| [ ] [D] | `WindowManager.LayoutParams.FLAG_FULLSCREEN` (API 30) | FrozenBubble, HomeScreen, PreferencesActivity, ScrollingCredits | `SystemUi` already hides the bars the modern way: remove the leftovers |
| [ ] [D] | `BluetoothAdapter.getDefaultAdapter()` (API 31) | BluetoothManager, FirstRunSetup, HomeScreen, NetworkManager | `context.getSystemService(BluetoothManager.class).getAdapter()` |
| [ ] [D] | `WifiManager.getConnectionInfo()`, `WifiInfo.getIpAddress()` (API 31) | NetworkManager | `ConnectivityManager` + `LinkProperties` of the active network |
| [ ] [D] | `NetworkInfo`, `getActiveNetworkInfo()` (API 29) | NetworkManager | `ConnectivityManager.getNetworkCapabilities()` |
| [ ] [D] | `AsyncTask` (API 30) | NetworkManager (reconnect) | A plain thread or `Executor` |
| [ ] [D] | `new SoundPool(n, STREAM_MUSIC, 0)` (API 21) | SoundManager | `SoundPool.Builder` + `AudioAttributes` (USAGE_GAME) |
| [ ] [D] | `new AudioTrack(STREAM_MUSIC, ...)` (API 26) | andmodplug PlayerThread | `AudioTrack.Builder` + `AudioAttributes` |
| [ ] [D] | `Resources.getDrawable(int)`, `getColor(int)` (API 22/23) | GameView, HomeScreen | `context.getDrawable()` / `getColor()` |
| [ ] [D] | `Context.VIBRATOR_SERVICE` (API 31) | Effects | `VibratorManager.getDefaultVibrator()` on 31+ |
| [ ] [D] | `overridePendingTransition` (API 34) | HomeScreen | `overrideActivityTransition` on 34+ |
| [ ] [D] | `AlertDialog.Builder.setMultiChoiceItems` sound dialog | FrozenBubble `soundOptionsDialog` | see dead code |

Not a problem: `startActivityForResult` / `onActivityResult` and
`requestPermissions` are only deprecated in AndroidX, which the app
doesn't use.  `Vibrator.vibrate(long)` only runs on Android 7.

## 3. Probably dead code (delete)

- [ ] [X] The old options menu (hardware Menu key): `onCreateOptionsMenu`,
  `onPrepareOptionsMenu`, `handleMenuAction` (except About), the
  `MENU_*` constants, `soundOptionsDialog`, `targetOptionsDialog`, and
  their `menu_*` strings.  Replaced by the bubble Options menus.
- [ ] [X] Level Editor install prompt: `MENU_EDITOR`, `install_editor`,
  `market_missing` (the old editor app and Android Market are gone).
  Keep the `EDITORACTION` intent path only if we keep accepting levels
  from other apps.
- [ ] [X] `drawLowScoreScreen` / `drawHighScoreScreen` / `drawArcadeScoreScreen`
  for Puzzle and Arcade (no longer shown); check the 2 player use.
- [ ] [X] `res/layout/seek_bar_preference.xml` and other old Preferences
  screen leftovers.

## 4. Other things to know

- Music has no audio focus handling: it doesn't pause for calls or other
  apps' audio.  (`AudioManager.requestAudioFocus` with
  `AudioFocusRequest`.)
- `Thread.stop()` is not used (GameThread uses interrupt + join).
- Locale handling (LocaleHelper) already uses the modern
  `Configuration.setLocales` / `createConfigurationContext`.
