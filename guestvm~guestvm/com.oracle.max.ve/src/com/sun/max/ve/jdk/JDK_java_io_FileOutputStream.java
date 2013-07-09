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

import static com.sun.max.ve.fs.VirtualFileSystem.*;
import static com.sun.max.ve.jdk.AliasCast.*;

import java.io.*;


import com.sun.max.annotate.*;
import com.sun.max.ve.fs.*;

/**
 * This is a MaxVE specific substitution for the native methods in FileOutputStream.
 *
 * @author Mick Jordan
 */

@METHOD_SUBSTITUTIONS(FileOutputStream.class)
final class JDK_java_io_FileOutputStream {
    
    @ALIAS(declaringClass = FileOutputStream.class)
    FileDescriptor fd;

    private JDK_java_io_FileOutputStream() {
    }

    @INLINE
    private static FileDescriptor getFileDescriptor(Object obj) {
        JDK_java_io_FileOutputStream thisFileOutputStream = asJDK_java_io_FileOutputStream(obj);
        return thisFileOutputStream.fd;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void open(String name) throws FileNotFoundException {
        JavaIOUtil.open(getFileDescriptor(this), name, O_WRONLY | O_CREAT | O_TRUNC);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void openAppend(String name) throws FileNotFoundException {
        final int fd = JavaIOUtil.open(getFileDescriptor(this), name, O_WRONLY | O_CREAT | O_APPEND);
        final  VirtualFileSystem vfs = VirtualFileSystemId.getVfsUnchecked(fd);
        VirtualFileSystemOffset.set(fd, vfs.getLength(VirtualFileSystemId.getFd(fd)));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void close0() throws IOException {
        JavaIOUtil.close0(getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void write(int b) throws IOException {
        JavaIOUtil.write(b, getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void writeBytes(byte[] bytes, int offset, int length) throws IOException {
        JavaIOUtil.writeBytes(bytes, offset, length, getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private static void initIDs() {
    }

}
