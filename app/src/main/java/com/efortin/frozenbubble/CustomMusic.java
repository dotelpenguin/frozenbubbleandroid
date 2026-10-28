/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lets the player use tracker songs (.mod, .xm, .it, .s3m and other
 * formats libmodplug supports) from a folder on their device in place
 * of the built-in level music.
 *
 * <p>The folder is chosen with Android's folder picker (Storage Access
 * Framework), so no broad storage permission is needed: the game only
 * ever sees that one folder.
 */
public final class CustomMusic {
  /** CheckBoxPreference key: use the custom folder instead of built-in. */
  public static final String KEY_ON     = "custom_music_option";
  /** Persisted content:// tree URI of the chosen folder. */
  public static final String KEY_FOLDER = "custom_music_folder";

  /** Tracker formats libmodplug can play. */
  private static final Set<String> EXTENSIONS = new HashSet<String>(
      // Only the formats the music engine is built with (see
      // cpp/CMakeLists.txt): the rarer readers were left out for safety.
      Arrays.asList("mod", "xm", "it", "s3m"));

  private static final int MAX_DEPTH = 3;    // subfolder levels scanned
  private static final int MAX_SONGS = 1000;
  private static final long CACHE_MS = 30 * 1000;

  private static String    cachedFolder = null;
  private static long      cachedAt     = 0;
  private static List<Uri> cachedSongs  = Collections.emptyList();

  private CustomMusic() {}

  /**
   * The songs to use for level music, or an empty list when the custom
   * folder is switched off, not set, unreadable, or has no songs (the
   * caller then plays the built-in music).
   */
  public static synchronized List<Uri> getActiveSongs(Context context) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    String folder = sp.getString(KEY_FOLDER, null);
    if (!sp.getBoolean(KEY_ON, false) || (folder == null)) {
      return Collections.emptyList();
    }
    long now = System.currentTimeMillis();
    if (!folder.equals(cachedFolder) || ((now - cachedAt) > CACHE_MS)) {
      cachedSongs  = listSongs(context, Uri.parse(folder));
      cachedFolder = folder;
      cachedAt     = now;
    }
    return cachedSongs;
  }

  public static Uri getFolder(Context context) {
    String folder = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(KEY_FOLDER, null);
    return (folder == null) ? null : Uri.parse(folder);
  }

  /**
   * Remember a folder returned by ACTION_OPEN_DOCUMENT_TREE, keeping read
   * access across restarts and releasing access to any previous folder.
   */
  public static synchronized void setFolder(Context context, Uri treeUri) {
    ContentResolver cr = context.getContentResolver();
    Uri old = getFolder(context);
    if ((old != null) && !old.equals(treeUri)) {
      try {
        cr.releasePersistableUriPermission(old,
            Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } catch (SecurityException se) {
        // Access was already gone.
      }
    }
    cr.takePersistableUriPermission(treeUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION);
    PreferenceManager.getDefaultSharedPreferences(context).edit()
        .putString(KEY_FOLDER, treeUri.toString())
        .apply();
    cachedFolder = null; // force a rescan
  }

  /** All playable song files in the folder and its subfolders, by name. */
  public static List<Uri> listSongs(Context context, Uri treeUri) {
    final List<String> names = new ArrayList<String>();
    final List<Uri>    uris  = new ArrayList<Uri>();
    try {
      collect(context.getContentResolver(), treeUri,
              DocumentsContract.getTreeDocumentId(treeUri), 0, names, uris);
    } catch (RuntimeException e) {
      // Permission revoked, folder deleted, or provider error.
      return Collections.emptyList();
    }
    // Sort by file name so the level-to-song order is stable.
    Integer[] order = new Integer[names.size()];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    Arrays.sort(order, (a, b) -> names.get(a).compareToIgnoreCase(names.get(b)));
    List<Uri> sorted = new ArrayList<Uri>(order.length);
    for (Integer i : order) {
      sorted.add(uris.get(i));
    }
    return Collections.unmodifiableList(sorted);
  }

  /** A short display name for the folder, e.g. "Mods". */
  public static String describeFolder(Context context, Uri treeUri) {
    Uri doc = DocumentsContract.buildDocumentUriUsingTree(treeUri,
        DocumentsContract.getTreeDocumentId(treeUri));
    try (Cursor c = context.getContentResolver().query(doc,
        new String[] { DocumentsContract.Document.COLUMN_DISPLAY_NAME },
        null, null, null)) {
      if ((c != null) && c.moveToFirst() && (c.getString(0) != null)) {
        return c.getString(0);
      }
    } catch (RuntimeException e) {
      return context.getString(R.string.folder_unavailable);
    }
    return context.getString(R.string.folder_chosen);
  }

  private static void collect(ContentResolver cr, Uri treeUri, String docId,
                              int depth, List<String> names, List<Uri> uris) {
    Uri children =
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
    try (Cursor c = cr.query(children, new String[] {
          DocumentsContract.Document.COLUMN_DOCUMENT_ID,
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_MIME_TYPE },
        null, null, null)) {
      if (c == null) {
        return;
      }
      while (c.moveToNext() && (uris.size() < MAX_SONGS)) {
        String id   = c.getString(0);
        String name = c.getString(1);
        String mime = c.getString(2);
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
          if (depth < MAX_DEPTH) {
            collect(cr, treeUri, id, depth + 1, names, uris);
          }
        }
        else if (isSong(name)) {
          names.add(name);
          uris.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, id));
        }
      }
    }
  }

  private static boolean isSong(String name) {
    if (name == null) {
      return false;
    }
    int dot = name.lastIndexOf('.');
    if (dot >= 0) {
      return EXTENSIONS.contains(
          name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
    // Amiga-style names put the format first, e.g. "mod.popcorn".
    dot = name.indexOf('.');
    return (dot > 0) &&
        EXTENSIONS.contains(name.substring(0, dot).toLowerCase(Locale.ROOT));
  }
}
