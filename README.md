# SudokuSolver

This Android app merely encapsulates code to solve 9x9 Sudoku boards by brute force...and
the only board it ever actually solves is a hard-coded test board.

When I have time I hope to add support for additional features that actually makes the app useful. In the meantime,
the Board class can serve as an example of a depth-first exhaustive search for a solution.

## Dev environment

Android Studio is enough on its own if the Android SDK is installed through its SDK Manager. No separate JDK install is required when using Studio’s bundled JDK.

| Requirement | Detail |
| --- | --- |
| IDE | [Android Studio](https://developer.android.com/studio) (current stable; needs AGP 9.5 support) |
| JDK | **21** for app source/target; **17+** to run Gradle (Studio’s bundled JBR is fine) |
| Android SDK | Platform **API 37** (compile/target SDK); Build-Tools **36.0.0+** |
| Device / emulator | API **23+** (Android 6.0; `minSdk`) |
| Build system | Gradle **9.9.2** via the included wrapper (`gradlew`); Android Gradle Plugin **9.5.3** |

Open the project root in Android Studio, let it sync Gradle, then run the `app` configuration on an emulator or device.
