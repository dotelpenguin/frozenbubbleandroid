# Builds libmodplug (the MOD/XM/IT music player) plus its JNI stubs.
# Replaces the old ndk-build Android.mk / Application.mk.
cmake_minimum_required(VERSION 3.22.1)
project(modplug CXX)

# Java loads this with System.loadLibrary("modplug-1.0").
#
# Only the MOD, XM, IT and S3M readers are built (MODPLUG_BASIC_SUPPORT
# below).  The built-in songs use only these, and the music folder only
# lists these.  The other readers (ABC, MIDI, patches and ~20 rarer
# formats) have known memory-safety bugs, and libmodplug picks a reader
# by a file's contents, not its name, so a damaged or disguised song in
# the player's own music folder could reach them and crash the app.
add_library(modplug-1.0 SHARED
    fastmix.cpp
    load_it.cpp load_mod.cpp load_s3m.cpp load_xm.cpp
    modplug.cpp
    snd_dsp.cpp snd_flt.cpp snd_fx.cpp
    sndfile.cpp sndmix.cpp
    jni_stubs.cpp
)

target_include_directories(modplug-1.0 PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/libmodplug
)

# Same feature flags the original Android.mk used.
target_compile_definitions(modplug-1.0 PRIVATE
    SMALLER_READS HAVE_SINF HAVE_SETENV HAVE_STDINT_H HAVE_INTTYPES_H
    MODPLUG_TRACKER PECULIAR_EXTENSIONS PECULIAR_FOURBYFOUR
    MODPLUG_BASIC_SUPPORT MODPLUG_NO_MMCMP
)

# This is decades-old third-party code: silence its warnings and keep
# old-style narrowing initializers from being hard errors on modern clang.
target_compile_options(modplug-1.0 PRIVATE -w -Wno-c++11-narrowing)

target_link_libraries(modplug-1.0 PRIVATE log)
