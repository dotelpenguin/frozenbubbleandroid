/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 *
 * adlib_jni.cpp - the AdLib (.ROL) music engine's native methods on
 * com.peculiargames.andmodplug.PlayerThread.  Like libmodplug, the
 * engine holds one song for the whole app; PlayerThread serializes
 * every call with its shared native lock.
 */

#include <jni.h>

#include "adlib_engine.h"

extern "C" {

JNIEXPORT void JNICALL
Java_com_peculiargames_andmodplug_PlayerThread_AdLib_1Init(
    JNIEnv *, jobject, jint rate) {
  adlib::init(rate);
}

JNIEXPORT jboolean JNICALL
Java_com_peculiargames_andmodplug_PlayerThread_AdLib_1JLoad(
    JNIEnv *env, jobject, jbyteArray rol, jbyteArray bnk) {
  if ((rol == NULL) || (bnk == NULL)) {
    return JNI_FALSE;
  }
  jsize  rolSize = env->GetArrayLength(rol);
  jsize  bnkSize = env->GetArrayLength(bnk);
  jbyte *rolData = env->GetByteArrayElements(rol, NULL);
  jbyte *bnkData = env->GetByteArrayElements(bnk, NULL);
  bool   ok      = false;
  if ((rolData != NULL) && (bnkData != NULL)) {
    ok = adlib::load(reinterpret_cast<unsigned char *>(rolData), rolSize,
                     reinterpret_cast<unsigned char *>(bnkData), bnkSize);
  }
  if (rolData != NULL) {
    env->ReleaseByteArrayElements(rol, rolData, JNI_ABORT);
  }
  if (bnkData != NULL) {
    env->ReleaseByteArrayElements(bnk, bnkData, JNI_ABORT);
  }
  return ok ? JNI_TRUE : JNI_FALSE;
}

/*
 * Fill the buffer (size shorts, stereo) with music.  Returns true if
 * the song reached its end and started again during this call.
 */
JNIEXPORT jboolean JNICALL
Java_com_peculiargames_andmodplug_PlayerThread_AdLib_1JGetSoundData(
    JNIEnv *env, jobject, jshortArray buffer, jint size) {
  if (buffer == NULL) {
    return JNI_FALSE;
  }
  jsize length = env->GetArrayLength(buffer);
  if ((size <= 0) || (size > length)) {
    size = length;
  }
  jshort *data = env->GetShortArrayElements(buffer, NULL);
  if (data == NULL) {
    return JNI_FALSE;
  }
  bool restarted = adlib::render(data, size / 2);
  env->ReleaseShortArrayElements(buffer, data, 0);
  return restarted ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_peculiargames_andmodplug_PlayerThread_AdLib_1JUnload(
    JNIEnv *, jobject) {
  adlib::unload();
}

JNIEXPORT jstring JNICALL
Java_com_peculiargames_andmodplug_PlayerThread_AdLib_1JGetName(
    JNIEnv *env, jobject) {
  return env->NewStringUTF(adlib::description());
}

}  // extern "C"
