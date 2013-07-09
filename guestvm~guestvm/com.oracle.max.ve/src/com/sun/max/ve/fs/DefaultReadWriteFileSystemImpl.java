/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ve.fs;

/**
 * Provides default implementations of read/write in terms of readBytes/writeBytes.
 *
 * @author Mick Jordan
 *
 */

public class DefaultReadWriteFileSystemImpl extends UnimplementedFileSystemImpl implements VirtualFileSystem {
    @Override
    public int read(int fd, long fileOffset) {
        final byte[] buf = new byte[1];
        final int result = readBytes(fd, buf, 0, 1, fileOffset);
        if (result == 0) {
            return -1;
        } else {
            return buf[0] & 0xFF;
        }
    }

    @Override
    public int write(int fd, int b, long fileOffset) {
        final byte[] buf = new byte[1];
        buf[0] = (byte) b;
        return writeBytes(fd, buf, 0, 1, fileOffset);
    }

}
