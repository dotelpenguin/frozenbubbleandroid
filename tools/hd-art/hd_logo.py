"""Add a stylised "HD" after the "Frozen Bubble" logo on the HD splash and
home screen art, in the logo's own style: wood-grain letters, a thick
white outline and a soft drop shadow, each letter tilted a little like the
hand-drawn logo.

The letter shapes come from the Luckiest Guy font (Apache 2.0, via
npm @fontsource/luckiest-guy); only the rendered image ships in the app.

Usage:
    python3 hd_logo.py <font.ttf> <res-dir> [--preview <dir>]
It reads tools/hd-art/base/{splash,home_screen}_hd.jpg (the art without
the badge, copied from the app's art on first run, kept out of the app)
and writes <res-dir>/drawable-nodpi/{splash,home_screen}_hd.jpg.
"""
import math
import os
import shutil
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

# Wood colours sampled from the logo letters, dark to light.
WOOD = [(0x3e, 0x1b, 0x06), (0x59, 0x2f, 0x16), (0x6c, 0x39, 0x1c),
        (0x7b, 0x55, 0x31), (0xa0, 0x64, 0x32)]

# Where the badge goes on each screen: (left, top, letter height) in the
# HD art's pixels, just after "BUBBLE".
PLACES = {
    "splash_hd":      (1262, 852, 130),
    "home_screen_hd": (1676, 128, 96),
}


def wood_texture(w, h, seed):
    """Swirly wood grain in the logo's browns."""
    rng = np.random.default_rng(seed)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    # Low-frequency warp so the grain bends like the drawn letters.
    warp = Image.fromarray((rng.random((max(2, h // 40), max(2, w // 40))) * 255)
                           .astype(np.uint8)).resize((w, h), Image.BICUBIC)
    warp = np.asarray(warp, np.float32) / 255.0
    grain = np.sin((y * 0.075) + (x * 0.018) + (warp * 11.0))
    fine = np.asarray(Image.fromarray((rng.random((h, w)) * 255).astype(np.uint8))
                      .filter(ImageFilter.GaussianBlur(1.2)), np.float32) / 255.0
    v = 0.62 + 0.33 * grain + 0.22 * (fine - 0.5)
    v = np.clip(v, 0, 1) * (len(WOOD) - 1)
    lo = np.floor(v).astype(int)
    hi = np.minimum(lo + 1, len(WOOD) - 1)
    t = (v - lo)[..., None]
    pal = np.array(WOOD, np.float32)
    rgb = pal[lo] * (1 - t) + pal[hi] * t
    return Image.fromarray(rgb.astype(np.uint8), "RGB")


def letter_mask(font, ch, size, angle):
    """One letter as a mask, tilted like the hand-drawn logo."""
    pad = size
    img = Image.new("L", (size * 3, size * 3), 0)
    ImageDraw.Draw(img).text((pad, pad), ch, font=font, fill=255)
    img = img.crop(img.getbbox())
    return img.rotate(angle, resample=Image.BICUBIC, expand=True)


def make_badge(font_path, letter_h, seed=7):
    """The "HD" badge as an RGBA image."""
    font = ImageFont.truetype(font_path, int(letter_h * 1.25))
    letters = [("H", -7.0, 0.00), ("D", 6.0, -0.07)]   # (char, tilt, raise)
    masks = [letter_mask(font, ch, letter_h, a) for ch, a, _ in letters]
    # Scale each letter to the wanted height.
    masks = [m.resize((max(1, round(m.width * letter_h / m.height)), letter_h),
                      Image.LANCZOS) for m in masks]
    border = int(letter_h * 0.16)
    spacing = int(letter_h * -0.02)
    w = sum(m.width for m in masks) + spacing + border * 4
    h = int(letter_h * 1.25) + border * 4
    shape = Image.new("L", (w, h), 0)
    x = border * 2
    for m, (_, _, rise) in zip(masks, letters):
        y = border * 2 + int(letter_h * 0.12) + int(rise * letter_h)
        shape.paste(m, (x, y), m)
        x += m.width + spacing

    # White outline: a fat, rounded blob around the letters.
    outline = shape.filter(ImageFilter.MaxFilter(_odd(border * 2)))
    outline = outline.filter(ImageFilter.GaussianBlur(border * 0.35))
    outline = outline.point(lambda v: 255 if v > 90 else 0)
    outline = outline.filter(ImageFilter.GaussianBlur(1.2))

    # Drop shadow under the outline.
    shadow = outline.filter(ImageFilter.GaussianBlur(border * 0.6))
    off = (int(letter_h * 0.035), int(letter_h * 0.05))

    badge = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sh = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sh.paste((20, 12, 6, 150), (off[0], off[1]), shadow)
    badge = Image.alpha_composite(badge, sh)
    white = Image.new("RGBA", (w, h), (250, 250, 248, 0))
    white.putalpha(outline)
    badge = Image.alpha_composite(badge, white)

    # Wood letters, darker towards their edges for depth, with a thin dark
    # rim like the drawn logo.
    wood = wood_texture(w, h, seed).convert("RGBA")
    edge = ImageChops.subtract(shape, shape.filter(ImageFilter.MinFilter(
        _odd(max(3, letter_h // 14)))))
    depth = ImageChops.invert(shape.filter(ImageFilter.MinFilter(
        _odd(max(3, letter_h // 9)))).filter(ImageFilter.GaussianBlur(letter_h / 18)))
    dark = Image.new("RGBA", (w, h), (30, 12, 2, 0))
    dark.putalpha(ImageChops.multiply(depth, shape).point(lambda v: v * 0.45))
    wood = Image.alpha_composite(wood, dark)
    wood.putalpha(shape)
    badge = Image.alpha_composite(badge, wood)
    rim = Image.new("RGBA", (w, h), (45, 18, 4, 0))
    rim.putalpha(edge.point(lambda v: int(v * 0.8)))
    badge = Image.alpha_composite(badge, rim)
    # A soft highlight along the top of the letters.
    hi = ImageChops.subtract(shape, ImageChops.offset(shape, 0, max(2, letter_h // 30)))
    gloss = Image.new("RGBA", (w, h), (255, 225, 180, 0))
    gloss.putalpha(hi.filter(ImageFilter.GaussianBlur(1.5)).point(lambda v: int(v * 0.5)))
    badge = Image.alpha_composite(badge, gloss)
    return badge


def _odd(n):
    n = int(n)
    return n if n % 2 else n + 1


def main(font_path, res_dir, preview_dir=None):
    out_dir = os.path.join(res_dir, "drawable-nodpi")
    for name, (left, top, letter_h) in PLACES.items():
        art = os.path.join(out_dir, name + ".jpg")
        base_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "base")
        os.makedirs(base_dir, exist_ok=True)
        base = os.path.join(base_dir, name + ".jpg")
        if not os.path.exists(base):
            shutil.copyfile(art, base)
        img = Image.open(base).convert("RGBA")
        badge = make_badge(font_path, letter_h)
        img.alpha_composite(badge, (left, top))
        img.convert("RGB").save(art, quality=90, optimize=True, progressive=True)
        print(f"{name}: badge {badge.size} at {(left, top)}")
        if preview_dir:
            img.convert("RGB").resize((img.width // 3, img.height // 3), Image.LANCZOS) \
               .save(os.path.join(preview_dir, name + "_preview.png"))


if __name__ == "__main__":
    args = sys.argv[1:]
    preview = None
    if "--preview" in args:
        i = args.index("--preview")
        preview = args[i + 1]
        del args[i:i + 2]
    main(args[0], args[1], preview)
