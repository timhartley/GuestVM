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
package com.sun.max.ve.fs;

import java.io.*;
import com.sun.max.annotate.*;

/**
 * Provides a way to map from VirtualFileSystem instances to small integers. This is because
 * the upper layers, e.g. FileInputStream traffic in file descriptors, aka small integers, and there
 * needs to be a way to get from file descriptors to the corresponding VirtualFileSystem instance.
 * This avoids having to ensure that file descriptors returned by VirtualFileSystem instances are
 * globally unique, since this can be achieved by merging a file descriptor and a VirtualFileSystem
 * instance id.
 *
 * The file descriptors 0, 1 and 2 are predefined globally; therefore the first entry in the table
 * must be the file system that supports these standard descriptors.
 *
 * @author Mick Jordan
 *
 */

public final class VirtualFileSystemId {

    private static VirtualFileSystem[] _fsTable = new VirtualFileSystem[16];
    private static int _nextFreeIndex = 0;

    private static int getVfsId(VirtualFileSystem fs) {
        for (int i = 0; i < _nextFreeIndex; i++) {
            if (_fsTable[i] == fs) {
                return i;
            }
        }
        _fsTable[_nextFreeIndex++] = fs;
        return _nextFreeIndex - 1;
    }

    public static int getUniqueFd(VirtualFileSystem fs, int fd) {
        return (getVfsId(fs) << 16) | fd;
    }

    public static VirtualFileSystem getVfs(int uniqueFd)  throws IOException {
        if (uniqueFd < 0) {
            throw new IOException(ErrorDecoder.Code.EBADF.getMessage());
        }
        return _fsTable[uniqueFd >> 16];
    }

    @INLINE
    public static VirtualFileSystem getVfsUnchecked(int uniqueFd) {
        return _fsTable[uniqueFd >> 16];
    }

    @INLINE
    public static int getFd(int uniqueFd) {
        return uniqueFd & 0xFFFF;
    }

    @INLINE
    public static int getVfsId(int uniqueFd) {
        return uniqueFd >> 16;
    }

    private VirtualFileSystemId() {
    }


}
