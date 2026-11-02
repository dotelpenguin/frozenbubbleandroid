# AdPlug subset (AdLib .ROL music)

The files needed to play AdLib Visual Composer songs (.ROL, with a .BNK
instrument bank) from the player's own music folder.  Used by
`../adlib_engine.cpp`; built as the `adlib` library in `../CMakeLists.txt`.

| Files | From | Licence |
|---|---|---|
| `rol.*`, `composer.*`, `player.*`, `opl.h`, `silentopl.h`, `nemuopl.*`, `fprovide.h`, `adplug.h`, `players.h`, `database.h`, `debug.h` | [AdPlug](https://github.com/adplug/adplug), commit `3d9457a` (2026-10-30) | LGPL 2.1 or later |
| `nukedopl.*` | Nuked OPL3 1.8 by Nuke.YKT, as included in AdPlug | LGPL 2.1 or later |
| `binio.cpp`, `binstr.*`, `binio.h` | [libbinio](https://github.com/adplug/libbinio), commit `3baed73` (2026-07-26) | LGPL 2.1 or later |

The files are unchanged, except `binio.h`, which is libbinio's
`binio.h.in` with its settings filled in (strings on, iostreams off,
`long long` integers, `double` floats).  `COPYING` is the LGPL 2.1.

Not built: AdPlug's other players, its format database and its debug
log (`../adlib_engine.cpp` supplies the two symbols the ROL player needs
from them), and libbinio's file and iostream classes (songs are read
from memory).

To update: copy the same files from newer AdPlug and libbinio releases,
regenerate `binio.h` from `binio.h.in`, and play a few .ROL songs.
