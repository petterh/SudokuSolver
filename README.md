# SudokuSolver

Android app that turns a photo of a 9×9 Sudoku into a solvable board, then finds a
solution with depth-first search.

## What it does

1. **Capture** a puzzle with the in-app camera (framing overlay) or load one from the **gallery**
2. **Preview** the OpenCV-warped board before committing
3. **OCR** clues on-device with ML Kit (edit mistakes via the digit keypad)
4. **Highlight** conflicting digits; **Solve** with a uniqueness check (warns if more than one solution)
5. **Undo**, **Clear**, and **persist** the board across rotation and process death

Given clues stay bold after solving so you can tell them apart from filled cells.

## Dev environment

Android Studio is enough on its own if the Android SDK is installed through its SDK Manager. No separate JDK install is required when using Studio’s bundled JDK.

| Requirement | Detail |
| --- | --- |
| IDE | [Android Studio](https://developer.android.com/studio) (current stable; needs AGP 9.5 support) |
| JDK | **21** for app source/target; **17+** to run Gradle (Studio’s bundled JBR is fine) |
| Android SDK | Platform **API 37** (compile/target SDK); Build-Tools **36.0.0+** |
| Device / emulator | API **24+** (Android 7.0; `minSdk`); `arm64-v8a` or `x86_64` |
| Build system | Gradle **9.9.2** via the included wrapper (`gradlew`); Android Gradle Plugin **9.5.3** |
| Native / ML | OpenCV **4.12**, ML Kit text-recognition **16.0.1**, CameraX **1.7** |

Open the project root in Android Studio, let it sync Gradle, then run the `app` configuration on an emulator or device.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Instrumented OCR regressions (device or emulator required):

```bash
./gradlew :app:connectedDebugAndroidTest
```
