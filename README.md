# Tap Pilot

An auto clicker for Android, built with Jetpack Compose and Material 3 Expressive. Place targets
anywhere on screen and Tap Pilot taps and swipes for you at the rhythm you choose.

## Features

- **Single point**: one draggable target that taps at a steady interval.
- **Sequences**: numbered taps, long presses and swipes that play in order, each with its own
  timing. Edit them on screen or precisely in the app; changes save automatically.
- **Stop rules**: run until stopped, for a set time, or for a number of cycles, with live progress
  on the floating play button.
- **Natural variation**: optional random tap position and timing.
- **Floating controls**: a draggable floating toolbar that collapses to a single button while
  running and moves itself out of the way of your targets.
- **Library**: rename, duplicate, delete with undo, and import or export sequences as JSON.
- Quick Settings tile, launcher shortcuts, dynamic color, light and dark themes.
- **Private by design**: no internet permission, no ads, no analytics. The accessibility service
  only performs gestures; it requests no events and cannot read screen content.

## Requirements

- Android 8.0 (API 26) or newer.
- JDK 17+ to build. The Gradle wrapper downloads Gradle 9.8; the Android SDK needs platform 37.

## Build

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug
./gradlew assembleRelease      # minified; signed with the debug key unless a release key is set
./gradlew testDebugUnitTest    # engine, storage and overlay tests
./gradlew recordRoborazziDebug # renders every screen to app/build/screenshots
```

To sign release builds, set `tappilot.storeFile`, `tappilot.storePassword`, `tappilot.keyAlias` and
`tappilot.keyPassword` as Gradle properties (or the matching `TAPPILOT_*` environment variables).

## First run

The app walks you through enabling its accessibility service. If you installed the APK from a file
and the switch is greyed out, open **App info → ⋮ → Allow restricted settings**, then try again.

## Project layout

| Package | Contents |
| --- | --- |
| `core.model` | Scripts, steps, stop rules and variation |
| `core.engine` | Gesture planning and the coroutine runner |
| `core.data` | DataStore persistence, settings, backup import and export |
| `service` | Accessibility service, Quick Settings tile, app/service bridge |
| `overlay` | Floating controls, target markers, path layer and dialogs |
| `ui` | Theme, navigation and screens |
