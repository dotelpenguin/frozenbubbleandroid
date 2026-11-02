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
import android.provider.DocumentsContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Lets the player use their own songs from a folder on their device in
 * place of the built-in level music: tracker songs (.mod, .xm, .it,
 * .s3m) and AdLib Visual Composer songs (.rol).
 *
 * <p>A .rol song needs an instrument bank (.bnk).  For each song the
 * bank is, in order: one with the same name next to it ("song.bnk"),
 * "standard.bnk" in its folder or the nearest folder above it, or any
 * "standard.bnk" found in the music folder.  Songs without a bank are
 * left out.
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

  /** AdLib songs' extension, and their instrument banks'. */
  private static final String ROL = "rol";
  private static final String BNK = "bnk";
  private static final String STANDARD_BANK = "standard.bnk";

  /** The instrument bank chosen for each .rol song found so far. */
  private static final Map<Uri, Uri> banks = new HashMap<Uri, Uri>();

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
    SharedPreferences sp = Prefs.get(context);
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
    String folder = Prefs.get(context)
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
    Prefs.get(context).edit()
        .putString(KEY_FOLDER, treeUri.toString())
        .apply();
    cachedFolder = null; // force a rescan
  }

  /**
   * The instrument bank (.bnk) to play an AdLib song with, or null if
   * the song is not an AdLib song (see {@link #listSongs}).
   */
  public static Uri bankFor(Uri song) {
    synchronized (banks) {
      return banks.get(song);
    }
  }

  /** All playable song files in the folder and its subfolders, by name. */
  public static List<Uri> listSongs(Context context, Uri treeUri) {
    final List<String> names = new ArrayList<String>();
    final List<Uri>    uris  = new ArrayList<Uri>();
    final Scan         scan  = new Scan();
    try {
      collect(context.getContentResolver(), treeUri,
              DocumentsContract.getTreeDocumentId(treeUri), 0, null,
              names, uris, scan);
    } catch (RuntimeException e) {
      // Permission revoked, folder deleted, or provider error.
      return Collections.emptyList();
    }
    /*
     * AdLib songs with no bank of their own or above them: use any
     * standard bank found elsewhere in the folder, or leave them out.
     */
    for (int i = uris.size() - 1; i >= 0; i--) {
      Uri song = uris.get(i);
      if (scan.waiting.contains(song)) {
        if (scan.anyStandard != null) {
          scan.found.put(song, scan.anyStandard);
        }
        else {
          uris.remove(i);
          names.remove(i);
        }
      }
    }
    synchronized (banks) {
      banks.putAll(scan.found);
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

  /** What a scan has found besides the songs. */
  private static final class Scan {
    /** AdLib song -> its bank. */
    final Map<Uri, Uri> found   = new HashMap<Uri, Uri>();
    /** AdLib songs with no bank in their folder or above it. */
    final Set<Uri>      waiting = new HashSet<Uri>();
    /** The first standard.bnk seen anywhere. */
    Uri anyStandard = null;
  }

  /**
   * Collect the songs in one folder, then in its subfolders.
   * @param standard - the nearest standard.bnk above this folder.
   */
  private static void collect(ContentResolver cr, Uri treeUri, String docId,
                              int depth, Uri standard, List<String> names,
                              List<Uri> uris, Scan scan) {
    Uri children =
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
    List<String>     dirs     = new ArrayList<String>();
    List<String>     rolNames = new ArrayList<String>();
    List<Uri>        rolUris  = new ArrayList<Uri>();
    Map<String, Uri> bnks     = new HashMap<String, Uri>();
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
          dirs.add(id);
        }
        else if (ROL.equals(extension(name))) {
          rolNames.add(name);
          rolUris.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, id));
        }
        else if (BNK.equals(extension(name))) {
          bnks.put(name.toLowerCase(Locale.ROOT),
                   DocumentsContract.buildDocumentUriUsingTree(treeUri, id));
        }
        else if (isSong(name)) {
          names.add(name);
          uris.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, id));
        }
      }
    }
    Uri here = bnks.get(STANDARD_BANK);
    if (here != null) {
      standard = here;
      if (scan.anyStandard == null) {
        scan.anyStandard = here;
      }
    }
    for (int i = 0; (i < rolNames.size()) && (uris.size() < MAX_SONGS); i++) {
      String name = rolNames.get(i);
      Uri    song = rolUris.get(i);
      Uri    bank = bnks.get(name.substring(0, name.length() - ROL.length())
                                 .toLowerCase(Locale.ROOT) + BNK);
      if (bank == null) {
        bank = standard;
      }
      if (bank != null) {
        scan.found.put(song, bank);
      }
      else {
        scan.waiting.add(song);
      }
      names.add(name);
      uris.add(song);
    }
    if (depth < MAX_DEPTH) {
      for (String dir : dirs) {
        if (uris.size() >= MAX_SONGS) {
          break;
        }
        collect(cr, treeUri, dir, depth + 1, standard, names, uris, scan);
      }
    }
  }

  /** "Song.ROL" -> "rol"; "" if there is none. */
  private static String extension(String name) {
    if (name == null) {
      return "";
    }
    int dot = name.lastIndexOf('.');
    return (dot >= 0) ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
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
