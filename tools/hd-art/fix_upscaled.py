"""Clean up AI-upscaled art (4x, e.g. from Upscayl) and install it as HD
resources.

Fixes applied:
- defringe(): the original paletted PNGs store transparent pixels as a
  bright "key" colour (red, green...).  Upscalers blend that colour into
  the edges, leaving a coloured halo.  Edge pixels are repainted from the
  nearby solid colours.
- restore_texture(): upscalers smooth away fine, low-contrast texture (the
  scratches on the ice).  The original's fine detail, enlarged, is added
  back in areas the upscaler left flat.

Usage:
    python3 fix_upscaled.py <upscaled-dir> <res-dir>
where <upscaled-dir> holds <name>-UPSCALED.png files.
"""
import os
import sys

from PIL import Image, ImageChops, ImageFilter

SCALE = 4


def _key_colour(orig):
    t = orig.info.get("transparency")
    if orig.mode == "P" and isinstance(t, int):
        pal = orig.getpalette()
        return tuple(pal[t * 3:t * 3 + 3])
    return None


def _fill_from_solid(rgba, solid_mask):
    """Colour every pixel from the average of nearby solid pixels."""
    rgb = rgba.convert("RGB")
    out = rgb.copy()
    filled = Image.new("L", rgba.size, 0)
    for radius in (2, 4, 8, 16, 32):
        w = solid_mask.filter(ImageFilter.BoxBlur(radius))
        prem = Image.composite(rgb, Image.new("RGB", rgb.size), solid_mask)
        avg = prem.filter(ImageFilter.BoxBlur(radius))
        # Colour = average of solid neighbours = avg / w.
        wl = w.load()
        al = avg.load()
        ol = out.load()
        fl = filled.load()
        W, H = rgba.size
        for y in range(H):
            for x in range(W):
                if fl[x, y] or wl[x, y] < 8:
                    continue
                k = 255.0 / wl[x, y]
                r, g, b = al[x, y]
                ol[x, y] = (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)))
                fl[x, y] = 255
    return out


def defringe(up, orig):
    """Repaint the coloured halo left by the transparent key colour."""
    key = _key_colour(orig)
    up = up.convert("RGBA")
    if key is None:
        return up
    a = up.getchannel("A")
    solid = a.point(lambda v: 255 if v == 255 else 0).filter(ImageFilter.MinFilter(5))
    near_edge = a.point(lambda v: 255 if v < 255 else 0).filter(ImageFilter.MaxFilter(9))
    fill = _fill_from_solid(up, solid)
    px, fp, ap, ne = up.load(), fill.load(), a.load(), near_edge.load()
    W, H = up.size
    for y in range(H):
        for x in range(W):
            if not ne[x, y]:
                continue
            r, g, b, al = px[x, y]
            d = ((r - key[0]) ** 2 + (g - key[1]) ** 2 + (b - key[2]) ** 2) ** 0.5
            if al < 255 or d < 140:
                fr, fg, fb = fp[x, y]
                px[x, y] = (fr, fg, fb, al)
    return up


def restore_texture(up, orig, strength=0.85, flat_threshold=10.0):
    """Add the original's fine texture back where the upscaled image is flat."""
    up = up.convert("RGB")
    big = orig.convert("RGB").resize(up.size, Image.BICUBIC)
    detail_src = big.filter(ImageFilter.GaussianBlur(SCALE * 1.5))
    # Local contrast of the upscaled image: low = flat (texture was lost).
    g = up.convert("L")
    gb = g.filter(ImageFilter.GaussianBlur(SCALE * 2))
    diff = ImageChops.difference(g, gb).filter(ImageFilter.GaussianBlur(SCALE * 2))
    weight = diff.point(lambda v: int(255 * strength * max(0.0, 1.0 - v / flat_threshold)))
    out = up.copy()
    op, bp, dp, wp = out.load(), big.load(), detail_src.load(), weight.load()
    W, H = up.size
    for y in range(H):
        for x in range(W):
            w = wp[x, y]
            if w == 0:
                continue
            k = w / 255.0
            o = op[x, y]
            b = bp[x, y]
            s = dp[x, y]
            op[x, y] = tuple(max(0, min(255, int(o[i] + (b[i] - s[i]) * k))) for i in range(3))
    return out


# name -> (fixes, output format)
JOBS = {
    "background":  (["texture"], "jpg"),
    "background2": (["texture"], "jpg"),
    "win_panel":   ([], "jpg"),
    "lose_panel":  ([], "jpg"),
    "pause_panel": ([], "jpg"),
    "penguins":    ([], "jpg"),
    "penguins2":   ([], "jpg"),
    "home_screen": ([], "jpg"),
    "splash":      ([], "jpg"),
    "victory":     ([], "jpg"),
    "hurry":       (["defringe"], "png"),
    "compressor":  (["defringe"], "png"),
    "compressor_body": ([], "png"),
    "bubble_font": (["defringe"], "png"),
}


def main(up_dir, res_dir):
    src_dir = os.path.join(res_dir, "drawable")
    out_dir = os.path.join(res_dir, "drawable-nodpi")
    os.makedirs(out_dir, exist_ok=True)
    done = []
    for name, (fixes, fmt) in JOBS.items():
        up_path = os.path.join(up_dir, name + "-UPSCALED.png")
        if not os.path.exists(up_path):
            continue
        orig_path = next(os.path.join(src_dir, name + e) for e in (".png", ".jpg")
                         if os.path.exists(os.path.join(src_dir, name + e)))
        orig = Image.open(orig_path)
        up = Image.open(up_path)
        expect = (orig.width * SCALE, orig.height * SCALE)
        if up.size != expect:
            print(f"skip {name}: size {up.size}, expected {expect}")
            continue
        if "defringe" in fixes:
            up = defringe(up, orig)
        if "texture" in fixes:
            up = restore_texture(up, orig)
        dst = os.path.join(out_dir, f"{name}_hd.{fmt}")
        if fmt == "jpg":
            up.convert("RGB").save(dst, quality=90, optimize=True, progressive=True)
        else:
            up.convert("RGBA").save(dst, optimize=True)
        done.append((name, os.path.getsize(dst)))
    for name, size in done:
        print(f"{name:16s} {size // 1024:6d} KB")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
