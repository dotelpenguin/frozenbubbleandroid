# Deprecated and soon-to-change Android APIs

Working list for the `deprecations` branch.  The app targets SDK 36
(Android 16), minSdk 24 (Android 7).  Google Play will require target
SDK 37 (Android 17) for new apps and updates from August 2027.

The build prints every deprecated call (`-Xlint:deprecation`, up to
1000 warnings, in `app/build.gradle.kts`): in Android Studio, Build >
Rebuild Project, then the Build window lists them with file and line.
From a terminal (the project has no `gradlew` yet; use the Gradle that
Android Studio downloaded):

    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    GRADLE=$(ls -d ~/.gradle/wrapper/dists/gradle-9.6.0-bin/*/gradle-9.6.0/bin/gradle | head -1)
    "$GRADLE" :app:compileDebugJavaWithJavac --rerun --no-build-cache

**Count on 2026-11-01: 130 warnings, all `[deprecation]`, none
`[unchecked]`.  On 2026-11-02: none** (sections 1 and 2 are done; the
Wi-Fi network changes still need a two-phone test).  Android Studio's Analyze > Inspect Code (or the
`lintDebug` task) adds Android lint's view.

Legend: **[B]** changes behaviour soon (do first), **[D]** deprecated but
still works, **[X]** probably dead code: delete rather than migrate,
**[OK]** a warning that only needs silencing.

## 1. Behaviour changes

- [x] **[B] Back key** - `KEYCODE_BACK` in `onKeyDown`: FrozenBubble,
  HomeScreen, PreferencesActivity, ScrollingCredits.  Only works because
  the manifest sets `enableOnBackInvokedCallback="false"`; Google warns a
  future release may stop honouring that.  Fix: `OnBackInvokedCallback`
  (Android 13+, no AndroidX needed), keeping `onKeyDown` for 7-12.
  Done: `BackKey`; the manifest now sets it to `"true"`.
- [x] **[B] Orientation on tablets / foldables** - `setRequestedOrientation()`
  (FrozenBubble).  Ignored on screens >= 600 dp for apps targeting 37.
  Fix: `android:appCategory="game"` on `<application>` (games are exempt).
  Done.

## 2. Deprecated, still working

| | API | Warnings | Where | Replace with |
|---|---|---|---|---|
| [x] [D] | `android.preference.PreferenceManager.getDefaultSharedPreferences` (API 29) | 82 | FrozenBubble 26, HomeScreen 8, GameView 8, Effects 6, CustomMusic 6, LevelSelect 6, ComputerAI 4, FirstRunSetup 4, LevelPacks 4, LocaleHelper 4, NetworkManager 2, OptionsMenus 2, PreferencesActivity 2 | Done: `Prefs.get(context)` opening the same file (`<package>_preferences`), so no data moves |
| [x] [D] | `WindowManager.LayoutParams.FLAG_FULLSCREEN`, `FLAG_FORCE_NOT_FULLSCREEN` (API 30) | 10 | FrozenBubble, HomeScreen, PreferencesActivity, ScrollingCredits | `SystemUi` already hides the bars the modern way: remove the leftovers |
| [x] [D] | `BluetoothAdapter.getDefaultAdapter()` (API 31) | 7 | BluetoothManager 3, HomeScreen 2, FirstRunSetup, NetworkManager | `context.getSystemService(BluetoothManager.class).getAdapter()` |
| [x] [D] | `AsyncTask` (API 30) | 5 | NetworkManager (reconnect) | A plain thread or `Executor` |
| [x] [D] | `WifiManager.getConnectionInfo()`, `WifiInfo.getIpAddress()`, `Formatter.formatIpAddress()` (API 31 / 23) | 5 | NetworkManager | `ConnectivityManager` + `LinkProperties` of the active network |
| [x] [D] | `NetworkInfo`, `getActiveNetworkInfo()`, `isConnected()` (API 29) | 3 | NetworkManager | `ConnectivityManager.getNetworkCapabilities()` |
| [x] [D] | `WifiManager.getDhcpInfo()` (API 31) | 1 | UDPSocket (Wi-Fi game broadcast address) | `LinkProperties` link addresses / prefix length |
| [x] [D] | `new AudioTrack(STREAM_MUSIC, ...)` (API 26), `AudioTrack.setStereoVolume` (API 21) | 5 | andmodplug PlayerThread | `AudioTrack.Builder` + `AudioAttributes`; `setVolume(float)` |
| [x] [D] | `new SoundPool(n, STREAM_MUSIC, 0)` (API 21) | 1 | SoundManager | `SoundPool.Builder` + `AudioAttributes` (USAGE_GAME) |
| [x] [D] | `WindowManager.getDefaultDisplay()`, `Display.getMetrics()` (API 30) | 3 | FrozenBubble | `WindowMetrics` (API 30+) / `context.getDisplay()`, old calls below 30 |
| [x] [D] | `Resources.getDrawable(int)`, `getColor(int)` (API 22/23) | 3 | GameView 2, HomeScreen | `context.getDrawable()` / `getColor()` |
| [x] [D] | `Context.VIBRATOR_SERVICE` (API 31), `Vibrator.vibrate(long)` (API 26) | 2 | Effects | `VibratorManager.getDefaultVibrator()` on 31+; the `vibrate(long)` call only runs on Android 7 |
| [x] [D] | `Activity.overridePendingTransition` (API 34) | 1 | HomeScreen | `overrideActivityTransition` on 34+ |
| [x] [OK] | `Drawable.getOpacity()` (API 29) | 2 | BubbleButtonDrawable, LevelThumbDrawable | Still required (abstract): mark `@SuppressWarnings("deprecation")` |

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
