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
package com.sun.max.ve.guk;

import com.sun.max.unsafe.*;
import com.sun.max.memory.Memory;

/**
 * An interface to creating separate processes (guests) via dom0.
 *
 * @author Mick Jordan
 *
 */

public class GUKExec {

    public static int forkAndExec(byte[] prog, byte[] argBlock, int argc, byte[] dir) {
        final Pointer nativeProg = Memory.allocate(Size.fromInt(prog.length));
        Memory.writeBytes(prog, nativeProg);
        final Pointer nativeArgs = Memory.allocate(Size.fromInt(argBlock.length));
        Memory.writeBytes(argBlock, nativeArgs);
        Pointer nativeDir = Pointer.zero();
        if (dir != null) {
            nativeDir = Memory.allocate(Size.fromInt(dir.length));
            Memory.writeBytes(dir, nativeDir);
        }
        final int rc = GUK.guk_exec_create(nativeProg, nativeArgs, argc, nativeDir);
        Memory.deallocate(nativeProg);
        Memory.deallocate(nativeArgs);
        if (dir != null) {
            Memory.deallocate(nativeDir);
        }
        return rc;
    }

    public static int waitForProcessExit(int pid) {
        return GUK.guk_exec_wait(pid);
    }

    public static int readBytes(int pid, byte[] bytes, int offset, int length, long fileOffset) {
        final Pointer nativeBytes = Memory.allocate(Size.fromInt(bytes.length));
        final int result = GUK.guk_exec_read_bytes(pid, nativeBytes, length, fileOffset);
        if (result > 0) {
            Memory.readBytes(nativeBytes, result, bytes, offset);
        }
        Memory.deallocate(nativeBytes);
        return result == 0 ? -1 : result;
    }

    public static int writeBytes(int pid, byte[] bytes, int offset, int length, long fileOffset) {
        final Pointer nativeBytes = Memory.allocate(Size.fromInt(bytes.length));
        Memory.writeBytes(bytes, offset, length, nativeBytes);
        final int result = GUK.guk_exec_write_bytes(pid, nativeBytes, length, fileOffset);
        Memory.deallocate(nativeBytes);
        return result;
    }

    public static int close(int pid) {
        return GUK.guk_exec_close(pid);
    }

    public static int destroyProcess(int pid) {
        return GUK.guk_exec_destroy(pid);
    }
}
