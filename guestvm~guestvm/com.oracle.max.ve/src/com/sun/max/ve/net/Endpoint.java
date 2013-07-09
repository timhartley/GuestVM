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
package com.sun.max.ve.net;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Endpoint.java
 *
 * This interface describes the methods common to TCP and UDP protocol
 * socket endpoints.  It is implemented by the protocol-specific
 * layers to provide the implementation (TcpEndpoint and UdpEndpoint).
 *
 */
public interface Endpoint {

    int connect(int addr, int port) throws IOException;

    int bind(int addr, int port, boolean reuse) throws IOException;

    void listen(int count) throws IOException;

    Endpoint accept() throws IOException;

    int SHUT_RD = 0;
    int SHUT_WR = 1;
    int SHUT_RDWR = 2;

    void close(int how) throws IOException;

    int getRemoteAddress();

    int getRemotePort();

    int getLocalAddress();

    int getLocalPort();

    int getRecvBufferSize();

    int getSendBufferSize();

    void setRecvBufferSize(int size);

    void setSendBufferSize(int size);

    int write(byte[] b, int off, int len) throws IOException;

    int read(byte[] b, int off, int len) throws IOException;

    int write(ByteBuffer bb) throws IOException;

    int read(ByteBuffer bb) throws IOException;

    int available() throws IOException;

    void setTimeout(int timeout);

    void configureBlocking(boolean blocking);

    int poll(int eventOps, long timeout);
}
