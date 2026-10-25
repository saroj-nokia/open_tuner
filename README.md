# Open Tuner

Free, ad-free guitar tuner. Kotlin + Jetpack Compose, Material 3 Expressive. Everything here is original code.

## Features
- YIN pitch detection (accurate on low E and bass strings), ~3 cent display resolution
- Auto string detection, or tap a string to lock it and hear a reference tone
- Chromatic, Guitar (Standard, Half step down, Drop D, DADGAD, Open G, Open D), Bass, Ukulele, Violin
- Adjustable reference pitch (A4 415–466 Hz)
- Haptic click when in tune, screen stays on while open
- Mic audio is analysed on-device only

## Material 3 Expressive bits
- `MaterialExpressiveTheme` + `MotionScheme.expressive()` (springy motion everywhere)
- Material shapes that morph (cookie → cookie → sunny) as you go from idle → listening → in tune
- Emphasized typography, `ToggleButton` shape-morphing, `FilledTonalIconButton` with shapes
- Dynamic (wallpaper) colour on Android 12+

## Build
1. Open the folder in Android Studio (it creates the Gradle wrapper), or run `gradle wrapper` once.
2. `./gradlew assembleDebug` (or `assembleRelease`, signed with the debug key for convenience).
3. `./gradlew test` runs the pitch-detector and note-math tests.

Material 3 is pinned to 1.5.0-beta02 because the Expressive APIs are not public in 1.4.0. That pulls Compose 1.12, which needs compileSdk 37, AGP 9.1+ (this project uses 9.4.0, with Kotlin built in) and therefore Gradle 9.6+.
