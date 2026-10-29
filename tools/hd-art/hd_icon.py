"""Build the app icon: an adaptive icon (Android 8+) and legacy icons.

Design (after the original icon): a big purple bubble with the logo's
wood "HD" on it, and the purple and green penguins peeking up in front,
on an icy blue background.

Layers are drawn at 432 px (108 dp at xxxhdpi) and scaled down:
  mipmap-*/ic_launcher_foreground.png  - bubble, HD and penguins
  mipmap-*/ic_launcher_background.png  - icy gradient
  mipmap-*/ic_launcher_monochrome.png  - one-colour shape (themed icons)
  mipmap-*/ic_launcher.png, ic_launcher_round.png - for Android 7
  <out>/play_store_icon_512.png        - for the Play Store listing

Usage:
    python3 hd_icon.py <font.ttf> <res-dir> <penguins-dir> [<out-dir>]
<penguins-dir> holds cut_purple.png and cut_green.png (penguin cut-outs
from penguins_hd.jpg / penguins2_hd.jpg, see cut_penguins.py).
"""
import math
import os
import random
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hdbubble import colors_from, hd_bubble   # noqa: E402
from hd_logo import make_badge                # noqa: E402

N = 432                      # 108 dp at xxxhdpi
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def background():
    """Icy blue radial gradient with faint frost sparkles."""
    img = Image.new("RGB", (N, N))
    px = img.load()
    cx, cy = N * 0.5, N * 0.38
    inner, outer = (205, 228, 255), (44, 74, 160)
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - cx, y - cy) / (N * 0.75)
            t = min(1.0, d) ** 1.2
            px[x, y] = tuple(int(inner[i] + (outer[i] - inner[i]) * t)
                             for i in range(3))
    spark = Image.new("L", (N, N), 0)
    dr = ImageDraw.Draw(spark)
    rnd = random.Random(3)
    for _ in range(70):
        x, y = rnd.uniform(0, N), rnd.uniform(0, N)
        r = rnd.uniform(1.0, 2.6)
        dr.ellipse((x - r, y - r, x + r, y + r), fill=rnd.randint(60, 150))
    spark = spark.filter(ImageFilter.GaussianBlur(0.8))
    white = Image.new("RGB", (N, N), (255, 255, 255))
    return Image.composite(white, img, spark)


def foreground(font, res_dir, peng_dir):
    fg = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    # The big purple bubble, drawn at full size from the game's bubble.
    d = 214
    top = 84
    orig = Image.open(os.path.join(res_dir, "drawable", "bubble_6.png"))
    body = Image.open(os.path.join(res_dir, "drawable-nodpi", "bubble_6_hd.png")) \
                .convert("RGB").resize((d, d), Image.LANCZOS)
    bubble = hd_bubble(orig, colors_from(orig.convert("RGBA")), d, body)
    # A soft shadow under the bubble.
    sh = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse((N / 2 - d / 2 + 6, top + 10, N / 2 + d / 2 + 6, top + d + 10),
                               fill=(10, 20, 60, 110))
    fg = Image.alpha_composite(fg, sh.filter(ImageFilter.GaussianBlur(9)))
    fg.alpha_composite(bubble, (int(N / 2 - d / 2), top))

    # The wood "HD" on the bubble.
    badge = make_badge(font, 44)
    fg.alpha_composite(badge, (int(N / 2 - badge.width / 2), top + 30))

    # The penguins in front, facing each other, their (cut-off) feet below
    # the visible part of the icon.
    purple = Image.open(os.path.join(peng_dir, "cut_purple.png")).transpose(
        Image.FLIP_LEFT_RIGHT)
    green = Image.open(os.path.join(peng_dir, "cut_green.png")).transpose(
        Image.FLIP_LEFT_RIGHT)
    for img, x in ((purple, 66), (green, 222)):
        bbox = img.getbbox()
        img = img.crop(bbox)
        scale = 148.0 / img.width
        img = img.resize((int(img.width * scale), int(img.height * scale)),
                         Image.LANCZOS)
        y = 372 - img.height          # feet just below the 90 dp line
        shadow = Image.new("RGBA", img.size, (10, 20, 60, 0))
        shadow.putalpha(img.getchannel("A").point(lambda v: int(v * 0.45)))
        fg.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(5)), (x + 4, y + 6))
        fg.alpha_composite(img, (x, y))
    return fg


def monochrome(fg):
    """One-colour shape for Android 13 themed icons (the launcher tints it)."""
    a = fg.getchannel("A").point(lambda v: 255 if v > 128 else 0)
    mono = Image.new("RGBA", fg.size, (255, 255, 255, 0))
    mono.putalpha(a.filter(ImageFilter.GaussianBlur(0.7)))
    return mono


def legacy(bg, fg, round_mask):
    """A full icon for Android 7: the visible 72 dp middle of the layers."""
    full = bg.convert("RGBA")
    full.alpha_composite(fg)
    m = N * 18 // 108
    icon = full.crop((m, m, N - m, N - m))
    size = icon.width
    mask = Image.new("L", (size * 4, size * 4), 0)
    dr = ImageDraw.Draw(mask)
    if round_mask:
        dr.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    else:
        dr.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * 4 * 0.18,
                             fill=255)
    icon.putalpha(mask.resize((size, size), Image.LANCZOS))
    return icon


def main(font, res_dir, peng_dir, out_dir=None):
    bg = background()
    fg = foreground(font, res_dir, peng_dir)
    mono = monochrome(fg)
    for dens, f in DENSITIES.items():
        d = os.path.join(res_dir, "mipmap-" + dens)
        os.makedirs(d, exist_ok=True)
        s = int(round(108 * f))
        bg.resize((s, s), Image.LANCZOS).save(os.path.join(d, "ic_launcher_background.png"), optimize=True)
        fg.resize((s, s), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
        mono.resize((s, s), Image.LANCZOS).save(os.path.join(d, "ic_launcher_monochrome.png"), optimize=True)
        s = int(round(48 * f))
        legacy(bg, fg, False).resize((s, s), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"), optimize=True)
        legacy(bg, fg, True).resize((s, s), Image.LANCZOS).save(os.path.join(d, "ic_launcher_round.png"), optimize=True)
    if out_dir:
        store = bg.convert("RGBA")
        store.alpha_composite(fg)
        m = N * 18 // 108
        store.crop((m, m, N - m, N - m)).resize((512, 512), Image.LANCZOS) \
             .convert("RGB").save(os.path.join(out_dir, "play_store_icon_512.png"))
        prev = Image.new("RGBA", (5 * 220, 220), (60, 60, 60, 255))
        shapes = [legacy(bg, fg, True), legacy(bg, fg, False)]
        for i, ic in enumerate(shapes):
            prev.alpha_composite(ic.resize((200, 200), Image.LANCZOS), (10 + i * 220, 10))
        prev.alpha_composite(store.crop((m, m, N - m, N - m)).resize((200, 200)), (10 + 2 * 220, 10))
        prev.alpha_composite(mono.resize((200, 200)), (10 + 3 * 220, 10))
        prev.alpha_composite(legacy(bg, fg, True).resize((48, 48), Image.LANCZOS), (10 + 4 * 220 + 76, 86))
        prev.save(os.path.join(out_dir, "icon_preview.png"))


if __name__ == "__main__":
    main(*sys.argv[1:])
