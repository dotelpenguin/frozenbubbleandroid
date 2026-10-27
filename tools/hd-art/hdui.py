"""High-resolution Frozen Bubble UI art, drawn in code at 4x.

Round buttons (back, pause, play, next song), the Bluetooth icon, the
launcher, and the tomato and banana attack markers, matching the style of
the original small images.  Everything is drawn large (supersampled) and
shrunk for clean anti-aliased edges.
"""
import math

from PIL import Image, ImageChops, ImageDraw, ImageFilter

SS = 4  # supersampling factor


def _lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def _vgradient(size, stops):
    """Vertical gradient image; stops = [(t, (r, g, b)), ...] with t in 0..1."""
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
            if t0 <= t <= t1:
                c = _lerp(c0, c1, (t - t0) / max(t1 - t0, 1e-6))
                break
        else:
            c = stops[-1][1]
        d.line([(0, y), (w, y)], fill=c)
    return img


def glossy_sphere(px, top, mid, bottom, rim, rim_width=0.03, gloss=105):
    """A glossy ball like the game's round buttons, `px` pixels across."""
    n = px * SS
    body = _vgradient((n, n), [(0.0, top), (0.55, mid), (1.0, bottom)])
    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, n - 1, n - 1], fill=255)
    # Dark rim, soft on its inner edge.
    inner = Image.new("L", (n, n), 0)
    r = int(n * rim_width)
    ImageDraw.Draw(inner).ellipse([r, r, n - 1 - r, n - 1 - r], fill=255)
    inner = inner.filter(ImageFilter.GaussianBlur(n * 0.012))
    rim_mask = ImageChops.subtract(mask, inner)
    body = Image.composite(Image.new("RGB", (n, n), rim), body, rim_mask)
    # Glossy highlight across the top.
    g = Image.new("L", (n, n), 0)
    ImageDraw.Draw(g).ellipse([n * 0.17, n * 0.05, n * 0.83, n * 0.44], fill=gloss)
    g = g.filter(ImageFilter.GaussianBlur(n * 0.03))
    body = Image.composite(Image.new("RGB", (n, n), (255, 255, 255)), body, g)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(body, (0, 0), mask)
    return out


BLUE = dict(top=(170, 218, 255), mid=(6, 172, 250), bottom=(14, 236, 250), rim=(20, 140, 225))
BT_BLUE = dict(top=(140, 205, 235), mid=(20, 140, 200), bottom=(40, 175, 215), rim=(15, 95, 150))


def _glyph(sphere, shape_mask, fill_top, fill_bottom, outline, outline_px, shadow=None):
    """Composite a glyph (given as a mask the size of the supersampled
    sphere) with a vertical fill gradient and a dark outline."""
    n = sphere.width
    out = sphere.copy()
    if shadow is not None:
        off, colour, blur = shadow
        sh = Image.new("L", (n, n), 0)
        sh.paste(shape_mask, (int(off[0] * n), int(off[1] * n)))
        sh = sh.filter(ImageFilter.GaussianBlur(blur * n))
        out.paste(Image.new("RGB", (n, n), colour), (0, 0), ImageChops.multiply(sh, sphere.getchannel("A")))
    grow = int(outline_px * n) | 1
    ol = shape_mask.filter(ImageFilter.MaxFilter(grow))
    out.paste(Image.new("RGB", (n, n), outline), (0, 0), ol)
    fill = _vgradient((n, n), [(0.25, fill_top), (0.75, fill_bottom)])
    out.paste(fill, (0, 0), shape_mask)
    return out


def _poly_mask(n, pts):
    m = Image.new("L", (n, n), 0)
    ImageDraw.Draw(m).polygon([(x * n, y * n) for x, y in pts], fill=255)
    return m


def _finish(img, px):
    return img.resize((px, px), Image.LANCZOS)


def pause_button(px=128):
    s = glossy_sphere(px, **BLUE)
    n = s.width
    m = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(m)
    for x0 in (0.33, 0.56):
        d.rounded_rectangle([x0 * n, 0.27 * n, (x0 + 0.115) * n, 0.73 * n], radius=0.02 * n, fill=255)
    return _finish(_glyph(s, m, (225, 240, 252), (120, 195, 235), (40, 75, 120), 0.025), px)


def play_button(px=128):
    s = glossy_sphere(px, **BLUE)
    m = _poly_mask(s.width, [(0.38, 0.28), (0.38, 0.72), (0.73, 0.50)])
    return _finish(_glyph(s, m, (225, 240, 252), (120, 195, 235), (40, 75, 120), 0.025), px)


def next_song_button(px=128):
    s = glossy_sphere(px, **BLUE)
    n = s.width
    m = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(m)
    d.polygon([(0.26 * n, 0.31 * n), (0.26 * n, 0.69 * n), (0.48 * n, 0.50 * n)], fill=255)
    d.polygon([(0.47 * n, 0.31 * n), (0.47 * n, 0.69 * n), (0.69 * n, 0.50 * n)], fill=255)
    d.rectangle([0.695 * n, 0.31 * n, 0.765 * n, 0.69 * n], fill=255)
    return _finish(_glyph(s, m, (225, 240, 252), (120, 195, 235), (40, 75, 120), 0.025), px)


def back_button(px=128):
    """Left-pointing arrow, grey with a dark drop shadow (as the original)."""
    s = glossy_sphere(px, **BLUE)
    n = s.width
    # Arrow: head on the left, thick shaft to the right.
    pts = [(0.20, 0.50), (0.43, 0.27), (0.43, 0.41), (0.78, 0.41),
           (0.78, 0.59), (0.43, 0.59), (0.43, 0.73)]
    m = _poly_mask(n, pts)
    m = m.filter(ImageFilter.GaussianBlur(n * 0.006))
    return _finish(_glyph(s, m, (175, 185, 200), (110, 120, 140), (45, 55, 75), 0.02,
                          shadow=((0.025, 0.05), (20, 30, 60), 0.012)), px)


def bluetooth_icon(px=128):
    s = glossy_sphere(px, **BT_BLUE, gloss=80)
    n = s.width
    m = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(m)
    w = int(0.075 * n)
    c = 0.50
    top, bot = 0.20, 0.80
    pts = [(0.33, 0.35), (0.66, 0.64), (c, bot), (c, top), (0.66, 0.36), (0.33, 0.65)]
    d.line([(x * n, y * n) for x, y in pts], fill=255, width=w, joint="curve")
    return _finish(_glyph(s, m, (255, 255, 255), (230, 240, 248), (20, 80, 120), 0.012), px)


def _blob(n, box, top, mid, bottom, rim):
    """Glossy rounded blob (tomato / banana body) in a supersampled canvas."""
    x0, y0, x1, y1 = [v * n for v in box]
    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).ellipse([x0, y0, x1, y1], fill=255)
    body = _vgradient((n, n), [(y0 / n, top), ((y0 + y1) / 2 / n, mid), (y1 / n, bottom)])
    inner = mask.filter(ImageFilter.MinFilter(int(n * 0.06) | 1)).filter(ImageFilter.GaussianBlur(n * 0.02))
    body = Image.composite(body, Image.new("RGB", (n, n), rim), inner)
    g = Image.new("L", (n, n), 0)
    gx0, gy0 = x0 + (x1 - x0) * 0.15, y0 + (y1 - y0) * 0.10
    ImageDraw.Draw(g).ellipse([gx0, gy0, gx0 + (x1 - x0) * 0.35, gy0 + (y1 - y0) * 0.30], fill=150)
    g = g.filter(ImageFilter.GaussianBlur(n * 0.03))
    body = Image.composite(Image.new("RGB", (n, n), (255, 255, 255)), body, ImageChops.multiply(g, mask))
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(body, (0, 0), mask)
    return out


def _slits(img, centres, w, h, colour):
    n = img.width
    d = ImageDraw.Draw(img)
    for cx, cy in centres:
        d.rounded_rectangle([(cx - w / 2) * n, (cy - h / 2) * n, (cx + w / 2) * n, (cy + h / 2) * n],
                            radius=w * n * 0.45, fill=colour)
    return img


def tomato(size=(88, 64)):
    """22x16 original -> 88x64.  Pink-red blob with two dark slits."""
    W, H = size
    n = max(W, H) * SS
    img = _blob(n, (0.02, 0.12, 0.98, 0.86), (255, 150, 220), (245, 45, 95), (175, 15, 50), (120, 10, 40))
    img = _slits(img, [(0.47, 0.47), (0.63, 0.47)], 0.07, 0.36, (90, 15, 25, 255))
    img = img.resize((max(W, H), max(W, H)), Image.LANCZOS)
    top = (max(W, H) - H) // 2
    return img.crop((0, top, W, top + H))


def banana(size=(64, 56)):
    """16x14 original -> 64x56.  Yellow blob with two dark green slits."""
    W, H = size
    n = max(W, H) * SS
    img = _blob(n, (0.02, 0.08, 0.98, 0.94), (255, 250, 150), (235, 215, 20), (170, 160, 0), (120, 110, 0))
    img = _slits(img, [(0.40, 0.50), (0.62, 0.50)], 0.14, 0.40, (95, 110, 20, 255))
    img = img.resize((max(W, H), max(W, H)), Image.LANCZOS)
    top = (max(W, H) - H) // 2
    return img.crop((0, top, W, top + H))


def launcher(px=400):
    """The rotating launcher (100x100 original -> 400x400): a wooden arrow
    above the bubble holder, a dark band with yellow hazard stripes and
    three rivets, wooden side wedges and a curved wooden rim."""
    n = px * SS
    k = n / 100.0                       # original pixel -> canvas
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def P(pts):
        return [(x * k, y * k) for x, y in pts]

    # Arrow: shaft then head, with a light streak for roundness.
    shaft = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shaft)
    sd.rounded_rectangle([48.2 * k, 19 * k, 52.2 * k, 33.5 * k], radius=1.2 * k,
                         fill=(103, 47, 6, 235))
    sd.rectangle([49.2 * k, 21 * k, 50.0 * k, 32.5 * k], fill=(160, 92, 40, 200))
    img.alpha_composite(shaft)
    d.polygon(P([(50.2, 7.6), (52.6, 11.0), (54.8, 16.0), (55.2, 19.2), (53.4, 21.6),
                 (47.0, 21.6), (45.2, 19.2), (45.6, 16.0), (47.8, 11.0)]), fill=(118, 52, 6, 240))
    d.polygon(P([(50.2, 9.6), (51.0, 13.0), (50.8, 20.0), (48.2, 20.0), (48.4, 15.0)]),
              fill=(170, 100, 45, 200))

    # Holder: wooden side wedges.
    wood = (100, 40, 4, 235)
    d.polygon(P([(34, 57.2), (40.5, 62.4), (34, 62.4)]), fill=wood)
    d.polygon(P([(66.4, 57.2), (59.9, 62.4), (66.4, 62.4)]), fill=wood)

    # Dark band with yellow diagonal stripes.
    band = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    bd = ImageDraw.Draw(band)
    bd.rectangle([34 * k, 62 * k, 66.4 * k, 77.2 * k], fill=(22, 22, 24, 235))
    stripes = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    st = ImageDraw.Draw(stripes)
    for x0 in (27, 41, 55):
        st.polygon(P([(x0, 77.2), (x0 + 7.0, 77.2), (x0 + 15.0, 62), (x0 + 8.0, 62)]),
                   fill=(236, 218, 36, 235))
    band_mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(band_mask).rectangle([34 * k, 62 * k, 66.4 * k, 77.2 * k], fill=255)
    band.alpha_composite(Image.composite(stripes, Image.new("RGBA", (n, n), (0, 0, 0, 0)), band_mask))
    # Soft shading: lighter top edge, darker bottom.
    shade = Image.new("L", (n, n), 0)
    sh = ImageDraw.Draw(shade)
    for y in range(int(62 * k), int(77.2 * k)):
        t = (y / k - 62) / 15.2
        sh.line([(34 * k, y), (66.4 * k, y)], fill=int(70 * t))
    band.paste(Image.new("RGBA", (n, n), (0, 0, 0, 255)), (0, 0),
               ImageChops.multiply(shade, band_mask))
    img.alpha_composite(band)
    d.rectangle([34 * k, 62 * k, 66.4 * k, 62.8 * k], fill=(120, 110, 60, 200))

    # Rivets.
    for rx in (37.2, 47.6, 59.2):
        d.rounded_rectangle([rx * k, 71.6 * k, (rx + 3.4) * k, 75.2 * k], radius=0.8 * k,
                            fill=(70, 70, 76, 240))
        d.rounded_rectangle([(rx + 0.5) * k, 72.0 * k, (rx + 2.9) * k, 74.6 * k], radius=0.6 * k,
                            fill=(205, 205, 212, 240))
        d.ellipse([(rx + 0.8) * k, 72.3 * k, (rx + 1.7) * k, 73.1 * k], fill=(250, 250, 255, 240))

    # Curved wooden rim under the band.
    rim = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ImageDraw.Draw(rim).chord([34 * k, 70 * k, 66.4 * k, 81.6 * k], 0, 180,
                              fill=(84, 32, 4, 235))
    rim_mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(rim_mask).rectangle([0, 77.2 * k, n, n], fill=255)
    img.alpha_composite(Image.composite(rim, Image.new("RGBA", (n, n), (0, 0, 0, 0)), rim_mask))
    d.line([(36 * k, 77.6 * k), (64.4 * k, 77.6 * k)], fill=(150, 80, 30, 200), width=int(0.6 * k))

    return img.resize((px, px), Image.LANCZOS)
