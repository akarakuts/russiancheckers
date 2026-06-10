#!/usr/bin/env bash
# Проверка паритета ключей strings.xml и values-ru/strings.xml.
set -euo pipefail

PROJ_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EN="$PROJ_ROOT/app/src/main/res/values/strings.xml"
RU="$PROJ_ROOT/app/src/main/res/values-ru/strings.xml"

keys() {
  sed -n 's/.*name="\([^"]*\)".*/\1/p' "$1" | sort -u
}

en_file="$(mktemp)"
ru_file="$(mktemp)"
keys "$EN" >"$en_file"
keys "$RU" >"$ru_file"

missing_ru="$(comm -23 "$en_file" "$ru_file" || true)"
missing_en="$(comm -13 "$en_file" "$ru_file" || true)"

if [[ -n "$missing_ru" || -n "$missing_en" ]]; then
  echo "check_strings_parity: расхождение ключей" >&2
  [[ -n "$missing_ru" ]] && echo "  нет в ru:" >&2 && echo "$missing_ru" >&2
  [[ -n "$missing_en" ]] && echo "  нет в en:" >&2 && echo "$missing_en" >&2
  rm -f "$en_file" "$ru_file"
  exit 1
fi

count="$(wc -l <"$en_file" | tr -d ' ')"
rm -f "$en_file" "$ru_file"
echo "OK: ${count} ключей en/ru"
