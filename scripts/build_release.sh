#!/usr/bin/env bash
# Подписанный release: Gradle + копирование APK/AAB в каталог из store-upload.dir или STORE_UPLOAD_DIR.
set -euo pipefail

PROJ_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJ_ROOT"

resolve_upload_dir() {
  if [[ -n "${STORE_UPLOAD_DIR:-}" ]]; then
    printf '%s' "$STORE_UPLOAD_DIR"
    return
  fi
  local f="$PROJ_ROOT/store-upload.dir"
  if [[ -f "$f" ]]; then
    head -1 "$f" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
    return
  fi
  echo "build_release: задайте STORE_UPLOAD_DIR или store-upload.dir (см. store-upload.dir.example)" >&2
  exit 1
}

OUT="$(resolve_upload_dir)"
if [[ -z "$OUT" ]]; then
  echo "build_release: пустой каталог в STORE_UPLOAD_DIR / store-upload.dir" >&2
  exit 1
fi

if [[ ! -f "$PROJ_ROOT/keystore.properties" ]]; then
  echo "build_release: keystore.properties не найден — release с debug-подписью (не для публикации)" >&2
fi

chmod +x ./gradlew
./gradlew :app:assembleRelease :app:bundleRelease --no-daemon --stacktrace

version_name="$(
  sed -n 's/.*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1
)"
if [[ -z "$version_name" ]]; then
  echo "build_release: не удалось прочитать versionName из app/build.gradle.kts" >&2
  exit 1
fi

mkdir -p "$OUT"

apk_src=(app/build/outputs/apk/release/*.apk)
aab_src=(app/build/outputs/bundle/release/*.aab)
if [[ ! -e "${apk_src[0]}" ]]; then
  echo "build_release: APK не найден в app/build/outputs/apk/release/" >&2
  exit 1
fi
if [[ ! -e "${aab_src[0]}" ]]; then
  echo "build_release: AAB не найден в app/build/outputs/bundle/release/" >&2
  exit 1
fi

apk_dst="$OUT/russiancheckers-${version_name}.apk"
aab_dst="$OUT/russiancheckers-${version_name}.aab"
cp -f "${apk_src[0]}" "$apk_dst"
cp -f "${aab_src[0]}" "$aab_dst"

echo "Release (${version_name}): $OUT"
ls -la "$apk_dst" "$aab_dst"
