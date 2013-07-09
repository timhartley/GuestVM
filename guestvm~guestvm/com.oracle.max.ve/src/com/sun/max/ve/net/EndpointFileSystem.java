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
package com.sun.max.ve.net;

import java.io.*;
import java.nio.*;
import com.sun.max.ve.fs.*;
import com.sun.max.ve.jdk.JavaNetUtil;

/**
 * Not really a file system, but ensures that network endpoints are mapped to file descriptors that
 * follow the conventions in VirtualFileSystemId. This became a requirement when implementing
 * parts of the java.nio package, which is heavily dependent on file descriptors and invokes
 * generic calls that require the underlying "file system" to be determinable.
 *
 * @author Mick Jordan
 *
 */

public class EndpointFileSystem extends UnimplementedFileSystemImpl implements VirtualFileSystem {
    private static EndpointFileSystem _singleton;

    public static EndpointFileSystem create() {
        if (_singleton == null) {
            _singleton = new EndpointFileSystem();
        }
        return _singleton;
    }

    @Override
    public void configureBlocking(int fd, boolean blocking) {
        final Endpoint endpoint = JavaNetUtil.getFromVfsId(fd);
        endpoint.configureBlocking(blocking);
    }

    @Override
    public int poll0(int fd, int eventOps, long timeout) {
        final Endpoint endpoint = JavaNetUtil.getFromVfsId(fd);
        return endpoint.poll(eventOps, timeout);
    }

    @Override
    public int writeBytes(int fd, ByteBuffer bb, long fileOffset) {
        try {
            final Endpoint endpoint = JavaNetUtil.getFromVfsId(fd);
            return endpoint.write(bb);
        } catch (IOException ex) {
            return -ErrorDecoder.Code.EIO.getCode();
        }
    }

    @Override
    public int readBytes(int fd, ByteBuffer bb, long fileOffset) {
        try {
            final Endpoint endpoint = JavaNetUtil.getFromVfsId(fd);
            return endpoint.read(bb);
        } catch (IOException ex) {
            return -ErrorDecoder.Code.EIO.getCode();
        }
    }

    @Override
    public int close0(int fd) {
        try {
            final Endpoint endpoint = JavaNetUtil.getFromVfsId(fd);
            endpoint.close(Endpoint.SHUT_RDWR);
            return 0;
        } catch (IOException ex) {
            return -ErrorDecoder.Code.EIO.getCode();
        }
    }

}
