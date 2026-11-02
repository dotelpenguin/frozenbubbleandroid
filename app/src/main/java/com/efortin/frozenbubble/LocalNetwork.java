/*
 *                 [[ Frozen-Bubble ]]
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
 */

package com.efortin.frozenbubble;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * The phone's address on the local (Wi-Fi) network, for local network
 * games.
 * <p>This replaces <code>WifiManager.getConnectionInfo()</code>,
 * <code>getDhcpInfo()</code> and <code>ConnectivityManager
 * .getActiveNetworkInfo()</code>, all deprecated.  The address and
 * subnet come from the active Wi-Fi network's link properties or, if
 * Wi-Fi is not the active network (a phone sharing its own hotspot,
 * say), from the Wi-Fi network interface itself.
 */
final class LocalNetwork {
  private LocalNetwork() {}

  /** An IPv4 address and its subnet prefix length (24 = 255.255.255.0). */
  static final class Info {
    final Inet4Address address;
    final int          prefix;

    Info(Inet4Address address, int prefix) {
      this.address = address;
      this.prefix  = prefix;
    }
  }

  /** The Wi-Fi IPv4 address, or null if there is none. */
  static Info wifi(Context context) {
    ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
    if (cm != null) {
      Network network = cm.getActiveNetwork();
      NetworkCapabilities caps =
          (network == null) ? null : cm.getNetworkCapabilities(network);
      if ((caps != null) &&
          (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
        LinkProperties link = cm.getLinkProperties(network);
        if (link != null) {
          for (LinkAddress la : link.getLinkAddresses()) {
            if (la.getAddress() instanceof Inet4Address) {
              return new Info((Inet4Address) la.getAddress(),
                              la.getPrefixLength());
            }
          }
        }
      }
    }
    return fromInterfaces();
  }

  /** Look at the Wi-Fi (or hotspot) network interface directly. */
  private static Info fromInterfaces() {
    try {
      Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
      if (all == null) {
        return null;
      }
      for (NetworkInterface ni : Collections.list(all)) {
        String name = ni.getName();
        if (!ni.isUp() || ni.isLoopback() || (name == null) ||
            !(name.startsWith("wlan") || name.startsWith("swlan") ||
              name.startsWith("ap")   || name.startsWith("eth"))) {
          continue;
        }
        for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
          if (ia.getAddress() instanceof Inet4Address) {
            return new Info((Inet4Address) ia.getAddress(),
                            ia.getNetworkPrefixLength());
          }
        }
      }
    } catch (Exception e) {
      // No interfaces we may look at.
    }
    return null;
  }

  /**
   * The local Wi-Fi address; 0.0.0.0 when there is none (as the old
   * <code>WifiInfo.getIpAddress()</code> gave).
   */
  static InetAddress address(Context context) {
    Info info = wifi(context);
    if (info != null) {
      return info.address;
    }
    try {
      return InetAddress.getByAddress(new byte[4]);
    } catch (UnknownHostException e) {
      return null;   // Not possible for a 4-byte address.
    }
  }

  /**
   * The broadcast address of the local Wi-Fi subnet, or null if there
   * is no Wi-Fi address.
   * @param limitMask - a further mask on the subnet, in the old
   * <code>DhcpInfo</code> byte order (low byte = first number), so
   * 0xFFFF limits it to at most a 255.255.0.0 subnet.
   */
  static InetAddress broadcast(Context context, int limitMask) {
    Info info = wifi(context);
    if (info == null) {
      return null;
    }
    byte[] ip    = info.address.getAddress();
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++) {
      int bits   = Math.max(0, Math.min(8, info.prefix - (k * 8)));
      int subnet = ((0xFF00 >> bits) & 0xFF) & ((limitMask >> (k * 8)) & 0xFF);
      quads[k]   = (byte) ((ip[k] & subnet) | (~subnet & 0xFF));
    }
    try {
      return InetAddress.getByAddress(quads);
    } catch (UnknownHostException e) {
      return null;
    }
  }

  /** Whether the phone has a network connection that reaches the internet. */
  static boolean isConnected(Context context) {
    ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
    Network network = (cm == null) ? null : cm.getActiveNetwork();
    NetworkCapabilities caps =
        (network == null) ? null : cm.getNetworkCapabilities(network);
    return (caps != null) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }
}
