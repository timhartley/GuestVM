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
/*
   IP packet reassembler.

   sritchie -- Apr 96

   notes

   The algorithm to add fragments to the fragment list is very simple minded
   and doesn't prune overlaps.  It rejects any packets that overlap.

   Once initialized, there is no dynamic allocation in this code.  All
   fragment and reassembler descriptors are preallocated in free lists
   and are recycled after use.

   When a new fragment comes in, and we don't have a free Fragment descriptor
   or Reassembler descriptor, the oldest reassembler in the active
   reassembly list is recycled.
*/

import java.util.*;

import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;


/**
 *  This class manages a list of reassemblers.  When a new fragment arrives,
 *  a reassembler is allocated and queued.  Subsequent fragments belonging
 *  to the same reassembler are added to the reassembler.
 *
 * @author Mick Jordan (modifications)
 *
 */
final class IPReass {

    private static final long REASSEMBLY_TIMEOUT = 30000;
    private static Map<FragmentId, IPReass> _reassemblers;
    private static Timer _timer;    // reassembly timer

    static void init() {
        _debug = System.getProperty("max.ve.net.ip.reass.debug") != null;
        _reassemblers = Collections.synchronizedMap(new HashMap<FragmentId, IPReass>());
        _timer = new Timer("IPReass", true);
    }

    /**
     * This class uniquely identifies a fragment and is the key for the reassemblers map.
     *
     */
    static class FragmentId {
        int _src_ip;   // source IP address
        int _dst_ip;   // dest IP address
        int _ident;    // packet identification
        int _prot;     // protocol number

        FragmentId(int ident, int src_ip, int dst_ip, int prot) {
            _ident = ident;
            _src_ip = src_ip;
            _dst_ip = dst_ip;
            _prot = prot;
        }

        @Override
        public boolean equals(Object other) {
            FragmentId otherFragmentId = (FragmentId)other;
            return otherFragmentId._ident == _ident && otherFragmentId._src_ip == _src_ip &&
                        otherFragmentId._dst_ip == _dst_ip && otherFragmentId._prot == _prot;
        }

        @Override
        public int hashCode() {
            return _src_ip | _dst_ip | _ident |_prot;
        }
    }

    static class Fragment {
        int _start_offset;    // IP fragment offset
        int _end_offset;
        Packet _pkt;
        Fragment _next;

        Fragment(Packet pkt) {
            _pkt = pkt;
        }
    }

    private FragmentId _fragmentId;
    private int _bytesQueued;  // number of bytes queued in fragment list
    private int _totalBytes;   // total size of resulting packet

    // A list of fragments which make up the reassembled packet.
    Fragment _fragments;

    private static int _ipReasmFails;

    static int getipReasmFails(){
        return _ipReasmFails;
    }

    private IPReass(FragmentId fragmentId) {
        _fragmentId = fragmentId;
        _reassemblers.put(_fragmentId, this);
        _timer.schedule(new ReassemblerTimerTask(), REASSEMBLY_TIMEOUT);
    }

    /**
     * Called when the timer task associated with this reassembler times out.
     */
    private void timeout() {
        _reassemblers.remove(_fragmentId);
    }

    /** This method is the main entry point for reassembling a packet.
    * If the insertion of this fragment results in a completed IP packet,
    * the reassembled packet is returned.  Otherwise null is returned.
    * */
    static Packet insertFragment(Packet pkt, int id, int src_ip, int dst_ip,
                                 int prot, int offset) {

        // Allocate a new Fragment descriptor for this packet.  We need
        // to copy() the packet because it will be held in the fragment list.
        Fragment frag = new Fragment(pkt.copy());

        if ( frag == null ) {
            // can't insert more fragment -- not enough buffer
          // return a pkt with length 0, will be discarded by upper layer
          // Maybe should return an ICMP of some sort

          // try {
            dprint("no more fragments. ");
          //Icmp.sendIcmpDstUnreachable(src_ip, Icmp.FRAG_REQUIRED,
          //                        pkt);
        //} catch (NetworkException ex) {
        //    return pkt;
        //  }
          pkt.setLength(0);
          return pkt;
        }

        FragmentId fragmentId = new FragmentId(id, src_ip, dst_ip, prot);
        IPReass reass = _reassemblers.get(fragmentId);

        if (reass != null) {
            pkt = reass.add(frag, offset);
            if (pkt != null) {
                // the IP packet is successfully reassembled!
                if (_debug) dprint("reassemble done id:" + id);
                _reassemblers.remove(fragmentId);
            }
            return pkt;
        }

        // We couldn't find a reassembler for this fragment, so let's
        // start a new one.
        reass = new IPReass(fragmentId);
        return reass.add(frag, offset);
    }

    private static final int IP_MF = 0x2000;   // more fragments bit

    /** Combine all the packets in the fragment list into one large Packet.
     *
     * @return
     */
    private Packet combine() {

        // allocate a packet large enough for the IP header and
        // total data bytes. need a full 20 byte IP header in case
        // this is a UDP message and the port is unreachable because
        // Udp.input() shifts the header -20 to get to the beginning
        // of the IP header.
        Packet pkt = Packet.get(IP.MIN_HEADER_LEN, _totalBytes);

        // generate the IP header. assume default TOS and ignore TTL
        // because this packet will not be going back up the stack.
        pkt.shiftHeader(-IP.MIN_HEADER_LEN);
        pkt.putByte((IP.IPVERSION << 4) | 5, IP.VERS_OFFSET);
        pkt.putInt(_fragmentId._src_ip, IP.SRCIP_OFFSET);
        pkt.putInt(_fragmentId._dst_ip, IP.DSTIP_OFFSET);
        pkt.putByte(_fragmentId._prot & 0xff, IP.PROT_OFFSET);
        pkt.putShort(_fragmentId._ident, IP.IDENT_OFFSET);

        pkt.shiftHeader(IP.MIN_HEADER_LEN);

        // iterate over all the fragments and copy each of them into
        // the large packet. they will be recycled when the reassembler
        // gets recycled.

        Fragment f = _fragments;
        while (f != null) {
            if (_debug) {
                dprint("combine offset:" + f._start_offset + " len:" + f._pkt.dataLength());
            }
            pkt.putBytes(f._pkt, 0, f._start_offset*8, f._pkt.dataLength());
            f = f._next;;
        }

        return pkt;
    }

    /** Add a fragment to the list of fragments.  If the addition of this
    * fragment results in a completed packet, the fragments are combined
    * and the completed packet is returned.  Otherwise we return null.
    * */
    Packet add(Fragment new_frag, int start_offset) {

        // check for the Last Fragment bit in the offset
        boolean moreFragments = true;
        if ((start_offset & IP_MF) == 0) {
            moreFragments = false;
        }

        // get the total number of bytes in this fragment
        int len = new_frag._pkt.dataLength();

        // mask out the fragment start offset and compute the end offset
        new_frag._start_offset = start_offset & 0x1fff;
        new_frag._end_offset = new_frag._start_offset + (((len+7) >> 3) - 1);

        if (_debug) {
            dprint("add start:" + new_frag._start_offset + " end:" + new_frag._end_offset + " len:" + len + " more:" + moreFragments);
        }

        // add the fragments in any order because combine() puts them
        // into the right slot in the final packet. we do not do overlap
        // detection.
        if (_fragments == null) {
            _fragments = new_frag;
        } else {
            new_frag._next = _fragments;
            _fragments = new_frag;
        }

        if (moreFragments == false) {
            // this is the last fragment, now we know how large the
            // reassembled packet is.
            _totalBytes = (new_frag._start_offset * 8) + len;
            if (_debug) {
                dprint("got last fragment, totalBytes:" + _totalBytes);
            }
        }

        _bytesQueued += len;
        if (_bytesQueued == _totalBytes) {
            return combine();
        }

        return null;
    }

    private static boolean _debug;

    private static void dprint(String s) {
        if (_debug)
            Debug.println("IPReass: " + s);
    }

     class ReassemblerTimerTask extends TimerTask {
        @Override
        public void run() {
            timeout();
        }

    }

}

