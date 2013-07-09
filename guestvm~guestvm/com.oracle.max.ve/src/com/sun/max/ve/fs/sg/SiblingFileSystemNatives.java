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

import com.sun.max.unsafe.*;

/**
 * Native method definitions for the SiblingFileSystem implementation of VirtualFileSystem.
 * Note that even though we sometimes pass no reference objects across the interface and do not
 * make upcalls from the native code, these methods are not C_FUNCTIONS because
 * they may block, and the safepoint mechanism requires the use of JNI stubs to handle
 * a safepoint occurring while the thread is blocked.
 *
 * However, in the future, we expect to optimize this further to remove as much as possible of
 * the JNI overhead.
 *
 * @author Mick Jordan
 */
public class SiblingFileSystemNatives {

    /**
     * Gets the number of imported file systems.
     * @return the number of imported file systems.
     */
    static native int getNumImports();
    /**
     * Get the opaque handle of the file system import identified by the index argument.
     * 0 <= index < getNumImports
     * @param index
     * @return the opaque handle of the file system import
     */
    static native Word getImport(int index);
    /**
     * get a C string defining the file system path of this import.
     * @param handle the handle for the import
     * @return C string defining the file system path of this import
     */
    static native Pointer getPath(Word handle);

    /*
     * These methods can be accessed from upper layers concurrently, so must be synchronized
     * as the underlying interface is not multi-thread safe.
     */

    static synchronized native long getLength(Word handle, Pointer file);
    static synchronized native long getLastModifiedTime(Word handle, Pointer file);
    static synchronized native int rename(Word handle, Pointer from, Pointer to);
    static synchronized native int createDirectory(Word handle, Pointer dir);
    static synchronized native int getMode(Word handle, Pointer p);

   /**
     *
     * @param file
     * @return 0 if successful, -2 for file exists, -1 for some other error
     */
    static synchronized native int createFileExclusively(Word handle, Pointer file);
    static synchronized native int delete(Word handle, Pointer file);

    /**
     *  Returns a C array of C strings representing the names of the files in the directory (@see dir) passed as argument.
     *  Owing to buffer limitations, the return may be partial, which is indicated by hasMore[0] == true, in which case a series of
     *  calls should be made. In the second and subsequent calls, the @see offset oparamater should be set to the
     *  value of @see nFiles from the previous call.
     *
     *  The C array and C strings should be freed by the caller once they are processed.
     *
     * @param dir C string of directory to be listed
     * @param offset offset in list of files to skip over
     * @param nFiles array whose first element is set to number of files returned
     * @param hasMore array whose first element is set to true if the list is partial
     * @return Pointer to C array of C strings or null if error
     */

    /* FileInputStream, FileOutputStream, RandomAccessFile
     *
     * Negative returns are the corresponding "errno" value negated.
     */


    static synchronized native Pointer list(Word handle, Pointer dir, int offset,  int[] nFiles, boolean[] hasMore);

    static synchronized native int open(Word handle, Pointer p, int flags);

    static synchronized native int close0(Word handle, int fd);

    static synchronized native int read(Word handle, int fd, long offset);

    static synchronized native int readBytes(Word handle, int fd, Pointer bytes, int length, long offset);

    static synchronized native int write(Word handle, int fd, int b, long offset);

    static synchronized native int writeBytes(Word handle, int fd, Pointer bytes, int len, long offset);

    static synchronized native long getLengthFd(Word handle, int fd);

    static synchronized native long setLengthFd(Word handle, int fd, long len);

}
