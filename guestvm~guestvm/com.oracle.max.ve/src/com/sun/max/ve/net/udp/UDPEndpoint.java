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
// UdpEndpoint.java
//
// This class implements the glue between the UDP protocol stack
// and the JDK Sockets API.
//
// sritchie -- Oct 95
//
// notes:
//
// We implement some trivial packet buffering for received packets
// (a linked list with a maximum upper bound).
//
// Packets are received via upcalls from the Udp protocol through
// the UdpUpcall interface.
//

import java.io.*;
import java.net.BindException;
import java.net.SocketException;
import java.nio.ByteBuffer;

import com.sun.max.ve.error.VEError;
import com.sun.max.ve.fs.ErrorDecoder;
import com.sun.max.ve.net.*;

public class UDPEndpoint implements Endpoint, UDPUpcall {

    // maximum number of packets to buffer per endpoint.
    private static final int maxRecvQueueSize = 4;

    // port number that this endpoint receives data on.
    private int localPort;
    private int destPort;
    private int destAddr;

    // these are used to implement the packet receive buffer.
    // It is a linked list, new recv packets on tail, read from head.
    private Packet recvHead;
    private Packet recvTail;
    private int numPackets;
    int timeout;        // used for timing out reads and accept (in millisecs)
    private Object _lock = new Object();

    // NIO support for non-blocking I/O
    private boolean blocking = true;

    public UDPEndpoint() {
    }

    //----------------------------------------------------------------------

    // if the given port is 0, we choose an unused port.  Whichever
    // port we use, we always return it back.
    public synchronized int bind(int addr, int port, boolean reuse) throws SocketException {
        // addr is silently ignored for now.
        localPort = UDP.register(this, port, reuse);
        if (localPort == 0) {
            throw new BindException("port in use");
        }
        return localPort;
    }

    //----------------------------------------------------------------------

    public int connect(int addr, int port) throws SocketException {

        if (destAddr != 0) {
            throw new SocketException("socket already connected");
        }

        destPort = port;
        destAddr = addr;

        return bind(0, 0, false);
    }


    //----------------------------------------------------------------------

    public void listen(int count) throws SocketException {
        throw new SocketException("can't listen on dgram socket");
    }

    public Endpoint accept() throws SocketException {
        throw new SocketException("operation not supported");
    }

    //----------------------------------------------------------------------

    public void close(int how) {

        synchronized (_lock) {

            UDP.deregister(this, localPort);

            // Recycle all the Packets in the receive queue.
            while (recvHead != null) {
                Packet pkt = recvHead._next;
                recvHead = pkt;
            }
            recvTail = null;
        }
    }

    //----------------------------------------------------------------------

    // implements Endpoint.write interface
    public int write(byte b[], int off, int len)
        throws InterruptedIOException {

        return write(destAddr, destPort, b, off, len, 0);
    }

    public int write(int dst_ip, int dst_port, byte b[], int off,
                            int len, int ttl) throws InterruptedIOException {

        synchronized (_lock) {

            // Allocate an Packet large enough to store the user's
            // data plus all the network headers we need.
            Packet pkt = Packet.getTx(dst_ip, UDP.headerHint(), len);
            if (pkt != null) {

                // copy the user's data into the Packet
                pkt.putBytes(b, off, 0, len);

                UDP.output(pkt, localPort, dst_ip, dst_port, len, ttl);
            }
        }
        return len;
    }

    // ----------------------------------------------------------------------

    // helper method, must be called with ProtocolStack.lock held.
    private Packet readPacket() throws InterruptedException,
            InterruptedIOException {

        while (numPackets == 0) {
            _lock.wait(timeout);
            if (numPackets == 0) {
                throw new InterruptedIOException("read timeout");
            }
        }


        // get the next Packet from the receive queue
        Packet pkt = recvHead;
        recvHead = pkt._next;

        numPackets--;
        if (numPackets == 0) {
            recvHead = null;
            recvTail = null;
        }

        return pkt;
    }

    /**
     * Implements read interface of Endpoint.java
     */
    public int read(byte buf[], int off, int len)
        throws InterruptedIOException {

        return read(buf, off, len, null);
    }


    public int read(byte buf[], int off, int len, Source source)
            throws InterruptedIOException {
        Packet pkt = null;
        int n = 0;

        if (!blocking && available() == 0) {
            return ErrorDecoder.Code.EAGAIN.getCode();
        }

        synchronized (_lock) {
            try {
                pkt = readPacket();

                // only copy as much data as we have
                n = pkt.dataLength();
                if (n > len) {
                    n = len;
                }

                // copy the packet data to the user's buffer
                pkt.getBytes(0, buf, off, n);

                // return the source address and port
                if (source != null) {
                    source.addr = pkt.getSrcIP();
                    source.port = pkt.getSrcPort();
                }

            } catch (InterruptedException ex) {
                throw new InterruptedIOException(ex.getMessage());
            }
        }

        return n;
    }

    public int read(ByteBuffer bb)  throws IOException {
        // temporary limitation
        assert bb.hasArray();
        return read(bb.array(), bb.arrayOffset(), bb.limit() - bb.position());
    }

    public int write(ByteBuffer bb)  throws IOException {
        // temporary limitation
        assert bb.hasArray();
        final int len = bb.limit() - bb.position();
        return write(bb.array(), bb.arrayOffset(), len);
    }
    public static class Source {
        public int addr;
        public int port;
    }


    public int peek(int addr[]) throws InterruptedIOException {
        int src_port = 0;

        synchronized (_lock) {
            try {
                while (recvHead == null) {
                    _lock.wait(timeout);
                    if (recvHead == null) {
                        throw new InterruptedIOException("read timeout");
                    }
                }

                addr[0] = recvHead.getSrcIP();
                src_port = recvHead.getSrcPort();
            } catch (InterruptedException ex) {
                throw new InterruptedIOException(ex.getMessage());
            }
        }

        return src_port;
    }


    // ----------------------------------------------------------------------

    public int available() {
        int n = 0;

        synchronized (_lock) {

            if (recvHead != null) {
                n = recvHead.dataLength();
            }
        }

        return n;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void configureBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    //----------------------------------------------------------------------

    public int getRemoteAddress() {
        return destAddr;
    }

    public int getRemotePort() {
        return destPort;
    }

    // There are no actual limits beyond the maximum packet that can be
    // allocated from the heap (which is unknown).
    private static final int NOMINAL_BUFFER_SIZE = 256 * 1024;
    private int _recvBufferSize = NOMINAL_BUFFER_SIZE;
    private int _sendBufferSize = NOMINAL_BUFFER_SIZE;

    public int getRecvBufferSize() {
        return _recvBufferSize;
    }

    public int getSendBufferSize() {
        return _sendBufferSize;
    }

    public void setRecvBufferSize(int size) {
        _recvBufferSize = size;
    }

    public void setSendBufferSize(int size) {
        _sendBufferSize = size;
    }

    // ----------------------------------------------------------------------

    public int getLocalAddress() {
        // return localAddr;
        // TODO implement local adress
        return 0;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int poll(int eventOps, long timeout) {
        VEError.unimplemented("UDPEndpoint.poll");
        return 0;
    }


    // ----------------------------------------------------------------------

    /**
     * Passed packets from the Udp protocol.
     *
     * @param pkt
     *
     */
    public void input(Packet pkt) {
        // insert the Packet onto the receive packet list.
        // Throw it away if there's not enough room.
        if (numPackets >= maxRecvQueueSize) {
            return;
        }

        // Create ourselves a copy of the packet so we can stick it
        // in the receive queue.
        Packet p = pkt.copy();
        if (p == null) {
            return;
        }

        synchronized (_lock) {
            if (recvTail != null) {
                recvTail._next = p;
            } else {
                recvHead = p;
            }

            recvTail = p;
            p._next = null;

            numPackets++;

            _lock.notify();
        }
    }

}



