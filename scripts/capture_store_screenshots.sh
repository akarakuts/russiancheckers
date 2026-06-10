#!/usr/bin/env bash
# Скриншоты для карточки приложения: полный кадр → без status bar → 9:16 (1080×1920 по умолчанию).
# Готовые файлы: docs/store-screenshots/store_${SHOT_W}x${SHOT_H}/
# Копия: STORE_COPY_TO или store-copy.dir (см. store-copy.dir.example); SKIP_STORE_COPY=1 — не копировать.
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJ_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT="${PROJ_ROOT}/docs/store-screenshots"
PKG="ru.akarakuts.russiancheckers"
ACT="${PKG}/.MainActivity"
STRIP_STATUS_TOP_PX="${STRIP_STATUS_TOP_PX:-156}"
SHOT_W="${SHOT_W:-1080}"
SHOT_H="${SHOT_H:-1920}"
SHOT_CROP_VERTICAL="${SHOT_CROP_VERTICAL:-bottom}"
SHOT_OUT="${OUT}/store_${SHOT_W}x${SHOT_H}"

resolve_copy_dest() {
  if [[ -n "${STORE_COPY_TO:-}" ]]; then
    printf '%s' "$STORE_COPY_TO"
    return
  fi
  local f="$PROJ_ROOT/store-copy.dir"
  if [[ -f "$f" ]]; then
    head -1 "$f" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
    return
  fi
  printf ''
}

COPY_DEST="$(resolve_copy_dest)"

mkdir -p "$OUT" "$SHOT_OUT"

wait_play_ui() {
  sleep 5
  return 0
}

cap() {
  local out="$1"
  adb shell input keyevent 224 2>/dev/null || true
  sleep 0.25
  adb shell screencap -p /sdcard/_store_cap.png
  adb pull /sdcard/_store_cap.png "$out" >/dev/null
  adb shell rm -f /sdcard/_store_cap.png 2>/dev/null || true
}

strip_status_bar() {
  local f="$1" top="${STRIP_STATUS_TOP_PX}" tmp="${f}.strip.tmp.png" w h nh
  if ! command -v ffmpeg >/dev/null 2>&1; then
    echo "capture_store_screenshots: ffmpeg не найден — status bar не обрезан: $f" >&2
    return 0
  fi
  w=$(sips -g pixelWidth "$f" 2>/dev/null | awk '/pixelWidth/ {print $2}')
  h=$(sips -g pixelHeight "$f" 2>/dev/null | awk '/pixelHeight/ {print $2}')
  nh=$((h - top))
  if [[ -z "$w" || -z "$h" || "$nh" -lt 32 ]]; then
    echo "capture_store_screenshots: не удалось определить размер: $f" >&2
    return 0
  fi
  ffmpeg -y -nostdin -hide_banner -loglevel error -i "$f" -vf "crop=${w}:${nh}:0:${top}" -frames:v 1 "$tmp"
  mv "$tmp" "$f"
}

store_portrait_9x16() {
  local src="$1" dst="$2" w="${SHOT_W}" h="${SHOT_H}" cy_expr
  case "${SHOT_CROP_VERTICAL}" in
    top) cy_expr="0" ;;
    center) cy_expr="(ih-${h})/2" ;;
    bottom) cy_expr="ih-${h}" ;;
    *)
      echo "capture_store_screenshots: SHOT_CROP_VERTICAL=${SHOT_CROP_VERTICAL} — допустимо top|center|bottom" >&2
      return 1
      ;;
  esac
  if ! command -v ffmpeg >/dev/null 2>&1; then
    echo "capture_store_screenshots: ffmpeg не найден" >&2
    return 1
  fi
  ffmpeg -y -nostdin -hide_banner -loglevel error -i "$src" \
    -vf "scale=${w}:${h}:force_original_aspect_ratio=increase,crop=${w}:${h}:(iw-${w})/2:${cy_expr}" \
    -frames:v 1 "$dst"
}

scroll_settings_down() {
  local j
  for j in $(seq 1 18); do
    adb shell input swipe 640 2200 640 700 320
    sleep 0.1
  done
  sleep 0.35
}

scroll_settings_up() {
  local j
  for j in $(seq 1 6); do
    adb shell input swipe 640 900 640 1700 280
    sleep 0.2
  done
  return 0
}

play_vs_bot_black_setup() {
  adb shell input tap 1074 2664
  sleep 1.2
  adb shell input tap 441 900
  sleep 0.5
  adb shell input tap 205 2664
  sleep 1
  adb shell input tap 640 1851
  sleep 0.5
}

play_black_move_1() {
  adb shell input tap 1162 1144
  sleep 0.6
  adb shell input tap 1022 1283
  sleep 0.5
}

play_black_move_2() {
  adb shell input tap 742 1283
  sleep 0.6
  adb shell input tap 880 1418
  sleep 0.5
}

play_black_move_3() {
  adb shell input tap 600 1418
  sleep 0.6
  adb shell input tap 742 1555
  sleep 0.5
}

wait_ai_turn() {
  sleep 20
}

adb shell input keyevent 224 2>/dev/null || true
sleep 0.35
adb shell pm clear "$PKG" >/dev/null 2>&1 || true
sleep 0.6
adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
sleep 0.35
adb shell am start -n "$ACT" >/dev/null
wait_play_ui || true
sleep 10

play_vs_bot_black_setup
wait_ai_turn
play_black_move_1
wait_ai_turn
play_black_move_2
wait_ai_turn
play_black_move_1
wait_ai_turn
cap "$OUT/01_ru_play_vs_bot.png"

play_black_move_3
wait_ai_turn
play_black_move_2
wait_ai_turn
play_black_move_3
wait_ai_turn
cap "$OUT/02_ru_play_midgame.png"

adb shell input tap 639 2664
sleep 1.15
cap "$OUT/04_ru_rules_top.png"

for j in $(seq 1 22); do
  adb shell input swipe 640 2100 640 450 340
  sleep 0.1
done
sleep 0.45
cap "$OUT/05_ru_rules_scrolled.png"

adb shell input tap 1074 2664
sleep 1.1
scroll_settings_down
scroll_settings_up
adb shell input swipe 640 1000 640 1300 220
sleep 0.35
cap "$OUT/03_ru_settings_bot_difficulty.png"

for f in "$OUT"/0*.png; do
  [[ -f "$f" ]] || continue
  strip_status_bar "$f"
done

rm -f "$SHOT_OUT"/*.png
for f in "$OUT"/0*.png; do
  [[ -f "$f" ]] || continue
  base=$(basename "$f" .png)
  store_portrait_9x16 "$f" "$SHOT_OUT/${base}.png"
done

echo "Готово: $OUT"
echo "9:16 (${SHOT_W}×${SHOT_H}): $SHOT_OUT"
ls -la "$SHOT_OUT"/*.png

if [[ "${SKIP_STORE_COPY:-}" != "1" && -n "$COPY_DEST" ]]; then
  mkdir -p "$COPY_DEST"
  cp -f "$SHOT_OUT"/*.png "$COPY_DEST/"
  echo "Скопировано в: $COPY_DEST"
  ls -la "$COPY_DEST"/*.png
elif [[ "${SKIP_STORE_COPY:-}" != "1" ]]; then
  echo "capture_store_screenshots: копирование пропущено (нет STORE_COPY_TO / store-copy.dir)" >&2
fi
