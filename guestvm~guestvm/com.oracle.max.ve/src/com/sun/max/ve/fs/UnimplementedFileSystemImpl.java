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

import java.io.IOException;
import java.nio.ByteBuffer;

import com.sun.max.ve.error.VEError;

/**
 * An implementation that throws an error for every method.
 * @author Mick Jordan
 *
 */

public class UnimplementedFileSystemImpl implements VirtualFileSystem {

    @Override
   public int available(int fd, long fileOffset) {
        unimplemented("available");
        return 0;
    }

    @Override
    public String canonicalize0(String path) throws IOException {
        unimplemented("canonicalize0");
        return null;
    }

    @Override
    public boolean checkAccess(String path, int access) {
        unimplemented("checkAccess");
        return false;
    }

    @Override
    public void close() {
        unimplemented("close");
    }

    @Override
    public int close0(int fd) {
        unimplemented("close0");
        return 0;
    }

    @Override
    public boolean createDirectory(String path) {
        unimplemented("createDirectory");
        return false;
    }

    @Override
    public boolean createFileExclusively(String path) throws IOException {
        unimplemented("createFileExclusively");
        return false;
    }

    @Override
    public boolean delete0(String path) {
        unimplemented("delete0");
        return false;
    }

    @Override
    public long getLastModifiedTime(String path) {
        unimplemented("getLastModifiedTime");
        return 0;
    }

    @Override
    public long getLength(String path) {
        unimplemented("getLength");
        return 0;
    }

    @Override
    public long getLength(int fd) {
        unimplemented("getLength");
        return 0;
    }

    @Override
    public int getMode(String path) {
        unimplemented("getMode");
        return 0;
    }

    @Override
    public long getSpace(String path, int t) {
        unimplemented("getSpace");
        return 0;
    }

    @Override
    public String[] list(String path) {
        unimplemented("list");
        return null;
    }

    @Override
    public int open(String name, int flags) {
        unimplemented("open");
        return 0;
    }

    @Override
    public int read(int fd, long fileOffset) {
        unimplemented("read");
        return 0;
    }

    @Override
    public boolean rename0(String path1, String path2) {
        unimplemented("rename0");
        return false;
    }

    @Override
    public boolean setLastModifiedTime(String path, long time) {
        unimplemented("setLastModifiedTime");
        return false;
    }

    @Override
    public int setLength(int fd, long length) {
        unimplemented("setLength");
        return 0;
    }

    @Override
    public int setMode(String path, int mode) {
        unimplemented("setPermission");
        return -1;
    }

    @Override
    public long skip(int fd, long n, long fileOffset) {
        unimplemented("skip");
        return 0;
    }

    @Override
    public long uniqueId(int fd) {
        unimplemented("uniqueId");
        return 0;
    }

    @Override
    public int readBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        unimplemented("readBytes");
        return 0;
    }


    @Override
    public int write(int fd, int b, long fileOffset) {
        unimplemented("write");
        return 0;
    }

    @Override
    public int writeBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        unimplemented("writeBytes");
        return 0;
    }

    @Override
    public int lock0(int fd, boolean blocking, long pos, long size, boolean shared) throws IOException {
        unimplemented("lock0");
        return 0;
    }

    @Override
    public void release0(int fd, long pos, long size) throws IOException {
        unimplemented("release0");
    }

    @Override
    public int force0(int fd, boolean metaData) throws IOException {
        unimplemented("force0");
        return 0;
    }

    @Override
    public int readBytes(int fd, ByteBuffer bb, long fileOffset) {
        unimplemented("readBytes-address");
        return 0;
    }

    @Override
    public int writeBytes(int fd, ByteBuffer bb, long fileOffset) {
        unimplemented("writeBytes-address");
        return 0;
    }

    @Override
    public int poll0(int fd, int eventOps, long timeout) {
        unimplemented("poll0");
        return 0;
    }

    @Override
    public void configureBlocking(int fd, boolean blocking) {
        unimplemented("configureBlocking");
    }

    private void unimplemented(String w) {
        VEError.unimplemented(getClass().getName() + "." + w);
    }

}
