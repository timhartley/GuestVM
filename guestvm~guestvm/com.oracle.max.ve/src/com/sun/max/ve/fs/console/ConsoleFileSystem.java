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
package com.sun.max.ve.fs.console;

import com.sun.max.unsafe.*;
import com.sun.max.ve.fs.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.memory.Memory;

/**
 * This is not really a file system, it just supports the standard file descriptors
 * that want to read/write from the console.
 *
 * @author Mick Jordan
 *
 */
public class ConsoleFileSystem extends UnimplementedFileSystemImpl implements VirtualFileSystem {

    @Override
    public void close() {

    }

    @Override
    public int write(int fd, int b, long fileOffset) {
        return nativeWrite(b);
    }

    /*
     * Hotspot native code avoids allocation for smallish buffers by
     * copying to an on-stack array. Since we can't do that (yet)
     * in Java, we rely on boot heap objects not being GC'ed
     * and synchronize console output
     */

    private static final byte[] writeBuffer = new byte[1024];
    private static final byte[] readBuffer = new byte[1024];
    /**
     * The offset of the byte array data from the byte array object's origin.
     */
    private static final Offset _dataOffset = VMConfiguration.vmConfig().layoutScheme().byteArrayLayout.getElementOffsetFromOrigin(0);

    /**
     *
     */
    @Override
    public synchronized int writeBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        final Pointer nativeBytes = Reference.fromJava(writeBuffer).toOrigin().plus(_dataOffset);
        int result = 0;
        int left = length;
        int newOffset = offset;
        while (left > 0) {
            final int toWrite = left > writeBuffer.length ? writeBuffer.length : left;
            Memory.writeBytes(bytes, newOffset, toWrite, nativeBytes);
            result += nativeWriteBytes(nativeBytes, toWrite);
            left -= toWrite;
            newOffset += toWrite;
        }
        return result;
    }

    @Override
    public int read(int fd, long fileOffset) {
        return nativeRead();
    }

    @Override
    public synchronized int readBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        final Pointer nativeBytes = Reference.fromJava(readBuffer).toOrigin().plus(_dataOffset);
        assert length >= readBuffer.length;
        final int n = nativeReadBytes(nativeBytes, readBuffer.length);
        Memory.readBytes(nativeBytes, n, bytes, offset);
        return n;
    }

    @Override
    public int available(int fd, long fileOffset) {
        return 0;
    }

    @Override
    public long skip(int fd, long n, long fileOffset) {
        return 0;
    }

    @Override
    public long uniqueId(int fd) {
        return fd;
    }

    private static native int nativeWriteBytes(Pointer p, int length);
    private static native int nativeWrite(int b);
    private static native int nativeReadBytes(Pointer p, int length);
    private static native int nativeRead();

}
