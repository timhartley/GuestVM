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
// TCPEndpoint.java
//
// This class implements the glue between the JDK Socket API and
// the TCP protocol state machine code.
//
// sritchie -- Nov 95
//
//
// notes
//

import java.net.*;
import java.io.*;
import java.nio.*;

import com.sun.max.ve.fs.VirtualFileSystem;
import com.sun.max.ve.net.*;
import com.sun.max.ve.util.*;


public class TCPEndpoint implements Endpoint {

    TCP tcp;
    int timeout;        // used for timing out reads and accept (in millisecs)

    public TCPEndpoint() throws IOException {
        tcp = TCP.get();

        if (tcp == null) {
            throw new SocketException("no more TCP sockets");
        }
    }

    TCPEndpoint(TCP t) {
        tcp = t;
    }

    // Bind a port number to this unused local endpoint. An endpoint
    // can't be bound twice. Passing in 0 chooses the next available port.
    // The addr argument is currently ignored, the local address is always
    // used.
    // The reuse argument is ignored. No matter what we do not allow
    // binding to the same port.
    // The bound port number is returned.
    public int bind(int addr, int port, boolean reuse) throws IOException {
        return tcp.setLocalPort(port);
    }

    public void listen(int count) throws IOException {
        boolean r = false;

        r = tcp.listen(count);
        if (r != true) {
            throw new SocketException(" can't listen()");
        }
    }


    // Wait for a new connection to arrive on this Endpoint.
    public Endpoint accept() throws IOException{

        TCP t = null;
        try {
            t = tcp.accept(timeout);
            if (t == null) {
                return null;
            }
        } catch (InterruptedException ex) {
            throw new InterruptedIOException(ex.getMessage());
        }
        TCPEndpoint endp = new TCPEndpoint(t);

        return (Endpoint) endp;
    }

    public int connect(int addr, int p) throws IOException {
        try {
            int result = tcp.connect(addr, p);
            if (result == TCP.CONN_FAIL_REFUSED) {
                throw new ConnectException("Connection refused");
            }
            if (result == TCP.CONN_FAIL_TIMEOUT) {
                throw new NoRouteToHostException("Connect timed out");
            }
            return result;
        } catch (InterruptedException ex) {
            throw new InterruptedIOException(ex.getMessage());
        } catch (NetworkException e) {
            throw new SocketException(e.getMessage());
        }
    }

    public void close(int how) throws IOException {
        try {
            if (tcp != null) {
                tcp.close(how);
            }
        } catch (NetworkException e) {
            throw new SocketException(e.getMessage());
        } finally {
            tcp = null;
        }
    }

    public int write(byte buf[], int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        try {
            return tcp.write(buf, off, len);
        } catch (InterruptedException ex) {
            throw new InterruptedIOException(ex.getMessage());
        } catch (NetworkException e) {
            throw new SocketException(e.getMessage());
        }
    }

    public int read(byte buf[], int off, int len) throws IOException {
        try {
            return tcp.read(buf, off, len, timeout);
        } catch (InterruptedException ex) {
            throw new InterruptedIOException(ex.getMessage());
        } catch (NetworkException e) {
            throw new SocketException(e.getMessage());
        }
    }

    public int read(ByteBuffer bb)  throws IOException {
        final int len = bb.limit() - bb.position();
        if (bb.hasArray()) {
            return read(bb.array(), bb.arrayOffset(), bb.limit() - bb.position());
        } else {
            // TODO make a TCP.read(ByteBuffer) method
            byte[] buf = new byte[len];
            final int ppos = bb.position();
            final int result = read(buf, 0, len);
            bb.put(buf);
            bb.position(ppos);
            return result;
        }
    }

    public int write(ByteBuffer bb)  throws IOException {
        final int len = bb.limit() - bb.position();
        if (bb.hasArray()) {
            return write(bb.array(), bb.arrayOffset(), len);
        } else {
            // TODO make a TCP.write(ByteBuffer) method
            byte[] buf = new byte[len];
            final int ppos = bb.position();
            bb.get(buf);
            bb.position(ppos);
            return write(buf, 0, len);
        }
    }

    public int available() {
        int n = 0;
        n = tcp.available();
        return n;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRemoteAddress() {
        return tcp.getRemoteAddress();
    }

    public int getRemotePort() {
        return tcp.getRemotePort();
    }

    public int getLocalAddress() {
        // return tcp._localIp;
        // TODO implement setting of local address
        return 0;
    }

    public int getLocalPort() {
        return tcp.getLocalPort();
    }

    public int getRecvBufferSize() {
        return 0;
    }

    public int getSendBufferSize() {
        return 0;
    }

    public void setRecvBufferSize(int size) {

    }

    public void setSendBufferSize(int size) {

    }

    public void setNoDelay() {
        tcp.setNoDelay();
    }

    public void configureBlocking(boolean blocking) {
        tcp.configureBlocking(blocking);
    }

    public int poll(int eventOps, long timeout) {
        final boolean input = eventOps == VirtualFileSystem.POLLIN;
        if (input) {
            // tcp.pollInput handles the listen state and the established state
            if (tcp.pollInput()) {
                return VirtualFileSystem.POLLIN;
            }
        } else {
            if (tcp.pollOutput()) {
                return VirtualFileSystem.POLLOUT;
            }
        }
        if (timeout == 0) {
            return 0;
        }
        synchronized (tcp) {
            final TimeLimitedProc timedProc = new TimeLimitedProc() {

                @Override
                protected int proc(long remaining) throws InterruptedException {
                    tcp.wait(remaining);
                    if (input) {
                        if (tcp.pollInput()) {
                            return terminate(VirtualFileSystem.POLLIN);
                        }
                    } else {
                        if (tcp.pollOutput()) {
                            return terminate(VirtualFileSystem.POLLOUT);
                        }
                    }
                    return 0;
                }
            };
            return timedProc.run(timeout);
        }
    }
}

