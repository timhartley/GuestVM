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
// TCPRecvQueue.java
//
// Implementation of the TCP state machine receive queue.
//
// sritchie -- Apr 96
//
// notes
//
// The receive queue is currently implemented by a FIFO byte array
// that we copy all data into.  We might want to get fancy and
// piggyback on the user's buffer in the future.
//
// There is an issue of how much memory to allow this queue to consume.
//

/*
 * There is no (additional) synchronization necessary in this class as all calls are made holding the
 * lock on the associated TCP instance.
 */

import com.sun.max.ve.net.Packet;


public class TCPRecvQueue {

    int bytesQueued;

    private int start;
    private int end;

    private byte buf[];

    private static boolean checked;

    public TCPRecvQueue(int size) {
        if (!checked) {
            debug = System.getProperty("max.ve.net.tcp.debug") != null;
            checked = true;
        }
        buf = new byte[size];
    }

    void append(Packet pkt) {

        int len = pkt.dataLength();

        //dprint("append " + len + " bytes to queue of " + bytesQueued);

        int n = len;
        if (n > buf.length - end ) {
            n = buf.length - end;
        }

        pkt.getBytes(0, buf, end, n);

        if (len > n) {
            end = len - n;
            pkt.getBytes(n, buf, 0, end);
        } else {
            end += len;
            if (end >= buf.length) {
                end = 0;
            }
        }

        bytesQueued += len;
    }

    int read(byte dst[], int dst_off, int len) {

        if (len > bytesQueued) {
            len = bytesQueued;
        }

        int n = len;
        if (n > buf.length - start) {
            n = buf.length - start;
        }

        System.arraycopy(buf, start, dst, dst_off, n);

        if (len > n) {
            start = len - n;
            System.arraycopy(buf, 0, dst, dst_off + n, start);
        } else {
            start += len;
            if (start >= buf.length) {
                start = 0;
            }
        }

        bytesQueued -= len;
        return len;
    }

    void cleanup() {
        buf = null;
    }

    //------------------------------------------------------------------------

    private static boolean debug = false;

    private static void dprint(String mess) {
        if (debug == true) {
            System.err.println("TCPRecvQueue: [" + Thread.currentThread().getName() + "]" + mess);
        }
    }

    private static void err(String mess) {
        System.err.println("TCPRecvQueue: " + mess);
    }
}
