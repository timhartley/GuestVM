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
 * Basic test of IP.
 * To see more info set -Dmax.ve.net.ip.debug
 *
 * @author Mick Jordan
 */
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.arp.ARP;
import com.sun.max.ve.net.debug.Debug;
import com.sun.max.ve.net.guk.GUKNetDevice;
import com.sun.max.ve.net.ip.IP;
import com.sun.max.ve.net.protocol.ether.Ether;

public class IPTest {

    public static void main(String[] args) throws InterruptedException {
        final GUKNetDevice device = GUKNetDevice.create();
        Init.checkConfig();
        final Ether ether = new Ether(device);
        final ARP arp = ARP.getARP(ether);
        final IP ip = IP.getIP(ether, arp);
        IP.init(Init.getLocalAddress().addressAsInt(), Init.getNetMask().addressAsInt());
        ProtocolStack.setRoute(Init.getGateway().addressAsInt());
        ether.registerHandler(arp, "ARP");
        ether.registerHandler(ip, "IP");
        while (true) {
            Thread.sleep(10000);
            Debug.println("IPTest.main: pkt count " + device.pktCount() + " drop count " + device.dropCount());
        }
    }

}
