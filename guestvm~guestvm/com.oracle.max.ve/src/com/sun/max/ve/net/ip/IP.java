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
package com.sun.max.ve.net.ip;

import com.sun.max.ve.igmp.IGMP;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.arp.ARP;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.device.*;
import com.sun.max.ve.net.icmp.ICMP;
import com.sun.max.ve.net.protocol.ether.*;
import com.sun.max.ve.net.tcp.TCP;
import com.sun.max.ve.net.udp.UDP;


/**
 * IP.java
 *
 * This class implements the Internet Protocol layer.
 *
 * sritchie -- Oct 95
 *
 * @author Mick Jordan (modifications)
 *
 */
public class IP implements NetDevice.Handler {
    // Ethernet packet type constants
    private static final int ETHERTYPE_IP   = 0x800;

    // snmp counters
    private static int ipForwarding;
    private static int ipDefaultTTL;
    private static int ipInReceives;
    private static int ipInHdrErrors;
    private static int ipInAddrErrors;
    private static int ipForwDatagrams;
    private static int ipInUnknownProtos;
    private static int ipInDiscards;
    private static int ipInDelivers;
    private static int ipOutRequests;
    private static int ipOutDiscards;
    private static int ipOutNoRoutes;
    private static int ipReasmTimeout;
    private static int ipReasmReqds;
    private static int ipReasmOKs;
    private static int ipFragFails;
    private static int ipFragOKs;
    private static int ipFragCreates;
    private static int ipRoutingDiscards;

    public static int getStatistic(int index){
        switch(index){
        case 1:                // ipForwarding
            return 2;        // not forwarding
        case 2:                // ipDefaultTTL
            return 255;        // ttl always provided by user - still return 255 thou
        case 3:
            return ipInReceives;
        case 4:
            return ipInHdrErrors;
        case 5:
            return ipInAddrErrors;
        case 6:                // ipForwDatagrams
            return 0;
        case 7:
            return ipInUnknownProtos;
        case 8:
            return ipInDiscards;
        case 9:
            return ipInDelivers;
        case 10:
            return ipOutRequests;
        case 11:
            return ipOutDiscards; // currently always zero
        case 12:
            return ipOutNoRoutes;
        case 13:        // ipReasmTimeout (seconds)
            return 30;        // value of private IPReass.REASSEMBLY_TIMEOUT (30000 milliseconds)
        case 14:
            return ipReasmReqds;
        case 15:
            return ipReasmOKs;
        case 16:        // ipReasmFails
            return IPReass.getipReasmFails();
        case 17:
            return ipFragOKs;
        case 18:        // ipFragFails
            return 0;        // won't be any as not routing
        case 19:
            return ipFragCreates;
        case 23:
            return ipRoutingDiscards;
        default:
            return -1;
        }
    }

    private static IP _ip;

    private static int _localAddr;
    private static int _localNetwork;   // contains (localAddr & netmask)
    private static int _netmask;
    private static int _netbits;        // contains ~netmask

    // identification field for each packet we send.
    private static int _ident;

    // some useful IP constants

    protected static final int IPVERSION = 4;

    // we don't handle options for now so limit the header size.
    // (header length is in bytes)
    protected static final int MIN_HEADER_LEN = 20;
    protected static final int MAX_HEADER_LEN = 20;

    // IP protocol type constants
    protected static final int IPPROTO_ICMP = 1;
    protected static final int IPPROTO_IGMP = 2;
    protected static final int IPPROTO_TCP  = 6;
    protected static final int IPPROTO_UDP  = 17;

    // IP address constants
    protected static final int IP_CLASSD_ADDR = 0xE0000000;
    protected static final int IP_CLASSD_MASK = 0xF0000000;

    // Offsets for various IP header fields.
    protected static final int VERS_OFFSET  = 0;
    protected static final int TOS_OFFSET   = 1;
    protected static final int LEN_OFFSET   = 2;
    protected static final int IDENT_OFFSET = 4;
    protected static final int FRAG_OFFSET  = 6;
    protected static final int TTL_OFFSET   = 8;
    protected static final int PROT_OFFSET  = 9;
    protected static final int CKSUM_OFFSET = 10;
    protected static final int SRCIP_OFFSET = 12;
    protected static final int DSTIP_OFFSET = 16;

    // IP fragment control bits
    protected static final int IP_MF = 0x2000;   // more fragments
    protected static final int IP_DF = 0x4000;   // don't fragment

    private static Ether _ether      = null;
    private static ARP _arp           = null;
    private static IP _singleton;

    protected IP() {
    }

    public static IP getIP(Ether ether, ARP a) {
        if (_singleton == null) {
            _singleton = new IP(ether, a);
        }
        return _singleton;
    }

    private IP(Ether ether, ARP a) {
        if (_ether == null) {
            _ether = ether;
            _arp    = a;
            _debug = System.getProperty("max.ve.net.ip.debug") != null;
            _logErrors = System.getProperty("max.ve.net.ip.logerrors") != null;
        }
    }

    public void handle(Packet pkt) {
        pkt.shiftHeader(14);
        input(pkt);
    }

    public static int getLocalAddress() {
        return _localAddr;
    }

    public static int getNetmask() {
        return _netmask;
    }

    public static int getLocalNetwork() {
        return _localNetwork;
    }

    static void setLocalAddress(int local) {
        _localAddr = local;
    }

    /**
     * Called by ProtocolStack every time the underlying network
     * driver comes up.
     *
     * @param        local        Our local IP address
     * @param        mask        Our Netmask. If zero, then one will be computed.
     */
    public static void init(int local, int mask) {

        _localAddr = local;
        if (mask == 0) {
            mask = computeNetmask(local);
        }
        _netmask = mask;

        _netbits = ~mask;
        _localNetwork = _localAddr & _netmask;
        IPReass.init();
    }


    /**
     * Utility routine to compute a netmask from an IP address
     * @param         addr        The IP address
     * @return        The netmask
     */
    public static int computeNetmask(int addr) {
        addr = (addr >> 24) & 0xff;
        if (addr < 128) {
            return 0xff000000;      // Class A
        } else if (addr < 192) {
            return 0xffff0000;      // Class B
        }
        return 0xffffff00;          // Class C
    }

    //----------------------------------------------------------------------

    private static void printPacket(String str, Packet pkt) {
        if (_debug) {
            int version = (pkt.getByte(0) >>> 4);
            int headerLength = (pkt.getByte(0) & 0x0f);
            int typeOfService = pkt.getByte(1) & 0xff;
            int packetLength = ((pkt.getByte(2) & 0xff) << 8)
                    | (pkt.getByte(3) & 0xff);

            int ident = ((pkt.getByte(4) & 0xff) << 8)
                    + (pkt.getByte(5) & 0xff);
            int b = pkt.getByte(6) & 0xff;
            boolean dontFragment = (b & 0x40) != 0;
            boolean moreFragments = (b & 0x20) != 0;
            int fragmentOffset = ((b & 0x1f) << 8) | (pkt.getByte(7) & 0xff);

            int timeToLive = pkt.getByte(8) & 0xff;
            int protocol = pkt.getByte(9) & 0xff;
            int checksum = ((pkt.getByte(10) & 0xff) << 8)
                    | (pkt.getByte(11) & 0xff);

            int sourceAddr = pkt.getInt(12);
            int destAddr = pkt.getInt(16);

            dprint(str + "vers:" + version + " hlen:"
                    + headerLength + " tos:"
                    + Integer.toHexString(typeOfService) + " len:"
                    + packetLength + " id:" + ident + " df:" + dontFragment
                    + " mf:" + moreFragments + " foff:"
                    + Integer.toHexString(fragmentOffset) + " ttl:"
                    + timeToLive + " prot:" + protocol + " ck:"
                    + Integer.toHexString(checksum) + " src:"
                    + IPAddress.toString(sourceAddr) + " dst:"
                    + IPAddress.toString(destAddr));
        }
    }

    /**
     *  Returns the amount of space required for the IP header plus any other headers below.
     * @return
     */
    protected static int headerHint() {
        // Assume only Ethernet for now.
        return 14 /* network.headerHint() */ + MIN_HEADER_LEN;
    }

    private static int MAX_IP_LEN = 1480; // maximum allowed length of IP dgram
    private static final int DIV8 = 3;
    public static void setMtu(int m) {
        m -= MIN_HEADER_LEN;
        MAX_IP_LEN = (m >> DIV8) * 8;
    }

    /**
     * Create an IP header for this packet and send it.
     * The caller is assumed to already put the src_ip and dst_ip
     * fields into the header.
     *
     * @param pkt        A Packet with data to be sent. This is NOT
     *                        automatically recycled.
     * @param dst_ip        Destination IP address (should also be put into IP hdr)
     * @param len        Length of data not including IP header
     * @param ttlProto  Time to live & IP protocol type
     *                        (ttl << 24) | (type << 16), low 16 bits zero
     * @param tos        Type of service, byte, usually zero
     * @exception        NetworkException
     */
    protected static void output(Packet pkt, int dst_ip, int len, int ttlProto, int tos)
        throws NetworkException {

        if (_localAddr == 0) {
            // Make an exception for DHCP broadcast.
            if (dst_ip != 0xffffffff) {
                throw new NetworkException("Network Down");
            }
        }

        // check if we need to do fragmentation
        if (len > MAX_IP_LEN) {
            _ident++;
            ipOutRequests++;
            ipFragOKs++;

            int offset = 0;
            int src_ip = pkt.getInt(SRCIP_OFFSET-MIN_HEADER_LEN);
            Packet origPkt = pkt;
            pkt = new FragPacket(origPkt, 0);

            /*  WARNING: The implementation strategy below requires that packets are sent out
             * serially since the underlying buffer is shared between all the fragment packets and
             * the header for all but the first fragment overwrites user data at the end of the previous
             * fragment.
             */
            while (len > MAX_IP_LEN) {
                // inc for each fragment (this one is non-last frag)
                ipFragCreates++;

                // set the More Fragments bit and output this packet.
                pkt.putShort((IP_MF | offset), FRAG_OFFSET-MIN_HEADER_LEN);

                pkt.setDataLength(MAX_IP_LEN);
                output(pkt, dst_ip, MAX_IP_LEN, ttlProto, tos);

                // advance to next data fragment and prepare header
                offset += MAX_IP_LEN >> DIV8;
                len -= MAX_IP_LEN;

                pkt = new FragPacket(origPkt, offset * 8);
                pkt.putInt(src_ip, SRCIP_OFFSET-MIN_HEADER_LEN);
                pkt.putInt(dst_ip, DSTIP_OFFSET-MIN_HEADER_LEN);
            }

            ipFragCreates++;  // inc for each fragment (this one is last frag)
            pkt.setDataLength(len);

            // For the last fragment, don't set the More Fragments bit
            pkt.putShort(offset, FRAG_OFFSET-MIN_HEADER_LEN);

        } else if (!pkt.isFragment()) {
            // no fragmentation for this packet
            pkt.putShort(0, FRAG_OFFSET-MIN_HEADER_LEN);

            _ident++;
            ipOutRequests++;
        }

        // move the header offset to give us access to the IP header.
        pkt.shiftHeader(-MIN_HEADER_LEN);

        // generate the IP header
        pkt.putByte((IPVERSION << 4) | 5, VERS_OFFSET);  // put vers and hdrlen
        pkt.putByte(tos, TOS_OFFSET);                    // put type of service
        pkt.putInt(ttlProto, TTL_OFFSET);                // put ttl and proto
        pkt.putShort(len + MIN_HEADER_LEN, LEN_OFFSET);  // put packet len
        pkt.putShort(_ident, IDENT_OFFSET);               // put identification

        // compute and insert the header checksum
        int cksum = pkt.cksum(0, MIN_HEADER_LEN);
        pkt.putShort(cksum, CKSUM_OFFSET);

        printPacket("output: ", pkt);

        //
        // Figure out where to send this packet based on its
        // destination IP address:
        //        1. Loopback to ourselves
        //        2. Multicast: possibly send a copy to ourselves
        //        3. subnet-wide broadcast
        //        4. Off our subnet (need to find a route)
        //
        // If none of these are true, we leave the dst_ip unchanged
        // and the network driver will figure it out (i.e. Ethernet
        // will use Arp)
        //

        // Check for loopback packets.
        if (dst_ip == 0x7f000001 || (dst_ip == _localAddr && _localAddr != 0)) {
            _ip.input(pkt);
            return;
        }

        if ((dst_ip & IP_CLASSD_MASK) == IP_CLASSD_ADDR) {
            //
            // Sending to a multicast (class D) address.
            // Note, the destination IP address remains the
            // multicast; don't go through getRoute()
            // Check to see if we should send it to ourselves.
            // We don't have to be a member of group to send to it.
            //
            if (IGMP.activeGroup(dst_ip)) {
                int off = pkt.getHeaderOffset();
                IP.input(pkt);
                pkt.setHeaderOffset(off);
            }
        } else if ((dst_ip & _netbits) == _netbits) {
            //
            // Subnet broadcast
            //
            dst_ip = 0xffffffff;
        } else if ((dst_ip & _netmask) != _localNetwork) {
            //
            // off our subnet, ask for help to determine next host
            //
            dst_ip = ProtocolStack.getRoute(dst_ip);
        }

        if (dst_ip == 0) {
            ipOutNoRoutes++;
            throw new NetworkException("No route to host");
        }

        // output the packet to the network device
        // network.output(pkt, dst_ip);
        // check for a subnet broadcast address.
        if (dst_ip == 0xffffffff) {
            // do an Ethernet broadcast of this packet
            _ether.transmitBroadcast(pkt, ETHERTYPE_IP);
            return;
        }
        long dest = _arp.resolve(dst_ip);
        if (dest != 0) {
            _ether.transmit(dest, ETHERTYPE_IP, pkt);
        } else {
            dprint("could not resolve: " + IPAddress.toString(dst_ip));
        }
    }

    private static short mss = 1460;

    public static void setMru(int m) {
        mss = (short) (m - MIN_HEADER_LEN);
    }

    protected static short getRouteMSS(int dst_ip) {

        //
        // When more complex routing is implemented, the
        // routing table's gateway entry would determine the MSS.
        //

        return mss;

    }

    //----------------------------------------------------------------------

    public static void input(Packet pkt) {

        //dprint("input id:" + pkt.getShort(IDENT_OFFSET));
        // inc for each packet will dec later if datagram is fragment
        ipInReceives++;

        if (_debug) printPacket("input: ", pkt);

        // check for an Packet too small to contain an IP header
        int len = pkt.dataLength();
        if (len < MIN_HEADER_LEN) {
            ipInHdrErrors++;
            err("Packet length too small: " + len);
            return;
        }

        // get the IP version number
        int b = pkt.getByte(VERS_OFFSET);
        int version = (b >>> 4);

        if (version != IPVERSION) {
            ipInHdrErrors++;
            err("bad header version: " + Integer.toHexString(version));
            return;
        }

        // check whether the header length is acceptable
        int headerLength = (b & 0x0f) * 4;
        if (headerLength != MIN_HEADER_LEN) {
            ipInHdrErrors++;
            err("bad header length: " + headerLength);
            return;
        }

        // do the header checksum
        int cksum = pkt.cksum(0, headerLength);
        if (cksum != 0) {
            ipInHdrErrors++;
            /**/
            String hex = "0123456789abcdef";
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < headerLength; i++) {
                int val = pkt.getByte(i);
                int i1 = val & 0xf;
                int i2 = (val & 0xf0) >> 4;
                sb.append(hex.charAt(i2)).append(hex.charAt(i1)).append(' ');
            }
            Debug.println("bad cksum: " + sb.toString());
            /**/
            return;
        }

        // check if the Packet is large enough for given packet length
        int packetLength = pkt.getShort(LEN_OFFSET);
        if (packetLength > len) {
            ipInHdrErrors++;
            err("Packet too short");
            return;
        } else if (packetLength < len) {

            // set the true size of the data for this packet
            pkt.setDataLength(packetLength);
        }

        int dst_ip = pkt.getInt(DSTIP_OFFSET);

        // Here we try to filter out packets with destination addresses
        // not intended for us.  Check for localAddr, broadcast and loopback.
/* This filter is not really needed -- experimenting without it for now.
        if (dst_ip != 0x7f000001 &&
            (dst_ip != localAddr && localAddr != 0) &&
            (dst_ip & netbits) != 0x0 &&
            (dst_ip & netbits) != 0xff) {
            // this packet is not intended for us.
            dprint("rejected dst_ip: " + addrToString(dst_ip));
            return;
        }
        */

        // While previous test may not be needed, now that we support
        // multicast, we have to filter since the underlying driver
        // may deliver packets to us for multicast groups we don't
        // belong to.
        if ((dst_ip & IP_CLASSD_MASK) == IP_CLASSD_ADDR) {
            if (IGMP.activeGroup(dst_ip) != true) {
                // This group not active
                ipInAddrErrors++;
                return;
            }
        }

        int src_ip = pkt.getInt(SRCIP_OFFSET);
        int prot =   pkt.getByte(PROT_OFFSET) & 0xff;
        int offset = pkt.getShort(FRAG_OFFSET);

        // The multicast RFC (1112) says that if the source IP address
        // is a group address (type D), then we should quietly discard it.
        if ((src_ip & IP_CLASSD_MASK) == IP_CLASSD_ADDR) {
            return;
        }

        // advance header offset beyond IP header.
        pkt.shiftHeader(headerLength);

        // Check if this packet is an IP fragment.
        if ((offset & 0x3fff) != 0) {

            // get the IP identification for reassembly
            int id = pkt.getShort(IDENT_OFFSET-headerLength);

            // give this IP fragment to the reassembler
            ipReasmReqds++;
            pkt = IPReass.insertFragment(pkt, id, src_ip, dst_ip, prot,offset);
            if (pkt == null) {
                // undo the inc for this datagram as just a fragment
                ipInReceives--;
                return;
            }
            ipReasmOKs++;  // inc if pkt non null
        }

        switch (prot) {
        case IPPROTO_UDP:
            UDP.input(pkt, src_ip, dst_ip);
            ipInDelivers++;
            break;

        case IPPROTO_ICMP:
            // don't accept ICMP packets until we have an IP address.
            if (_localAddr != 0) {
                ICMP.input(pkt, src_ip);
                ipInDelivers++;
            }
            break;

        case IPPROTO_IGMP:
            // don't accept ICMP packets until we have an IP address.
            if (_localAddr != 0) {
                IGMP.input(pkt, src_ip, dst_ip);
                ipInDelivers++;
            }
            break;

        case IPPROTO_TCP:
            // Don't accept TCP packets until we have an IP address and
            // filter out broadcasts and other packets not unicast to us
            if (_localAddr != 0 && (dst_ip == _localAddr || dst_ip == 0x7f000001)) {
                TCP.input(pkt, src_ip);
                ipInDelivers++;
            }
            break;

        default: // unknown protocol
            dprint("unknown.input " + prot);
            if (_localAddr != 0 && (dst_ip == _localAddr || dst_ip == 0x7f000001)) {
                ipInUnknownProtos++;
            }
        }
    }

    //----------------------------------------------------------------------

    private static boolean _debug;
    private static boolean _logErrors;

    private static void dprint(String mess) {
        if (_debug) {
            Debug.println("IP: " + mess);
        }
    }

    private static void err(String mess) {
        if (_logErrors) {
            Debug.println("IP: " + mess);
        }
    }
}

