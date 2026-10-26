# Builds libmodplug (the MOD/XM/IT music player) plus its JNI stubs.
# Replaces the old ndk-build Android.mk / Application.mk.
cmake_minimum_required(VERSION 3.22.1)
project(modplug CXX)

# Java loads this with System.loadLibrary("modplug-1.0").
add_library(modplug-1.0 SHARED
    fastmix.cpp
    load_669.cpp load_abc.cpp load_amf.cpp load_ams.cpp load_dbm.cpp
    load_dmf.cpp load_dsm.cpp load_far.cpp load_it.cpp  load_j2b.cpp
    load_mdl.cpp load_med.cpp load_mid.cpp load_mod.cpp load_mt2.cpp
    load_mtm.cpp load_okt.cpp load_pat.cpp load_psm.cpp load_ptm.cpp
    load_s3m.cpp load_stm.cpp load_ult.cpp load_umx.cpp load_wav.cpp
    load_xm.cpp
    mmcmp.cpp modplug.cpp
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
)

# This is decades-old third-party code: silence its warnings and keep
# old-style narrowing initializers from being hard errors on modern clang.
target_compile_options(modplug-1.0 PRIVATE -w -Wno-c++11-narrowing)

target_link_libraries(modplug-1.0 PRIVATE log)
