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
import java.util.*;

import com.sun.max.ve.jdk.JDK_java_io_UnixFileSystem;

/**
 * The file system exists to create the illusion of a true root for all the mount points.
 * It is read-only (obviously). There are no files just the directories implied by the mount points.
 * Note: This could be refactored to replace FSTable and do the delegation to the mounted
 * file systems directly.
 *
 * @author Mick Jordan
 *
 */

final class RootFileSystem extends UnimplementedFileSystemImpl implements VirtualFileSystem {

    private static RootFileSystem _singleton;
    private static Dir _root;

    private static class Dir {
        Map<String, Dir> _contents = new HashMap<String, Dir>();
        Dir(Dir parent) {
            _contents.put(".", this);
            _contents.put("..", parent == null ? this : parent);
        }

        void put(String key, Dir dir) {
            _contents.put(key, dir);
        }

        Dir get(String key) {
            return _contents.get(key);
        }
    }

    private RootFileSystem() {
        _root = new Dir(null);
    }

    static RootFileSystem create() {
        if (_singleton == null) {
            _singleton = new RootFileSystem();
        }
        return _singleton;
    }

    static void mount(FSTable.Info info) {
        //System.out.println("mounting " + info.mountPath());
        final String[] parts = info.mountPath().split("/");
        Dir dir = _root;
        for (int i = 1; i < parts.length; i++) {
            final String part = parts[i];
            Dir subdir = dir.get(part);
            if (subdir == null) {
                subdir = new Dir(dir);
                dir.put(part, subdir);
                dir = subdir;
            } else {
                dir = subdir;
            }
        }
        //System.out.println("Contents of root");
        //debugList(_root, 0);
    }

    private Dir matchPath(String name) {
        final String[] parts = name.split(File.separator);
        Dir d = _root;
        for (int i = 1; i < parts.length; i++) {
            final Dir dd = d.get(parts[i]);
            if (dd == null) {
                return null;
            }
            d = (Dir) dd;
        }
        return d;
    }

    @Override
    public int available(int fd, long fileOffset) {
        return 0;
    }

    @Override
    public String canonicalize0(String path) throws IOException {
        return path;
    }

    @Override
    public boolean checkAccess(String path, int access) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public int close0(int fd) {
        return 0;
    }

    @Override
    public boolean createDirectory(String path) {
        return false;
    }

    @Override
    public boolean createFileExclusively(String path) throws IOException {
        return false;
    }

    @Override
    public boolean delete0(String path) {
        return false;
    }

    @Override
    public long getLastModifiedTime(String path) {
        return 0;
    }

    @Override
    public long getLength(String path) {
        return 0;
    }

    @Override
    public long getLength(int fd) {
        return 0;
    }

    @Override
    public int getMode(String path) {
        int result;
        final Dir d = matchPath(path);
        if (d != null) {
            result = S_IFDIR + S_IEXEC + S_IRUSR;
        } else {
            result = -ErrorDecoder.Code.ENOENT.getCode();
        }
        return result;
    }

    @Override
    public long getSpace(String path, int t) {
        return 0;
    }

    @Override
    public String[] list(String path) {
        final Dir d = matchPath(path);
        if (d == null) {
            return null;
        } else {
            final String[] result = new String[d._contents.size() - 2];
            int k = 0;
            for (String name : d._contents.keySet()) {
                if (!JDK_java_io_UnixFileSystem.currentOrParent(name)) {
                    result[k++] = name;
                }
            }
            return result;
        }
    }

    @Override
    public int open(String name, int flags) {
        return 0;
    }

    @Override
    public int read(int fd, long fileOffset) {
        return 0;
    }

    @Override
    public int readBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        return 0;
    }

    @Override
    public boolean rename0(String path1, String path2) {
        return false;
    }

    @Override
    public boolean setLastModifiedTime(String path, long time) {
        return false;
    }

    @Override
    public int setLength(int fd, long length) {
        return 0;
    }

    @Override
    public int setMode(String path, int mode) {
        return 0;
    }

    @Override
    public long skip(int fd, long n, long fileOffset) {
        return 0;
    }

    @Override
    public long uniqueId(int fd) {
        return 0;
    }

    @Override
    public int write(int fd, int b, long fileOffset) {
        return 0;
    }

    @Override
    public int writeBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        return 0;
    }

/*
    private static void debugList(Dir d, int indent) {
        for (String key : d._contents.keySet()) {
            if (!JDK_java_io_UnixFileSystem.currentOrParent(key)) {
                for (int j = 0; j < indent; j++) {
                    System.out.print(" ");
                }
                System.out.println(key);
                debugList(d._contents.get(key), indent + 2);
            }
        }
    }
*/

}
