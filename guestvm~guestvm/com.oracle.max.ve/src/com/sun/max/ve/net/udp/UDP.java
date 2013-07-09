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
package com.sun.max.ve.net.udp;
//
// Udp.java
//
// User Datagram Protocol layer.
//
// sritchie -- Oct 95
//




import com.sun.max.ve.net.NetworkException;
import com.sun.max.ve.net.Packet;
import com.sun.max.ve.net.icmp.ICMP;
import com.sun.max.ve.net.ip.*;

/**
 * A lightweight class for creating a linked list of upcall objects.
 *
 * There is one linked list of Upcall objects, linked through the
 * 'next' field. Each of these is listening on a different UDP port.
 *
 * Each the objects in the linked list may have other Upcall objects
 * hanging off its 'others' field. All of these Upcall objects are
 * to receive a copy of the incoming packet.
 *
 * NOTE - HACK ALERT in deregister(). If any more fields are added to
 * this object, they may have to be copied in one of the cases
 */
class UpcallLink {

    int port;
    UDPUpcall udp;

    UpcallLink next;                // Linked list of Upcall objects
    UpcallLink prev;

    UpcallLink others;                // List of other Upcall objects on same port #

    UpcallLink(UDPUpcall u, int p) {
        port = p;
        udp = u;
    }
}


public class UDP extends IP {

    private static boolean _debug;

    public static void initialize() {
        _debug = System.getProperty("max.ve.net.udp.debug") != null;
    }

    protected UDP() {
    }

    // returns the number of bytes required by this layer's header
    // plus all the layers below.
    protected static int headerHint() {
        return IP.headerHint() + UDP_HDR_LEN;
    }


    // General output routine for UDP packets.  The caller has filled in
    // the user data which the packet header offset is currently pointing at.
    public static void output(Packet pkt, int src_port, int dst_ip,
                       int dst_port, int length, int ttl) {


        if (_debug) {
            dprint("output src_port:" + src_port + " dst_ip:"+IPAddress.toString(dst_ip)+
                   " dst_port:" + dst_port + " len:" + length + " off:" +
                   pkt.getHeaderOffset());
        }

        pkt.shiftHeader(-UDP_HDR_LEN);   // make room for our UDP header.
        length += UDP_HDR_LEN;           // add header length to packet len

        // fill in the remaining UDP header fields.
        pkt.putShort(src_port, SRCPORT_OFFSET);
        pkt.putShort(dst_port, DSTPORT_OFFSET);
        pkt.putInt(length<<16, LEN_OFFSET);   // this also zeroes the cksum

        // We need to compute the UDP checksum.  To do this first we
        // must create the UDP psuedo header.  Use the space
        // reserved for the IP header for this.
        pkt.putInt((IP.IPPROTO_UDP << 16) | length, -12);
        pkt.putInt(IP.getLocalAddress(), -8);   // put local IP addr
        pkt.putInt(dst_ip, -4);                 // put dest IP addr

        int cksum = pkt.cksum(-12, length+12);
        if (cksum == 0) {
            cksum = 0xffff;
        }

        pkt.putShort(cksum, CKSUM_OFFSET);

        try {
            // We're finished building the UDP header, now send the packet to IP
            IP.output(pkt, dst_ip, length,
                    (((ttl == 0) ? TTL : ttl) << 24) | (IP.IPPROTO_UDP << 16),
                    TOS);
        } catch (Exception e) {
            //
            // Someday, we need to something with this exception.
            //
            e.printStackTrace();
        }

    }

    /**
     * Handle all incoming UDP packets and dispatch them to
     * the appropriate UDP endpoint
     *
     * @param        pkt        The Packet. We are responsible for recycling
     *                        it before returning
     * @param        src_ip        The IP address of the sender
     * @param        dst_ip        The IP address of the destination
     */
    public static void input(Packet pkt, int src_ip, int dst_ip) {

        // make sure we have at least enough data for a header.
        int length = pkt.dataLength();
        if (length < UDP_HDR_LEN) {
            return;
        }

        int dest_port = pkt.getShort(DSTPORT_OFFSET);
        int src_port = pkt.getShort(SRCPORT_OFFSET);
        int udp_length = pkt.getShort(LEN_OFFSET);
        if (_debug) {
           dprint("input: src " + IPAddress.toString(src_ip) + ":" + src_port + " dst " +
                   IPAddress.toString(dst_ip) + ":" + dest_port + " len " + udp_length);
        }

        // Before we do anything else, see if this destination
        // port exists.  If it doesn't exist, we can throw
        // away the packet without wasting cycles decoding the
        // rest of the header and computing the checksum.
        UpcallLink link = find(dest_port);
        if (link == null) {
            if (dst_ip == IP.getLocalAddress()) {
                pkt.shiftHeader(-IP.MIN_HEADER_LEN);
                try {
                    ICMP.sendICMPDstUnreachable(src_ip, ICMP.PORT_UNREACHABLE, pkt);
                } catch (NetworkException ex) {
                    return;
                }
            }
            dprint("can't find port " + dest_port);
            return;
        }

        if (_debug) dprint("found port " + dest_port);

        pkt.setPortAndIPs(src_port, src_ip, dst_ip);

        // sanity check UDP packet length
        if (length < udp_length) {
            err("length " + udp_length + " < IP data length " + length);
            return;
        } else if (length > udp_length) {
            // set the true data length of this packet
            pkt.setDataLength(udp_length);
        }

        // See if we need to compute the UDP checksum.  If the supplied
        // checksum is 0, we don't need to compute it.
        int cksum = pkt.getShort(CKSUM_OFFSET);
        if (cksum != 0) {

            // Compute the UDP checksum.  To do this we need to create
            // a 12 byte UDP pseudo header.
            // The psuedo header is supposed to look like:
            //                32-bit IP src addr
            //          32-bit IP dst addr
            //          zero | protocol (17) | 16-bit length
            // Fortunately, the IP header already looks like:
            //          ttl | protocol (17) | 16-bit IP cksum
            //                32-bit IP src addr
            //          32-bit IP dst addr
            // Since the checksum can be calculated in any order,
            // we'll take advantage of the two IP addresses already
            // being in the packet. We need to preserve the
            // existing 32-bits (ttl, protocol, IP cksum), replace
            // it with (zero, 17, 16-bit length), compute the UDP
            // checksum, and put the original 32-bits back

            int prev32 = pkt.getInt(-12);
            pkt.putInt((IP.IPPROTO_UDP << 16) | udp_length, -12);

            cksum = pkt.cksum(-12, udp_length+12);
            pkt.putInt(prev32, -12);

            if (cksum != 0) {
                err("bad checksum!");
                return;
            }
        }

        // Advance over the UDP header and upcall packet to upper layer.
        pkt.shiftHeader(UDP_HDR_LEN);

        //
        // Deliver packet to all listeners registered on this port
        //
        link.udp.input(pkt);                // Deliver to first client
        UpcallLink l = link.others;        // Any others using same port?
        while (l != null) {
            l.udp.input(pkt);
            l = l.others;
        } // while
    }

    //----------------------------------------------------------------------

    private static UpcallLink head;
    private static int nextPort = 1024;

    // Search through the upcall list looking for the specified port.
    // Returns the link if found, null otherwise.
    private static UpcallLink find(int port) {

        UpcallLink link = head;

        if (_debug) dprint("find " + port );
        while (link != null) {
            if (_debug) dprint("matching against " + link.port);
            if (link.port == port) {
                return link;
            }

            link = link.next;
        }

        if (_debug) dprint("port " + port + " not found");
        return null;
    }

    /**
     * This method registers a user's upcall object with Udp.  When a
     * packet arrives on the given port, the user's upcall object input()
     * method will be called, passing the packet info.
     *
     * ProtocolStack.lock must be held upon entry
     *
     * @param        udp        the upcall object
     * @param        port        the requested port
     * @param        reuse        true if OK to reuse a port (UDP only)
     * @return        The port number registered.
     */
    public static int register(UDPUpcall udp, int port, boolean reuse) {

        if (_debug) dprint("register: " + port);
        if (port == 0) {
            //
            // Pick an unused port for the client
            //
            for (;;) {
                port = nextPort++;
                if (nextPort > 65535) {
                    nextPort = 1024;
                }

                if (find(port) == null) {
                    break;
                }
            }
        } else {
            //
            // client asked for a specific port. See if the port
            // is already in use.
            UpcallLink prev = find(port);
            if (prev != null) {
                if (!reuse) {
                    // the port already exists... return failure.
                    return 0;
                }
                //
                // The client wants to have more than one listener
                // on the same port. Allocate link and insert
                // into the existing link's 'others' field
                //
                UpcallLink link = new UpcallLink(udp, port);
                link.others = prev.others;
                prev.others = link;
                return port;
            }
        }

        // We now have a unique valid port number.  Allocate a link
        // and stick it in the upcall list.
        UpcallLink link = new UpcallLink(udp, port);

        // insert at head of list.
        link.next = head;
        if (head != null) {
            head.prev = link;
        }
        head = link;

        return port;
    }

    /**
     * Called by the user to remove knowledge of the given port.  If
     * packets to this port continue to arrive, they will be dropped
     * dropped and recycled().
     *
     * ProtocolStack.lock must be held upon entry
     *
     * @param udp        The Upcall object
     * @param port        The port it was listening on
     */
    static void deregister(UDPUpcall udp, int port) {
        UpcallLink link = find(port);

        if (link == null) {                // Nobody listening on that port.
            return;
        }


        if ((link.udp == udp) && (link.others == null)) {
            //
            // Only listener on this port
            //
            if (link == head) {
                head = link.next;
            } else {
                link.prev.next = link.next;
            }
            if (link.next != null) {
                link.next.prev = link.prev;
            }
        }
        else if (link.udp == udp) {
            //
            // The first guy is the one to remove, but rather than
            // unlink him (difficult), copy next guy's content
            // here and unlink that one! HACK ALERT.
            //
            UpcallLink removed = link.others;   // remember link we remove
            link.udp = link.others.udp;                // copy contents of next guy
            link.others = link.others.others;        // remove next guy
            link = removed;                            // point to removed link
        }
        else {
            //
            //
            // The first listener on this port is not the one,
            // but it might be further down the 'others' list
            //
            while (link.others != null) {
                if (link.others.udp == udp) {
                    UpcallLink removed = link.others;   // link we remove
                    // the next guy is it, remove him
                    link.others = link.others.others;
                    link = removed;                            // point to removed link
                    break;
                }

                link = link.others;                // not found, iterate
            } // while

            if (link.udp != udp) {                // Return if never found
                return;
            }
        }

        link.next = null;                        // Make sure no lingering refs
        link.prev = null;
        link.others = null;
        link.udp = null;
    }

    //----------------------------------------------------------------------

    public static void udpDestinationUnreachable(int dst_ip, int dst_port,
                                                 int src_port, int code) {

        switch(code) {
        case ICMP.NET_UNREACHABLE:
        case ICMP.HOST_UNREACHABLE:
        case ICMP.PROTO_UNREACHABLE:
        case ICMP.PORT_UNREACHABLE:
        case ICMP.FRAG_REQUIRED:
        case ICMP.SOURCE_ROUTE_FAIL:
        default:
        }

    }

    //----------------------------------------------------------------------

    static void dumpListeners() {

        UpcallLink link = head;

        System.out.println("UDP Listeners>>>>>");
        if (link == null) {
            System.out.println(" < No Entries >");
        }
        while (link != null) {

            System.out.println("    :: port " + link.port + ", " + link.udp);

            link = link.next;
        }

    }

    //----------------------------------------------------------------------

    private static void dprint(String mess) {
        if (_debug)
            err(mess);
    }

    private static void err(String mess) {
        System.out.println("UDP: " + mess);
    }

    //----------------------------------------------------------------------


    // default time-to-live for UDP packets
    private static final int TTL = 64;
    private static final int TOS = 0;                // Type of service

    private static final int UDP_HDR_LEN = 8;

    // Offsets for fields in the UDP header.
    private static final int SRCPORT_OFFSET = 0;
    private static final int DSTPORT_OFFSET = 2;
    private static final int LEN_OFFSET     = 4;
    private static final int CKSUM_OFFSET   = 6;



    //
    // snmp stuff
    //

    public static int getNumPorts(){
        UpcallLink link;
        int numPorts;

        numPorts = 0;
        for (link = head; link != null; link = link.next) {
            numPorts++;
        }

        return numPorts;
    }

    public static int getPorts(int[] arr){
        UpcallLink link = head;
        int i;

        for (i = 0; i < arr.length && link != null; i++, link = link.next) {
            arr[i] = link.port;
        }

        return i;
    }

    private static int udpInDatagrams;
    private static int udpNoPorts;
    private static int udpInErrors;
    private static int udpOutDatagrams;

    public static int getStatistic(int index){
        switch(index){
            case 1:
                return udpInDatagrams;
            case 2:
                return udpNoPorts;
            case 3:
                return udpInErrors;
            case 4:
                return udpOutDatagrams;
            default:
                return -1;
        }
    }
}
