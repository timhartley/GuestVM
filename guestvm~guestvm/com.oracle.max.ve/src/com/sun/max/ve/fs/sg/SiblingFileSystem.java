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
package com.sun.max.ve.fs.sg;

import java.io.*;
import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.memory.Memory;
import com.sun.max.util.Utf8Exception;
import com.sun.max.ve.error.VEError;
import com.sun.max.ve.fs.*;
import com.sun.max.ve.guk.x64.X64VM;
import com.sun.max.ve.jdk.JDK_java_io_UnixFileSystem;
import com.sun.max.vm.Intrinsics;

/**
 * This class represents the file system that is based on inter-domain communication to
 * a sibling guest operating system acting as a file server.
 *
 * @author Mick Jordan
 *
 */
public final class SiblingFileSystem extends UnimplementedFileSystemImpl implements VirtualFileSystem {

    private Word _handle;
    private String _exportPath;
    private String _mountPath;
    private int _mountPathLength;

    private SiblingFileSystem(Word handle, String exportPath, String mountPath) {
        _handle = handle;
        _exportPath = exportPath;
        _mountPath = mountPath;
        _mountPathLength = _mountPath.length();
    }

    /**
     * Create the file system identified by exportPath.
     *
     * @param exportPath
     * @param mountPath
     * @return
     */
    public static SiblingFileSystem create(String exportPath, String mountPath) {
        final int numImports = SiblingFileSystemNatives.getNumImports();
        for (int i = 0; i < numImports; i++) {
            final Word handle = SiblingFileSystemNatives.getImport(i);
            try {
                final String remotePath = CString.utf8ToJava(SiblingFileSystemNatives.getPath(handle));
                if (exportPath.equals(remotePath)) {
                    return new SiblingFileSystem(handle, exportPath, mountPath);
                }
            } catch (Utf8Exception ex) {
                VEError.unexpected("UTFException in SiblingFileSystem.create");
            }

        }
        return null;
    }

    /**
     * Maps the given path, that is based on the mount path to the server path.
     * @param path
     * @return
     */
    private Pointer remap(String path) {
        return CString.utf8FromJava(_exportPath + path.substring(_mountPathLength));
    }

    @Override
    public void close() {

    }

    @Override
    public String canonicalize0(String path) throws IOException {
        // TODO correct implementation
        return path;
    }

    @Override
    public boolean createDirectory(String path) {
        final Pointer p = remap(path);
        final int rc = SiblingFileSystemNatives.createDirectory(_handle, p);
        Memory.deallocate(p);
        return rc == 0;
    }

    @Override
    public boolean createFileExclusively(String path) throws IOException {
        final Pointer p = remap(path);
        final int rc = SiblingFileSystemNatives.createFileExclusively(_handle, p);
        Memory.deallocate(p);
        if (rc == 0) {
            return true;
        } else if (rc == -2) {
            return false;
        } else {
            throw new IOException("create of file " + path + " failed");
        }
    }

    @Override
    public boolean delete0(String path) {
        final Pointer p = remap(path);
        final int rc = SiblingFileSystemNatives.delete(_handle, p);
        Memory.deallocate(p);
        return rc == 0;
    }

    @Override
    public int getMode(String path) {
        final Pointer p = remap(path);
        final int mode = SiblingFileSystemNatives.getMode(_handle, p);
        Memory.deallocate(p);
        return mode;
    }

    @Override
    public long getLastModifiedTime(String path) {
        final Pointer p = remap(path);
        final long mTime = SiblingFileSystemNatives.getLastModifiedTime(_handle, p);
        Memory.deallocate(p);
        return mTime < 0 ? 0 : mTime * 1000;
    }

    @Override
    public long getLength(String path) {
        final Pointer p = remap(path);
        final long length = SiblingFileSystemNatives.getLength(_handle, p);
        Memory.deallocate(p);
        return length < 0 ? 0 : length;
    }

    @Override
    public String[] list(String path) {
        final Pointer dir = remap(path);
        final int[] nFiles = new int[1];
        final boolean[] hasMore = new boolean[1];
        final List<String> strings = new ArrayList<String>();
        int offset = 0;
        do {
            hasMore[0] = false; nFiles[0] = 0;
            final Pointer files = SiblingFileSystemNatives.list(_handle, dir, offset, nFiles, hasMore);
            if (files.isZero()) {
                return null;
            }
            for (int i = 0; i < nFiles[0]; i++) {
                final Pointer cString = files.getWord(i).asPointer();
                try {
                    final String name = CString.utf8ToJava(cString);
                    if (!JDK_java_io_UnixFileSystem.currentOrParent(name)) {
                        strings.add(name);
                    }
                    Memory.deallocate(cString);
                } catch (Utf8Exception ex) {
                    return null;
                }
            }
            offset += nFiles[0];
            Memory.deallocate(files);
        } while (hasMore[0]);
        Memory.deallocate(dir);
        return strings.toArray(new String[strings.size()]);
    }

    @Override
    public boolean rename0(String path1, String path2) {
        final Pointer p1 = remap(path1);
        final Pointer p2 = remap(path2);
        final int rc = SiblingFileSystemNatives.rename(_handle, p1, p2);
        Memory.deallocate(p1); Memory.deallocate(p2);
        return rc == 0;
    }

    // FileInputStream, FileOutputStream
    // Max transfer size is one page

    @Override
    public int read(int fd, long fileOffset) {
        return SiblingFileSystemNatives.read(_handle, fd, fileOffset);
    }

    @Override
    public int readBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        final Pointer nativeBytes = Intrinsics.stackAllocate(4096);
        int left = length;
        while (left > 0) {
            final int toDo = left > X64VM.PAGE_SIZE ? X64VM.PAGE_SIZE : left;
            final int result = SiblingFileSystemNatives.readBytes(_handle, fd, nativeBytes, toDo, fileOffset);
            if (result != toDo) {
                return -1;
            }
            Memory.readBytes(nativeBytes, toDo, bytes, offset);
            left -= toDo;
            offset += toDo;
            fileOffset += toDo;
        }
        return length;
    }

    @Override
    public int write(int fd, int b, long fileOffset) {
        return SiblingFileSystemNatives.write(_handle, fd, b, fileOffset);
    }
    
    @Override
    public int writeBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        final Pointer nativeBytes = Intrinsics.stackAllocate(4096);
        int left = length;
        while (left > 0) {
            final int toDo = left > X64VM.PAGE_SIZE ? X64VM.PAGE_SIZE : left;
            Memory.writeBytes(bytes, offset, toDo, nativeBytes);
            final int result = SiblingFileSystemNatives.writeBytes(_handle, fd, nativeBytes, toDo, fileOffset);
            if (result != toDo) {
                return -1;
            }
            left -= toDo;
            offset += toDo;
            fileOffset += toDo;
        }
        return length;
    }

    @Override
    public int open(String name, int flags) {
        final Pointer p = remap(name);
        final int fd = SiblingFileSystemNatives.open(_handle, p, flags);
        Memory.deallocate(p);
        return fd;
    }

    @Override
    public int close0(int fd) {
        return SiblingFileSystemNatives.close0(_handle, fd);
    }

    @Override
    public long getLength(int fd) {
        return SiblingFileSystemNatives.getLengthFd(_handle, fd);
    }

    @Override
    public int setLength(int fd, long length) {
        return (int) SiblingFileSystemNatives.setLengthFd(_handle, fd, length);
    }

    @Override
    public int available(int fd, long fileOffset) {
        return 0;
    }

}
