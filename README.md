# SudokuSolver

This Android app merely encapsulates code to solve 9x9 Sudoku boards by brute force...and
the only board it ever actually solves is a hard-coded test board.

When I have time I hope to add support for additional features that actually makes the app useful. In the meantime,
the Board class can serve as an example of a depth-first exhaustive search for a solution.

## Dev environment

Android Studio is enough on its own if the Android SDK is installed through its SDK Manager. No separate JDK install is required when using Studio’s bundled JDK.

| Requirement | Detail |
| --- | --- |
| IDE | [Android Studio](https://developer.android.com/studio) 4.1 or newer |
| JDK | 8 (source/target compatibility; Studio’s embedded JDK is fine) |
| Android SDK | Platform **API 30** (compile/target SDK) |
| Device / emulator | API **21+** (Android 5.0; `minSdkVersion`) |
| Build system | Gradle **6.5** via the included wrapper (`gradlew`); Android Gradle Plugin **4.1.1** |

Open the project root in Android Studio, let it sync Gradle, then run the `app` configuration on an emulator or device.

**Note:** This project’s AGP/Gradle versions date from 2020. Very recent Android Studio releases may prompt you to upgrade the Gradle plugin before syncing succeeds. Accepting that upgrade (or using an older Studio build) is the usual fix; the app itself is plain Java with AndroidX and view binding.
