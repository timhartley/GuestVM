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
import java.nio.*;

/**
 * Of course we have to have a virtual file system interface.
 * These are almost exactly the native methods substituted by UnixFileSystem,
 * FileInputStream/FileOutputStream, RandonAccessFile and the nio classes.
 *
 * @author Mick Jordan
 *
 */
public interface VirtualFileSystem {

    // Unix open flags
    int O_RDONLY = 0;
    int O_WRONLY = 1;
    int O_RDWR = 2;
    int O_APPEND = 0x8;
    int O_CREAT = 0x40;
    int O_TRUNC = 0x200;

    // Unix file modes
    int S_IFMT = 0xF000;
     int S_IFREG = 0x8000;
    int S_IFDIR = 0x4000;
    int S_IREAD = 0x100;
    int S_IWRITE = 0x80;
    int S_IEXEC = 0x40;

    int S_IRUSR = 0x100;
    int S_IWUSR = 0x80;
    int S_IXUSR = 0x40;
    int S_IRGRP = 0x20;
    int S_IWGRP = 0x10;
    int S_IXGRP = 0x8;
    int S_IROTH = 0x4;
    int S_IWOTH = 0x2;
    int S_IXOTH = 0x1;
    int S_IAMB = 0x1FF;

    // copied from java.io.FileSystem.java
    int BA_EXISTS    = 0x01;
    int BA_REGULAR   = 0x02;
    int BA_DIRECTORY = 0x04;

    int ACCESS_READ    = 0x04;
    int ACCESS_WRITE   = 0x02;
    int ACCESS_EXECUTE = 0x01;

    int SPACE_TOTAL  = 0;
    int SPACE_FREE   = 1;
    int SPACE_USABLE = 2;
    int SPACE_USED = 3; // added, notionally SPACE_TOTAL-SPACE_FREE, but what if SPACE_TOTAL is fuzzy, e.g. /tmp?

    /**
     * Shutdown the file system.
     */
    void close();

    /*
     *  UnixFileSystem methods
     *  All file system paths passed to the following methods should be absolute.
     */

    String canonicalize0(String path) throws IOException;

    // This replaces getBooleanAttributes0, it returns the Unix mode
    int getMode(String path);

    // This replaces setPermission, it sets the Unix mode
    int setMode(String path, int mode);

    long getLastModifiedTime(String path);

    boolean checkAccess(String path, int access);

    long getLength(String path);

    boolean createFileExclusively(String path) throws IOException;

    boolean delete0(String path);

    String[] list(String path);

    boolean createDirectory(String path);

    boolean rename0(String path1, String path2);

    boolean setLastModifiedTime(String path, long time);

    long getSpace(String path, int t);

    /*
     * FileInputStream, FileOutputStream methods
     *
     *  These functions enode the traditional C errno value in their results.
     *  A negative return is to be interpreted as an error and equals to -errno.
     *
     */

    int available(int fd, long fileOffset);

    long skip(int fd, long n, long fileOffset);

    /**
     * Reads one byte from given offset in file.
     * Result is -1 for EOF, < 0 for error, 0-255 otherwise
     * @param fd
     * @param fileOffset
     * @return
     */
    int read(int fd, long fileOffset);

    /**
     * Reads up to length bytes from given offset in file.
     * Result is -1 for EOF, < 0 for error, number of bytes read otherwise
     * N.B. Returning -1 for EOF is for consistency with read but differs from OS conventions
     * @param fd
     * @param bytes
     * @param offset
     * @param length
     * @param fileOffset
     * @return
     */
    int readBytes(int fd, byte[] bytes, int offset, int length, long fileOffset);

    int write(int fd, int b, long fileOffset);

    int writeBytes(int fd, byte[] bytes, int offset, int length, long fileOffset);

    int open(String name, int flags);

    int close0(int fd);

    /*
     * RandomAccessFile methods
     */

    long getLength(int fd);

    int setLength(int fd, long length);

    /*
     * nio.* method support
     */

    /**
     * an "inode" in Unix terms, aka a unique value that identifies the file open on fd.
     */
    long uniqueId(int fd);

    int lock0(int fd, boolean blocking, long pos, long size, boolean shared) throws IOException;

    void release0(int fd, long pos, long size) throws IOException;

    int force0(int fd, boolean metaData) throws IOException;

    short POLLIN = 0x0001;
    short POLLOUT = 0x0004;

    int poll0(int fd, int eventOps, long timeout);

    void configureBlocking(int fd, boolean blocking);

    /*
     * Variants of readBytes/writeBytes that use byte buffers.
     * Arguably these could be the primitives and the array forms could
     * be hoisted and made fs independent.
     */
    int readBytes(int fd, ByteBuffer bb, long fileOffset);

    int writeBytes(int fd, ByteBuffer bb, long fileOffset);
}
