# Frozen Bubble for Android: modernized

The Android port of the Frozen Bubble game, brought up to date for
Android 16 with HD art, new menus, level packs, Arcade scoring and more.

This fork continues from the original project's last commit (`c3ac715`,
April 2020, version 4.4).  The work is on the **`modernize`** branch;
`master` is the original.

- **What's new:** [docs/CHANGES.md](docs/CHANGES.md)
- **Building:** [docs/BUILDING.md](docs/BUILDING.md)
- **Level packs and level tools:** [docs/LEVEL_PACKS.md](docs/LEVEL_PACKS.md)
- **Deprecated Android APIs (work list):** [docs/DEPRECATIONS.md](docs/DEPRECATIONS.md)

## Highlights

- Android 16 (targetSdk 36), minSdk 24 (Android 7.0); edge to edge, smooth
  on 90/120 Hz screens, no deprecated API warnings.
- HD art at 4x throughout (the original art is an option).
- Bubble-style menus and grouped Options, a first-start setup, and seven
  languages.
- Puzzle: a Choose Level screen with level previews, and four new level
  packs (Easy, Medium, Hard, Pictures) beside the original 100 levels.
- Arcade: points, combos and a difficulty multiplier on one leaderboard, a
  live score and a game over summary.
- 2 Player: Vs CPU with four skill levels; Bluetooth and Wi-Fi fixes.
- Your own music folder (`.mod`, `.xm`, `.it`, `.s3m`), a next-song
  button, a podium High Scores screen, and optional effects.

## Building

Open the folder in Android Studio, let Gradle sync, then Run 'app'.  Built
with Android Gradle Plugin 9.4, Gradle 9.6, Java 17; the libmodplug music
player is built with CMake from `app/src/main/cpp`.  See
[docs/BUILDING.md](docs/BUILDING.md) for terminal builds.

## History

Originally developed with SDK v20.1.0, using API level 4 (version 1.6
Donut), later with SDK v25.2.5 and NDK r10e using API level 12.  The store
links below are the original project's releases:

<a href="https://f-droid.org/repository/browse/?fdid=org.jfedor.frozenbubble" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=org.jfedor.frozenbubble" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>
<a href="https://www.amazon.com/Eric-Fortin-Frozen-Bubble/dp/B00F4ITDME" target="_blank">
<img src="https://images-na.ssl-images-amazon.com/images/G/01/mobile-apps/devportal2/res/images/amazon-underground-app-us-black.png" alt="Get it on F-Droid" height="80"/></a>

## Credits and licence

The code is based on the Java version of Frozen Bubble created by Glenn Sanson.
The original Frozen Bubble was created by Guillaume Cottenceau (programming),
Alexis Younes and Amaury Amblard-Ladurantie (artwork) and Matthias Le Bidan
(soundtrack).

The Android port was initially created by Pawel Aleksander Fedorynski, with
additional development provided by Eric Fortin.  The 2026 modernization
was done by Drew Baker.  Titles use the Luckiest Guy font by Astigmatic
(Apache License 2.0, `app/src/main/assets/fonts`).

This code is distributed under the GNU General Public License.

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License version 2 or 3, as published by the
Free Software Foundation.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program; if not, write to:
Free Software Foundation, Inc.
675 Mass Ave
Cambridge, MA 02139, USA
