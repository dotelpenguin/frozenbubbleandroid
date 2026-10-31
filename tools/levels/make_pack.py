"""Check picture levels and write them as a level pack.

    python3 make_pack.py pictures_src.txt ../../app/src/main/assets/packs/04_pictures.txt "Pictures" [preview.jpg]

Levels are checked (rows, spots, nothing floating) and ordered: the ones
one good shot can clear first, then the rest, each from the simplest to
the busiest by the complexity estimate.
"""
import sys
import fblevels as fb


def read_src(path):
    """[(name, rows)]; a "stripe: 6>5 4>7" line two-tones those colours."""
    items, name, rows, pairs = [], None, [], {}

    def done():
        if name:
            items.append((name, fb.stripe(rows, pairs) if pairs else rows))

    for line in open(path, encoding="utf-8").read().split("\n"):
        if line.startswith("## "):
            done()
            name, rows, pairs = line[3:].strip(), [], {}
        elif line.startswith("stripe:"):
            for p in line[7:].split():
                a, b = p.split(">")
                pairs[int(a)] = int(b)
        elif line.startswith("#") or not line.strip():
            continue
        else:
            rows.append(line.strip())
    done()
    return items


def main(src, out, title, preview=None):
    items = read_src(src)
    bad = False
    for name, rows in items:
        p = fb.problems(rows)
        if p:
            bad = True
            print(f"{name}: " + "; ".join(p))
    if bad:
        sys.exit(1)
    # Pictures that one good shot can clear come first (a gentle start:
    # find the key bubble), then the two-tone ones, each by complexity.
    def order(it):
        m = fb.measure(it[1])
        return (m["one_shot"] < 0.8, m["complexity"])
    items.sort(key=order)
    for name, rows in items:
        m = fb.measure(rows)
        print(f"{name:15} colours {m['colours']}  bubbles {m['bubbles']:2}  "
              f"singles {m['singles']:2}  one shot {m['one_shot']:.0%}  "
              f"complexity {m['complexity']}")
    fb.write_pack(out, [r for _, r in items], title,
                  notes=["Picture levels, simplest first.",
                         "Made with tools/levels/make_pack.py from pictures_src.txt."],
                  names=[n for n, _ in items])
    if preview:
        fb.render_sheet([r for _, r in items], preview,
                        titles=[f"{i + 1}. {n}" for i, (n, _) in enumerate(items)])


if __name__ == "__main__":
    main(*sys.argv[1:])
