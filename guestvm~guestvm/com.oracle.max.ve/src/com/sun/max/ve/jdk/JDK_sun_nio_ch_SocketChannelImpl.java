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
import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.ve.net.Endpoint;

/**
 * Substitutions for native methods in sun.nio.ch.SocketChannelImpl.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.nio.ch.SocketChannelImpl")
public class JDK_sun_nio_ch_SocketChannelImpl {
    @SUBSTITUTE
    private static int checkConnect(FileDescriptor fd, boolean block, boolean ready) throws IOException {
        VEError.unimplemented("sun.nio.ch.SocketChannelImpl.checkConnect");
        return 0;
    }


    @SUBSTITUTE
    private static void shutdown(FileDescriptor fdObj, int how) throws IOException {
        final Endpoint endpoint = JavaNetUtil.get(fdObj);
        endpoint.close(how);
    }


}
