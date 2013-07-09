/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.ve.net;

/**
 * A test of the ARP protocol handler.
 * Usage: ip address [reap r] [tick n] [t]
 *
 * reap explicitly sets the cache reap interval
 * tick explicitly sets the ARP cache entry timeout
 * t runs the check for addr as a separate thread
 *
 * @author Mick Jordan
 */

import java.util.*;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.arp.*;
import com.sun.max.ve.net.device.*;
import com.sun.max.ve.net.guk.GUKNetDevice;
import com.sun.max.ve.net.ip.*;
import com.sun.max.ve.net.protocol.ether.*;

public class ARPTest  implements Runnable {
    private static ARP _arp;

    private IPAddress _ipAddress;
    ARPTest(IPAddress ipAddress) {
        _ipAddress = ipAddress;
    }

    public void run() {
        _arp.checkForIP(_ipAddress.addressAsInt(), 4);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {
        final List<String> ipAddressStrings = new ArrayList<String>();
        int reapInterval = 0;
        int ticks = 0;
        boolean threads = false;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("ip")) {
                ipAddressStrings.add(args[++i]);
            } else if (arg.equals("reap")) {
                reapInterval = Integer.parseInt(args[++i]);
            } else if (arg.equals("tick")) {
                ticks = Integer.parseInt(args[++i]);
            } else if (arg.equals("t")) {
                threads = true;
            }
        }
        // Checkstyle: resume modified control variable check
        final NetDevice device = GUKNetDevice.create();
        final Ether ether = new Ether(device);
        Init.checkConfig();
        if (reapInterval > 0) {
            ARP.setCacheReapInterval(reapInterval);
        }
        if (ticks > 0) {
            ARP.setCacheEntryTimeout(ticks);
        }

        _arp = ARP.getARP(ether);
        IP.init(Init.getLocalAddress().addressAsInt(), Init.getNetMask().addressAsInt());
        ether.registerHandler(_arp, "ARP");
        for (String ipAddressString : ipAddressStrings) {
            if (threads) {
                new Thread(new ARPTest(IPAddress.parse(ipAddressString))).start();
            } else {
                new ARPTest(IPAddress.parse(ipAddressString)).run();
            }
        }
        while (true) {
            Thread.sleep(10000);
            System.out.println("ARP Cache");
            final ARP.CacheEntry[] entries = _arp.getArpCache();
            for (ARP.CacheEntry entry : entries) {
                System.out.println("  " + entry.getIPAddress() + " : " + Ether.addressToString(entry.getEthAddress()));
            }
        }
    }
}

