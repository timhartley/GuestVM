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
package com.sun.max.ve.fs.heap;

import java.io.*;
import java.util.*;

import com.sun.max.ve.fs.*;
import com.sun.max.ve.jdk.*;

/**
 * A heap-based file system for /tmp.
 * Single global lock protects everything accessed through public methods.
 */

public final class HeapFileSystem extends UnimplementedFileSystemImpl implements VirtualFileSystem {

    @SuppressWarnings("unused")
    private String _devPath;
    private String _mountPath;
    private int _mountPathPrefixIndex;
    private SubDirEntry _root = new SubDirEntry(null);
    private List<FileEntry> _openFiles = new ArrayList<FileEntry>();
    private static final int READ_WRITE = S_IREAD | S_IWRITE;
    private static int _tmpSize;

    abstract static  class DirEntry {
        int _mode = READ_WRITE;
        long _modified;

        DirEntry() {
            _modified = System.currentTimeMillis();
        }

        boolean isFile() {
            return false;
        }

        boolean isDir() {
            return false;
        }
    }

    static class FileEntry extends DirEntry {
        static long _nextId = 0;
        static final int FILE_BLOCK_SIZE = 1024;
        static final int DIV_FILE_BLOCK_SIZE = 10;
        static final int MOD_FILE_BLOCK_SIZE = FILE_BLOCK_SIZE - 1;
        final List<byte[]> _blocks = new ArrayList<byte[]>();
        long _size;
        long _maxSize;
        int _nextIndex;
        long _id;

        FileEntry() {
            _mode |= S_IFREG;
            _id = _nextId++;
        }

        @Override
        boolean isFile() {
            return true;
        }

        void addCapacity(long fileOffset) {
            while (fileOffset >= _maxSize) {
                _blocks.add(_nextIndex++, new byte[FILE_BLOCK_SIZE]);
                _maxSize += FILE_BLOCK_SIZE;
                _tmpSize += FILE_BLOCK_SIZE;
            }
        }

        void write(int b, long fileOffset) {
            final int index = (int) fileOffset >> DIV_FILE_BLOCK_SIZE;
            final int offset = (int) fileOffset & MOD_FILE_BLOCK_SIZE;
            // Checkstyle: stop indentation check
            _blocks.get(index)[offset] = (byte) b;
            // Checkstyle: resume indentation check
            _size++;
        }

        void writeBytes(byte[] bytes, int offset, int length, long fileOffset) {
            int index = (int) fileOffset >> DIV_FILE_BLOCK_SIZE;
            int blockoffset = (int) fileOffset & MOD_FILE_BLOCK_SIZE;
            byte[] block = _blocks.get(index);
            for (int i = 0; i < length; i++) {
                if (blockoffset == FILE_BLOCK_SIZE) {
                    blockoffset = 0;
                    index++;
                    block = _blocks.get(index);
                }
                block[blockoffset++] = bytes[offset + i];
            }
            _size += length;
        }

        int read(long fileOffset) {
            if (fileOffset >= _size) {
                return -1;
            } else {
                final int index = (int) fileOffset >> DIV_FILE_BLOCK_SIZE;
                final int offset = (int) fileOffset & MOD_FILE_BLOCK_SIZE;
                return (int) _blocks.get(index)[offset] & 0xFF;
            }
        }

        int readBytes(byte[] bytes, int offset, int length, long fileOffset) {
            int index = (int) fileOffset >> DIV_FILE_BLOCK_SIZE;
            int blockoffset = (int) fileOffset & MOD_FILE_BLOCK_SIZE;
            if (index >= _blocks.size()) {
                return 0;
            }
            byte[] block = _blocks.get(index);
            for (int i = 0; i < length; i++) {
                if (blockoffset == FILE_BLOCK_SIZE) {
                    blockoffset = 0;
                    index++;
                    if (index == _blocks.size()) {
                        return i;
                    }
                    block = _blocks.get(index);
                }
                bytes[offset + i] = block[blockoffset++];
            }
            return length;
        }
    }

    static class SubDirEntry extends DirEntry {
        Map<String, DirEntry> _contents = new HashMap<String, DirEntry>();

        SubDirEntry(SubDirEntry parent) {
            _mode |= S_IFDIR + S_IEXEC;
            put(".", this);
            put("..", parent);
        }

        @Override
        boolean isDir() {
            return true;
        }

        void put(String name, DirEntry entry) {
            _contents.put(name, entry);
        }

        DirEntry get(String name) {
            return _contents.get(name);
        }
    }

    private HeapFileSystem(String devPath, String mountPath) {
        _devPath = devPath;
        _mountPath = mountPath;
        _mountPathPrefixIndex = _mountPath.split(File.separator).length;
    }

    public static HeapFileSystem create(String devPath, String mountPath) {
        return new HeapFileSystem(devPath, mountPath);
    }

    @Override
    public void close() {

    }

    @Override
    public synchronized String canonicalize0(String path) throws IOException {
        // TODO Auto-generated method stub
        return path;
    }

    @Override
    public synchronized boolean checkAccess(String path, int access) {
        final DirEntry d = matchPath(path);
        if (d == null) {
            return false;
        }
        switch (access) {
            case ACCESS_READ:
                return (d._mode & S_IREAD) != 0;
            case ACCESS_WRITE:
                return (d._mode & S_IWRITE) != 0;
            case ACCESS_EXECUTE:
                return (d._mode & S_IEXEC) != 0;
        }
        return false;
    }

    @Override
    public synchronized int close0(int fd) {
        _openFiles.set(fd, null);
        return 0;
    }

    @Override
    public synchronized boolean createDirectory(String path) {
        return create(path, false);
    }

    @Override
    public synchronized boolean createFileExclusively(String path) throws IOException {
        return create(path, true);
    }

    private synchronized boolean create(String path, boolean isFile) {
        final Match m = match(path, false);
        if (m == null || m.matchTail() != null) {
            return false;
        }
        m._d.put(m._tail, isFile ? new FileEntry() : new SubDirEntry(m._d));
        return true;

    }

    @Override
    public synchronized boolean delete0(String path) {
        final Match m = match(path, false);
        if (m != null) {
            final DirEntry dd = m.matchTail();
            if (dd != null) {
                if (dd.isFile()) {
                    // TODO permissions
                    final FileEntry fdd = (FileEntry) dd;
                    m._d._contents.remove(m._tail);
                    _tmpSize -= fdd._maxSize;
                    return true;
                } else {
                    // check empty (but remember . and ..)
                    final SubDirEntry sdd = (SubDirEntry) dd;
                    if (sdd._contents.size() <= 2) {
                        m._d._contents.remove(m._tail);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public synchronized long getLastModifiedTime(String path) {
        long result = 0;
        final DirEntry d = matchPath(path);
        if (d != null) {
            result = d._modified;
        }
        return result;
    }

    @Override
    public synchronized long getLength(String path) {
        long result = 0;
        final DirEntry d = matchPath(path);
        if (d != null && d.isFile()) {
            result = ((FileEntry) d)._size;
        }
        return result;
    }

    @Override
    public synchronized int getMode(String path) {
        final DirEntry d = matchPath(path);
        if (d != null) {
            return d._mode;
        }
        return -ErrorDecoder.Code.ENOENT.getCode();
    }

    @Override
    public synchronized long getSpace(String path, int t) {
        switch (t) {
            case SPACE_TOTAL:
                return Runtime.getRuntime().freeMemory() + _tmpSize;
            case SPACE_USABLE:
            case SPACE_FREE:
                return Runtime.getRuntime().freeMemory();
            case SPACE_USED:
                return _tmpSize;
            default:
                return 0;
        }
    }

    @Override
    public synchronized String[] list(String path) {
        final Match m = match(path, true);
        if (m == null) {
            return null;
        } else {
            final String[] result = new String[m._d._contents.size() - 2];
            int k = 0;
            for (String name : m._d._contents.keySet()) {
                if (!JDK_java_io_UnixFileSystem.currentOrParent(name)) {
                    result[k++] = name;
                }
            }
            return result;
        }
    }

    @Override
    public synchronized int open(String name, int flags) {
        final Match m = match(name, false);
        if (m == null) {
            return -ErrorDecoder.Code.ENOENT.getCode();
        }
        if (flags == VirtualFileSystem.O_RDONLY) {
            // reading
            final DirEntry fe = m.matchTail();
            if (fe == null) {
                return -ErrorDecoder.Code.ENOENT.getCode();
            } else if (!fe.isFile()) {
                return  -ErrorDecoder.Code.EISDIR.getCode();
            }
            return addFd((FileEntry) fe);
        } else {
            // writing
            DirEntry fe = m.matchTail();
            if (fe == null) {
                fe = new FileEntry();
                m._d.put(m._tail,  fe);
            } else {
                // exists, check is a file
                if (fe.isDir()) {
                    return  -ErrorDecoder.Code.EISDIR.getCode();
                }
                final FileEntry ffe = (FileEntry) fe;
                // do we need to truncate?
                if ((flags & O_TRUNC) != 0) {
                    ffe._modified = System.currentTimeMillis();
                    ffe._size = 0;
                }
            }
            return addFd((FileEntry) fe);
        }
    }

    @Override
    public synchronized int read(int fd, long fileOffset) {
        final FileEntry fe = _openFiles.get(fd);
        return fe.read(fileOffset);
    }

    @Override
    public synchronized int readBytes(int fd, byte[] bytes, int offset, int length,
            long fileOffset) {
        final FileEntry fe = _openFiles.get(fd);
        if ((int) fileOffset >= fe._size) {
            return -1;
        }
        if (length > (int) (fe._size - fileOffset)) {
            // CheckStyle: stop parameter assignment check
            length = (int) (fe._size - fileOffset);
            // CheckStyle: resume parameter assignment check
        }
        fe.readBytes(bytes, offset, length, fileOffset);
        return length;
    }

    @Override
    public synchronized boolean rename0(String path1, String path2) {
        final Match m1 = match(path1, false);
        final Match m2 = match(path2, false);
        /* At this point we should have matched up to the last component of both paths */
        if (m1 != null && m2 != null) {
            final DirEntry d1 = m1.matchTail();
            if (d1 == null) {
                /* path1 does not exist */
                return false;
            }
            final DirEntry d2 = m2.matchTail();
            if (d1 == d2) {
                /* rename to self */
                return true;
            }
            if (d2 != null) {
                /* path2 already exists */
                return false;
            }
            m1._d._contents.remove(m1._tail);
            m2._d._contents.put(m2._tail, d1);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean setLastModifiedTime(String path, long time) {
        final DirEntry d = matchPath(path);
        if (d != null) {
            d._modified = time;
            return true;
        }
        return false;
    }

    @Override
    public synchronized int setMode(String path, int mode) {
        final DirEntry d = matchPath(path);
        if (d != null) {
            d._mode |= mode & S_IAMB;
            return 0;
        }
        return -ErrorDecoder.Code.ENOENT.getCode();
    }

    @Override
    public synchronized int write(int fd, int b, long fileOffset) {
        final FileEntry fe = _openFiles.get(fd);
        if (fileOffset >= fe._maxSize) {
            fe.addCapacity(fileOffset);
        }
        fe.write(b, fileOffset);
        return 1;
    }

    @Override
    public synchronized int writeBytes(int fd, byte[] bytes, int offset, int length,
            long fileOffset) {
        final FileEntry fe = _openFiles.get(fd);
        if (fileOffset + length > fe._maxSize) {
            fe.addCapacity(fileOffset + length);
        }
        fe.writeBytes(bytes, offset, length, fileOffset);
        return length;
    }

    @Override
    public long getLength(int fd) {
        final FileEntry fe = _openFiles.get(fd);
        return fe._size;
    }

    @Override
    public int setLength(int fd, long length) {
        final FileEntry fe = _openFiles.get(fd);
        if (fe._size == length) {
            return 0;
        } else if (fe._size < length) {
            if (length >= fe._maxSize) {
                fe.addCapacity(length);
            }
            return 0;
        } else {
            fe._size = length;
            return 0;
        }
    }

    @Override
    public int available(int fd, long fileOffset) {
        final FileEntry fe = _openFiles.get(fd);
        if ((int) fileOffset >= fe._size) {
            return 0;
        }
        final long avail = fe._size - fileOffset;
        if (avail > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) avail;
    }

    @Override
    public long skip(int fd, long n, long fileOffset) {
        // spec allows skip to go past end of file
        return n;
    }

    @Override
    public long uniqueId(int fd) {
        return _openFiles.get(fd)._id;
    }

    /*
     * private support methods
     */

    static class Match {
        SubDirEntry _d;
        String _tail;
        Match(SubDirEntry d, String tail) {
            _d = d;
            _tail = tail;
        }

        DirEntry matchTail() {
            return _d.get(_tail);
        }
    }

    private int addFd(FileEntry fe) {
        final int size = _openFiles.size();
        for (int i = 0; i < size; i++) {
            if (_openFiles.get(i) == null) {
                _openFiles.set(i, fe);
                return i;
            }
        }
        _openFiles.add(fe);
        return size;
    }

    /**
     * Matches the sequence of names in parts against the directory hierarchy.
     * If complete is true, expects the last component to represent a directory
     * otherwise expects next to last to be a directory. Returns the
     * @param parts
     * @param complete
     * @return matching SubDirEntry or null if no match
     */
    private Match match(String name, boolean complete) {
        final String[] parts = name.split(File.separator);
        if (parts.length <= _mountPathPrefixIndex) {
            return new Match(_root, ".");
        }
        SubDirEntry d = _root;
        final int length = complete ? parts.length : parts.length - 1;
        for (int i = _mountPathPrefixIndex; i < length; i++) {
            final DirEntry dd = d.get(parts[i]);
            if (dd == null || dd.isFile() || (dd._mode & S_IEXEC) == 0) {
                return null;
            }
            d = (SubDirEntry) dd;
        }
        return new Match(d, parts[parts.length - 1]);
    }

    private DirEntry matchPath(String name) {
        final Match m = match(name, false);
        if (m != null) {
            return m.matchTail();
        }
        return null;
    }

}
