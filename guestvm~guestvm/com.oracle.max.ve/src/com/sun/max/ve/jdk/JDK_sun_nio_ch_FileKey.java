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
package com.sun.max.ve.jdk;

import static com.sun.max.ve.jdk.AliasCast.*;

import java.io.*;
import sun.nio.ch.FileKey;
import com.sun.max.annotate.*;
import com.sun.max.ve.fs.*;

/**
 * MaxVE implementation of sun.nio.ch.FileKey
 *
 * A FileKey is just a unique identifier for a file, whereas a pathname could be aliased.
 * Unix Hotspot uses the st_dev and st_ino fields from the stat system call.
 * MaxVE has no direct equivalent but the VirtualFileSystemId encoded in the
 * fd field of a {@link FileDescriptor} is equivalent to st_dev and the st_ino case is handled by a
 * method in {@link VirtualFileSystem}.
 *
 * @author Mick Jordan
 */


@METHOD_SUBSTITUTIONS(FileKey.class)
public class JDK_sun_nio_ch_FileKey {
    
    @SuppressWarnings("unused")
    @ALIAS(declaringClass = FileKey.class)
    private long st_dev;
    
    @SuppressWarnings("unused")
    @ALIAS(declaringClass = FileKey.class)
    private long st_ino;
    
    @SuppressWarnings("unused")
    @SUBSTITUTE
    private void init(FileDescriptor fdObj) throws IOException {
        final int fd = JDK_java_io_FileDescriptor.getFd(fdObj);
        JDK_sun_nio_ch_FileKey thisJDK_sun_nio_ch_FileKey = asJDK_sun_nio_ch_FileKey(this);
        thisJDK_sun_nio_ch_FileKey.st_dev = VirtualFileSystemId.getVfsId(fd);
        thisJDK_sun_nio_ch_FileKey.st_ino = VirtualFileSystemId.getVfs(fd).uniqueId(VirtualFileSystemId.getFd(fd));
    }

    @SuppressWarnings("unused")
    @SUBSTITUTE
    private static void initIDs() {

    }

}
