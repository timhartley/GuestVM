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
package com.sun.max.ve.fs.exec;

import com.sun.max.ve.fs.VirtualFileSystem;
import com.sun.max.ve.guk.GUKExec;

/**
 * This is not really a file system but it supports the ability to communicate
 * with the fork/exec backend using file descriptors.
 *
 * @author Mick Jordan
 *
 */

public class ExecFileSystem extends ExecHelperFileSystem implements VirtualFileSystem {

    protected static ExecFileSystem _singleton;

    public static ExecFileSystem create() {
        if (_singleton == null) {
            _singleton = new ExecFileSystem();
        }
        return (ExecFileSystem) _singleton;
    }

    @Override
    public int available(int fd, long fileOffset) {
        // TODO implement
        return 0;
    }


    @Override
    public int close0(int fd) {
        return GUKExec.close(fd);
    }

    @Override
    public int readBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        return GUKExec.readBytes(fd, bytes, offset, length, fileOffset);
    }

    @Override
    public int writeBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        return GUKExec.writeBytes(fd, bytes, offset, length, fileOffset);
    }
}
