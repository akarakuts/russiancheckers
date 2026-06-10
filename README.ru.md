# Russian checkers

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

English: [README.md](README.md)

**Русские шашки** на Android (доска 8×8, обязательное взятие по максимуму, «летающие» дамки): **Kotlin**, **Jetpack Compose**, **Navigation Compose**, сохранение партии и настроек в **DataStore**.

## Возможности

- **Правила** — русские шашки: ход по диагонали вперёд, взятие через клетку; при нескольких вариантах **обязательно самое длинное взятие**; превращение в дамку на дальнем ряду; дамка ходит на любое число клеток по диагонали (в том числе цепочки взятий).
- **Режимы** — два игрока на одном устройстве или **против компьютера** (негамакс + α–β, глубина зависит от сложности).
- **Сложность ИИ** — лёгкая / средняя / сложная; запоминается вместе с остальными настройками.
- **Сохранение** — текущая позиция, чей ход и настройки в **DataStore**.
- **Экраны** — **Игра** (доска, выбор пути хода, статус, новая партия), **Правила** (краткая справка в приложении), **Настройки** (режим, сложность, координаты на доске). Нижняя навигация; **Выход** с подтверждением.
- **Языки** — русский и английский (`values-ru` / `values`).

## Требования и сборка

Как в [README.md](README.md): JDK 11+, Android SDK (compile 36, min 24), `./gradlew :app:assembleDebug` / установка через Android Studio. Подпись **release** — в англ. README, раздел [Release signing](README.md#release-signing).

**Иконки лаунчера** — векторы `ic_launcher_background.xml` / `ic_launcher_foreground.xml` и скрипт `scripts/generate_launcher_icons.py` (venv с Pillow: `.venv-icon`). Команда: `.venv-icon/bin/python scripts/generate_launcher_icons.py` — обновит `mipmap-*/ic_launcher*.webp` и 512×512 в `docs/`.

## CI (GitHub Actions)

Как в англ. README: [CI](.github/workflows/ci.yml) (`:app:check`), [Security](.github/workflows/security.yml) (OSV + CodeQL по расписанию), [Release](.github/workflows/release.yml) по тегу `v*` (подписанные APK/AAB в GitHub Release). Секреты и подпись release — в [README.md](README.md#release-signing). [Dependabot](.github/dependabot.yml) — еженедельные PR по Gradle и Actions.

## Контакты

**Aleksey Karakuts** — [aleksey@karakuts.com](mailto:aleksey@karakuts.com)

## Лицензия

Программа распространяется на условиях **GNU GPLv3** — полный текст в файле [`LICENSE`](LICENSE).

Copyright (C) 2026 Aleksey Karakuts &lt;aleksey@karakuts.com&gt;
