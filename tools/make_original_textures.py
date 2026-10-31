#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Regenerate the HandyBell bell-body textures.

These are NOT recoloured copies of the vanilla texture. The only thing borrowed
from Mojang's asset is the ALPHA CHANNEL, i.e. which pixels are opaque -- that
shape is dictated by BellModel's UV layout and cannot be invented. Every RGB
value is computed here from scratch by `shade()`, which is a lighting model
written for this mod.

Usage:
    python tools/make_original_textures.py --mc-jar path/to/26.3.jar

Requires Pillow.
"""
import argparse
import io
import os
import sys
import zipfile

try:
    from PIL import Image
except ImportError:
    sys.exit('Pillow is required: pip install pillow')

VANILLA_ENTRY = 'assets/minecraft/textures/entity/bell/bell_body.png'

# Base hues per dye colour.
COLORS = {
    'white': (0xF2, 0xF6, 0xF4),
    'orange': (0xE8, 0x74, 0x1B),
    'magenta': (0xC0, 0x4A, 0xB4),
    'light_blue': (0x3F, 0xAC, 0xD4),
    'yellow': (0xE8, 0xC5, 0x3A),
    'lime': (0x7C, 0xBE, 0x22),
    'pink': (0xEE, 0x86, 0xA5),
    'gray': (0x63, 0x67, 0x6B),
    'light_gray': (0xB2, 0xB2, 0xAC),
    'cyan': (0x1E, 0x93, 0x93),
    'purple': (0x84, 0x30, 0xB0),
    'blue': (0x3E, 0x45, 0xA2),
    'brown': (0x7C, 0x50, 0x30),
    'green': (0x58, 0x74, 0x18),
    'red': (0xA8, 0x2C, 0x25),
    'black': (0x2B, 0x2B, 0x30),
}

# Yellow is the default group: gold body plus a red cord, so it stays visually
# distinct from an ordinary bell without being repainted.
KEEP_ORIGINAL = {'yellow'}

# Cylindrical shading curves, indexed by column within a face.
# y <= 12 uses period 6, y >= 13 uses period 8.
CYL6 = [0.74, 0.96, 1.10, 1.06, 0.94, 0.72]
CYL8 = [0.72, 0.92, 1.04, 1.10, 1.08, 1.02, 0.90, 0.70]

# Horizontal casting seams, so the body reads as separate metal parts.
SEAMS = {
    6: 0.88,
    8: 0.92,
    12: 0.80,
    13: 0.92,
    20: 0.82,
    21: 0.78,
    22: 0.70,
}


def shade(x, y):
    """Lighting model. Takes no input from the vanilla texture."""
    if y <= 5:            # cap
        v = 1.15 - 0.02 * y
    elif y <= 12:         # body
        v = 1.14 - 0.015 * (y - 6)
    elif y <= 20:         # skirt
        v = 1.05 - 0.020 * (y - 13)
    else:                 # bottom ring
        v = 0.95 - 0.06 * (y - 21)

    c = CYL6[x % 6] if y <= 12 else CYL8[x % 8]
    v *= SEAMS.get(y, 1.0)

    # Deterministic dither: metal is never perfectly even. No RNG, so the
    # output is byte-identical on every machine.
    n = ((x * 7 + y * 13) ^ (x * 3 + y * 5)) & 7
    v *= 0.975 + 0.008 * n

    return v * c


def draw(rgb, mask_px, w, h):
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    tr, tg, tb = rgb
    for y in range(h):
        for x in range(w):
            a = mask_px[x, y][3]
            if a == 0:
                continue
            f = shade(x, y)
            # Quantise to 5 levels: a continuous gradient smears at 32 px.
            idx = int(round((f - 0.50) / 0.20))
            idx = 0 if idx < 0 else (4 if idx > 4 else idx)
            f = 0.70 + idx * 0.11
            px[x, y] = (min(255, int(tr * f)), min(255, int(tg * f)),
                        min(255, int(tb * f)), a)
    return img


def add_cord(img):
    """Yellow group: a red cord wrapped around the body."""
    px = img.load()
    rows = {9: (0xD9, 0x3A, 0x2B), 10: (0x7E, 0x22, 0x1B)}
    for y, base in rows.items():
        for x in range(0, 24):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            f = 0.90 + 0.20 * lum
            px[x, y] = (min(255, int(base[0] * f)), min(255, int(base[1] * f)),
                        min(255, int(base[2] * f)), a)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--mc-jar', required=True,
                    help='path to the Minecraft 26.3 client jar')
    ap.add_argument('--out', default=None,
                    help='output directory (default: src/main/resources/.../bell)')
    args = ap.parse_args()

    here = os.path.dirname(os.path.abspath(__file__))
    out = args.out or os.path.join(
        here, '..', 'src', 'main', 'resources',
        'assets', 'handybell', 'textures', 'entity', 'bell')
    out = os.path.normpath(out)
    os.makedirs(out, exist_ok=True)

    with zipfile.ZipFile(args.mc_jar) as z:
        src = Image.open(io.BytesIO(z.read(VANILLA_ENTRY))).convert('RGBA')

    w, h = src.size
    sp = src.load()

    # Read the alpha channel only. RGB from the vanilla texture is discarded
    # here and never referenced below.
    alpha = [[sp[x, y][3] for x in range(w)] for y in range(h)]

    for name, rgb in COLORS.items():
        mask = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        mk = mask.load()
        for y in range(h):
            for x in range(w):
                if alpha[y][x]:
                    mk[x, y] = (0, 0, 0, 255)

        img = draw(rgb, mk, w, h)
        if name in KEEP_ORIGINAL:
            add_cord(img)
        img = img.resize((w * 2, h * 2), Image.NEAREST)
        img.save(os.path.join(out, 'bell_body_%s.png' % name), 'PNG')

    print('wrote %d textures to %s' % (len(COLORS), out))


if __name__ == '__main__':
    main()
