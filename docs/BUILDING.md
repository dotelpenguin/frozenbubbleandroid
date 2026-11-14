# Building Frozen Bubble for Android

## What you need

- **Android Studio** (a recent release; it brings its own Java 17+ in
  `jbr`, the Android SDK manager and Gradle).
- From the SDK manager: **Android SDK Platform 36**, the **NDK** and
  **CMake** (for the libmodplug music player in `app/src/main/cpp`).
  Android Studio offers to install whatever is missing when it syncs.

The build uses Android Gradle Plugin 9.4 (`gradle/libs.versions.toml`)
and Gradle 9.6 (`gradle/wrapper/gradle-wrapper.properties`), Java 17,
compileSdk/targetSdk 36 and minSdk 24 (Android 7.0).

## In Android Studio

1. File > Open, and choose the project folder (the one with
   `settings.gradle.kts`).
2. Let Gradle sync (the first time downloads Gradle 9.6 and the
   plugin).
3. Pick a phone or emulator and Run 'app'.

If Run is greyed out, sync again (File > Sync Project with Gradle Files)
and check a Run configuration for `app` is selected.

## From a terminal

The repository includes the Gradle wrapper, which downloads Gradle 9.6
the first time it runs.  It needs Java 17 or newer; Android Studio's own
will do.  On a Mac:

    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    ./gradlew :app:assembleDebug

On Windows, use `gradlew.bat` instead.  The Android SDK is found from
`local.properties` (Android Studio writes it) or `ANDROID_HOME`.

The APK is written to `app/build/outputs/apk/debug/`.

To see every compiler warning (the build lists deprecated API use; see
[DEPRECATIONS.md](DEPRECATIONS.md)), force Java to recompile:

    ./gradlew :app:compileDebugJavaWithJavac --rerun --no-build-cache

Without `--rerun` only the files that changed are recompiled, so only
their warnings show.

## Project layout

| Path | What it is |
|---|---|
| `app/src/main/java/org/jfedor/frozenbubble/` | The game: activity, game view and thread, game rules, sprites, levels |
| `app/src/main/java/com/efortin/frozenbubble/` | Home screen, menus and Options, level select, level packs, high scores, network and Bluetooth play, helpers |
| `app/src/main/java/com/peculiargames/andmodplug/` | Java side of the music player |
| `app/src/main/cpp/` | libmodplug and its JNI glue (CMake) |
| `app/src/main/assets/levels.txt` | The 100 original Puzzle levels |
| `app/src/main/assets/packs/` | The other level packs ([LEVEL_PACKS.md](LEVEL_PACKS.md)) |
| `app/src/main/assets/fonts/` | Luckiest Guy (Apache 2.0), for titles |
| `app/src/main/res/drawable-nodpi/` | The HD art (`*_hd.png`) |
| `app/src/main/res/raw/` | Music modules and sound effects |
| `app/src/main/res/values-*/` | Translations (de, es, fr, is, pt-BR, zh-CN) |
| `tools/hd-art/` | Scripts that made the HD art (Python 3 + Pillow) |
| `tools/levels/` | Level tools: check, measure, generate and pack levels |
| `docs/` | These notes |
