#!/usr/bin/env python3
"""Generates short game sounds (WAV, 44.1 kHz mono 16-bit) into app/src/main/res/raw/.

Deterministic synth (no random) so the repo stays reproducible: run it again after
tweaking envelopes instead of committing hand-edited binaries.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"


def render(samples):
    clipped = [max(-1.0, min(1.0, s)) for s in samples]
    return b"".join(struct.pack("<h", int(s * 32767)) for s in clipped)


def write_wav(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(render(samples))
    print(f"wrote {path} ({len(samples) / RATE * 1000:.0f} ms)")


def tone(freq, dur, amp=0.6, decay=8.0, sweep=0.0):
    """Sine burst with exponential decay; sweep shifts frequency over time."""
    n = int(RATE * dur)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        f = freq + sweep * t / dur
        phase += 2 * math.pi * f / RATE
        env = math.exp(-decay * t / dur)
        out.append(amp * env * math.sin(phase))
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    return [sum(p[i] if i < len(p) else 0.0 for p in parts) for i in range(n)]


def seq(*parts):
    out = []
    for p in parts:
        out.extend(p)
    return out


def main():
    # Тихий деревянный "ток" — обычный ход.
    write_wav("snd_move.wav", mix(
        tone(620, 0.07, amp=0.45, decay=10, sweep=-180),
        tone(310, 0.07, amp=0.25, decay=12),
    ))
    # Более низкий и резкий "щёлк" — взятие.
    write_wav("snd_capture.wav", mix(
        tone(340, 0.11, amp=0.6, decay=9, sweep=-120),
        tone(170, 0.11, amp=0.35, decay=10),
        tone(950, 0.03, amp=0.2, decay=14),
    ))
    # Восходящее арпеджио — коронование в дамки.
    write_wav("snd_crown.wav", seq(
        tone(523, 0.09, amp=0.4, decay=5),
        tone(659, 0.09, amp=0.4, decay=5),
        tone(784, 0.16, amp=0.45, decay=4),
    ))
    # Фанфара — победа.
    write_wav("snd_win.wav", seq(
        tone(523, 0.12, amp=0.4, decay=4),
        tone(659, 0.12, amp=0.4, decay=4),
        tone(784, 0.12, amp=0.4, decay=4),
        mix(tone(1047, 0.35, amp=0.35, decay=4), tone(784, 0.35, amp=0.2, decay=4)),
    ))
    # Нисходящие тоны — поражение.
    write_wav("snd_lose.wav", seq(
        tone(392, 0.18, amp=0.4, decay=4),
        tone(330, 0.18, amp=0.4, decay=4),
        tone(262, 0.3, amp=0.4, decay=4),
    ))


if __name__ == "__main__":
    main()
