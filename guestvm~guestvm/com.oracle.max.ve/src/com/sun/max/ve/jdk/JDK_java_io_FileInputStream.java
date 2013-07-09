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
 * This is a MaxVE specific substitution for the native methods in FileInputStream.
 *
 * @author Mick Jordan
 */

@METHOD_SUBSTITUTIONS(FileInputStream.class)
final class JDK_java_io_FileInputStream {
    
    @ALIAS(declaringClass = FileInputStream.class)
    FileDescriptor fd;

    private JDK_java_io_FileInputStream() {
    }
    
    @INLINE
    private static FileDescriptor getFileDescriptor(Object obj) {
        JDK_java_io_FileInputStream thisFileInputStream = asJDK_java_io_FileInputStream(obj);
        return thisFileInputStream.fd;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void open(String name) throws FileNotFoundException {
        JavaIOUtil.open(getFileDescriptor(this), name, O_RDONLY);
    }

    @SUBSTITUTE
    int read() throws IOException {
        return JavaIOUtil.read(getFileDescriptor(this));
    }

    @SUBSTITUTE
    int readBytes(byte[] bytes, int offset, int length) throws IOException {
        return JavaIOUtil.readBytes(bytes, offset, length, getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private long skip(long n) throws IOException {
        if (n < 0) {
            throw new IOException("skip with negative argument: " + n);
        }
        final int fd = JDK_java_io_FileDescriptor.getFd(getFileDescriptor(this));
        final long result = VirtualFileSystemId.getVfs(fd).skip(VirtualFileSystemId.getFd(fd), n, VirtualFileSystemOffset.get(fd));
        if (result < 0) {
            throw new IOException("error in skip: " + ErrorDecoder.getMessage((int) -result));
        } else {
            VirtualFileSystemOffset.add(fd, result);
            return result;
        }
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private int available() throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(getFileDescriptor(this));
        final int result = VirtualFileSystemId.getVfs(fd).available(VirtualFileSystemId.getFd(fd), VirtualFileSystemOffset.get(fd));
        if (result < 0) {
            throw new IOException("error in available: " + ErrorDecoder.getMessage(-result));
        } else {
            return result;
        }
    }

    @SUBSTITUTE
    void close0() throws IOException {
        JavaIOUtil.close0(getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private static void initIDs() {

    }


}
