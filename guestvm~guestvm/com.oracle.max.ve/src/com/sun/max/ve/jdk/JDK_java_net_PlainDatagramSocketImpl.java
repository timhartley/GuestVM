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
package com.sun.max.ve.jdk;

import static com.sun.max.ve.jdk.AliasCast.*;
import static java.net.SocketOptions.*;

import java.io.*;
import java.net.*;
import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.ve.logging.*;
import com.sun.max.ve.net.ip.IPAddress;
import com.sun.max.ve.net.udp.*;

/**
 * This class implements the native methods in @see java.net.PlainDatagramSocketImpl in terms
 * of com.sun.max.ve.net classes. PlainDatagramSocketImpl assumes the use of file descriptors
 * with a socket represented with an int as per standard Unix. We have to emulate that even though
 * we represent sockets using UDPEndpoint objects.
 *
 * @author Mick Jordan
 *
 */

@METHOD_SUBSTITUTIONS(className = "java.net.PlainDatagramSocketImpl")
final class JDK_java_net_PlainDatagramSocketImpl {

    private static Logger _logger;
    
    @ALIAS(declaringClassName = "java.net.DatagramSocketImpl")
    private FileDescriptor fd;
    @ALIAS(declaringClassName = "java.net.PlainDatagramSocketImpl")
    private boolean connected;
    @ALIAS(declaringClassName = "java.net.PlainDatagramSocketImpl")
    private int timeout;

    @INLINE
    private static FileDescriptor getFileDescriptor(Object obj) {
        JDK_java_net_PlainDatagramSocketImpl thisPlainDatagramSocketImpl =  asJDK_java_net_PlainDatagramSocketImpl(obj);
        return thisPlainDatagramSocketImpl.fd;
        
    }
    
    private static FileDescriptor checkOpen(Object obj) throws SocketException {
        final FileDescriptor fd = getFileDescriptor(obj);
        if (fd == null) {
            throw new SocketException("socket closed");
        }
        return fd;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private synchronized void bind0(int lport, InetAddress laddr) throws SocketException {
        final FileDescriptor fdObj = checkOpen(this);
        final UDPEndpoint endpoint = JavaNetUtil.getU(JDK_java_io_FileDescriptor.getFd(fdObj));
        // TODO fix bind's first argument
        endpoint.bind(0, lport, false);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void send(DatagramPacket p) throws IOException {
        final FileDescriptor fdObj = checkOpen(this);
        final UDPEndpoint endpoint = JavaNetUtil.getU(JDK_java_io_FileDescriptor.getFd(fdObj));
        final byte[] buf = p.getData();
        final int off = p.getOffset();
        final int len = p.getLength();
        int port;
        int addr;
        JDK_java_net_PlainDatagramSocketImpl thisPlainDatagramSocketImpl =  asJDK_java_net_PlainDatagramSocketImpl(this);
        if (thisPlainDatagramSocketImpl.connected) {
            port = endpoint.getRemotePort();
            addr = endpoint.getRemoteAddress();
        } else {
            port = p.getPort();
            addr = IPAddress.byteToInt(p.getAddress().getAddress());
        }
        endpoint.write(addr, port, buf, off, len, 0);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private synchronized int peek(InetAddress i) throws IOException {
        VEError.unimplemented("PlainDatagramSocket.peek");
        return 0;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private synchronized int peekData(DatagramPacket p) throws IOException {
        VEError.unimplemented("PlainDatagramSocket.peekData");
        return 0;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private synchronized void receive0(DatagramPacket p) throws IOException {
        final FileDescriptor fdObj = checkOpen(this);
        final UDPEndpoint endpoint = JavaNetUtil.getU(JDK_java_io_FileDescriptor.getFd(fdObj));
        final byte[] buf = p.getData();
        final int off = p.getOffset();
        final int len = p.getLength();
        final int timeout =   asJDK_java_net_PlainDatagramSocketImpl(this).timeout;
        if (timeout > 0) {
            endpoint.setTimeout(timeout);
        }
        final UDPEndpoint.Source source = new UDPEndpoint.Source();
        final int n = endpoint.read(buf, off, len, source);
        InetAddress inetAddress = p.getAddress();
        if (inetAddress != null) {
            if (source.addr != IPAddress.byteToInt(inetAddress.getAddress())) {
                inetAddress = null;
            }
        }
        if (inetAddress == null) {
            p.setAddress(JDK_java_net_Inet4AddressImpl.createInet4Address(null, source.addr));
        }
        p.setPort(source.port);
        p.setLength(n);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void setTimeToLive(int ttl) throws IOException {
        VEError.unimplemented("PlainDatagramSocket.setTimeToLive");
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private int getTimeToLive() throws IOException {
        VEError.unimplemented("PlainDatagramSocket.getTimeToLive");
        return 0;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void setTTL(byte ttl) throws IOException {
        VEError.unimplemented("PlainDatagramSocket.setTTL");
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private byte getTTL() throws IOException {
        VEError.unimplemented("PlainDatagramSocket.getTTL");
        return 0;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void join(InetAddress inetaddr, NetworkInterface netIf) throws IOException {
        VEError.unimplemented("PlainDatagramSocket.join");
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void leave(InetAddress inetaddr, NetworkInterface netIf) throws IOException {
        VEError.unimplemented("PlainDatagramSocket.leave");
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void datagramSocketCreate() throws SocketException {
        final FileDescriptor fdObj = checkOpen(this);
        final int fd = JavaNetUtil.getFreeIndex(new UDPEndpoint());
        JDK_java_io_FileDescriptor.setFd(fdObj, fd);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void datagramSocketClose() {
        final FileDescriptor fdObj = getFileDescriptor(this);
        if (fdObj != null) {
            final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
            if (fd != -1) {
                JavaNetUtil.setNull(fd);
                JDK_java_io_FileDescriptor.setFd(fdObj, -1);
            }
        }
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void socketSetOption(int opt, Object val) throws SocketException {
        final FileDescriptor fdObj = checkOpen(this);
        final UDPEndpoint endpoint = JavaNetUtil.getU(JDK_java_io_FileDescriptor.getFd(fdObj));
        final int size = (Integer) val;
        switch (opt) {
            case SO_RCVBUF:
                endpoint.setRecvBufferSize(size);
                break;
            case SO_SNDBUF:
                endpoint.setSendBufferSize(size);
                break;
            default:
                _logger.warning("PlainDatagramSocket.socketGetOption " + Integer.toHexString(opt) + " not implemented");
        }
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private Object socketGetOption(int opt) throws SocketException {
        final FileDescriptor fdObj = checkOpen(this);
        final UDPEndpoint endpoint = JavaNetUtil.getU(JDK_java_io_FileDescriptor.getFd(fdObj));
        switch (opt) {
            case SO_RCVBUF:
                return endpoint.getRecvBufferSize();
            case SO_SNDBUF:
                return endpoint.getSendBufferSize();
            default:
                _logger.warning("PlainDatagramSocket.socketGetOption " + Integer.toHexString(opt) + " not implemented");
                return null;
        }
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void connect0(InetAddress address, int port) throws SocketException {
        final FileDescriptor fdObj = checkOpen(this);
        final UDPEndpoint endpoint = JavaNetUtil.getU(JDK_java_io_FileDescriptor.getFd(fdObj));
        endpoint.connect(IPAddress.byteToInt(address.getAddress()), port);
        asJDK_java_net_PlainDatagramSocketImpl(this).connected = true;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void disconnect0(int family) {
        VEError.unimplemented("PlainDatagramSocket.disconnect0");
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private static void init() {
        _logger = Logger.getLogger("java.net.PlainDatagramSocketImpl");
    }

}
