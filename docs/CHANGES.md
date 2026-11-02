# Changes since the original Frozen Bubble for Android

This fork starts from the last commit on the original project's
`master` branch
([videogameboy76/frozenbubbleandroid](https://github.com/videogameboy76/frozenbubbleandroid)),
`c3ac715` "Fixed sound volume bug." (April 2020, version 4.4, built
with Eclipse/Ant for API level 12).  The original developer later
continued on the upstream `android_studio` branch (2023-2026: target
SDK 33 to 36, Bluetooth runtime permissions, a level-parsing fix, a
frozen top row in Arcade, faster credits, 16 KB page-size native
build); none of that is merged here.  Everything below was added on the
`modernize` branch from October 2026 onwards.  To see it all in git:

    git log --oneline c3ac715..modernize
    git diff --stat c3ac715 modernize

Version: 4.4 (code 54) became **4.5 (code 55)**.  The application ID is
unchanged (`org.jfedor.frozenbubble`), so it keeps the settings and
progress of an original build signed with the same key.  It can't
install over the Google Play or F-Droid versions, which are signed by
their publishers.

## Build and platform

- **Android Studio and Gradle** in place of Eclipse/Ant: Gradle Kotlin
  DSL, a version catalog (`gradle/libs.versions.toml`), Android Gradle
  Plugin 9.4, Gradle 9.6, Java 17.  The standard `app/src/main` layout;
  the Eclipse project files are gone.
- **libmodplug** (the music player) is built with CMake
  (`app/src/main/cpp/CMakeLists.txt`) instead of `ndk-build`, with a
  C++17 fix.
- **Android 16**: compileSdk and targetSdk 36; minSdk 24 (Android 7.0,
  was 3.1).
- Manifest: `android:exported`, Android 12+ Bluetooth permissions
  (`BLUETOOTH_CONNECT`, asked for at run time), screen-size config
  changes, `appCategory="game"`, and the language list
  (`localeConfig`).
- **Edge to edge and full screen**: the `SystemUi` helper hides the
  system bars the modern way and pads for the cut-outs and insets that
  Android 15+ enforces.  The letterbox bars around the playfield show a
  blurred copy of the background.
- **Smooth motion**: steady frame pacing, GPU rendering (an option), and
  motion and scaling worked out at draw time, so the game is smooth on
  90/120 Hz screens.
- **Deprecated Android APIs replaced** (130 compiler warnings to none):
  settings (`Prefs`), full screen flags, Bluetooth adapter, display
  size, drawables and colours, vibration, screen transitions, sound
  effects (`SoundPool.Builder`), music (`AudioTrack.Builder`), Wi-Fi
  address and connectivity (`LocalNetwork`), the reconnect task, and the
  Back key (`BackKey`, `OnBackInvokedCallback` on Android 13+).  See
  [DEPRECATIONS.md](DEPRECATIONS.md).
- **Hardening**: music file readers trimmed, and requests from other
  apps (the old level editor intent) validated.

## Look

- **HD art** throughout, at 4x the original: bubbles (including the
  colour-blind and frozen ones), buttons, launcher, attack markers,
  backgrounds, panels, penguins, compressor, the bubble font, menu
  screens, the logo, and the score plank.  "Original Art" in Options
  switches back to the original pixels.  The tools that made them are in
  `tools/hd-art/`.
- New adaptive launcher icon, launch screen, and a single Recents card.
- **Bubble-style menus**: every menu (home screen, Options, in-game
  menu) is a set of glossy blue bubble buttons over a dimmed,
  see-through background.  Labels are short so none wrap.
- **Effects** (Options > Effects, each on or off): pop sparkles,
  falling fade, impact wobble, screen shake, shot trail, aiming guide,
  vibration.  The drawing effects need GPU Rendering.
- In-game score in a crisp outlined font on a wooden sign, with
  "+points" pop-ups and "COMBO xN!" flashes.

## Menus and settings

- **First start setup**: language, player name, and the Bluetooth
  permission.
- **Home screen**: Resume (the saved game, named by mode, level or
  difficulty), Puzzle, Arcade, 2 Player (Local, Vs CPU, Wi-Fi,
  Bluetooth), High Scores, Options, Exit.  The phone's Back key goes
  back a page.
- **Options** (home screen): Audio (music, sound effects, My Music and
  its folder), Display (full screen, GPU rendering, original art,
  colour-blind), Controls (aim & shoot / tap to shoot / rotate & shoot,
  sensitivity), Gameplay (player name, hurry mode, 2P compressor, best
  score in game, reset progress), Effects, Language, and Credits.
- **In-game menu**: Resume, Options, Quit.  In-game Options has the
  same groups plus High Scores, About, and Restart Level and Choose
  Level in Puzzle (New Game in the other modes).  Options that only
  apply to a new game offer to restart it.
- **Language**: chosen in Options or follows the phone; all new text is
  translated into German, Spanish, French, Icelandic, Brazilian
  Portuguese and Simplified Chinese.
- The old phone Menu-key menu is still in the code but replaced by the
  above; it is due to be removed.
- The Difficulty option was removed: Arcade asks for the difficulty
  when a game starts (Normal by default).

## Music

- **My Music**: choose a folder of your own `.mod`, `.xm`, `.it` or
  `.s3m` songs (Android's folder picker); the built-in songs play if it
  has none.
- A **next song** button, in 1 and 2 player games.
- Music loops correctly after the credits; volume follows the phone's
  media volume.

## Puzzle

- **Choose Level**: a paged grid of level bubbles, each showing a
  preview of the level.  Tap an unlocked level to play it.  Cheat mode:
  tap a locked level 7 times quickly to unlock every level; tap the
  title 7 times to lock them again.
- **Level packs**: Original (the 100 classic levels), Easy (30),
  Medium (30), Hard (30) and Pictures (24 pixel-art levels).  The
  ◀ ▶ arrows under the title switch packs.  Each pack keeps its own progress
  and best scores.  See [LEVEL_PACKS.md](LEVEL_PACKS.md).
- Winning a level goes straight to the next one (no score screen in
  between).  Finishing a pack starts it again at level 1.
- **Reset Progress** in Options.
- Fixed: a crash after the last level of a pack.

## Arcade

- Asks for the difficulty when a game starts: Easy, Normal, Medium,
  Hard or Insane (4 to 8 colours).
- **Scoring**: points for every bubble cleared, more for dropped ones;
  a combo multiplier for clearing shots in a row (x1 to x3); and a
  difficulty multiplier (x1 Easy to x3 Insane).  The speed no longer
  increases.  So all difficulties share one leaderboard.
- **Live score** on a wooden sign above "1 UP".
- **Game over summary**: the score in a big bubble, counting up, with
  the details, a NEW BEST burst and applause, and Play Again / Quit
  buttons, in place of the old high score screen.

## 2 Player

- **Vs CPU** with four skill levels: Easy, Normal, Hard, Expert.
- Landscape 2 player layout and an HD 2 player background; a random
  field each game; an option for the compressor.
- Bluetooth: fixed a crash on Android 12+, a freeze when backing out,
  and busy loops; asks for the permission and explains when no phones
  are paired.
- Wi-Fi: works out the phone's address and broadcast address the
  current way, including when a phone shares its own hotspot.

## Saving and scores

- Autosave, with a **separate saved game per mode** (Puzzle, Arcade, 2
  Player).
- **High Scores** screen: a podium for the top 3 and a list to 10th,
  with tabs for Arcade (points) and Vs CPU (wins, best first: hardest
  CPU, then fewest shots, then fastest).  Scores use
  thin spaces (12 345) instead of commas.  Puzzle best scores show per
  level in Choose Level.
- The scores database gained a points column (version 2); old Arcade
  scores, which counted something different, were cleared.

## Tools

- `tools/hd-art/`: scripts that made the HD art.
- `tools/levels/`: read, check, measure and generate levels, and build
  packs.
