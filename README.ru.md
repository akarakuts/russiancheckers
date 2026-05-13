# Russian checkers

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

English: [README.md](README.md)

**Русские шашки** на Android (доска 8×8, обязательное взятие по максимуму, «летающие» дамки): **Kotlin**, **Jetpack Compose**, **Navigation Compose**, сохранение партии и настроек в **DataStore**.

## Возможности

- **Правила** — русские шашки: ход по диагонали вперёд, взятие через клетку; при нескольких вариантах **обязательно самое длинное взятие**; превращение в дамку на дальнем ряду; дамка ходит на любое число клеток по диагонали (в том числе цепочки взятий).
- **Режимы** — два игрока на одном устройстве или **против компьютера** (негамакс + α–β, глубина зависит от сложности).
- **Сложность ИИ** — лёгкая / средняя / сложная; запоминается вместе с остальными настройками.
- **Сохранение** — текущая позиция, чей ход и настройки в **DataStore**.
- **Экраны** — **Игра** (доска, выбор пути хода, статус, новая партия), **Правила** (краткая справка в приложении), **Настройки** (режим, сложность, координаты на доске). Нижняя навигация; **Выход** завершает активность.
- **Языки** — русский и английский (`values-ru` / `values`).

## Требования и сборка

Как в [README.md](README.md): JDK 11+, Android SDK (compile 36, min 24), `./gradlew :app:assembleDebug` / установка через Android Studio. Для **магазинной** подписи см. [Подпись релиза (RuStore / GitHub Actions)](#подпись-релиза-rustore--github-actions).

## CI (GitHub Actions)

Как в англ. README: [CI](.github/workflows/ci.yml) (`:app:check`), [Security](.github/workflows/security.yml) (OSV + CodeQL по расписанию), [Release](.github/workflows/release.yml) по тегу `v*` (подписанные APK/AAB в GitHub Release). Нужны секреты `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` — см. раздел подписи в [README.md](README.md). [Dependabot](.github/dependabot.yml) — еженедельные PR по Gradle и Actions.

## Подпись релиза (RuStore / GitHub Actions)

RuStore и аналогичные витрины ожидают **релизную сборку с вашим upload-ключом** (часто загружают **AAB**). Официальные шаги RuStore — в их справке, например [загрузка AAB / подпись](https://www.rustore.ru/help/developers/publishing-and-verifying-apps/app-publication/new-version-app/upload-aab).

В `app/build.gradle.kts` — как в [tic-tac-toe-android](https://github.com/akarakuts/tic-tac-toe-android): при наличии **`keystore.properties`** в корне репозитория для **`release`** используется **`signingConfigs.upload`**; иначе **`release`** подписывается **debug-ключом** (удобно для чистого клона и CI).

### 1. Создать keystore (один раз)

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Храните **`upload-keystore.jks`** и пароли надёжно и **сделайте резервную копию** — без файла нельзя выпускать совместимые обновления.

### 2. Локальная подпись `release`

1. Скопируйте [`keystore.properties.example`](keystore.properties.example) в **`keystore.properties`** в **корне репозитория** (файл в `.gitignore`).
2. Укажите `storeFile`, пароли и `keyAlias` как в вашем keystore.
3. Сборка:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

Артефакты: `app/build/outputs/apk/release/*.apk` и `app/build/outputs/bundle/release/*.aab`.

Если **`keystore.properties` нет**, `release` подписывается **debug-ключом** — **такую сборку нельзя** отдавать в RuStore как финальную.

### 3. Секреты GitHub для тегов `v*`

В настройках репозитория (**Settings → Secrets and variables → Actions**):

| Secret | Значение |
|--------|----------|
| `RELEASE_KEYSTORE_BASE64` | Base64 файла `upload-keystore.jks` (например macOS: `base64 -i upload-keystore.jks \| tr -d '\n'`) |
| `RELEASE_STORE_PASSWORD` | Пароль keystore |
| `RELEASE_KEY_ALIAS` | Alias ключа (например `upload`) |
| `RELEASE_KEY_PASSWORD` | Пароль ключа |

Workflow [Release](.github/workflows/release.yml) создаёт на раннере `keystore.properties` и `upload-keystore.jks`, собирает **`assembleRelease`** и **`bundleRelease`**, прикладывает к GitHub Release файлы **`russiancheckers-<тег>.apk`** и **`.aab`**. Если секрет не задан — workflow **завершится с ошибкой**.

## Релизы на GitHub

Пуш тега `v*` запускает Release: **APK + AAB** с **upload-keystore** из секретов GitHub. Без секретов job завершится ошибкой (см. таблицу выше).

## Лицензия

Программа распространяется на условиях **GNU GPLv3** — полный текст в файле [`LICENSE`](LICENSE).
