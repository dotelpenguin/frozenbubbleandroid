"""Draw the wooden plank behind the Arcade score (score_plank_hd.png).

It matches the "1 UP" sign it covers: warm brown planks with horizontal
grain, darker edges, a nail in each corner and a soft shadow below.
The game draws the plank's left and right halves to any width (the
plank grows with the score), so it is made wide: 240 x 30 game pixels
at 4x, plus room for the shadow.

Usage:
    python3 hd_score_plank.py <res-dir> [<preview.png>]
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

K = 4                       # HD art is 4x the game's pixels
W, H = 240 * K, 30 * K      # the plank
SHADOW = 4 * K              # room below for the shadow
EDGE = (92, 48, 20)
MID = (156, 92, 44)
LIGHT = (184, 118, 62)


def plank():
    rng = np.random.default_rng(5)
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    # Long horizontal streaks, slightly wavy.
    streaks = np.asarray(Image.fromarray((rng.random((H // 6, W // 60)) * 255)
                         .astype(np.uint8)).resize((W, H), Image.BICUBIC),
                         np.float32) / 255.0
    wave = np.sin(x / 90.0 + y / 14.0 + streaks * 6.0)
    fine = np.asarray(Image.fromarray((rng.random((H, W)) * 255).astype(np.uint8))
                      .filter(ImageFilter.GaussianBlur(1.0))
                      .resize((W, H)), np.float32) / 255.0
    v = 0.55 + 0.22 * wave + 0.25 * (streaks - 0.5) + 0.12 * (fine - 0.5)
    v = np.clip(v, 0, 1)[..., None]
    rgb = np.array(MID, np.float32) * (1 - v) + np.array(LIGHT, np.float32) * v
    # Darker towards the edges (like the sign's worn border).
    ex = np.minimum(x, W - 1 - x) / (10.0 * K)
    ey = np.minimum(y, H - 1 - y) / (6.0 * K)
    e = np.clip(np.minimum(ex, ey), 0, 1)[..., None]
    rgb = np.array(EDGE, np.float32) * (1 - e) + rgb * e
    img = Image.fromarray(rgb.astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W - 1, H - 1), outline=(60, 28, 10, 255), width=K)
    for nx in (6 * K, W - 6 * K):
        for ny in (5 * K, H - 5 * K):
            r = 1.6 * K
            d.ellipse((nx - r, ny - r, nx + r, ny + r), fill=(235, 225, 210, 255),
                      outline=(70, 50, 35, 255), width=max(1, K // 2))
    return img


def main(res_dir, preview=None):
    out = Image.new("RGBA", (W, H + SHADOW), (0, 0, 0, 0))
    sh = Image.new("L", out.size, 0)
    ImageDraw.Draw(sh).rectangle((2 * K, 3 * K, W - 2 * K, H + 2 * K), fill=120)
    shadow = Image.new("RGBA", out.size, (10, 20, 60, 255))
    shadow.putalpha(sh.filter(ImageFilter.GaussianBlur(2 * K)))
    out.alpha_composite(shadow)
    out.alpha_composite(plank())
    out.save(os.path.join(res_dir, "drawable-nodpi", "score_plank_hd.png"),
             optimize=True)
    if preview:
        out.save(preview)


if __name__ == "__main__":
    main(*sys.argv[1:])
