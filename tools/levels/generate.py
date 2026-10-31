"""Generate a level pack (Easy, Medium or Hard).

    python3 generate.py easy   ../../app/src/main/assets/packs/01_easy.txt   [preview.jpg]
    python3 generate.py medium ../../app/src/main/assets/packs/02_medium.txt [preview.jpg]
    python3 generate.py hard   ../../app/src/main/assets/packs/03_hard.txt   [preview.jpg]

Levels are built like the hand-made ones: a mirror-symmetric shape hung
from the top (blocks, V shapes, pillars, diamonds...), filled with
same-colour patches.  The pack's settings choose the number of colours
and the patch sizes.  Every candidate is checked (nothing floating),
measured (fblevels.measure) and kept only if its complexity and "one
shot" (the most one shot can clear) suit the pack.  The kept levels are
spread evenly over the pack's complexity range, easiest first, so the
pack climbs smoothly.  A fixed seed makes the same pack every time.
"""
import random
import sys

import fblevels as fb

PACKS = {
    # colours, patch sizes, complexity range, max one-shot share, depth
    "easy":   dict(title="Easy",   colours=(3, 4), patch=(3, 7),
                   complexity=(4, 15), one_shot=0.6, depth=(3, 6), seed=11),
    "medium": dict(title="Medium", colours=(5, 6), patch=(2, 4),
                   complexity=(15, 26), one_shot=0.35, depth=(5, 8), seed=22),
    "hard":   dict(title="Hard",   colours=(7, 8), patch=(1, 3),
                   complexity=(26, 40), one_shot=0.25, depth=(6, 9), seed=33),
}
COUNT = 30


def mirror(x, y):
    return (7 - x, y) if y % 2 == 0 else (8 - x, y)


def row_cells(y):
    return list(range(8)) if y % 2 == 0 else list(range(1, 8))


def shape(rng, depth):
    """A mirror-symmetric set of spots, every one hanging from the top."""
    kind = rng.choice(["block", "v", "diamond", "pillars", "hourglass",
                       "blob", "arch", "holes"])
    cells = set()
    for y in range(depth):
        t = y / max(1, depth - 1)
        for x in row_cells(y):
            # Distance from the centre line, 0 (middle) .. 1 (edge).
            cx = 3.5 if y % 2 == 0 else 4.0
            d = abs(x - cx) / 3.5
            keep = {
                "block": True,
                "v": d <= 1.05 - 0.9 * t,
                "diamond": d <= 0.35 + 0.8 * (1 - abs(2 * t - 1)),
                "pillars": (y < 2) or (round(x) % 3 != 1),
                "hourglass": d <= 0.3 + 0.8 * abs(2 * t - 1),
                "blob": rng.random() < 0.85 - 0.3 * d,
                "arch": (y < 2) or d > 0.45,
                "holes": not (y > 1 and (x + y) % 4 == 0),
            }[kind]
            if keep:
                cells.add((x, y))
                cells.add(mirror(x, y))
    # Keep only what hangs from the top row.
    g = {c: 0 for c in cells}
    return fb.attached(g), kind


def fill(rng, cells, n_colours, patch):
    """Colour the spots with same-colour patches, mirrored."""
    palette = rng.sample(range(8), n_colours)
    colour = {}
    todo = sorted(cells, key=lambda c: (c[1], c[0]))
    for start in todo:
        if start in colour:
            continue
        c = rng.choice(palette)
        size = rng.randint(*patch)
        group, frontier = [start], [start]
        while frontier and len(group) < size:
            p = frontier.pop(rng.randrange(len(frontier)))
            for q in fb.neighbours(*p):
                if q in cells and q not in colour and q not in group:
                    group.append(q)
                    frontier.append(q)
                    if len(group) >= size:
                        break
        for p in group:
            colour[p] = c
            colour.setdefault(mirror(*p), c)
    return colour


def to_rows(colour, rows=fb.ROWS):
    out = []
    for y in range(rows):
        out.append("".join(str(colour[(x, y)]) if (x, y) in colour else "."
                           for x in row_cells(y)))
    return out


def candidates(cfg, tries=6000):
    rng = random.Random(cfg["seed"])
    seen = set()
    for _ in range(tries):
        depth = rng.randint(*cfg["depth"])
        cells, kind = shape(rng, depth)
        if len(cells) < 12:
            continue
        n = rng.randint(*cfg["colours"])
        rows = to_rows(fill(rng, cells, n, cfg["patch"]))
        key = "".join(rows)
        if key in seen or fb.problems(rows):
            continue
        seen.add(key)
        m = fb.measure(rows)
        lo, hi = cfg["complexity"]
        if (lo <= m["complexity"] <= hi and m["one_shot"] <= cfg["one_shot"]
                and m["colours"] >= cfg["colours"][0]):
            yield rows, m, kind


def pick(cands, count=COUNT):
    """Spread the levels evenly over the complexity range, and vary the
    shapes (no two of the same kind in a row)."""
    cands.sort(key=lambda c: c[1]["complexity"])
    lo, hi = cands[0][1]["complexity"], cands[-1][1]["complexity"]
    chosen, used, last_kind = [], set(), None
    for i in range(count):
        target = lo + (hi - lo) * i / max(1, count - 1)
        best = None
        for j, (rows, m, kind) in enumerate(cands):
            if j in used:
                continue
            cost = abs(m["complexity"] - target) + (3 if kind == last_kind else 0)
            if best is None or cost < best[0]:
                best = (cost, j)
        if best is None:
            break
        used.add(best[1])
        chosen.append(cands[best[1]])
        last_kind = cands[best[1]][2]
    chosen.sort(key=lambda c: c[1]["complexity"])
    return chosen


def main(which, out, preview=None):
    cfg = PACKS[which]
    cands = list(candidates(cfg))
    chosen = pick(cands)
    for i, (rows, m, kind) in enumerate(chosen):
        print(f"{i + 1:2} {kind:9} colours {m['colours']} bubbles {m['bubbles']:2} "
              f"singles {m['singles']:2} one shot {m['one_shot']:.0%} "
              f"complexity {m['complexity']}")
    print(f"{len(cands)} candidates, {len(chosen)} kept")
    fb.write_pack(out, [c[0] for c in chosen], cfg["title"],
                  notes=[f"{cfg['title']} levels, made by tools/levels/generate.py "
                         f"{which} (seed {cfg['seed']}).",
                         "Easiest first.  Edit freely: the game only reads the levels."])
    if preview:
        fb.render_sheet([c[0] for c in chosen], preview,
                        titles=[f"{i + 1}  ({c[1]['complexity']})"
                                for i, c in enumerate(chosen)], cols=6)


if __name__ == "__main__":
    main(*sys.argv[1:])
