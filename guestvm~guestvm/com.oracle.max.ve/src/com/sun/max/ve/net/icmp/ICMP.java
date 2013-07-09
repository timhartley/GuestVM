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
package com.sun.max.ve.net.icmp;
//
// Implementation of the ICMP protocol.
//
// notes:
//
// We are super minimal for now.  It is yet to be decided which
// features of ICMP we need.
//
// sritchie -- Oct 95



import java.util.*;

import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.ip.*;
import com.sun.max.ve.net.tcp.*;
import com.sun.max.ve.net.udp.*;


public class ICMP extends IP {

    /**
     * constructor is private; this is a totally static class
     */
    private ICMP() {
    }

    //----------------------------------------------------------------------

    // ICMP packet type codes.
    private static final int ICMP_ECHOREPLY = 0;
    private static final int ICMP_DEST_UNREACHABLE = 3;
    private static final int ICMP_REDIRECT = 5;
    private static final int ICMP_ECHO      = 8;
    private static final int ICMP_ROUTER_AD = 9;
    private static final int ICMP_ROUTER_SOLICIT = 10;

    private static final int DEFAULT_TTL = 255;
    private static final int TOS = 0;

    // minimum size of an ICMP packet
    private static final int ICMP_MINLEN    = 8;

    // ICMP header field offsets
    private static final int TYPE_OFFSET  = 0;
    private static final int CODE_OFFSET  = 1;
    private static final int CKSUM_OFFSET = 2;

    // type ECHO field offsets
    private static final int IDENT_OFFSET = 4;
    private static final int SEQ_OFFSET   = 6;
    private static final int DATA_OFFSET  = 8;

    //
    // icmp destination unreachable code types.
    //
    public static final int NET_UNREACHABLE = 0;
    public static final int HOST_UNREACHABLE = 1;
    public static final int PROTO_UNREACHABLE = 2;
    public static final int PORT_UNREACHABLE = 3;
    public static final int FRAG_REQUIRED = 4;
    public static final int SOURCE_ROUTE_FAIL = 5;

    //
    // snmp vars
    //

    public static int[] snmpInputStats = new int[19];
    public static int[] snmpOutputStats = new int[19];
    public static int icmpInMsgs,icmpOutMsgs,icmpInErrors,icmpOutErrors;

    static class IdKey {
        protected int _id;
        IdKey(int id) {
            _id = id;
        }
        @Override
        public boolean equals(Object other) {
            return other instanceof IdKey && ((IdKey) other)._id == _id;
        }
        @Override
        public int hashCode() {
            return _id;
        }
    }

    static class IdSeqKey extends IdKey {
        int _seq;
        IdSeqKey(int id, int seq) {
            super(id);
            _seq = seq;
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof IdSeqKey) {
                IdSeqKey otherKey = (IdSeqKey) other;
                return  otherKey._id == _id && otherKey._seq == _seq;
            }
            return false;
        }
        @Override
        public int hashCode() {
            return _id + _seq;
        }
    }

    static class Response {
        boolean _haveReply;
        int _seq;
    }

    private static final Map<IdKey, Response> _results = Collections.synchronizedMap(new HashMap<IdKey, Response>());
    private static int _icmpId = 1;
    private static final int DEFAULT_ECHO_TIMEOUT = 2000;

    private static final Map<Integer, ICMPHandler> _handlers = new HashMap<Integer, ICMPHandler>();

    /**
     * Return an id that can be used repeatedly in a series of sendICMPEchoReq calls.
     * @return the id, guaranteed to not be used by any other ICMP request.
     */
    public static int nextId() {
        return _icmpId++;
    }

    public static int defaultTimeout() {
        return DEFAULT_ECHO_TIMEOUT;
    }

    public static int defaultTTL() {
        return DEFAULT_TTL;
    }

    /**
     * Sends a echo request to a given destination and wait for reply with matching sequence number (or timeout).
     * @param ipAddress echo request destination
     * @param timeout time to wait for reply
     * @param ttl time to live (max number of hops)
     * @param id a unique id for the ICMP request (@see nextId)
     * @param seq a sequence number to identify this request in a sequence
     * @return seq if there was a matching reply from the destination or -1 if the request timed out
     */
    public static int doSeqMatchingICMPEchoReq(IPAddress ipAddress, int timeout, int ttl, int id, int seq) {
        return sendICMPEchoReq(ipAddress, timeout, ttl, id, seq, false);
    }

    public static int doSeqMatchingICMPEchoReq(IPAddress ipAddress, int id, int seq) {
        return sendICMPEchoReq(ipAddress, DEFAULT_ECHO_TIMEOUT, DEFAULT_TTL, id, seq, false);
    }

   /**
     * Sends a echo request to a given destination and wait for any reply or timeout.
     * @param ipAddress echo request destination
     * @param timeout time to wait for reply
     * @param ttl time to live (max number of hops)
     * @param id a unique id for the ICMP request (@see nextId)
     * @param seq a sequence number to identify this request in a sequence
     * @return the reply sequence number if there was a reply from the destination or -1 if the request timed out
     */
    public static int doICMPEchoReq(IPAddress ipAddress, int timeout, int ttl, int id, int seq) {
        return sendICMPEchoReq(ipAddress, timeout, ttl, id, seq, true);
    }

    public static int doICMPEchoReq(IPAddress ipAddress, int id, int seq) {
        return sendICMPEchoReq(ipAddress, DEFAULT_ECHO_TIMEOUT, DEFAULT_TTL, id, seq, true);
    }

    private static int sendICMPEchoReq(IPAddress ipAddress, int timeout,
            int ttl, int id, int seq, boolean anyReply) {
        Response response = new Response();
        _results.put(getKey(id, seq, anyReply), response);
        rawSendICMPEchoReq(ipAddress.addressAsInt(), timeout, ttl, id, seq);

        synchronized (response) {
            try {
                response.wait(timeout);
            } catch (InterruptedException ex) {
            }
        }

        _results.remove(response);
        if (response._haveReply) {
            return response._seq;
        }
        return -1;
    }

    /**
     * Sends an echo request without waiting for a reply. The assumption is that a handler has been
     * registered with @see registerHandler to handler the reply (or lack of it).
     * @param ipAddress
     * @param timeout
     * @param ttl
     * @param id
     * @param seq
     */
    public static void rawSendICMPEchoReq(int destination, int timeout, int ttl, int id, int seq) {
        Packet pkt = Packet.getTx(destination, IP.headerHint(), 8);
        pkt.putByte(ICMP_ECHO, TYPE_OFFSET);                // type = ECHO
        pkt.putByte(0, CODE_OFFSET);                        // Code = 0
        pkt.putShort(0, CKSUM_OFFSET);                        // set cksum to 0
        pkt.putShort(id, IDENT_OFFSET);                        // Id.
        pkt.putShort(seq, SEQ_OFFSET);                        // seq

        // checksum the packet and retro-fill it in.
        int cksum = pkt.cksum(0, 8);
        pkt.putShort(cksum, 2);

        pkt.putInt(IP.getLocalAddress(), -8);
        pkt.putInt(destination, -4);

        try {
            output(pkt, destination, 8, (ttl << 24) | (IP.IPPROTO_ICMP << 16), TOS);
            icmpOutMsgs++;
            snmpOutputStats[ICMP_ECHO]++;
        } catch (NetworkException ex) {
            icmpOutErrors++;
        }
    }

    private static IdKey getKey(int id, int seq, boolean anyReply) {
        if (anyReply) {
            return new IdKey(id);
        } else {
            return new IdSeqKey(id, seq);
        }
    }

    private static void echo(Packet pkt, int src_ip, int length)
        throws NetworkException {

        // Get the identifier and sequence number
        int id = pkt.getShort(IDENT_OFFSET);
        int seq = pkt.getShort(SEQ_OFFSET);

        //err("ECHO id:0x"+Util.hex(id)+" seq:0x"+Util.hex(seq));

        Packet reply = Packet.getTx(src_ip, IP.headerHint(), length);
        if (reply == null) {
            return;
        }

        // copy echo data bytes from source packet to reply packet
        reply.putBytes(pkt, IDENT_OFFSET, IDENT_OFFSET, length-IDENT_OFFSET);

        reply.putByte(ICMP_ECHOREPLY, TYPE_OFFSET);
        reply.putByte(0, CODE_OFFSET);

        // zero out the checksum field and recompute.
        reply.putShort(0, CKSUM_OFFSET);
        int cksum = reply.cksum(0, length);
        reply.putShort(cksum, CKSUM_OFFSET);

        //err("sending ICMP_ECHOREPLY len:" + length + " cksum:" +
        //    Util.hex(cksum));

        // store the source and destination IP addresses into IP header
        reply.putInt(IP.getLocalAddress(), IP.SRCIP_OFFSET-20);
        reply.putInt(src_ip, IP.DSTIP_OFFSET-20);

        output(reply, src_ip, length, (DEFAULT_TTL<<24) | (IP.IPPROTO_ICMP<<16), TOS);
        snmpOutputStats[ICMP_ECHOREPLY]++;
        icmpOutMsgs++;
    }


    //----------------------------------------------------------------------

    static public void input(Packet pkt, int src_ip) {

        icmpInMsgs++;

        // sanity check if there is enough data in the packet for a
        // minimum ICMP header.
        int length = pkt.dataLength();
        if (length < ICMP_MINLEN) {
            err("short packet");
            icmpInErrors++;
            return;
        }

        printPacket("input: ", pkt, true, src_ip);

        // get the type, code and checksum fields from the ICMP header.
        int type = pkt.getByte(TYPE_OFFSET);
        int code = pkt.getByte(CODE_OFFSET);
        int cksum = pkt.getShort(CKSUM_OFFSET);
        // we don't verify cksum for now

        //dprint("type:" + type + " code:" + code + " cksum:0x" +
        //       Util.hex(cksum) + " len:" + length);

        if(type >= 0 && type < snmpInputStats.length)
            snmpInputStats[type]++;
        else
            icmpInErrors++;

        switch (type) {

        case ICMP_ECHO:
            try {
                echo(pkt, src_ip, length);
            } catch (NetworkException ex) {
                return;
            }
            break;

        case ICMP_ECHOREPLY:
            echoreply(pkt, src_ip, length);
            break;

        case ICMP_ROUTER_AD:
            routerAdvertisement(pkt, src_ip, length);
            break;

        case ICMP_DEST_UNREACHABLE:
            destUnreachable(pkt, src_ip, length);
            break;

        case ICMP_REDIRECT:
            icmpRedirect(pkt, src_ip, length);
            break;

        default:
            // do nothing for now
        }
    }


    //----------------------------------------------------------------------

    public static void sendRouterSolicit() throws NetworkException {

        //
        // Build an ICMP Router Solicitation packet and send it off.
        // Currently we send to the limited broadcast address. Someday we
        // ought to make this support multicast.
        //

        //dprint("sending Router Solicit");

        Packet pkt = Packet.getTx(0xffffffff, IP.headerHint(), 8);
        if (pkt == null)
            return;

         pkt.putByte(ICMP_ROUTER_SOLICIT, TYPE_OFFSET);        // type.
         pkt.putByte(0, CODE_OFFSET);                        // Code = 0
        pkt.putShort(0, CKSUM_OFFSET);                        // set cksum to 0
        pkt.putInt(0, 4);                                // Reserved.

        // checksum the packet and retro-fill it in.
        int cksum = pkt.cksum(0, 8);
        pkt.putShort(cksum, 2);

        pkt.putInt(IP.getLocalAddress(), -8);
        pkt.putInt(0xffffffff, -4);

        output(pkt, 0xffffffff, 8, (DEFAULT_TTL << 24) | (IP.IPPROTO_ICMP << 16), TOS);
        snmpOutputStats[ICMP_ROUTER_SOLICIT]++;
        icmpOutMsgs++;
    }

    //----------------------------------------------------------------------

    private static void routerAdvertisement(Packet pkt, int src_ip, int len) {

        int code = pkt.getByte(1);
        int num = pkt.getByte(4);
        int size = pkt.getByte(5);
        int life = pkt.getShort(6);

        if (code != 0 || size < 2) {
            return;
        }

        /*
         * System.err.println("RouterAd: code " + code
         *                         + " num " + num
         *                         + " size " + size
         *                         + " life " + life);
         */

        int        router, pref;
        for(int off = 8; off < (8 + (num * size * 4)); off += (size * 4)) {
            router = pkt.getInt(off);
            pref = pkt.getInt(off + 4);

            Route.addDefaultRouter(router, pref, life);
            // Route.dumpRoutingInfo();
        }
    }

    //----------------------------------------------------------------------

    private static void icmpRedirect(Packet pkt, int src_ip, int len) {

        int code = pkt.getByte(1);
        int gway = pkt.getInt(4);
        int dest = pkt.getInt(24);

        switch (code) {
        case 0:                        // Network redirect
        case 1:                        // Host redirect
            Route.redirectRoute(dest, gway);
            break;
        default:
            break;
        }
    }

    public static void registerHandler(int src_ip, ICMPHandler handler) {
        _handlers.put(src_ip, handler);
    }

    private static void echoreply(Packet pkt, int src_ip, int length) {
        int id = pkt.getShort(IDENT_OFFSET);
        int seq = pkt.getShort(SEQ_OFFSET);

        ICMPHandler handler = _handlers.get(src_ip);
        if (handler != null) {
            handler.handle(pkt, src_ip, id, seq);
            return;
        }

        // Look for an exact match with sequence number
        Response response = _results.get(getKey(id, seq, false));
        if (response == null) {
            response = _results.get(getKey(id, seq, true));
        }
        if (response != null) {
            synchronized (response) {
                response._haveReply = true;
                response._seq = seq;
                response.notify();
            }
        }
        // Otherwise, no-one is interested in this reply
    }

    //----------------------------------------------------------------------

    private static void destUnreachable(Packet pkt, int src_ip, int len) {

        int prot = pkt.getByte(17);
        int dst_ip = pkt.getInt(24);
        int dst_port = pkt.getShort(30);
        int src_port = pkt.getShort(28);
        int code = pkt.getByte(CODE_OFFSET);

        switch(prot) {
        case 6:
            TCP.tcpDestinationUnreachable(dst_ip, dst_port, src_port, code);
            break;
        case 17:
            UDP.udpDestinationUnreachable(dst_ip, dst_port, src_port, code);
            break;
        }
    }

    // ----------------------------------------------------------------------

    public static void sendICMPDstUnreachable(int dest, int code, Packet ipkt)
            throws NetworkException {

        // dprint("sending DST UNREACHABLE dst: " + IP.addrToString(dest) +
        // ", code " + code);

        //
        // header size is calculated as follows:
        // 8 - ICMP header
        // 20 - IP header of pkt that caused the unreachable.
        // 8 - Next protocol header(TCP or UDP)
        //
        Packet pkt = null;

        pkt = Packet.getTx(dest, IP.headerHint(), 36);
        if (pkt == null)
            return;

        pkt.putByte(ICMP_DEST_UNREACHABLE, TYPE_OFFSET);// type DEST UNREACH
        pkt.putByte(code, CODE_OFFSET); // Code = passed in
        pkt.putShort(0, CKSUM_OFFSET); // set cksum to 0
        pkt.putInt(0, 4); // set unused to 0
        pkt.putBytes(ipkt, 0, 8, 28);

        // checksum the packet and retro-fill it in.
        int cksum = pkt.cksum(0, 36);
        pkt.putShort(cksum, CKSUM_OFFSET);

        pkt.putInt(IP.getLocalAddress(), -8);
        pkt.putInt(dest, -4);

        output(pkt, dest, 36, (DEFAULT_TTL << 24) | (IP.IPPROTO_ICMP << 16),
                TOS);
        icmpOutMsgs++;
        snmpOutputStats[ICMP_DEST_UNREACHABLE]++;
    }

    protected static void output(Packet pkt, int dst_ip, int len, int ttlProto, int tos) throws NetworkException {
        printPacket("output: ", pkt, false, dst_ip);
        IP.output(pkt, dst_ip, len, ttlProto, tos);
    }


    // ----------------------------------------------------------------------

    private static boolean _debug = System.getProperty("max.ve.net.icmp.debug") != null;

    private static void dprint(String mess) {
        if (_debug) {
            err(mess);
        }
    }

    private static void err(String mess) {
        Debug.println("ICMP: " + mess);
    }

    private static void printPacket(String str, Packet pkt, boolean input, int ipAddr) {
        if (_debug) {
            int type = pkt.getByte(TYPE_OFFSET);
            int code = pkt.getByte(CODE_OFFSET);
            int cksum = pkt.getShort(CKSUM_OFFSET);
            int id = pkt.getShort(IDENT_OFFSET);
            int seq = pkt.getShort(SEQ_OFFSET);
            String srcDest = input ? "src" : "dest";
            dprint(str + srcDest + IPAddress.toString(ipAddr) + " type:" + type + " code:" + code + " cksum:" + Integer.toHexString(cksum) + " id:" + id + " seq:" + seq);

        }
    }
}

