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

import java.io.*;
import java.net.*;
import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.ve.logging.*;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.ip.IPAddress;
import com.sun.max.ve.net.tcp.*;
import com.sun.max.ve.net.udp.*;

/**
 * Substitutions for native methods in sun.nio.ch.Net.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.nio.ch.Net")
final  class JDK_sun_nio_ch_Net {

    private static final int SOCK_STREAM = 2;
    private static final int SOCK_DGRAM = 1;

    @SUBSTITUTE
    private static int socket0(boolean stream, boolean reuse) throws IOException {
        int result = -1;
        // TODO reuse
        if (stream) {
            result = JavaNetUtil.getFreeIndex(new TCPEndpoint());
        } else {
            result = JavaNetUtil.getFreeIndex(new UDPEndpoint());
        }
        return result;
    }

    @SUBSTITUTE
    private static void bind(FileDescriptor fd, InetAddress addr, int port) throws IOException {
        final Endpoint endpoint = JavaNetUtil.get(fd);
        endpoint.bind(IPAddress.byteToInt(addr.getAddress()), port, false);
    }

    @SUBSTITUTE
    private static int connect(FileDescriptor fd, InetAddress remote, int remotePort, int trafficClass) throws IOException {
        final Endpoint endpoint = JavaNetUtil.get(fd);
        endpoint.connect(IPAddress.byteToInt(remote.getAddress()), remotePort);
        return 1;
    }

    @SUBSTITUTE
    private static int localPort(FileDescriptor fd) {
        final Endpoint endpoint = JavaNetUtil.get(fd);
        return endpoint.getLocalPort();
    }

    @SUBSTITUTE
    private static InetAddress localInetAddress(FileDescriptor fd) {
        final Endpoint endpoint = JavaNetUtil.get(fd);
        final int ra = endpoint.getLocalAddress();
        assert ra == 0;
        return JDK_java_net_Inet4AddressImpl.createInet4Address("0.0.0.0", ra);
    }

    @SUBSTITUTE
    private static int getIntOption0(FileDescriptor fd, int opt) {
        VEError.unimplemented("sun.nio.net.getIntOption0");
        return 0;
    }

    @SUBSTITUTE
    private static void setIntOption0(FileDescriptor fd, int opt, int arg) throws IOException {
        Logger.getLogger("sun.nio.ch.Net").warning("option: " + opt + " not implemented");
    }

    @SUBSTITUTE
    private static void initIDs() {
    }


}
