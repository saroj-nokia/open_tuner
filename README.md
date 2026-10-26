# Open Tuner

A free, ad-free instrument tuner for Android with a metronome and a chord library. Kotlin + Jetpack Compose, Material 3 Expressive. No accounts, no analytics, no network access: the only permission is the microphone, and audio is analysed on the device and never stored.

All code, drawings and sounds in this repo are original. The app is inspired by popular tuner apps but copies nothing from them.

## Features

**Tuner**
- YIN pitch detection, accurate down to a bass low E, with a cents meter or an optional strobe-style display
- Auto string detection, or tap a string to lock onto it and hear a reference tone
- Chromatic mode plus tunings for guitar (Standard, Half step down, Drop D, Drop C, Open E/G/D, DADGAD, 7-string, 12-string), bass (4-string, 5-string, Drop D), ukulele (standard, baritone), banjo, mandolin, violin, viola, cello and double bass
- Custom tunings: name them, set 4 to 8 strings, saved on the device
- Left-handed mode, adjustable reference pitch (A4 415-466 Hz)
- Input level meter with an adjustable noise gate, plus an optional chime and haptic tick when you lock in tune
- Reference tones are plucked-string synthesis (Karplus-Strong)

**Metronome**
- 30-300 BPM, tap tempo, time signatures 2/4, 3/4, 4/4, 5/4, 6/8 and 7/8, accented first beat
- Clicks are generated into an audio buffer on their own thread, so the timing doesn't drift with the UI. It stops when you leave the app.

**Chords**
- Root and type browser (major, minor, 7, maj7, m7, sus2, sus4, dim, aug) with a fretboard diagram, strum playback and a left-handed flip

**Look and feel**
- Material 3 Expressive: springy motion, morphing Material shapes, emphasized type, wallpaper colours on Android 12+
- An About screen (Settings > About) with the version, license, source link and credits
- A short intro animation (a tuning peg turns and the string settles) that grows into the tuner's note circle. Tap to skip. It is drawn in code, with no video or image files.

## Download

Every push to `main` is built by GitHub Actions. Open the **Actions** tab, pick the latest green run and download the `OpenTuner-debug-apk` artifact, then install the APK on your phone (you may need to allow installs from your browser or file manager).

## Build

Needs JDK 17, the Android SDK (platform 37) and Gradle 9.3 or newer. There is no committed Gradle wrapper, so either open the folder in Android Studio or run Gradle directly:

```
gradle test assembleDebug
```

`gradle test` runs the unit tests (pitch detection, note and tuning maths, metronome timing, chords, strobe and signal maths, the tone generator). The APK lands in `app/build/outputs/apk/debug/`.

## Toolchain

| | Version | Why |
|---|---|---|
| Android Gradle Plugin | 9.1.1 | Compose 1.12 requires AGP 9.1 or newer. Kotlin is built in, so there is no separate Kotlin Android plugin. |
| Kotlin | 2.2.10 | |
| Gradle | 9.3.1 | |
| Material 3 | 1.5.0-beta02 | The Expressive APIs (shapes, toggle buttons, motion scheme) are only public from the 1.5 line. |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 | Compose 1.12 needs compileSdk 37. |

Material 3 1.5 is a beta, so expect the occasional API change when you update it.

## Project layout

```
app/src/main/java/com/opentuner/app/
  audio/       microphone capture, YIN detector, tone generator (plucked-string synth, in-tune chime)
  music/       notes, tunings, chords, signal and strobe maths, in-tune tracker
  metronome/   click engine and view model
  ui/          tuner, metronome and chord screens, intro overlay, shared slider, theme
```

## Notes and limits

- Reference tones are synthesised. The tone generator takes a pluggable audio source, so recorded samples can be dropped in later.
- `assembleRelease` is signed with the debug key so it installs straight away. Use your own keystore before publishing anywhere.
- There are no on-device tests yet. The unit tests cover the maths and audio generation, but sliders, the intro handover and live microphone behaviour are only checked by hand, so report anything that feels off.

## License

GNU General Public License v3.0, see [LICENSE](LICENSE). Open Tuner is free software: you can use, study, share and change it, and any version you distribute must stay under the same license with its source available. The libraries it uses (Jetpack Compose, Material 3, AndroidX) are Apache License 2.0, which is compatible with GPL v3.
