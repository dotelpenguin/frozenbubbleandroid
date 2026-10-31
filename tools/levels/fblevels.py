"""Frozen Bubble level tools: read, check, measure and preview levels.

A level is 10 rows of bubbles on the game's honeycomb grid: even rows
hold 8 bubbles, odd rows 7 (shifted half a bubble right).  Colours are
0-7 (0 black, 1 white, 2 blue, 3 green, 4 yellow, 5 purple, 6 red,
7 orange); "-" is an empty spot.

In a level file (assets/levels.txt, assets/packs/*.txt) each row is
written with its bubbles separated by spaces, odd rows indented, and a
blank line between levels.  Lines starting with "#" are comments.

Used by make_pack.py, generate.py and preview.py.
"""
import os

ROWS = 10
COLOURS = "black white blue green yellow purple red orange".split()


def parse_file(path):
    """The levels in a level file, as lists of row strings ("66..33.")."""
    levels, cur = [], []
    names, name = [], None
    for line in open(path, encoding="utf-8").read().replace("\r", "").split("\n"):
        s = line.strip()
        if s.startswith("#"):
            if s[1:].strip() and not s[1:].strip().startswith("name:") and not cur:
                name = s[1:].strip()
            continue
        toks = s.split()
        if not toks:
            if cur:
                levels.append(cur); names.append(name); cur, name = [], None
            continue
        cur.append("".join("." if t == "-" else t for t in toks))
    if cur:
        levels.append(cur); names.append(name)
    return levels, names


def format_level(rows):
    out = []
    for y, r in enumerate(rows):
        cells = ["-" if c == "." else c for c in r]
        out.append(("  " if y % 2 else "") + "   ".join(cells))
    return "\n".join(out)


def write_pack(path, levels, title, notes=(), names=None):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(f"# name: {title}\n")
        for n in notes:
            f.write(f"# {n}\n")
        for i, rows in enumerate(levels):
            f.write("\n")
            if names and names[i]:
                f.write(f"# {i + 1}. {names[i]}\n")
            f.write(format_level(rows) + "\n")


def grid(rows):
    """{(x, y): colour}; odd rows' bubbles are x = 1..7 (as in the game)."""
    g = {}
    for y, r in enumerate(rows):
        for i, c in enumerate(r):
            if c != ".":
                g[(i + (y % 2), y)] = int(c)
    return g


def neighbours(x, y):
    out = [(x - 1, y), (x + 1, y)]
    xs = (x - 1, x) if y % 2 else (x, x + 1)
    for yy in (y - 1, y + 1):
        out += [(xx, yy) for xx in xs]
    return [(a, b) for a, b in out
            if 0 <= a < 8 and 0 <= b < 12 and not (b % 2 == 1 and a == 0)]


def attached(g):
    """The bubbles hanging from the ceiling (row 0), directly or not."""
    seen = {k for k in g if k[1] == 0}
    stack = list(seen)
    while stack:
        p = stack.pop()
        for q in neighbours(*p):
            if q in g and q not in seen:
                seen.add(q); stack.append(q)
    return seen


def clusters(g):
    seen, out = set(), []
    for c in g:
        if c in seen:
            continue
        comp, stack = [], [c]
        seen.add(c)
        while stack:
            p = stack.pop(); comp.append(p)
            for q in neighbours(*p):
                if q in g and q not in seen and g[q] == g[p]:
                    seen.add(q); stack.append(q)
        out.append(comp)
    return out


def problems(rows):
    """What is wrong with a level (an empty list if nothing)."""
    errs = []
    if len(rows) != ROWS:
        errs.append(f"{len(rows)} rows, not {ROWS}")
    for y, r in enumerate(rows):
        want = 7 if y % 2 else 8
        if len(r) != want:
            errs.append(f"row {y + 1} has {len(r)} spots, not {want}")
        bad = set(r) - set(".01234567")
        if bad:
            errs.append(f"row {y + 1}: unknown {''.join(sorted(bad))}")
    if errs:
        return errs
    g = grid(rows)
    if not g:
        return ["no bubbles"]
    floating = len(g) - len(attached(g))
    if floating:
        errs.append(f"{floating} bubble(s) not hanging from the top row")
    return errs


def stripe(rows, pairs):
    """Two-tone a picture: bubbles of colour a in odd columns become b
    ({a: b}).  Same-colour bubbles then form vertical chains that each
    hang from the top, so no single shot drops the whole picture."""
    out = []
    for y, r in enumerate(rows):
        cells = list(r)
        for i, c in enumerate(cells):
            x = i + (y % 2)
            if c != "." and int(c) in pairs and x % 2 == 1:
                cells[i] = str(pairs[int(c)])
        out.append("".join(cells))
    return out


def drop_after(g, removed):
    rest = {k: v for k, v in g.items() if k not in removed}
    return len(rest) - len(attached(rest))


def measure(rows):
    """Layout measures and a rough complexity estimate (higher = harder)."""
    g = grid(rows)
    cl = clusters(g)
    sizes = [len(c) for c in cl]
    best_drop = max([drop_after(g, set(c)) for c in cl if len(c) >= 2] or [0])
    m = dict(bubbles=len(g), colours=len(set(g.values())), clusters=len(cl),
             singles=sum(1 for s in sizes if s == 1),
             pairs=sum(1 for s in sizes if s == 2),
             depth=max(y for _, y in g) + 1, best_drop=best_drop)
    total = len(g)
    m["one_shot"] = round(max([(len(c) + drop_after(g, set(c))) / total
                               for c in cl if len(c) >= 2] or [0]), 2)
    m["complexity"] = round(2 * m["colours"] + 0.6 * m["singles"] +
                            0.2 * m["bubbles"] - 0.25 * best_drop, 1)
    return m


def render_sheet(levels, out_path, titles=None, cols=5, res_dir=None):
    """A preview sheet of levels drawn with the game's bubbles."""
    from PIL import Image, ImageDraw, ImageFont
    res_dir = res_dir or os.path.join(os.path.dirname(__file__), "..", "..",
                                      "app", "src", "main", "res")
    nd = os.path.join(res_dir, "drawable-nodpi")
    S = 1
    bub = [Image.open(os.path.join(nd, f"bubble_{i}_hd.png")).convert("RGBA")
           .resize((32 * S, 32 * S), Image.LANCZOS) for i in range(1, 9)]
    bg = Image.open(os.path.join(nd, "background_hd.jpg")).convert("RGBA") \
              .resize((640 * S, 480 * S), Image.LANCZOS) \
              .crop((178 * S, 38 * S, 462 * S, 340 * S))
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    except OSError:
        font = ImageFont.load_default()
    W, H = bg.size
    rows_n = (len(levels) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * (W + 10) + 10, rows_n * (H + 34) + 10),
                      (30, 30, 36, 255))
    for k, rows in enumerate(levels):
        p = bg.copy()
        for (x, y), c in grid(rows).items():
            px = 190 + x * 32 - (y % 2) * 16 - 178
            p.alpha_composite(bub[c], (px * S, (44 + y * 28 - 38) * S))
        ox = 10 + (k % cols) * (W + 10)
        oy = 10 + (k // cols) * (H + 34)
        t = titles[k] if titles else f"{k + 1}"
        ImageDraw.Draw(sheet).text((ox, oy), t, font=font, fill=(255, 255, 255, 255))
        sheet.alpha_composite(p, (ox, oy + 22))
    sheet.convert("RGB").save(out_path, quality=88)
