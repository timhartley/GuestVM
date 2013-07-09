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

/**
 * Substitutions for native methods in sun.nio.ch.DatagramChannelImpl.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.nio.ch.DatagramChannelImpl")
public class JDK_sun_nio_ch_DatagramChannelImpl {
    @SUBSTITUTE
    private static void initIDs() {
    }

    @SUBSTITUTE
    private static void disconnect0(FileDescriptor fd) throws IOException {
        VEError.unimplemented("sun.nio.ch.Net.disconnect0");
    }

    @SUBSTITUTE
    private int receive0(FileDescriptor fd, long address, int len, boolean connected) throws IOException {
        VEError.unimplemented("sun.nio.ch.Net.receive0");
        return 0;
    }

    @SUBSTITUTE
    private int send0(FileDescriptor fd, long address, int len, SocketAddress sa) throws IOException {
        VEError.unimplemented("sun.nio.ch.Net.send0");
        return 0;
    }


}
