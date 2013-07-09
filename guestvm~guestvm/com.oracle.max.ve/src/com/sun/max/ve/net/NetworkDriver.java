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
package com.sun.max.ve.net;

/**
 * NetworkDriver defines an interface for network device drivers
 * to implement.  This is so that we can have more than one implementation
 * of a network driver.
 *
 */
public interface NetworkDriver {

    /**
     * General initialization routine.  Must be called before any
     * packet activity.
     */
    void initNetworkDriver();

    /**
     * Get a device-dependent packet suitable for output.
     *
     * @param hlen Usually the size of the link header returned by headerHint()
     * @param dlen The maximum size of the IP pkt incl IP header
     * @return A packet to use or null if none available
     */
    Packet getTransmitPacket(int hlen, int dlen);

    /**
     * Sends an IP packet out the network.
     *
     * @param pkt Everything in the packet except any link headers
     * @param dst_ip The destination IP address for the packet
     */
    void output(Packet pkt, int dstIp);

    /**
     * Returns default router  - called by ProtocolStack
     * Some links, like PPP know the default router. Others,
     * like Ethernet don't, and should return 0.
     *
     * @param dst The destination IP address
     */
    int    getDefaultRouter(int dst);

    /**
     * Return boot configuration - called by ProtocolStack
     * Some links, like PPP provide boot configuration.
     * Others, like Ethernet don't and should return null
     *
     * @return a BootConfiguration object
     */
    // BootConfiguration getConfig();

    /**
     * print a summary of interface status.
     *
     * @param out A PrintStream to use
     */
    // void report(java.io.PrintStream out);

    /**
     * @return the MTU for this network interface
     */
    int getMtu();

    /**
     * @return the packet header offset for this interface
     */
    int headerHint();

    /*
     * Checks whether the given IP address is available for use.
     * This is useful for protecting against duplicate IP addresses
     * on a network during auto-configuration.
     *
     * @param ip_addr The IP address to test
     * @return true if ip_addr already exists, false otherwise.
     */
    boolean checkForIP(int ipAddr);

    /**
     * Enable multicast reception. The network driver need not use-
     * count, as IP will only call this once for any given address
     * Some links may not support multicasting and should ignore this call
     *
     * @param group the class-D IP address of the group to join
     */
    void joinGroup(int ipAddr);

    /**
     * Disable multicast reception.
     * IP will not call this for a particular address if joinGroup
     * hasn't already been called.
     * Some links may not support multicasting and should ignore this call
     *
     * @param group the class-D IP address of the group to leave
     */
    void leaveGroup(int ipAddr);
}
