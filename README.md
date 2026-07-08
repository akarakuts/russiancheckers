# Russian checkers

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Russian / Русский: [README.ru.md](README.ru.md)

**Russian checkers** (8×8, mandatory maximum capture, flying kings) for Android — **Kotlin**, **Jetpack Compose**, **Navigation Compose**, **DataStore** for settings and in-progress game.

## Features

- **Rules** — Russian draughts on a dark board: men move diagonally forward, capture by jump, **must take the longest capture sequence** when several exist; promotion to king on the far rank; kings move any distance diagonally (including capture chains).
- **Modes** — two players on one device or **vs computer** (negamax + α–β, iterative deepening with a time budget and a transposition table).
- **AI difficulty** — Easy / Medium / Hard / Expert; persisted with other settings.
- **Quality of life** — move/capture animations, last-move highlight, **undo**, **hint**, move history in notation, sounds and haptics (both optional), game-over screen with confetti, win/loss statistics.
- **Persistence** — current position, side to move, preferences, and statistics via **DataStore**.
- **UI** — **Play** (board, path selection, status, new game), **Rules** (short in-app reference), **Settings** (mode, difficulty, coordinates, sound, vibration, statistics). Bottom navigation; **Exit** with confirmation. Night board palette and a wide tablet layout.
- **Locales** — English and Russian (`values` / `values-ru`).

## Android stack

| Area | Choice |
|------|--------|
| UI | Compose Material 3, `material-icons-core` |
| Navigation | Navigation Compose |
| State | ViewModel, Lifecycle Compose |
| Async | Kotlin Coroutines (Android) |
| Preferences / save game | DataStore Preferences |

See `app/build.gradle.kts` for versions and the full dependency list.

## Requirements

- **JDK 11+**
- **Android SDK** with compile SDK **36** (minor 1 as in the project); **minSdk 24**, **targetSdk 36**
- **Android Studio** Ladybug+ or Gradle **8+/9+** via `./gradlew`

## CI & automation

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [CI](.github/workflows/ci.yml) | push / PR to `main`, manual | `:app:check` (unit tests, Lint, compile) |
| [Security](.github/workflows/security.yml) | push / PR to `main`, weekly | OSV dependency scan, CodeQL |
| [Release](.github/workflows/release.yml) | tag `v*` | Upload-keystore–signed **APK + AAB** + GitHub Release (requires secrets) |

[Dependabot](.github/dependabot.yml) opens weekly PRs for Gradle and GitHub Actions dependencies.

## Build & run

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Open the `app` run configuration in Android Studio and deploy to a device or emulator. For **signed release** builds, see [Release signing](#release-signing).

**Launcher icons:** adaptive layers in `app/src/main/res/drawable/ic_launcher_*.xml`; run `.venv-icon/bin/python scripts/generate_launcher_icons.py` (Pillow in `.venv-icon`) to refresh all `mipmap-*/ic_launcher*.webp` and the 512×512 store asset under `docs/`.

## Release signing

`app/build.gradle.kts` loads **`keystore.properties`** from the repo root; if it exists, **`signingConfigs.upload`** is applied to **`release`**; otherwise **`release`** uses the **debug** keystore so fresh clones and CI still build installable APKs.

### 1. Create an upload keystore (once)

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Keep **`upload-keystore.jks`** and passwords in a password manager; **back up** the file — without it you cannot ship compatible updates.

### 2. Local signed `release` builds

1. Copy [`keystore.properties.example`](keystore.properties.example) to **`keystore.properties`** in the **repository root** (this file is gitignored).
2. Set `storeFile`, passwords, and `keyAlias` to match your keystore.
3. Run:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

Outputs: `app/build/outputs/apk/release/*.apk` and `app/build/outputs/bundle/release/*.aab`.

If **`keystore.properties` is missing**, `release` still signs with the **debug** keystore so the project builds on fresh clones — **do not** publish that build to an app store.

### 3. GitHub Actions tag releases (`v*`)

Configure these **repository secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | Base64 of `upload-keystore.jks` (e.g. `base64 -i upload-keystore.jks \| tr -d '\n'` on macOS) |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias (e.g. `upload`) |
| `RELEASE_KEY_PASSWORD` | Key password |

The [Release](.github/workflows/release.yml) workflow writes `keystore.properties` and `upload-keystore.jks` on the runner, then runs **`assembleRelease`** and **`bundleRelease`**, and attaches **`russiancheckers-<tag>.apk`** and **`.aab`** to the GitHub Release. If any secret is missing, the workflow **fails** with an error message (no silent debug-signed store builds).

## GitHub Releases

Tagged pushes (`v*`) run the Release workflow: **APK + AAB** signed with your **upload keystore** from GitHub secrets. Without secrets the workflow fails on purpose (see table above).

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

```bash
./gradlew :app:check
./scripts/check_strings_parity.sh   # en / ru string keys
./gradlew :app:connectedDebugAndroidTest   # Compose UI tests (device / emulator)
```

| Suite | Location | Coverage |
|-------|----------|----------|
| Engine | `app/src/test/.../RussianCheckersEngineTest.kt` | Captures, promotion, kings, `capturedAlong` |
| AI | `app/src/test/.../CheckersAiTest.kt` | Legal moves, forced capture, time budget |
| ViewModel | `app/src/test/.../CheckersViewModelTest.kt` | Undo, hint, move log (Robolectric) |
| DataStore | `app/src/test/.../GamePreferencesRepositoryTest.kt` | Settings, save game, stats |
| Compose UI | `app/src/androidTest/.../PlayScreenComposeTest.kt` | Controls, game-over overlay |

Robolectric unit tests need **JDK 21** (same as CI). DataStore tests use an isolated temp file per test class.

Release builds ship a **baseline profile** (`app/src/main/baseline-prof.txt`) installed via `profileinstaller` for faster cold start.

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/build_release.sh` | Signed APK/AAB → path from `store-upload.dir` (see `store-upload.dir.example`) |
| `scripts/check_strings_parity.sh` | Verify `values` / `values-ru` string key parity |
| `scripts/generate_launcher_icons.py` | Regenerate launcher webp + 512×512 store asset |
| `scripts/generate_sounds.py` | Regenerate game sounds (`res/raw/snd_*.wav`) |
| `scripts/capture_store_screenshots.sh` | Emulator screenshots for store listing (optional) |

## Contact

**Aleksey Karakuts** — [aleksey@karakuts.com](mailto:aleksey@karakuts.com)

## License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License** as published by the Free Software Foundation, either **version 3** of the License, or (at your option) any later version.

See the [`LICENSE`](LICENSE) file for the full GPLv3 text.

Copyright (C) 2026 Aleksey Karakuts &lt;aleksey@karakuts.com&gt;
