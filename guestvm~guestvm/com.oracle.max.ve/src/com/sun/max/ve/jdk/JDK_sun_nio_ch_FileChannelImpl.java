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

import java.io.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.ve.error.*;
import com.sun.max.ve.fs.*;
import com.sun.max.ve.guk.*;
import com.sun.max.memory.Memory;
import com.sun.max.memory.VirtualMemory;

/**
 * Substitutions for  @see sun.nio.ch.FileChannelImpl.
 * @author Mick Jordan
 *
 */


@METHOD_SUBSTITUTIONS(sun.nio.ch.FileChannelImpl.class)
public class JDK_sun_nio_ch_FileChannelImpl {

    @ALIAS(declaringClass =sun.nio.ch.FileChannelImpl.class)
    FileDescriptor fd;
    

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private int lock0(FileDescriptor fdObj, boolean blocking, long pos, long size, boolean shared) throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
        return VirtualFileSystemId.getVfs(fd).lock0(fd, blocking, pos, size, shared);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void release0(FileDescriptor fdObj, long pos, long size) throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
        VirtualFileSystemId.getVfs(fd).release0(VirtualFileSystemId.getFd(fd), pos, size);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private long map0(int prot, long position, long length) throws IOException {
        /* Unfortunately the notion of a MappedByteBuffer that is is inherently "direct" is
         * embedded in the FileChannel API. So we have choice but to go with that.
         * A more efficient implementation would be required for large files
         * (which is what this is supposed to be used for actually).
         */
        assert length <= Integer.MAX_VALUE;
        final int len = (int) length;
        final int numPages = len / GUKPagePool.PAGE_SIZE + 1;
        final Pointer p = GUKPagePool.allocatePages(numPages, VirtualMemory.Type.DATA);
        final int fd = JDK_java_io_FileDescriptor.getFd(asJDK_sun_nio_ch_FileChannelImpl(this).fd);
        // this is not optimal, should work with a direct byte buffer, and find a way to give it to caller.
        final byte[] buf = new byte[(int) length];
        final int result = VirtualFileSystemId.getVfs(fd).readBytes(VirtualFileSystemId.getFd(fd), buf, 0, len, position);
        if (result < 0) {
            throw new IOException("Map failed: " + ErrorDecoder.getMessage(-result));
        }
        Memory.writeBytes(buf, 0, result, p);
        return p.toLong();
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private static int unmap0(long address, long length) {
        VEError.unimplemented("sun.nio.FileChannelImpl.unmap0");
        return -1;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private int force0(FileDescriptor fdObj, boolean metaData) throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
        return VirtualFileSystemId.getVfs(fd).force0(VirtualFileSystemId.getFd(fd), metaData);
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private int truncate0(FileDescriptor fdObj, long size) {
        final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
        try {
            VirtualFileSystemId.getVfs(fd).setLength(VirtualFileSystemId.getFd(fd), size);
            return 0;
        } catch (IOException ex) {
            return -1;
        }
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private long transferTo0(int src, long position, long count, int dst) {
        return -2;
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private long position0(FileDescriptor fdObj, long offset) {
        final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
        if (offset < 0) {
            return VirtualFileSystemOffset.get(fd);
        } else {
            VirtualFileSystemOffset.set(fd, offset);
            return offset;
        }
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private long size0(FileDescriptor fdObj) {
        final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
        try {
            return VirtualFileSystemId.getVfs(fd).getLength(VirtualFileSystemId.getFd(fd));
        } catch (IOException ex) {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private static long initIDs() {
        return 4096; // pagesize - should be acquired from GUK
    }

}
