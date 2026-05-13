# Russiancheckers

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Russian / Русский: [README.ru.md](README.ru.md)

**Russian checkers** (8×8, mandatory maximum capture, flying kings) for Android — **Kotlin**, **Jetpack Compose**, **Navigation Compose**, **DataStore** for settings and in-progress game.

## Features

- **Rules** — Russian draughts on a dark board: men move diagonally forward, capture by jump, **must take the longest capture sequence** when several exist; promotion to king on the far rank; kings move any distance diagonally (including capture chains).
- **Modes** — two players on one device or **vs computer** (negamax + α–β, depth capped by difficulty).
- **AI difficulty** — Easy / Medium / Hard; persisted with other settings.
- **Persistence** — current position, side to move, and preferences via **DataStore**.
- **UI** — **Play** (board, path selection, status, new game), **Rules** (short in-app reference), **Settings** (mode, difficulty, coordinates on/off). Bottom navigation; **Exit** finishes the activity. Layout adapts to phone and tablet width.
- **Locales** — English and Russian (`values` / `values-ru`).

## Android stack

| Area | Choice |
|------|--------|
| UI | Compose Material 3, `material-icons-extended` |
| Navigation | Navigation Compose |
| State | ViewModel, Lifecycle Compose |
| Async | Kotlin Coroutines (Android) |
| Preferences / save game | DataStore Preferences |

See `app/build.gradle.kts` for versions and the full dependency list.

## Requirements

- **JDK 11+**
- **Android SDK** with compile SDK **36** (minor 1 as in the project); **minSdk 24**, **targetSdk 36**
- **Android Studio** Ladybug+ or Gradle **8+/9+** via `./gradlew`

## Build & run

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Open the `app` run configuration in Android Studio and deploy to a device or emulator.

## Release signing (RuStore / stores)

Release builds use your **upload keystore** when `keystore.properties` exists; otherwise they fall back to the **debug** keystore (fine for local testing only — **do not** publish that build).

1. Copy [`keystore.properties.example`](keystore.properties.example) to **`keystore.properties`** in the repo root (gitignored).
2. Point `storeFile`, passwords, and `keyAlias` at your keystore.
3. Run:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

RuStore and similar consoles: follow their docs for AAB / certificate upload, e.g. [RuStore — upload AAB](https://www.rustore.ru/help/developers/publishing-and-verifying-apps/app-publication/new-version-app/upload-aab).

## Project layout

| Path | Role |
|------|------|
| `app/.../game/` | `RussianCheckersEngine`, `Board`, move generation, `CheckersAi`, `AiDifficulty` |
| `app/.../data/` | `GamePreferencesRepository`, `CheckersSettings` — DataStore |
| `app/.../ui/CheckersViewModel.kt` | Game + settings state, AI coroutine |
| `app/.../ui/RussianCheckersApp.kt` | Navigation scaffold |
| `app/.../ui/PlayScreen.kt` | Board and interaction |
| `app/.../ui/RulesScreen.kt`, `SettingsScreen.kt` | Rules and options |

## Testing

Unit and instrumentation test stubs live under `app/src/test` and `app/src/androidTest`. Extend them for engine edge cases and UI flows as needed.

## License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License** as published by the Free Software Foundation, either **version 3** of the License, or (at your option) any later version.

See the [`LICENSE`](LICENSE) file for the full GPLv3 text.
