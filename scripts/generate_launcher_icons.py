#!/usr/bin/env python3
"""Генерация ic_launcher.webp для всех mipmap-*dpi и ic_launcher_store_512.png (витрина).

Запуск из корня Russiancheckers:
  .venv-icon/bin/python scripts/generate_launcher_icons.py

Требуется Pillow (см. .venv-icon в проекте)."""
from __future__ import annotations

import math
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError as e:
    print("Установите Pillow: python3 -m venv .venv-icon && .venv-icon/bin/pip install Pillow", file=sys.stderr)
    raise SystemExit(1) from e

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"

DARK_SQ = (0x5D, 0x40, 0x37)
LIGHT_SQ = (0xD7, 0xCC, 0xC8)
DISK_DARK_OUT = (0x3E, 0x27, 0x23)
DISK_DARK_IN = (0x5D, 0x40, 0x37)
DISK_LIGHT_OUT = (0xEC, 0xEF, 0xF1)
DISK_LIGHT_IN = (0xFF, 0xFF, 0xFF)
CROWN_OUT = (0xFF, 0xC1, 0x07)
CROWN_IN = (0xFF, 0xEC, 0xB3)


def draw_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cell = size / 8.0
    for j in range(8):
        for i in range(8):
            c = DARK_SQ if (i + j) % 2 == 0 else LIGHT_SQ
            x0 = int(round(i * cell))
            y0 = int(round(j * cell))
            x1 = int(round((i + 1) * cell)) - 1
            y1 = int(round((j + 1) * cell)) - 1
            d.rectangle([x0, y0, x1, y1], fill=c)

    def circle(cx: float, cy: float, r: float, fill, outline=None, width=1):
        bb = [
            int(round(cx - r)),
            int(round(cy - r)),
            int(round(cx + r)),
            int(round(cy + r)),
        ]
        d.ellipse(bb, fill=fill, outline=outline, width=max(1, int(round(width * size / 108))))

    # Тёмная шашка (как во foreground vector)
    cx_d, cy_d, r_d = 63 * size / 108, 58 * size / 108, 19 * size / 108
    circle(cx_d, cy_d, r_d, DISK_DARK_OUT, (0x1A, 0x10, 0x08), 1.2)
    circle(cx_d, cy_d, r_d * 13 / 19, DISK_DARK_IN, None, 0)

    # Светлая шашка
    cx_w, cy_w, r_w = 45 * size / 108, 54 * size / 108, 20 * size / 108
    circle(cx_w, cy_w, r_w, DISK_LIGHT_OUT, (0x90, 0xA4, 0xAE), 1.2)
    circle(cx_w, cy_w, r_w * 14 / 20, DISK_LIGHT_IN, None, 0)

    # Корона (многоугольник в координатах viewport 108, масштаб)
    s = size / 108.0
    pts = [
        (29 * s, 35 * s),
        (33 * s, 20 * s),
        (38 * s, 28 * s),
        (45 * s, 16 * s),
        (52 * s, 28 * s),
        (57 * s, 20 * s),
        (61 * s, 35 * s),
    ]
    pts_i = [(int(round(x)), int(round(y))) for x, y in pts]
    d.polygon(pts_i, fill=CROWN_OUT, outline=(0xF5, 0x7F, 0x17))
    inner = [
        (34 * s, 32 * s),
        (38 * s, 24 * s),
        (45 * s, 22 * s),
        (52 * s, 24 * s),
        (56 * s, 32 * s),
    ]
    d.polygon([(int(round(x)), int(round(y))) for x, y in inner], fill=CROWN_IN)

    return img


def main() -> None:
    mip = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    base = draw_icon(512)
    docs_store = ROOT / "docs" / "store-listing"
    docs_store.mkdir(parents=True, exist_ok=True)
    base.resize((512, 512), Image.Resampling.LANCZOS).save(
        docs_store / "ic_launcher_store_512.png", "PNG"
    )

    for folder, dim in mip.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        im = draw_icon(dim)
        im.save(out_dir / "ic_launcher.webp", "WEBP", quality=92, method=6)
        # round icon — тот же рисунок в круге (launcher сам маскирует на API 26+)
        im.save(out_dir / "ic_launcher_round.webp", "WEBP", quality=92, method=6)

    print("OK:", ", ".join(f"{k}={v}" for k, v in mip.items()))
    print("Store:", docs_store / "ic_launcher_store_512.png")


if __name__ == "__main__":
    main()
