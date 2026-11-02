/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 *
 * adlib_engine.cpp - see adlib_engine.h.
 *
 * AdPlug's ROL player reads the song and its instrument bank through a
 * "file provider".  The game has both files in memory (Android's folder
 * picker gives no file paths), so a small provider serves them from
 * memory: the song as "song.rol" and the bank as "standard.bnk", the
 * name AdPlug falls back to.  Which bank goes with which song is decided
 * on the Java side (see CustomMusic).
 */

#include "adlib_engine.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <new>
#include <set>
#include <string>
#include <vector>

#include "binstr.h"
#include "fprovide.h"
#include "nemuopl.h"
#include "rol.h"
#include "adplug.h"

/*
 * The parts of AdPlug the ROL player needs but that live in files we
 * don't build (the format database and the debug log).
 */
CAdPlugDatabase *CAdPlug::database = 0;
extern "C" {
void AdPlug_LogFile(const char *) {}
void AdPlug_LogWrite(const char *, ...) {}
}

namespace {

/* Limits for files from the player's music folder. */
const size_t MAX_ROL_BYTES = 4 * 1024 * 1024;
const size_t MAX_BNK_BYTES = 4 * 1024 * 1024;

/*
 * FM music is quieter than most tracker songs; this brings it to a
 * similar loudness (output is clipped, not wrapped, if it overshoots).
 */
const float GAIN = 2.0f;

class MemoryProvider : public CFileProvider {
public:
  std::vector<unsigned char> rol;
  std::vector<unsigned char> bnk;

  binistream *open(std::string name) const {
    std::vector<unsigned char> const *file = 0;
    std::string lower(name);
    std::transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
    if (ends(lower, ".rol")) {
      file = &rol;
    }
    else if (ends(lower, "standard.bnk")) {
      file = &bnk;
    }
    if ((file == 0) || file->empty()) {
      return 0;   // e.g. "song.bnk": AdPlug then uses standard.bnk
    }
    return new binisstream(const_cast<unsigned char *>(&(*file)[0]),
                           file->size());
  }

  void close(binistream *f) const {
    delete f;
  }

private:
  static bool ends(const std::string &s, const char *tail) {
    size_t n = strlen(tail);
    return (s.size() >= n) && (s.compare(s.size() - n, n, tail) == 0);
  }
};

int             sRate     = 44100;
CNemuopl       *sOpl      = 0;
CrolPlayer     *sPlayer   = 0;
MemoryProvider *sProvider = 0;
double          sTickLeft = 0;   // samples until the next player tick
std::string     sDesc;

/*
 * The instrument names in a bank (lower case), or none if it is not an
 * AdLib Visual Composer bank.  The layout: 2 version bytes, "ADLIB-",
 * the entries used and in total (2 bytes each), the offsets of the name
 * list and the data (4 bytes each); each name entry is a 2 byte index,
 * a "used" byte and a 9 byte name.
 */
std::set<std::string> bankNames(const std::vector<unsigned char> &b) {
  std::set<std::string> names;
  if ((b.size() < 20) || (memcmp(&b[2], "ADLIB-", 6) != 0)) {
    return names;
  }
  unsigned total = b[10] | (b[11] << 8);
  size_t   off   = b[12] | (b[13] << 8) | (b[14] << 16) | ((size_t) b[15] << 24);
  for (unsigned i = 0; i < total; i++) {
    size_t p = off + ((size_t) i * 12);
    if ((p < off) || ((p + 12) > b.size())) {
      break;
    }
    if (b[p + 2] == 0) {
      continue;
    }
    std::string name;
    for (size_t k = 0; (k < 9) && (b[p + 3 + k] != 0); k++) {
      name += (char) tolower(b[p + 3 + k]);
    }
    names.insert(name);
  }
  return names;
}

/*
 * Whether the bank has at least one of the song's instruments.  A bank
 * meant for other songs would otherwise play the song as near silence.
 */
bool bankFits(CrolPlayer *player, const std::vector<unsigned char> &bnk) {
  std::set<std::string> names = bankNames(bnk);
  if (names.empty()) {
    return false;
  }
  unsigned used = player->getinstruments();
  if (used == 0) {
    return true;
  }
  for (unsigned i = 0; i < used; i++) {
    std::string name = player->getinstrument(i);
    std::transform(name.begin(), name.end(), name.begin(), ::tolower);
    if (names.count(name) != 0) {
      return true;
    }
  }
  return false;
}

float refreshRate() {
  float r = sPlayer->getrefresh();
  if (!(r >= 1.0f)) {     // also catches NaN
    r = 18.2f;            // the PC timer's default rate
  }
  return std::min(r, 1000.0f);
}

}  // namespace

namespace adlib {

void init(int rate) {
  int newRate = (rate > 0) ? rate : 44100;
  if (newRate != sRate) {   // a loaded song was set up for the old rate
    unload();
    sRate = newRate;
  }
}

void unload() {
  delete sPlayer;
  sPlayer = 0;
  delete sOpl;
  sOpl = 0;
  delete sProvider;
  sProvider = 0;
  sTickLeft = 0;
  sDesc.clear();
}

bool loaded() {
  return sPlayer != 0;
}

bool load(const unsigned char *rol, size_t rolSize,
          const unsigned char *bnk, size_t bnkSize) {
  unload();
  if ((rol == 0) || (bnk == 0) || (rolSize == 0) || (bnkSize == 0) ||
      (rolSize > MAX_ROL_BYTES) || (bnkSize > MAX_BNK_BYTES)) {
    return false;
  }
  try {
    sProvider = new MemoryProvider();
    sProvider->rol.assign(rol, rol + rolSize);
    sProvider->bnk.assign(bnk, bnk + bnkSize);
    sOpl    = new CNemuopl(sRate);
    sPlayer = new CrolPlayer(sOpl);
    if (!sPlayer->load("song.rol", *sProvider) ||
        !bankFits(sPlayer, sProvider->bnk)) {
      unload();
      return false;
    }
    sPlayer->rewind(0);
    // Only plain printable ASCII: it is passed to Java as a string.
    std::string desc = sPlayer->getdesc();
    sDesc.clear();
    for (size_t i = 0; (i < desc.size()) && (sDesc.size() < 64); i++) {
      unsigned char c = (unsigned char) desc[i];
      if ((c >= 32) && (c < 127)) {
        sDesc += (char) c;
      }
    }
    sTickLeft = 0;
    return true;
  } catch (...) {   // out of memory on a damaged file
    unload();
    return false;
  }
}

bool render(short *out, int frames) {
  if (sPlayer == 0) {
    memset(out, 0, sizeof(short) * 2 * (size_t) std::max(frames, 0));
    return false;
  }
  bool   restarted = false;
  short *p         = out;
  while (frames > 0) {
    if (sTickLeft < 1.0) {
      if (!sPlayer->update()) {
        sPlayer->rewind(0);   // the end: play it again
        restarted = true;
      }
      sTickLeft += (double) sRate / refreshRate();
    }
    int n = std::min(frames, (int) sTickLeft);
    sOpl->update(p, n);
    for (int i = 0; i < (n * 2); i++) {
      float v = p[i] * GAIN;
      p[i] = (short) std::max(-32768.0f, std::min(32767.0f, v));
    }
    p         += n * 2;
    frames    -= n;
    sTickLeft -= n;
  }
  return restarted;
}

const char *description() {
  return sDesc.c_str();
}

}  // namespace adlib
