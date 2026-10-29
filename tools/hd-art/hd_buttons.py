"""Draw the in-game round buttons (back, next song, pause, play) at 128 px.

Styles:
  blue   - the current glossy blue circle; every symbol light blue with a
           dark outline (the back arrow used to be grey metal)
  bubble - a purple game bubble (like the app icon) with white symbols
  wood   - a wood disc with the logo's white outline and cream symbols

Usage:
  python3 hd_buttons.py <res-dir> <style> [<out-dir>]
Without <out-dir> it writes the app's drawable-nodpi/*_button_hd.png.

The game uses "blue".  That style repaints the original blue play
button, so <res-dir>/drawable-nodpi/play_button_hd.png must be the
original (from commit bf03eeb or earlier), e.g.:
  mkdir -p /tmp/orig/drawable-nodpi
  git show bf03eeb:app/src/main/res/drawable-nodpi/play_button_hd.png \\
      > /tmp/orig/drawable-nodpi/play_button_hd.png
  python3 hd_buttons.py /tmp/orig blue /tmp/out
then copy /tmp/out/blue_<name>.png to drawable-nodpi/<name>_button_hd.png.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hdbubble import colors_from, hd_bubble   # noqa: E402
from hd_logo import wood_texture              # noqa: E402

S = 128          # final size
SS = 4           # supersampling for the symbols
NAMES = ["back", "next_song", "pause", "play"]


def symbol_polys(name):
    """Symbol shapes in a 0..1 box, as lists of polygons."""
    if name == "back":
        return [[(0.20, 0.50), (0.50, 0.22), (0.50, 0.39), (0.80, 0.39),
                 (0.80, 0.61), (0.50, 0.61), (0.50, 0.78)]]
    if name == "next_song":
        return [[(0.20, 0.27), (0.45, 0.50), (0.20, 0.73)],
                [(0.45, 0.27), (0.70, 0.50), (0.45, 0.73)],
                [(0.71, 0.27), (0.80, 0.27), (0.80, 0.73), (0.71, 0.73)]]
    if name == "pause":
        return [[(0.30, 0.25), (0.45, 0.25), (0.45, 0.75), (0.30, 0.75)],
                [(0.55, 0.25), (0.70, 0.25), (0.70, 0.75), (0.55, 0.75)]]
    if name == "play":
        return [[(0.33, 0.24), (0.78, 0.50), (0.33, 0.76)]]
    raise ValueError(name)


def symbol_mask(name, inset=0.0):
    big = S * SS
    m = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(m)
    for poly in symbol_polys(name):
        d.polygon([(x * big, y * big) for x, y in poly], fill=255)
    return m


def draw_symbol(base, name, fill_top, fill_bottom, outline, outline_px, shadow):
    """Symbol with a vertical gradient fill, an outline and a soft shadow."""
    big = S * SS
    m = symbol_mask(name)
    grown = m.filter(ImageFilter.MaxFilter(_odd(outline_px * SS * 2)))
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    if shadow:
        sh = Image.new("RGBA", (big, big), shadow)
        sh.putalpha(grown.filter(ImageFilter.GaussianBlur(3 * SS)))
        out.alpha_composite(sh, (int(1.5 * SS), int(2 * SS)))
    ol = Image.new("RGBA", (big, big), outline)
    ol.putalpha(grown)
    out.alpha_composite(ol)
    grad = np.zeros((big, big, 4), np.uint8)
    t = np.linspace(0, 1, big)[:, None]
    for i in range(3):
        grad[:, :, i] = (fill_top[i] * (1 - t) + fill_bottom[i] * t).astype(np.uint8)
    grad[:, :, 3] = 255
    g = Image.fromarray(grad, "RGBA")
    g.putalpha(m)
    out.alpha_composite(g)
    out = out.resize((S, S), Image.LANCZOS)
    base.alpha_composite(out)
    return base


def _odd(n):
    n = max(1, int(n))
    return n if n % 2 else n + 1


def blue_disc(res_dir):
    """The current blue glossy disc, with its old symbol painted out."""
    import cv2
    src = Image.open(os.path.join(res_dir, "drawable-nodpi", "play_button_hd.png")).convert("RGBA")
    arr = np.array(src)
    rgb = cv2.cvtColor(arr[:, :, :3], cv2.COLOR_RGB2BGR)
    # The symbol: the triangle and its outline, from its known shape.
    m = np.array(symbol_mask("play").resize((S, S), Image.LANCZOS))
    m = cv2.dilate((m > 20).astype(np.uint8) * 255, np.ones((9, 9), np.uint8))
    clean = cv2.inpaint(rgb, m, 6, cv2.INPAINT_TELEA)
    clean = cv2.cvtColor(clean, cv2.COLOR_BGR2RGB)
    out = Image.fromarray(np.dstack([clean, arr[:, :, 3]]), "RGBA")
    return out


def bubble_disc(res_dir):
    orig = Image.open(os.path.join(res_dir, "drawable", "bubble_6.png"))
    body = Image.open(os.path.join(res_dir, "drawable-nodpi", "bubble_6_hd.png")) \
                .convert("RGB").resize((S, S), Image.LANCZOS)
    return hd_bubble(orig, colors_from(orig.convert("RGBA")), S, body)


def wood_disc():
    big = S * SS
    disc = Image.new("L", (big, big), 0)
    ImageDraw.Draw(disc).ellipse((6 * SS, 6 * SS, big - 6 * SS, big - 6 * SS), fill=255)
    ring = Image.new("L", (big, big), 0)
    ImageDraw.Draw(ring).ellipse((1 * SS, 1 * SS, big - 1 * SS, big - 1 * SS), fill=255)
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    white = Image.new("RGBA", (big, big), (250, 250, 248, 255))
    white.putalpha(ring)
    out.alpha_composite(white)
    # Coarser grain at this small size: make it at a third and enlarge.
    wood = wood_texture(big // 3, big // 3, 11).resize((big, big), Image.BICUBIC) \
                                              .convert("RGBA")
    # Darker towards the rim for a rounded look.
    yy, xx = np.mgrid[0:big, 0:big]
    r = np.hypot(xx - big / 2, yy - big / 2) / (big / 2)
    shade = (np.clip((r - 0.55) / 0.45, 0, 1) * 120).astype(np.uint8)
    dark = Image.new("RGBA", (big, big), (30, 12, 2, 0))
    dark.putalpha(Image.fromarray(shade))
    wood.alpha_composite(dark)
    wood.putalpha(disc)
    out.alpha_composite(wood)
    rim = Image.new("L", (big, big), 0)
    ImageDraw.Draw(rim).ellipse((6 * SS, 6 * SS, big - 6 * SS, big - 6 * SS),
                                outline=255, width=2 * SS)
    rimc = Image.new("RGBA", (big, big), (45, 18, 4, 255))
    rimc.putalpha(rim)
    out.alpha_composite(rimc)
    return out.resize((S, S), Image.LANCZOS)


def make(res_dir, style, name):
    if style == "blue":
        img = blue_disc(res_dir)
        return draw_symbol(img, name, (225, 242, 255), (150, 200, 240),
                           (10, 30, 70, 255), 1.6, None)
    if style == "bubble":
        img = bubble_disc(res_dir).copy()
        return draw_symbol(img, name, (255, 255, 255), (235, 225, 245),
                           (60, 10, 70, 255), 1.6, (20, 0, 30, 140))
    if style == "wood":
        img = wood_disc()
        return draw_symbol(img, name, (255, 248, 230), (235, 215, 180),
                           (45, 18, 4, 255), 1.6, (20, 8, 0, 140))
    raise ValueError(style)


def main(res_dir, style, out_dir=None):
    for name in NAMES:
        img = make(res_dir, style, name)
        if out_dir:
            img.save(os.path.join(out_dir, f"{style}_{name}.png"))
        else:
            img.save(os.path.join(res_dir, "drawable-nodpi", f"{name}_button_hd.png"),
                     optimize=True)


if __name__ == "__main__":
    main(*sys.argv[1:])
