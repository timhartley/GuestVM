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
package com.sun.max.ve.igmp;
//
// Implementation of IGMP (Internet Group Management Protocol) version 1
//
//
// Mike Shoemaker 12/96
//

import java.io.IOException;
import java.net.SocketException;

import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.ip.*;



/**
 * The IGMP protocol stack.
 *
 * We listen for incoming IGMP packets
 * We handle requests to join/leave groups
 * We send IGMP Host Membership Report packets as necessary
 */
public class IGMP extends IP {
    // Constants for use with group addresses (Class D)
    private static final int IGMP_ALLHOSTS = 0xE0000001;

    // constants for fields of IGMP packets
    private static final int IGMP_TYPEMASK = 0xF;
    private static final int IGMP_VERSMASK = 0xF0;
    private static final int IGMP_VERS_ONE = 0x10;
    private static final int IGMP_QUERY_TYPE = 1;
    private static final int IGMP_REPORT_TYPE = 2;
    private static final int IGMP_TOS = 0;              // Type of Service

    // constants for IGMP header length & offsets
    private static final int IGMP_PACKETLENGTH = 8;
    private static final int TYPEVERSION_OFFSET = 0;
    private static final int UNUSED_OFFSET = 1;
    private static final int CHECKSUM_OFFSET = 2;
    private static final int ADDRESS_OFFSET = 4;

    // local implementation constants
    private static final int GROUP_GROW_SIZE = 10;

    // Local variables
    private static GroupEntry[]    entries;
    private static int             lastEntry = 0;
    private static NetworkDriver   net;
    private static boolean           debug = false;

    /**
     * constructor is private; this is a totally static class
     */
    private IGMP() {
    }

    /**
     * init routine to be called by IP at startup.
     *
     * @param n         NetworkDriver the protocol stack is using
     *                  Needed for adding/deleting multicasts
     */
    public static void setNetworkDriver(NetworkDriver n) {
        net = n;
    }


    /**
     * Tests if the group address is currently active
     * Used by IP to filter incoming traffic.
     *
     * @param grp       The group address (an IP address)
     * @return          True or False
     */
    public static boolean activeGroup(int grp) {
        if (grp == IGMP_ALLHOSTS) {
            return true;
        }
        return findGroupIndex(grp) >= 0 ? true : false;
    }

    /**
     * Find index into group entry array corresponding to group
     *
     * @param grp       The group address (an IP address)
     * @return          Index or -1 if not found
     */
    static private int findGroupIndex(int grp) {
        if (entries == null) {
            return -1;
        }
        for (int i = 0 ; i <= lastEntry ; i++) {
            if (entries[i] != null && entries[i].group == grp) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Returns true if there are any active groups (beyond the ALLHOSTS
     * group)
     *
     * @return        true        if active groups left
     */
    static private boolean noMoreActiveGroups() {
        for (int i = 0 ; i <= lastEntry ; i++) {
            if (entries[i] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Send an ICMP Host Membership Report packet
     *
     * @param grp       The group number
     */
    static void sendReport(int grp) {
        // Get a new packet
        Packet pkt = Packet.getTx(grp, IP.headerHint(), IGMP_PACKETLENGTH);
        if (pkt == null) {
            //dprint("sendReport(), can't allocate a packet.");
            return;                             // Best effort...
        }

        // Fill in the ICMP header
        pkt.putByte(IGMP_VERS_ONE | IGMP_REPORT_TYPE, TYPEVERSION_OFFSET);
        pkt.putByte(0, UNUSED_OFFSET);
        pkt.putShort(0, CHECKSUM_OFFSET);
        pkt.putInt(grp, ADDRESS_OFFSET);

        // Compute the checksum
        int cksum = pkt.cksum(0, IGMP_PACKETLENGTH);
        pkt.putShort(cksum, CHECKSUM_OFFSET);

        // We're expected to put in src & destination IP addresses
        // into header before calling output
        pkt.putInt(IP.getLocalAddress(), -8);
        pkt.putInt(grp, -4);                    // dest is grp, NOT all hosts

        // Send it off to la-la land, ignoring any failures
        try {
            IP.output(pkt, grp, IGMP_PACKETLENGTH,
                            (1<<24) | (IP.IPPROTO_IGMP << 16),  // ttl & proto
                            IGMP_TOS);
        } catch (NetworkException ex) {
            //dprint("NetworkException trying to send packet");
            return;
        }
    }

    /**
     * Handling upcalls from IP with incoming IGMP packets
     *
     * There are only two we care about:
     *   1. Host Membership Query
     *      We send out Host Membership Reports for all
     *      currently active groups (except the "all-hosts" group)
     *      We send one packet per group, but we delay sending
     *      packets by random amounts in case we hear reports
     *      from other hosts on same net.
     *   2. Host Membership Report
     *      We hear these from other hosts on network.
     *      If they are reporting for a group that we're planning
     *      on reporting, then we can cancel our report
     *
     * @param pkt               The incoming packet
     * @param src_ip            IP address of sender
     * @param dst_ip                IP address of destination
     */
    static public void input(Packet pkt, int src_ip, int dst_ip) {

        //
        // Ignore our own packets
        //
        if (src_ip == IP.getLocalAddress()) {
            return;
        }
        //
        // Check to make there's enough data for valid packet
        //
        if (pkt.dataLength() < IGMP_PACKETLENGTH) {
            //dprint("pkt too small: " + pkt.dataLength());
            return;
        }

        // Make sure it's the right protocol version
        int typeVersion = pkt.getByte(TYPEVERSION_OFFSET);

        if ((typeVersion & IGMP_VERSMASK) != IGMP_VERS_ONE) {
            return;
        }

        //
        // Verify checksum %%% ICMP doesn't do it, why should we? :-)
        //
        int cksum = pkt.getShort(CHECKSUM_OFFSET);

        int grp = pkt.getInt(ADDRESS_OFFSET);

        switch (typeVersion & IGMP_TYPEMASK) {

            case IGMP_QUERY_TYPE:
                if (dst_ip == IGMP_ALLHOSTS) {        // only valid if sent here
                    //
                    // For every group, start the process of sending a
                    // membership report
                    //
                    if (entries == null) {            // there are none!
                        break;
                    }

                    for (int i = 0 ; i <= lastEntry ; i++) {
                        if (entries[i] != null)
                            entries[i].startReport();
                    }
                }
                break;

            case IGMP_REPORT_TYPE:
                //
                // Received report from another host on network.
                // If this group is active on our machine, cancel
                // any outstanding report we have pending
                //
                if (dst_ip == grp) {    // RFC says silenty ignore if ip != grp
                    //dprint("Received report for group " +
                                //Integer.toHexString(grp));
                    int i = findGroupIndex(grp);
                    if (i >= 0) {
                        entries[i].cancelReport();
                    }
                }
                break;

            default:
                break;

        } // switch

    } // input


    /**
     * Interface to join a new group. Protocol lock must be grabbed.
     *
     * @param ipAddr    The group's IP address
     * @exception       IOException if bad address or no resources
     *                  to add the group data structures.
     */
    static public void joinGroup(int ipAddr) throws IOException {

        //dprint("joinGroup(" + Integer.toHexString(ipAddr) + ")");

        if (
            // Must be class D address
            ((ipAddr & IP.IP_CLASSD_MASK) != IP.IP_CLASSD_ADDR) ||

            // Must not be group 0 or 1
            ((ipAddr & ~IP.IP_CLASSD_MASK) <= 1)   ) {

            throw new SocketException("not a multicast address");
        }

        if (entries == null) {                  // The first client!
            // Since this is the first, tell net driver to join
            // the ALLHOSTS group. Until now, we didn't care about
            // any traffic on that group.
            //dprint("first client, calling network driver to join ALLHOSTS");
            net.joinGroup(IGMP_ALLHOSTS);

            // Allocate the initial array of group entries
            entries = new GroupEntry[GROUP_GROW_SIZE];

        }

        //
        // Check to see if we already know about this group.
        // If so, increment use count and return
        //
        int i = findGroupIndex(ipAddr);
        if (i >= 0) {
            entries[i].useCount++;
            return;
        }

        // look for empty spot
        for (i = 0 ; i < entries.length ; i++) {
            if (entries[i] == null) {
                break;
            }
        }

        //
        // If no empty spots, we need to allocate a larger
        // array, and copy over the contents of the current one
        //
        if (i >= entries.length) {
            GroupEntry[] n =
                        new GroupEntry[entries.length + GROUP_GROW_SIZE];
            // Fill in new array
            System.arraycopy(entries, 0, n, 0, entries.length);

            // Make new array the current one
            entries = n;

            // Fall through with i set to next empty spot
        }

        // Found empty spot. new an entry. throw exception on failure
        entries[i] = new GroupEntry(ipAddr);

        // Update index of last valid entry
        if (i > lastEntry) {
            lastEntry++;
        }

        net.joinGroup(ipAddr);

        //
        // Send an usolicited membership report right now. Plus, send another
        // one a little later in case the first gets dropped (as
        // recommended by RFC)
        sendReport(ipAddr);                 // Send one now
        entries[i].startReport();        // Send one later
    }

    /**
     * Interface for leaving a group. Protocol stack lock must be grabbed
     *
     *
     * @param ipAddr    The group's IP address
     * @exception       IOException if badd address or no resources
     *                  to add the group data structures.
     */
    static public void leaveGroup(int ipAddr) throws IOException {
        //dprint("leaveGroup(" + Integer.toHexString(ipAddr) + ")");

        if (
            // Must be class D address
            ((ipAddr & IP.IP_CLASSD_MASK) != IP.IP_CLASSD_ADDR) ||

            // Must not be group 0 or 1
            ((ipAddr & ~IP.IP_CLASSD_MASK) <= 1) ||

            // Must have some existing groups
            (entries == null)
            ) {
            throw new SocketException("not a multicast address");
        }

        int i = findGroupIndex(ipAddr);
        if (i >= 0) {
            // Found match, decrement use count. If goes to zero,
            // remove entry
            if (--entries[i].useCount <= 0) {
                entries[i].cancelReport();        // In case one pending
                entries[i] = null;
                //dprint("IGMP: calling net.leaveGroup(" +
                            //Integer.toHexString(ipAddr) + ")");
                net.leaveGroup(ipAddr);
            }
            if (noMoreActiveGroups()) {
                entries = null;
                //dprint("No more active groups, calling network driver to leave ALLHOSTS");
                net.leaveGroup(IGMP_ALLHOSTS);
            }
        }
        else {
            throw new IOException("IGMP: Group not in use");
        }
    }

    /**
     * Turn on or off debugging messages
     *
     * @param        enable        true to turn them on, false to turn them off
     */
    public static void setDebug(boolean enable) {
        debug = enable;
    }

    /**
     * Print a debugging message if debugging is enabled
     *
     * @param        mess        The message to print. It will be preceeded
     *                        by our class name.
     */
    private static void dprint(String mess) {
        if (debug) {
            Debug.println("IGMP: " + mess);
        }
    }

}
