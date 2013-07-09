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

import java.io.*;
import java.nio.*;

import sun.nio.ch.ByteBufferNativeDispatcher;
import sun.nio.ch.NativeDispatcher;


import static com.sun.max.ve.jdk.JavaIOUtil.*;

import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.ve.fs.*;
import com.sun.max.ve.fs.pipe.*;
import com.sun.max.ve.jdk.JDK_java_io_FileDescriptor;
import com.sun.max.ve.jdk.JavaIOUtil;
import com.sun.max.ve.jdk.JavaIOUtil.FdInfo;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.unsafe.*;

/**
 * Substitutions for  @see sun.nio.ch.IOUtil.
 * We substitute more than just the native methods to workaround the code that forces all buffers to be direct
 * at this early stage. Essentially, we want NativeDispatcher to support a ByteBuffer interface.
 * 
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.nio.ch.IOUtil")
public class JDK_sun_nio_ch_IOUtil {

    private static PipeFileSystem _pipeFS = new PipeFileSystem();

    @SUBSTITUTE
    private static void initIDs() {

    }

    // Copied from IOUtil.java
    /*
     * Returns the index of first buffer in bufs with remaining,
     * or -1 if there is nothing left
     */
    private static int remaining(ByteBuffer[] bufs) {
        int numBufs = bufs.length;
        boolean remaining = false;
        for (int i=0; i<numBufs; i++) {
            if (bufs[i].hasRemaining()) {
                return i;
            }
        }
        return -1;
    }

    // Copied from IOUtil.java
    /*
     * Returns a new ByteBuffer array with only unfinished buffers in it
     */
    private static ByteBuffer[] skipBufs(ByteBuffer[] bufs,
                                         int nextWithRemaining)
    {
        int newSize = bufs.length - nextWithRemaining;
        ByteBuffer[] temp = new ByteBuffer[newSize];
        for (int i=0; i<newSize; i++) {
            temp[i] = bufs[i + nextWithRemaining];
        }
        return temp;
    }

    @SUBSTITUTE
    private static int write(FileDescriptor fd, ByteBuffer bb, long position, NativeDispatcher nd, Object lock)  throws IOException {
        /*
         * This code is copied from IOUtil.java (writeFromNativeBuffer) and modified to use the ByteBufferNativeDispatcher interface
         */
        final ByteBufferNativeDispatcher bnd = (ByteBufferNativeDispatcher) nd;
        int pos = bb.position();
        int lim = bb.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        int written = 0;
        if (rem == 0)
            return 0;
        if (position != -1) {
            bnd.write(fd, position, bb);
            /*
            written = nd.pwrite(fd,
                                ((DirectBuffer)bb).address() + pos,
                                rem, position, lock);
                                */
        } else {
            written = bnd.write(fd, bb);
        }
        if (written > 0)
            bb.position(pos + written);
        return written;

    }

    @SUBSTITUTE
    private static int read(FileDescriptor fd, ByteBuffer bb, long position, NativeDispatcher nd, Object lock) throws IOException {
        /*
         * This code is copied from IOUtil.java (readIntoNativeBuffer) and modified to use the ByteBufferNativeDispatcher interface
         */
        if (bb.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        final ByteBufferNativeDispatcher bnd = (ByteBufferNativeDispatcher) nd;
        int pos = bb.position();
        int lim = bb.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (rem == 0)
            return 0;
        int n = 0;
        if (position != -1) {
            n = bnd.read(fd, position, bb);
            /*
            n = nd.pread(fd, ((DirectBuffer)bb).address() + pos,
                         rem, position, lock);
                         */
        } else {
            n = bnd.read(fd, bb);
        }
        if (n > 0)
            bb.position(pos + n);
        return n;
    }

    @SUBSTITUTE
    static long write(FileDescriptor fd, ByteBuffer[] bufs, NativeDispatcher nd)  throws IOException {
        // This code is copied from IOUtil.java and modified to use the ByteBufferNativeDispatcher interface.
        // The middle section if the method setting up the shadow array of direct buffers is omitted.
        int nextWithRemaining = remaining(bufs);
        // if all bufs are empty we should return immediately
        if (nextWithRemaining < 0)
            return 0;
        // If some bufs are empty we should skip them
        if (nextWithRemaining > 0)
            bufs = skipBufs(bufs, nextWithRemaining);

        int numBufs = bufs.length;

        long bytesWritten = 0;
        // Invoke native call to fill the buffers

        final ByteBufferNativeDispatcher bnd = (ByteBufferNativeDispatcher) nd;
        bytesWritten = bnd.write(fd, bufs);
        long returnVal = bytesWritten;

        // Notify the buffers how many bytes were taken
        for (int i=0; i<numBufs; i++) {
            ByteBuffer nextBuffer = bufs[i];
            int pos = nextBuffer.position();
            int lim = nextBuffer.limit();
            assert (pos <= lim);
            int len = (pos <= lim ? lim - pos : lim);
            if (bytesWritten >= len) {
                bytesWritten -= len;
                int newPosition = pos + len;
                nextBuffer.position(newPosition);
            } else { // Buffers not completely filled
                if (bytesWritten > 0) {
                    assert(pos + bytesWritten < (long)Integer.MAX_VALUE);
                    int newPosition = (int)(pos + bytesWritten);
                    nextBuffer.position(newPosition);
                }
                break;
            }
        }
        return returnVal;
    }

    @SUBSTITUTE
    static boolean randomBytes(byte[] someBytes) {
        VEError.unimplemented("sun.nio.ch.IOUtil.randomBytes");
        return false;
    }

    @SUBSTITUTE
    static void initPipe(int[] fda, boolean blocking) {
        _pipeFS.createPipe(fda, blocking);
    }

    private static final int BUFSIZE = 128;
    @SUBSTITUTE
    static boolean drain(int fd) throws IOException {
        final FdInfo fdInfo = FdInfo.getFdInfo(fd);
        // throw it all away!
        // a StackByteBuffer is what we need here really!
        // final Pointer buf = StackAllocate.stackAllocate(BUFSIZE);
        final ByteBuffer bb = java.nio.ByteBuffer.allocate(BUFSIZE);
        int tn = 0;
        while (true) {
            final int n = fdInfo._vfs.readBytes(VirtualFileSystemId.getFd(fdInfo._fd), bb, VirtualFileSystemOffset.get(fd));
            if (n < 0) {
                if (n != -ErrorDecoder.Code.EAGAIN.getCode()) {
                    throw new IOException("Drain " + ErrorDecoder.getMessage(-n));
                }
            } else {
                tn += n;
                VirtualFileSystemOffset.add(fd, n);
                if (n == BUFSIZE) {
                    bb.position(0);
                    continue;
                }
                return tn > 0;
            }

        }
    }

    @SUBSTITUTE
    static void configureBlocking(FileDescriptor fd, boolean blocking) throws IOException {
        final FdInfo fdInfo = FdInfo.getFdInfo(fd);
        fdInfo._vfs.configureBlocking(VirtualFileSystemId.getFd(fdInfo._fd), blocking);
    }

    @SUBSTITUTE
    static int fdVal(FileDescriptor fdObj) {
        return JDK_java_io_FileDescriptor.getFd(fdObj);
    }

    @SUBSTITUTE
    static void setfdVal(FileDescriptor fdObj, int value) {
        JDK_java_io_FileDescriptor.setFd(fdObj, value);
    }

}
