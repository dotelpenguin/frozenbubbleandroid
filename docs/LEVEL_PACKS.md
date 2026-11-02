# Puzzle level packs

Puzzle plays one pack at a time; the ◀ ▶ arrows on the Choose Level
screen switch packs.  Each pack keeps its own progress (current and
highest level) and its own best scores, so packs never mix.

| Pack | Levels | File | Made by |
|---|---|---|---|
| Original | 100 | `app/src/main/assets/levels.txt` | The original game's levels |
| Easy | 30 | `assets/packs/01_easy.txt` | `tools/levels/generate.py easy` (3-4 colours, big patches) |
| Medium | 30 | `assets/packs/02_medium.txt` | `generate.py medium` (5-6 colours) |
| Hard | 30 | `assets/packs/03_hard.txt` | `generate.py hard` (7-8 colours, small patches) |
| Pictures | 24 | `assets/packs/04_pictures.txt` | `tools/levels/make_pack.py` from `pictures_src.txt` |

## The level format

A level is 10 rows on the game's honeycomb: even rows hold 8 bubbles,
odd rows 7 (shifted half a bubble).  Colours are 0 black, 1 white,
2 blue, 3 green, 4 yellow, 5 purple, 6 red, 7 orange; `-` is an empty
spot.  Bubbles are separated by spaces, odd rows indented, and a blank
line separates levels.  Lines starting with `#` are comments; a
`# name: ...` line at the top names the pack.

    # 1. Apple
    -   -   -   -   3   -   -   -
      -   -   -   3   3   -   -
    -   6   6   -   3   6   6   -
      6   6   6   6   6   6   6
    ...

Every bubble must hang from the top row through its neighbours, or it
falls as soon as the level starts.

## Adding a pack

1. Add `app/src/main/assets/packs/NN_id.txt`.  `NN` sets the order in
   the switcher; `id` names the pack's progress and scores (keep it
   lowercase, and don't rename it later or its progress is lost).
2. Give it a name: a string `pack_<id>` in `res/values/strings.xml`
   (and the translations), or a `# name: ...` line in the file.

`LevelPacks.java` finds packs by listing that folder, so no code
changes are needed.

## The tools (`tools/levels/`, Python 3; previews need Pillow)

- `fblevels.py`: reads level files, checks levels (rows, spots, nothing
  floating), and measures them: bubbles, colours, clusters, single
  bubbles, depth, the most one shot can clear, and a rough complexity
  score.  It can also draw a preview sheet with the game's HD bubbles.
- `generate.py easy|medium|hard <out.txt> [preview.jpg]`: builds a pack
  the way the hand-made levels look (a mirror-symmetric shape hung from
  the top, filled with same-colour patches), keeps only levels whose
  complexity suits the pack, and orders them easiest first.  A fixed
  seed makes the same pack every time.
- `make_pack.py <src.txt> <out.txt> "Name" [preview.jpg]`: checks and
  orders hand-drawn levels.  The source uses `## Name` headings and
  compact rows (`.` for empty, odd rows with a leading space); a
  `stripe: 6>5` line two-tones a colour.

Run them from `tools/levels/`, for example:

    python3 generate.py medium ../../app/src/main/assets/packs/02_medium.txt preview.jpg
