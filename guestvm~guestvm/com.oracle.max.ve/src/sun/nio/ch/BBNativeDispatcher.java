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
package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.sun.max.ve.error.*;
import com.sun.max.ve.fs.ErrorDecoder;
import com.sun.max.ve.fs.VirtualFileSystemId;
import com.sun.max.ve.jdk.JavaIOUtil;
import com.sun.max.ve.jdk.JavaIOUtil.FdInfo;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.FieldActor;

/**
 * This is part of the mechanism that replaces the part of the Sun JDK that uses an "address, length"
 * pattern for the native nio interface (NativeDispatcher). This class extends NativeDispatcher
 * with methods that use ByteBuffers. These methods are used in the substituted methods of sun.nio.ch.IOUtil.
 *
 * @author Mick Jordan
 *
 */

public class BBNativeDispatcher extends ByteBufferNativeDispatcher {

    // copied from sun.nio.IOStatus (not public)
    private static final int EOF = -1;                       // end of file
    private static final int UNAVAILABLE = -2;      // Nothing available (non-blocking)
    private static final int INTERRUPTED = -3;    // System call interrupted

    public static void resetNativeDispatchers() {
        final BBNativeDispatcher bbnd = new BBNativeDispatcher();
        resetNativeDispatcher("sun.nio.ch.DatagramChannelImpl", bbnd);
        resetNativeDispatcher("sun.nio.ch.ServerSocketChannelImpl", bbnd);
        resetNativeDispatcher("sun.nio.ch.SocketChannelImpl", bbnd);
        resetNativeDispatcher("sun.nio.ch.SinkChannelImpl", bbnd);
        resetNativeDispatcher("sun.nio.ch.SourceChannelImpl", bbnd);
        resetNativeDispatcher("sun.nio.ch.FileChannelImpl", bbnd);
    }

    private static void resetNativeDispatcher(String name, BBNativeDispatcher nd) {
        try {
            final FieldActor rfa = ClassActor.fromJava(Class.forName(name)).findLocalStaticFieldActor("nd");
            assert rfa != null;
            rfa.setObject(null, nd);
        } catch (ClassNotFoundException ex) {
            VEError.unexpected("problem with Class.forName: " + name);
        }
    }

    private static int convertReturnValue(int n, boolean reading) throws IOException {
        if (n > 0) {
            return n;
        } else if (n < 0) {
            if (-n == ErrorDecoder.Code.EINTR.getCode()) {
                return INTERRUPTED;
            } else if (-n == ErrorDecoder.Code.EAGAIN.getCode()) {
                return UNAVAILABLE;
            }
            throw new IOException("Read error: " + ErrorDecoder.getMessage(-n));
        } else {
            if (reading) {
                return EOF;
            } else {
                return 0;
            }
        }
    }

    @Override
    public int write(FileDescriptor fdObj, ByteBuffer bb) throws IOException {
        final JavaIOUtil.FdInfo fdInfo = JavaIOUtil.FdInfo.getFdInfo(fdObj);
        final int result = convertReturnValue(fdInfo._vfs.writeBytes(VirtualFileSystemId.getFd(fdInfo._fd), bb, fdInfo._fileOffset), false);
        return result;
    }

    @Override
    public int write(FileDescriptor fdObj, ByteBuffer[] bbs) throws IOException {
        final JavaIOUtil.FdInfo fdInfo = JavaIOUtil.FdInfo.getFdInfo(fdObj);
        final int fd = VirtualFileSystemId.getFd(fdInfo._fd);
        return write(fdObj, fdInfo, fd, fdInfo._fileOffset, bbs);
    }


    @Override
    public int write(FileDescriptor fdObj, long fileOffset, ByteBuffer... bbs) throws IOException {
        final JavaIOUtil.FdInfo fdInfo = JavaIOUtil.FdInfo.getFdInfo(fdObj);
        final int fd = VirtualFileSystemId.getFd(fdInfo._fd);
        return write(fdObj,fdInfo,fd,fileOffset,bbs);
    }

    private int write(FileDescriptor fdObj, FdInfo fdInfo, int fd, long fileOffset, ByteBuffer... bbs)throws IOException {
        int bytesWritten = 0;
        for (int i = 0; i < bbs.length; i++) {
            final int result = convertReturnValue(fdInfo._vfs.writeBytes(fd, bbs[i], fileOffset), false);
            if (result < 0) {
                return result;
            }
            bytesWritten += result;
        }
        return bytesWritten;
    }
    @Override
    public int read(FileDescriptor fdObj, ByteBuffer bb) throws IOException {
        final JavaIOUtil.FdInfo fdInfo = JavaIOUtil.FdInfo.getFdInfo(fdObj);
        final int result = convertReturnValue(fdInfo._vfs.readBytes(VirtualFileSystemId.getFd(fdInfo._fd), bb, fdInfo._fileOffset), true);
        return result;
    }

    @Override
    public int read(FileDescriptor fdObj, long fileOffset, ByteBuffer... bb) throws IOException {
        final JavaIOUtil.FdInfo fdInfo = JavaIOUtil.FdInfo.getFdInfo(fdObj);
        final int fd = VirtualFileSystemId.getFd(fdInfo._fd);
        return read(fdObj, fdInfo, fd, fileOffset, bb);
    }

    private int read(FileDescriptor fdObj, FdInfo fdInfo, int fd, long fileOffset, ByteBuffer... bbs) throws IOException {
        int bytesRead = 0;
        for (int i = 0; i < bbs.length; i++) {
            final int result = convertReturnValue(fdInfo._vfs.readBytes(fd, bbs[i], fileOffset), true);
            if (result < 0) {
                return result;
            }
            bytesRead += result;
        }
        return bytesRead;
    }

    @Override
    public int read(FileDescriptor fdObj, ByteBuffer[] bbs) throws IOException {
        final JavaIOUtil.FdInfo fdInfo = JavaIOUtil.FdInfo.getFdInfo(fdObj);
        final int fd = VirtualFileSystemId.getFd(fdInfo._fd);
        return read(fdObj, fdInfo, fd, fdInfo._fileOffset, bbs);
    }

    @Override
    void close(FileDescriptor fd) throws IOException {
        JavaIOUtil.close0(fd);
    }

    @Override
    int read(FileDescriptor fd, long address, int len) throws IOException {
        unexpected("read");
        return 0;
    }

    @Override
    long readv(FileDescriptor fd, long address, int len) throws IOException {
        unexpected("readv");
        return 0;
    }

    @Override
    int write(FileDescriptor fd, long address, int len) throws IOException {
        unexpected("write");
        return 0;
    }

    @Override
    long writev(FileDescriptor fd, long address, int len) throws IOException {
        unexpected("writev");
        return 0;
    }

    static void unexpected(String name) {
        VEError.unexpected("BBNativeDispatcher." + name + " invoked");
    }



}
