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
 * A filter for bash executions. (Mostly for Hadoop).
 *
 * @author Mick Jordan
 *
 */
public class BashProcessFilter extends VEProcessFilter {

    public BashProcessFilter() {
        super("bash");
    }

    @Override
    public int exec(byte[] prog, byte[] argBlock, int argc, byte[] envBlock, int envc, byte[] dir) {
        final String[] args = cmdArgs(argBlock);
        int result = -1;
        assert args[0].equals("-c");
        if (args[1].equals("groups")) {
            result = nextId();
            final String groups = System.getProperty("max.ve.groups");
            if (groups != null) {
                setData(result, StdIO.OUT, groups.getBytes());
            }
        } else if (args[1].startsWith("exec")) {
            final String[] execArgs = args[1].split(" ");
            if (stripQuotes(execArgs[1]).equals("df")) {
                final String arg2 = stripQuotes(execArgs[2]);
                String path;
                final boolean onek = true;
                if (arg2.equals("-k")) {
                    path = stripQuotes(execArgs[3]);
                } else {
                    path = arg2;
                }
                final VirtualFileSystem vfs = FSTable.exports(path);
                if (vfs != null) {
                    long used = vfs.getSpace(path, VirtualFileSystem.SPACE_USED);
                    long total = vfs.getSpace(path, VirtualFileSystem.SPACE_TOTAL);
                    long avail = vfs.getSpace(path, VirtualFileSystem.SPACE_USABLE);
                    if (onek) {
                        used = used / 1024;
                        total = total / 1024;
                        avail = avail / 1024;
                    }
                    final StringBuilder data = new StringBuilder("Filesystem           1K-blocks      Used Available Use% Mounted on\n");
                    data.append(path); data.append(' ');
                    data.append(total); data.append(' ');
                    data.append(used); data.append(' ');
                    data.append(avail); data.append(' ');
                    data.append(used / total * 100); data.append(' ');
                    data.append(FSTable.getInfo(vfs).mountPath()); data.append('\n');
                    result = nextId();
                    setData(result, StdIO.OUT, data.toString().getBytes());
                }

            }
        }
        return result;
    }

    private static String stripQuotes(String s) {
        final int length = s.length();
        if (s.charAt(0) == '\'' && s.charAt(length - 1) == '\'') {
            return s.substring(1, length - 1);
        }
        return s;
    }

}
