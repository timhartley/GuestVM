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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import com.sun.max.ve.fs.*;

/**
 * Implementation of the native methods of PollArrayWrapper leveraging
 * access to package private definition of PollArrayWrapper.
 *
 * poll0 first polls every file descriptor with a timeout of zero. If any match the
 * required event ops, the method returns. Otherwise, if there is only one
 * file descriptor, and the timeout is non-zero, the current thread repeats the poll
 * with the requested timeout. If there is more than one file descriptor a set of
 * worker threads are used to do the poll, with the current thread waiting for the
 * first one to return a match, or all of them timing out.
 *
 * @author Mick Jordan
 *
 */

public class MaxVENativePollArrayWrapper {

    /**
     * Implements the substituted native method. This is forwarded from the substituted native method in
     * JDK_sun_nio_ch.PollArrayWrapper.
     *
     * @param pObj
     *                the "this" argument in the substituted method, i.e., the PollArrayWrapper instance
     * @param pollAddress
     *                native address of the actual pollfd_t instance to work with
     * @param numfds number of channels (file descriptors) to poll for.
     * @param timeout how long to wait for response (<0 infinite)
     * @return
     */
    public static int poll0(Object pObj, long pollAddress, int numfds, long timeout) throws IOException {
        // slog("poll0 entered: " + numfds + ", " + timeout);
        final PollArrayWrapper p = (PollArrayWrapper) pObj;
        // pollAddress is already offset from the value of p.pollArrayAddress to the start of the pollfd array elements
        // so we must use the p.getXXX methods with index starting at 0.
        final VirtualFileSystem[] vfsArray = timeout == 0 ? null : new VirtualFileSystem[numfds];
        int count = 0;
        for (int i = 0; i < numfds; i++) {
            final int fd = p.getDescriptor(i);
            final int eventOps = p.getEventOps(i);
            final VirtualFileSystem vfs = VirtualFileSystemId.getVfs(fd);
            final int reventOps = vfs.poll0(VirtualFileSystemId.getFd(fd), eventOps, 0);
            count += checkMatch(p, i, reventOps);
            if (timeout != 0) {
                vfsArray[i] = vfs;
            }
        }
        if (timeout == 0 || count > 0) {
            return count;
        }
        // now wait for timeout for one event
        if (numfds == 1) {
            // we can wait for one fd on the current thread
            final int reventOps = vfsArray[0].poll0(VirtualFileSystemId.getFd(p.getDescriptor(0)), p.getEventOps(0), timeout);
            return checkMatch(p, 0, reventOps);
        }
        // multiple file descriptors to wait for, need a thread for each
        final PollOut pollOut = new PollOut(numfds);
        final PollThread[] pollThreads = new PollThread[numfds];
        for (int t = 0; t < numfds; t++) {
            pollThreads[t] = PollThread.getThread(vfsArray, p, t, timeout, pollOut);
        }
        for (int t = 0; t < numfds; t++) {
            pollThreads[t].release();
        }
        synchronized (pollOut) {
            try {
                while (pollOut._waiterCount > 0 && pollOut._index < 0) {
                    pollOut.wait();
                    if (pollOut._index >= 0) {
                        return 1;
                    }
                }
                // if all waiters return without a match we drop through and return 0
            } catch (InterruptedException ex) {
                return -ErrorDecoder.Code.EINTR.getCode();
            } finally {
                if (pollOut._waiterCount > 0) {
                    PollThread.cancelThreads(pollThreads, pollOut);
                }
            }
        }
        // slog("poll0 returning");
        return 0;
    }

    static class PollOut {
        int _index;
        int _reventOps;
        int _waiterCount;
        PollOut(int waiterCount) {
            _waiterCount = waiterCount;
            _index = -1;
        }
    }

    private static int checkMatch(PollArrayWrapper p, int i, int reventOps) throws IOException {
        if (reventOps < 0) {
            throw new IOException("poll failed");
        } else {
            return match(p, i, reventOps);
        }
    }

    private static int match(PollArrayWrapper p, int i, int reventOps) {
        if (reventOps < 0) {
            return reventOps;
        } else {
            if ((reventOps & p.getEventOps(i)) != 0) {
                p.putReventOps(i, reventOps);
                return 1;
            }
        }
        return 0;

    }

    static class PollThread extends Thread {
        static List<PollThread> _workers = new ArrayList<PollThread>(0);
        static int _nextWorkerId;

        VirtualFileSystem[] _vfsArray;
        PollArrayWrapper _p;
        int _index;
        long _timeout;
        PollOut _pollOut;

        PollThread() {
            setDaemon(true);
            setName("PollThread-" + _nextWorkerId++);
        }

        static synchronized PollThread getThread(VirtualFileSystem[] vfsArray, PollArrayWrapper p, int index, long timeout, PollOut pollOut) {
            PollThread result = null;
            for (int i = 0; i < _workers.size(); i++) {
                final PollThread thread = _workers.get(i);
                if (thread.idle()) {
                    result = thread;
                    break;
                }
            }
            if (result == null) {
                result = new PollThread();
                _workers.add(result);
                result.start();
            }
            // this sets the thread going on the task
            result.setInfo(vfsArray, p, index, timeout, pollOut);
            return result;
        }

        static void cancelThreads(PollThread[] pollThreads, PollOut pollOut) {
            // assert: hold pollOut monitor
            for (PollThread pollThread : pollThreads) {
                synchronized (pollThread) {
                    // if it was working for caller, interrupt it, unless it was the winner
                    if (pollThread._pollOut == pollOut && pollThread._index != pollOut._index) {
                        // slog("cancelling " + pollThread.getName());
                        pollThread.interrupt();
                    }
                }
            }
        }


        private synchronized void setInfo(VirtualFileSystem[] vfsArray, PollArrayWrapper p, int index, long timeout, PollOut pollOut) {
            _vfsArray = vfsArray;
            _p = p;
            _index = index;
            _timeout = timeout;
            _pollOut = pollOut;
            // slog("setInfo: [" + getName() + "]: " + index + ", I: " + isInterrupted());
        }

        synchronized boolean idle() {
            return _pollOut == null;
        }

        private synchronized void release() {
            // log("release: [" + getName() + "]");
            notify();
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    while (_pollOut == null) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            // for now we don't care; this could be used as
                            // part of a mechanism to terminate a worker thread
                        }
                    }
                }
                // log("running: " + Integer.toHexString(_p.getDescriptor(_index)) + ", " + _p.getEventOps(_index) + ", " + _timeout);
                try {
                    final int reventOps = _vfsArray[_index].poll0(VirtualFileSystemId.getFd(_p.getDescriptor(_index)), _p.getEventOps(_index), _timeout);
                    // log("poll returned: " + reventOps);
                    synchronized (_pollOut) {
                        if (_pollOut._index < 0 && match(_p, _index, reventOps) > 0) {
                            _pollOut._reventOps = reventOps;
                            _pollOut._index = _index;
                        }
                        // wake up the coordinator - either there was a match or we are the last thread
                        _pollOut._waiterCount--;
                        _pollOut.notify();
                    }
                } finally {
                    // we are done whatever
                    // log("finally");
                    synchronized (this) {
                        _pollOut = null;
                    }
                }
            }
        }

        /*
        private void log(String msg) {
            Log.println("[" + Thread.currentThread().getName() + "]: " + msg);
        }
        */
    }

    /*
    private static void slog(String msg) {
        Log.println(msg);
    }
    */


    private static final ByteBuffer _fakeBuffer = ByteBuffer.allocate(1);

    public static void interrupt(int fd) throws IOException {
        final VirtualFileSystem vfs = VirtualFileSystemId.getVfs(fd);
        // Following the native implementation of PollArrayWrapper, we write one byte to fd.
        // This all seems a bit convoluted, there must be a better way.
        _fakeBuffer.position(0);
        _fakeBuffer.put((byte) 1);
        vfs.writeBytes(VirtualFileSystemId.getFd(fd), _fakeBuffer, 0);
    }

}
