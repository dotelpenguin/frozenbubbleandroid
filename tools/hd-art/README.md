# HD bubble art generator

Builds the high-resolution bubble set in `app/src/main/res/drawable-nodpi/`
(`*_hd.png`, 4x the original 32 px art) from the originals in
`app/src/main/res/drawable/`.

- Base bubbles: the original marbling enlarged smoothly, with the edge,
  outline and inner rim redrawn crisply.
- Colourblind bubbles: the HD base bubble with each symbol redrawn as a
  clean shape.
- Frozen bubbles: the original icy glow and icicles enlarged with a
  smooth outline, and the bubble and icy ring redrawn crisply.
- Stuck flash rings and blink: redrawn.

Run from the project root (needs Python 3 and Pillow):

    python3 tools/hd-art/build_set.py app/src/main/res

## HD UI art

`build_ui.py` draws the round buttons (back, pause, play, next song), the
launcher, the tomato and banana attack markers and the Bluetooth dialog
icon at 4x (`hdui.py`):

    python3 tools/hd-art/build_ui.py app/src/main/res

Both sets are used unless "Use Original Art" is switched on in Options.
