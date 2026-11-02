/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 *
 * adlib_engine.h - plays AdLib Visual Composer songs (.ROL, with an
 * instrument bank, .BNK) through AdPlug and the Nuked OPL3 emulator.
 * Kept free of JNI so it can be tested on a desktop.
 */

#ifndef FB_ADLIB_ENGINE_H
#define FB_ADLIB_ENGINE_H

#include <stddef.h>

namespace adlib {

/** Set the output sample rate (Hz); unloads any song. */
void init(int rate);

/**
 * Load a song from memory.  Both buffers are copied.
 * @return false if the song or bank is not usable.
 */
bool load(const unsigned char *rol, size_t rolSize,
          const unsigned char *bnk, size_t bnkSize);

/** Unload the song (safe to call when none is loaded). */
void unload();

bool loaded();

/**
 * Fill out[] with stereo 16-bit samples (frames * 2 shorts).  At the end
 * of the song it starts again from the beginning.
 * @return true if the song reached its end (and restarted) during this
 * call.
 */
bool render(short *out, int frames);

/** The song's description from its header ("" for none). */
const char *description();

}  // namespace adlib

#endif
