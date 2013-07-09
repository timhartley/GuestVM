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
package com.sun.max.ve.fs.pipe;

import java.nio.ByteBuffer;
import java.util.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.ve.fs.DefaultReadWriteFileSystemImpl;
import com.sun.max.ve.fs.ErrorDecoder;
import com.sun.max.ve.fs.VirtualFileSystem;
import com.sun.max.ve.fs.VirtualFileSystemId;
import com.sun.max.ve.util.*;

/**
 * Not really a file system, just supports NIO pipes.
 * @author Mick Jordan
 *
 */

public class PipeFileSystem extends DefaultReadWriteFileSystemImpl implements VirtualFileSystem {
    private static PipeFileSystem _singleton;
    private static int _nextFd;
    private static Map<Integer, Pipe> _pipes = Collections.synchronizedMap(new HashMap<Integer, Pipe>());

    private static class Pipe {
        static final int BUFFER_SIZE = 128;
        byte[] _buffer = new byte[BUFFER_SIZE];
        int _readIndex;
        int _writeIndex;
        int _available;
        boolean _readClosed;
        boolean _writeClosed;
        boolean _blocking;
        Thread _waiter;

        Pipe(boolean blocking) {
            _blocking = blocking;
        }

        @INLINE
        final boolean full() {
            return _available >= _buffer.length;
        }

        @INLINE
        final boolean empty() {
            return _available == 0;
        }

        @INLINE
        final int free() {
            return BUFFER_SIZE - _available;
        }

        @INLINE
        final int available() {
            return _available;
        }

        @INLINE
        final boolean readClosed() {
            return _readClosed;
        }

        @INLINE
        final void closeRead() {
            _readClosed = true;
        }

        @INLINE
        final boolean writeClosed() {
            return _readClosed;
        }

        @INLINE
        final void closeWrite() {
            _readClosed = true;
        }

        @INLINE
        final boolean blocking() {
            return _blocking;
        }

        @INLINE
        final byte consumeOne() {
            final byte result = _buffer[_readIndex];
            _readIndex = Unsigned.irem(_readIndex + 1, _buffer.length);
            _available--;
            return result;
        }

        @INLINE
        final void produceOne(byte b) {
            _buffer[_writeIndex] = b;
            _writeIndex = Unsigned.irem(_writeIndex + 1, _buffer.length);
            _available++;
        }
    }

    public static PipeFileSystem create() {
        if (_singleton == null) {
            _singleton = new PipeFileSystem();
        }
        return _singleton;
    }

    /**
     * Create a pipe.
     * @param fds
     */
    public synchronized void createPipe(int[] fds, boolean blocking) {
        final int fd = _nextFd;
        fds[0] = VirtualFileSystemId.getUniqueFd(this, fd);
        fds[1] = VirtualFileSystemId.getUniqueFd(this, fd + 1);
        final Pipe pipe = new Pipe(blocking);
        _pipes.put(fd, pipe);
        _pipes.put(fd + 1, pipe);
        _nextFd += 2;
    }

    @Override
    public int readBytes(int fd, ByteBuffer bb, long fileOffset) {
        final Pipe pipe = _pipes.get(fd);
        int read = 0;
        final int length = bb.limit();
        // If no data is available we check for a close write end first, which means that
        // we never wait on a closed pipe. That means that a close that happens while
        // we are waiting will terminate the wait (by the notify)
        synchronized (pipe) {
             // if no data available block
            int available = pipe.available();
            if (available == 0) {
                if (pipe.writeClosed()) {
                    return 0;  // EOF
                }
                try {
                    pipe.wait();
                    available = pipe.available();
                    if (available == 0) {
                        if (pipe.writeClosed()) {
                            return 0;  // EOF
                        }
                        return -ErrorDecoder.Code.EAGAIN.getCode();
                    }
                } catch (InterruptedException ex) {
                    return -ErrorDecoder.Code.EINTR.getCode();
                }
            }
            // read what is available up to requested length
            int toRead = available < length ? available : length;
            while (toRead > 0) {
                bb.put(pipe.consumeOne());
                toRead--;
                read++;
            }
            pipe.notifyAll();
        }
        return read;
    }

    @Override
    public int writeBytes(int fd, ByteBuffer bb, long fileOffset) {
        final Pipe pipe = _pipes.get(fd);
        final int pos = bb.position();
        final int lim = bb.limit();
        final int length = pos <= lim ? lim - pos : 0;
        int toDo = length;
        // Writer blocks until all data is written or read end of the pipe is closed.
        // As per read a blocked write will be woken up by a close of the read end.
        // We are not precisely implementing POSIX semantics here regarding blocking,
        // as we only write atomically the number of bytes given by pipe.free() and not PIPE_BUF.
        while (toDo > 0) {
            synchronized (pipe) {
                if (pipe.readClosed()) {
                    return -ErrorDecoder.Code.EPIPE.getCode();
                }
                while (pipe.full()) {
                    try {
                        pipe.wait();
                        if (pipe.readClosed()) {
                            return -ErrorDecoder.Code.EPIPE.getCode();
                        }
                    } catch (InterruptedException ex) {
                        return -ErrorDecoder.Code.EINTR.getCode();
                    }
                }
                // write as much as we can then notify readers and give up lock
                int canWrite = pipe.free();
                while (toDo > 0 && canWrite > 0) {
                    pipe.produceOne(bb.get());
                    toDo--;
                    canWrite--;
                }
                pipe.notifyAll();
            }
        }
        return length;
    }

    @Override
    public int close0(int fd) {
        final Pipe pipe = _pipes.get(fd);
        synchronized (pipe) {
            if ((fd & 1) == 0) {
                // read end
                pipe.closeRead();
            } else {
                // write end
                pipe.closeWrite();
            }
            // wake up any waiting readers or writers
            pipe.notifyAll();
        }
        return 0;
    }

    @Override
    public int poll0(int fd, int eventOps, long timeout) {
        final Pipe pipe = _pipes.get(fd);
        synchronized (pipe) {
            if ((fd & 1) == 0) {
                // read end, anything available?
                if (pipe.available() > 0) {
                    return VirtualFileSystem.POLLIN;
                }
                if (timeout == 0) {
                    return 0;
                }
                final TimeLimitedProc timedProc = new TimeLimitedProc() {
                    @Override
                    protected int proc(long remaining) throws InterruptedException {
                        pipe.wait(remaining);
                        if (pipe.available() > 0) {
                            return terminate(VirtualFileSystem.POLLIN);
                        } else if (pipe.available() == 0) {
                            if (pipe.writeClosed()) {
                                return terminate(0); // EOF
                            }
                        }
                        return 0;
                    }
                };
                return timedProc.run(timeout);
            } else {
                // write end, can we write?
                if (!pipe.full()) {
                    return VirtualFileSystem.POLLOUT;
                }
                if (timeout == 0) {
                    return 0;
                }
                final TimeLimitedProc timedProc = new TimeLimitedProc() {
                    @Override
                    protected int proc(long remaining) throws InterruptedException {
                        pipe.wait(remaining);
                        if (pipe.readClosed()) {
                            return terminate(0);
                        }
                        if (!pipe.full()) {
                            return terminate(VirtualFileSystem.POLLOUT);
                        }
                        return 0;
                    }
                };
                return timedProc.run(timeout);
            }
        }

    }

}
