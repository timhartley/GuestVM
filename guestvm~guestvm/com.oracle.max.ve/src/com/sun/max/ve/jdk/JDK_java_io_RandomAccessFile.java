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
import com.sun.max.ve.error.*;
import com.sun.max.ve.fs.*;


/**
 * Substitutions for @see java.io.RandomAccessFile.
 * @author Mick Jordan
 */

@METHOD_SUBSTITUTIONS(RandomAccessFile.class)

public class JDK_java_io_RandomAccessFile {

    // Copied from RandomAccessFile.java (O -> RA because RDONLY differs from standard Unix value (in VirtualFileSystem)
    private static final int RA_RDONLY = 1;
    private static final int RA_RDWR =   2;
    private static final int RA_SYNC =   4;
    private static final int RA_DSYNC =  8;
    
    @ALIAS(declaringClass = RandomAccessFile.class)
    FileDescriptor fd;

    @INLINE
    private static FileDescriptor getFileDescriptor(Object obj) {
        JDK_java_io_RandomAccessFile thisRandomAccessFile = asJDK_java_io_RandomAccessFile(obj);
        return thisRandomAccessFile.fd;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void open(String name, int mode) throws FileNotFoundException {
        int uMode = 0;
        if ((mode & RA_RDONLY) != 0) {
            uMode = O_RDONLY;
        } else if ((mode & RA_RDWR) != 0) {
            uMode = O_RDWR | O_CREAT;
            if ((mode & RA_SYNC) != 0) {
                uMode = O_RDWR | O_CREAT;
                //VEError.unimplemented("RandomAccessFile SYNC mode");
            } else if ((mode & RA_DSYNC) != 0) {
                uMode = O_RDWR | O_CREAT;
                //VEError.unimplemented("RandomAccessFile DSYNC mode");
            }
        } else {
            VEError.unexpected("RandomAccessFile.open mode: " + mode);
        }
        JavaIOUtil.open(getFileDescriptor(this), name, uMode);

    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private int read() throws IOException {
        return JavaIOUtil.read(getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private int readBytes(byte[] b, int offset, int length) throws IOException {
        return JavaIOUtil.readBytes(b, offset, length, getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void write(int b) throws IOException {
        JavaIOUtil.write(b, getFileDescriptor(this));
    }

    @SUBSTITUTE
    private void writeBytes(byte[] bytes, int offset, int length) throws IOException {
        JavaIOUtil.writeBytes(bytes, offset, length, getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private long getFilePointer() throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(getFileDescriptor(this));
        final long fileOffset = VirtualFileSystemOffset.get(fd);
        return fileOffset;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void seek(long pos) throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(getFileDescriptor(this));
        VirtualFileSystemOffset.set(fd, pos);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private long length() throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(getFileDescriptor(this));
        return VirtualFileSystemId.getVfs(fd).getLength(VirtualFileSystemId.getFd(fd));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void setLength(long newLength) throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(getFileDescriptor(this));
        VirtualFileSystemId.getVfs(fd).setLength(VirtualFileSystemId.getFd(fd), newLength);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void close0() throws IOException {
        JavaIOUtil.close0(getFileDescriptor(this));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private static void initIDs() {

    }

}
