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
package com.sun.max.ve.process;

import com.sun.max.ve.fs.*;

/**
 * A filter for for chmod for internal file systems.
 *
 * @author Mick Jordan
 *
 */

public class ChmodProcessFilter extends VEProcessFilter {

    public ChmodProcessFilter() {
        super("chmod");
    }

    @Override
    public int exec(byte[] prog, byte[] argBlock, int argc, byte[] envBlock, int envc, byte[] dir) {
        assert argc == 2;
        final String[] args = cmdArgs(argBlock);
        /* this is a very partial implementation, just support "chmod octal-mode file" */
        final int mode = Integer.parseInt(args[0], 8);
        final String path = args[1];
        final VirtualFileSystem vfs = FSTable.exports(path);
        if (vfs == null) {
            return -1;
        } else {
            vfs.setMode(path, mode);
        }
        return nextId();
    }

}
