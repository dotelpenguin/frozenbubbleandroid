"""High-resolution Frozen Bubble bubbles, built from the 32x32 originals.

The soft marbled body is taken from the original and enlarged smoothly
(it is low-detail by design); the parts that read as blurry when the
original is blown up -- the circular edge, near-black outline, dark
colour band and bright inner rim -- are redrawn crisply at full size,
using colours sampled from the original.
"""
import math

from PIL import Image, ImageFilter

SCALE = 4                 # 32 px originals -> 128 px
SS = 4                    # supersampling for anti-aliased edges


def _ring_avg(im, r0, r1, cx=16.0, cy=16.0):
    """Average RGB of opaque pixels whose centre lies between radii r0..r1."""
    acc, n = [0, 0, 0], 0
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] < 128:
                continue
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r0 <= r < r1:
                for i in range(3):
                    acc[i] += p[i]
                n += 1
    return tuple(a // max(n, 1) for a in acc)


def _fill_edge_from_inside(im, keep_r, cx=16.0, cy=16.0):
    """Replace pixels outside keep_r with the pixel found further in along the
    same ray, so the old outline does not smear into the enlarged body."""
    src = im.copy()
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > keep_r:
                k = (keep_r - 0.6) / max(r, 1e-6)
                sx = min(im.width - 1, max(0, int(cx + dx * k)))
                sy = min(im.height - 1, max(0, int(cy + dy * k)))
                p = src.getpixel((sx, sy))
                out.putpixel((x, y), p[:3] + (255,))
    return out


def colors_from(orig):
    """Band colours derived from the bubble's main body colour (sampling the
    1 px bands directly picks up too much of the body)."""
    body = _ring_avg(orig, 5.0, 12.0)
    return {
        "outline": (6, 6, 12),
        "dark": tuple(int(c * 0.42) for c in body),
        "rim": tuple(int(c + (255 - c) * 0.78) for c in body),
    }


def hd_body(orig, size=None):
    """The marbled body of a 32x32 bubble, enlarged smoothly (RGB)."""
    orig = orig.convert("RGBA")
    size = size or orig.width * SCALE
    body_src = _fill_edge_from_inside(orig, keep_r=11.8)
    body = body_src.convert("RGB").resize((size, size), Image.LANCZOS)
    return body.filter(ImageFilter.UnsharpMask(radius=2, percent=50, threshold=2))


def hd_bubble(orig, colors=None, size=None, body_hd=None):
    """Return an RGBA high-resolution bubble.

    orig    -- 32x32 RGBA original bubble
    colors  -- band colours (derived from orig when omitted)
    body_hd -- optional ready-made high-resolution RGB body
    """
    orig = orig.convert("RGBA")
    colors = colors or colors_from(orig)
    size = size or orig.width * SCALE
    body = body_hd if body_hd is not None else hd_body(orig, size)

    # Radii in output pixels (original bands are ~1 px each, x4).
    R = size / 2.0
    k = size / 32.0
    r_outline = R - 0.85 * k        # near-black outline, outermost
    r_dark = r_outline - 0.75 * k   # dark colour band
    r_rim_out = r_dark - 0.05 * k   # bright inner rim, crisp outer edge
    r_rim_in = r_rim_out - 1.6 * k  # ...fading into the body

    big = size * SS
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    o_px = out.load()
    b_px = body.load()
    cx = cy = size / 2.0
    oc, dc, rc = colors["outline"], colors["dark"], colors["rim"]
    for y in range(size):
        for x in range(size):
            # Supersample coverage of each band for anti-aliasing.
            cov_total = cov_body = cov_rim = cov_dark = cov_out = 0
            for sy in range(SS):
                for sx in range(SS):
                    px = x + (sx + 0.5) / SS - cx
                    py = y + (sy + 0.5) / SS - cy
                    r = math.hypot(px, py)
                    if r > R:
                        continue
                    cov_total += 1
                    if r > r_outline:
                        cov_out += 1
                    elif r > r_dark:
                        cov_dark += 1
                    elif r > r_rim_in:
                        cov_rim += 1
                    else:
                        cov_body += 1
            if cov_total == 0:
                continue
            n = float(cov_total)
            bc = b_px[x, y]
            # Rim: brightest at its outer edge, blending into the body.
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            t = min(1.0, max(0.0, (r - r_rim_in) / (r_rim_out - r_rim_in)))
            t = t ** 1.3
            rim_col = tuple(bc[i] + (rc[i] - bc[i]) * t for i in range(3))
            col = [0.0, 0.0, 0.0]
            for c_, w in ((oc, cov_out), (dc, cov_dark), (rim_col, cov_rim), (bc, cov_body)):
                for i in range(3):
                    col[i] += c_[i] * w / n
            o_px[x, y] = (int(col[0]), int(col[1]), int(col[2]),
                          int(255 * cov_total / (SS * SS)))
    return out


def _smooth_mask(mask32, size, blur=2.2):
    """Enlarge a hard 32 px mask into a smooth, anti-aliased mask."""
    m = mask32.resize((size, size), Image.BICUBIC).filter(ImageFilter.GaussianBlur(blur))
    # Steep curve around 50%: crisp edge with about 1.5 px of anti-aliasing.
    return m.point(lambda v: 0 if v < 100 else (255 if v > 156 else int((v - 100) * 255 / 56)))


def hd_colourblind(base32, cb32, size=None):
    """Colourblind bubble: the HD base bubble with the symbol redrawn crisply.

    The symbol is found as the difference between the colourblind and the
    plain original, enlarged as a smooth mask, filled from the enlarged
    colourblind art, and given a thin dark outline like the original.
    """
    base32, cb32 = base32.convert("RGBA"), cb32.convert("RGBA")
    size = size or base32.width * SCALE
    diff = Image.new("L", base32.size, 0)
    dp, bp, cp = diff.load(), base32.load(), cb32.load()
    for y in range(base32.height):
        for x in range(base32.width):
            a, b = bp[x, y], cp[x, y]
            r = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if r < 13.5 and max(abs(a[i] - b[i]) for i in range(3)) > 38:
                dp[x, y] = 255
    # Close small holes so each symbol is one solid shape.
    diff = diff.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
    mask = _smooth_mask(diff, size)
    ring = mask.filter(ImageFilter.MaxFilter(7))
    body = hd_body(base32, size)
    sym = hd_body(cb32, size)
    sym_avg = _ring_avg(cb32.convert("RGBA"), 0, 9)
    dark = Image.new("RGB", (size, size), tuple(int(c * 0.28) for c in sym_avg))
    body = Image.composite(dark, body, ring)        # outline
    body = Image.composite(sym, body, mask)         # symbol fill
    return hd_bubble(base32, size=size, body_hd=body)


# --- Colourblind symbols, drawn as clean shapes --------------------------
from PIL import ImageDraw, ImageChops

# Coordinates are in the original 32 px space.
def _symbol_shapes(index):
    """Return (list of shape specs, fill colour) for colourblind bubble
    index 1..8.  Each spec is ('poly', points) or ('ellipse', box) or
    ('rrect', box, radius) or ('crescent', outer_box, cut_box)."""
    if index == 1:   # black: teal crescent opening up-left
        return [("crescent", (5.5, 5.5, 27.5, 27.5), (2.0, 3.5, 21.5, 23.0))], (20, 165, 160)
    if index == 2:   # white: two grey squares
        return [("rrect", (5.5, 6.5, 14.8, 15.8), 1.6),
                ("rrect", (14.8, 15.0, 24.0, 24.2), 1.6)], (165, 165, 170)
    if index == 3:   # blue: triangle pointing up
        return [("poly", [(15.5, 6.5), (26.0, 25.5), (5.5, 25.5)])], (30, 40, 205)
    if index == 4:   # green: three bars
        return [("rrect", (6.0, 6.5, 26.0, 10.5), 1.2),
                ("rrect", (6.0, 13.5, 26.0, 17.5), 1.2),
                ("rrect", (6.0, 20.5, 26.0, 24.5), 1.2)], (25, 170, 45)
    if index == 5:   # yellow: X
        pts = []
        c, a, w = 16.0, 8.5, 2.9      # centre, arm reach, half thickness
        for ang in (45, 135, 225, 315):
            r = math.radians(ang)
            ux, uy = math.cos(r), math.sin(r)
            px_, py_ = -uy, ux
            pts.append((ang, (ux, uy, px_, py_)))
        return [("x", (c, a, w))], (205, 210, 40)
    if index == 6:   # purple: circle
        return [("ellipse", (9.0, 9.0, 23.0, 23.0))], (215, 25, 220)
    if index == 7:   # red: square
        return [("rrect", (9.0, 9.0, 23.0, 23.0), 1.2)], (235, 25, 30)
    # orange: six-pointed star with points left and right
    pts = []
    for k in range(12):
        ang = math.radians(k * 30)
        r = 10.5 if k % 2 == 0 else 6.2
        pts.append((16.0 + r * math.cos(ang), 16.0 + r * math.sin(ang)))
    return [("poly", pts)], (235, 120, 20)


def _draw_shape(draw, spec, f, fill):
    kind = spec[0]
    if kind == "poly":
        draw.polygon([(x * f, y * f) for x, y in spec[1]], fill=fill)
    elif kind == "ellipse":
        draw.ellipse([v * f for v in spec[1]], fill=fill)
    elif kind == "rrect":
        draw.rounded_rectangle([v * f for v in spec[1]], radius=spec[2] * f, fill=fill)
    elif kind == "crescent":
        # Drawn on a separate mask so the cut doesn't erase other shapes.
        m = Image.new("L", draw.im.size, 0)
        md = ImageDraw.Draw(m)
        md.ellipse([v * f for v in spec[1]], fill=255)
        md.ellipse([v * f for v in spec[2]], fill=0)
        draw.bitmap((0, 0), m, fill=fill)
    elif kind == "x":
        c, a, w = spec[1]
        for ang in (45, 135):
            r = math.radians(ang)
            ux, uy = math.cos(r), math.sin(r)
            nx, ny = -uy, ux
            quad = [(c + ux * a + nx * w, c + uy * a + ny * w),
                    (c + ux * a - nx * w, c + uy * a - ny * w),
                    (c - ux * a - nx * w, c - uy * a - ny * w),
                    (c - ux * a + nx * w, c - uy * a + ny * w)]
            draw.polygon([(x * f, y * f) for x, y in quad], fill=fill)


def _symbol_layer(index, size):
    """RGBA symbol at `size`, anti-aliased by drawing large and shrinking."""
    big = size * SS
    f = big / 32.0
    specs, colour = _symbol_shapes(index)
    mask = Image.new("L", (big, big), 0)
    md = ImageDraw.Draw(mask)
    for spec in specs:
        _draw_shape(md, spec, f, 255)
    # Thin dark outline: the mask grown by ~0.8 original px.
    grow = int(0.8 * f) | 1
    outline = mask.filter(ImageFilter.MaxFilter(grow if grow % 2 else grow + 1))
    # Shading: lighter towards the top-left, darker towards the bottom-right.
    shade = Image.new("RGB", (big, big))
    sd = ImageDraw.Draw(shade)
    for y in range(big):
        t = y / big
        k = 1.25 - 0.45 * t
        sd.line([(0, y), (big, y)],
                fill=tuple(min(255, int(c * k)) for c in colour))
    # Inner highlight: a soft lighter band just inside the top edges.
    inner = mask.filter(ImageFilter.MinFilter(int(1.2 * f) | 1))
    rim = ImageChops.subtract(mask, inner).filter(ImageFilter.GaussianBlur(f * 0.4))
    light = Image.new("RGB", (big, big), tuple(min(255, int(c + (255 - c) * 0.55)) for c in colour))
    shade = Image.composite(light, shade, ImageChops.multiply(rim, Image.linear_gradient("L").rotate(180).resize((big, big))))
    dark = Image.new("RGB", (big, big), tuple(int(c * 0.30) for c in colour))
    layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    layer.paste(dark, (0, 0), outline)
    layer.paste(shade, (0, 0), mask)
    return layer.resize((size, size), Image.LANCZOS)


def hd_colourblind_shapes(base32, index, size=None):
    """Colourblind bubble: HD base bubble with a clean redrawn symbol."""
    base32 = base32.convert("RGBA")
    size = size or base32.width * SCALE
    body = hd_body(base32, size).convert("RGBA")
    sym = _symbol_layer(index, size)
    body.alpha_composite(sym)
    return hd_bubble(base32, size=size, body_hd=body.convert("RGB"))


# --- Stuck-flash rings ------------------------------------------------------
FIXED_RINGS = {1: (0.7, 4.3), 2: (0.7, 5.1), 3: (2.5, 7.4),
               4: (4.3, 9.3), 5: (6.5, 11.5), 6: (9.6, 14.3)}


def hd_fixed(frame, size=128):
    """White ring of the 'bubble stuck' flash, crisply anti-aliased."""
    r_in, r_out = FIXED_RINGS[frame]
    k = size / 32.0
    r_in, r_out = r_in * k, r_out * k
    edge = (200, 226, 220)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = out.load()
    c = size / 2.0
    for y in range(size):
        for x in range(size):
            cov, col = 0, [0.0, 0.0, 0.0]
            for sy in range(SS):
                for sx in range(SS):
                    r = math.hypot(x + (sx + 0.5) / SS - c, y + (sy + 0.5) / SS - c)
                    if r_in <= r <= r_out:
                        cov += 1
                        # White in the middle of the band, pale edges.
                        t = abs((r - r_in) / max(r_out - r_in, 1e-6) - 0.5) * 2
                        t = max(0.0, (t - 0.45) / 0.55)
                        for i in range(3):
                            col[i] += 255 + (edge[i] - 255) * t
            if cov:
                px[x, y] = tuple(int(v / cov) for v in col) + (int(255 * cov / (SS * SS)),)
    return out


# --- Frozen bubbles ---------------------------------------------------------
ICE_COLOURS = {"outline": (60, 190, 235), "dark": (165, 232, 250), "rim": (240, 252, 255)}


def _icicles(f):
    """Find the icicles below a frozen bubble: groups of adjacent columns
    with ice below the bubble, as (x_left, x_right, tip_y) in original px."""
    px = f.load()
    w, h = f.size
    tips = []
    for x in range(w):
        tip = None
        for y in range(31, h):
            if px[x, y][3] > 40:
                tip = y + 1
        tips.append(tip)
    groups, x = [], 0
    while x < w:
        if tips[x] is None or tips[x] < 34:
            x += 1
            continue
        x0 = x
        while x + 1 < w and tips[x + 1] is not None and tips[x + 1] >= 34:
            x += 1
        # Split wide groups at local minima so each spike has one tip.
        seg = list(range(x0, x + 1))
        start = seg[0]
        for i in range(1, len(seg) - 1):
            if tips[seg[i]] < tips[seg[i - 1]] and tips[seg[i]] <= tips[seg[i + 1]]:
                groups.append((start, seg[i], max(tips[c] for c in range(start, seg[i] + 1))))
                start = seg[i] + 1
        groups.append((start, seg[-1] + 1, max(tips[c] for c in range(start, seg[-1] + 1))))
        x += 1
    return groups


def hd_frozen(frozen34x42):
    """Frozen bubble: the original icy layer (glow halo and icicles)
    enlarged with a smooth traced outline instead of its stair-stepped
    pixel edge, and the bubble with its icy ring redrawn crisply on top."""
    f = frozen34x42.convert("RGBA")
    w, h = f.size
    W, H = w * SCALE, h * SCALE
    # Colours: enlarge in premultiplied alpha so transparent pixels don't
    # bleed into the edges as dark speckles.
    ice = f.convert("RGBa").resize((W, H), Image.LANCZOS).convert("RGBA")
    # Outline: trace the original's silhouette smoothly (thin icicle
    # spikes survive the gentle smoothing).
    m32 = f.getchannel("A").point(lambda v: 255 if v > 40 else 0)
    mask = m32.resize((W, H), Image.BICUBIC).filter(ImageFilter.GaussianBlur(SCALE * 0.3))
    mask = mask.point(lambda v: 0 if v < 70 else (255 if v > 130 else int((v - 70) * 255 / 60)))
    ice.putalpha(ImageChops.multiply(mask, ice.getchannel("A").point(lambda v: min(255, v * 2))))
    # The bubble sits one pixel in from the top-left of the frozen image.
    crop = f.crop((1, 1, 33, 33))
    bubble = hd_bubble(crop, colors=ICE_COLOURS)
    ice.alpha_composite(bubble, (SCALE, SCALE))
    return ice
