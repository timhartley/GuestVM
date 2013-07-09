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
import java.nio.*;
import sun.nio.ch.*;

import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.ve.fs.*;
import com.sun.max.vm.runtime.*;

/**
 * Substitutions for  @see sun.nio.ch.FileDispatcher.
 * N.B. None of these methods should ever be called, except closeIntFD, as we
 * install a MaxVE specific dispatcher that works with ByteBuffers.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.nio.ch.FileDispatcher")
final class JDK_sun_nio_ch_FileDispatcher {

    @SUBSTITUTE
    private static int read0(FileDescriptor fdObj, long address, int length) throws IOException {
        VEError.unimplemented("sun.nio.ch.FileDispatcher.read0");
        return 0;
    }

    @SUBSTITUTE
    private static int pread0(FileDescriptor fd, long address, int len, long position) throws IOException {
        VEError.unimplemented("sun.nio.ch.FileDispatcher.pread0");
        return 0;
    }

    @SUBSTITUTE
    private static long readv0(FileDescriptor fd, long address, int len) throws IOException {
        VEError.unimplemented("sun.nio.ch.FileDispatcher.readv0");
        return 0;
    }

    @SUBSTITUTE
    private static int write0(FileDescriptor fdObj, long address, int length) throws IOException {
        VEError.unimplemented("sun.nio.ch.FileDispatcher.write0");
        return 0;
    }

    @SUBSTITUTE
    private static int pwrite0(FileDescriptor fd, long address, int len, long position) throws IOException {
        VEError.unimplemented("sun.nio.ch.FileDispatcher.pwrite0");
        return 0;
    }

    @SUBSTITUTE
    private static long writev0(FileDescriptor fd, long address, int len) throws IOException {
        VEError.unimplemented("sun.nio.ch.FileDispatcher.writev0");
        return 0;
    }

    @SUBSTITUTE
    private static void close0(FileDescriptor fd) throws IOException {
        JavaIOUtil.close0(fd);
    }

    @SUBSTITUTE
    private static void preClose0(FileDescriptor fd) throws IOException {
        // TODO the HotSpot native code does the "dup" thing, what is our equivalent?
    }

    @SUBSTITUTE
    private static void closeIntFD(int fd) throws IOException {
        JavaIOUtil.close0FD(fd);
    }

    @SUBSTITUTE
    private static void init() {
    }

}
