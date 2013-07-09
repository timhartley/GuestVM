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
package com.sun.max.ve.net.tcp;
//
// TCPSendQueue.java
//
// Implementation of the TCP state machine send queue.
//
// sritchie -- Apr 96
//
// notes
//
// The send queue is currently implemented by a FIFO byte array
// that we copy all data into.  We might want to get fancy and
// piggyback on the user's buffer in the future.
//
// There is an issue of how much memory to allow this queue to consume.
//

/*
 * There is no (additional) synchronization necessary in this class as all calls are made holding the
 * lock on the associated TCP instance.
 */

import com.sun.max.ve.fs.ErrorDecoder;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;

public class TCPSendQueue {

    private TCP tcp;        // our connection
    private int bytesFree;

    private int start;          // start of data index in buf
    private int end;            // end of data index in buf

    private byte buf[];         // data storage area

    // Since we're not interested in high throughput write performance,
    // we can make the send window really small and save memory.
    private static final int maxBufSize = 8760;


    TCPSendQueue(TCP tcp, int size) {

        if (size > maxBufSize) {
            size = maxBufSize;
        }

        this.tcp = tcp;
        bytesFree = size;
        buf = new byte[size];

    }

    // Append data from the given buffer to the send queue.
    // Returns the number of bytes appended.
    int append(byte src[], int src_off, int len)
        throws NetworkException, InterruptedException {

        // Wait until there is room available.
        while (bytesFree == 0) {

            // Bubble up InterruptedException.  The user will never know
            // how much data was actually queued, however.  JDK java.io
            // needs an API to tell the user how much data was
            // successfully written when InterruptedIOException comes in.
            // This is the best we can do for now...
            if (!tcp._blocking) {
                return -ErrorDecoder.Code.EAGAIN.getCode();
            }
            tcp.wait();

            if (buf == null) {
                // The connection has been blown away from underneath us,
                // probably caused by receiving a RST from the peer.
                throw new NetworkException("connection reset by peer");
            }
        }

        // Figure out how much we can queue at this point.
        if (len > bytesFree) {
            len = bytesFree;
        }

        int n = len;
        if (n > buf.length - end) {
            n = buf.length - end;
        }

        System.arraycopy(src, src_off, buf, end, n);

        // Check for wrap around case and copy wrapped portion if necessary.
        if (len > n) {
            end = len - n;
            System.arraycopy(src, src_off+n, buf, 0, end);
        } else {
            end += len;
            if (end >= buf.length) {
                end = 0;
            }
        }

        bytesFree -= len;
        return len;
    }

    void drop(int todrop) {

        if (TCP._debug) TCP.tcpdprint(tcp, "dropping:" + todrop + " start:" + start + " end:" + end);

        if (bytesFree == buf.length) {
            if (TCP._debug) TCP.sdprint("can't drop anything");
            return;
        }

        bytesFree += todrop;

        start += todrop;
        if (start >= buf.length) {
            start -= buf.length;
        }

        tcp.notify();

        if (TCP._debug) TCP.tcpdprint(tcp, "drop() bytesFree:" + bytesFree + " start:" + start + " end:" + end);
    }

    Packet getPacket(int dest_ip, int pos, int hlen, int dlen) {

        if (TCP._debug) TCP.tcpdprint(tcp, "getPacket() pos " + pos);

        Packet pkt = Packet.getTx(dest_ip, hlen, dlen);
        if (pkt == null) {
            return null;
        }

        pos += start;

        if (pos >= buf.length) {
            pos -= buf.length;
        }

        int n = dlen;
        if (n > buf.length - pos) {
            n = buf.length - pos;
        }

        assert n >= 0;
        pkt.putBytes(buf, pos, 0, n);

        if (n < dlen) {
            pkt.putBytes(buf, 0, n, dlen - n);
        }

        return pkt;
    }

    void cleanup() {
        buf = null;
        tcp.notify();
    }

}
