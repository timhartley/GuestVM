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
package test.com.sun.max.ve.util;

import java.util.*;

import com.sun.max.ve.util.*;
public class TimeLimitedProcTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        long timeout = 1000;
        final boolean[] tests = new boolean[10];

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("t")) {
                tests[Integer.parseInt(args[++i])] = true;
            } else if (arg.equals("r")) {
                timeout = Integer.parseInt(args[++i]);
            }
        }

        final TimeLimitedProc tlp1 = new TimeLimitedProc1();
        final TimeLimitedProc tlp2 = new TimeLimitedProc2();

        if (tests[0]) {
            identifyTest(0);
            test(tlp1, timeout);
        }
        if (tests[1]) {
            identifyTest(1);
            test(tlp2, timeout);
        }
        if (tests[2]) {
            identifyTest(2);
            testInterrupt(tlp1, timeout);
        }
        if (tests[3]) {
            identifyTest(3);
            testReusable(timeout, new TimeLimitedProc[] {tlp1, tlp2});
        }
        if (tests[4]) {
            identifyTest(4);
            testReusable(timeout, new TimeLimitedProc[] {tlp1, new TimeLimitedProc1()});
        }
        if (tests[5]) {
            identifyTest(5);
            testReusable(timeout, new TimeLimitedProc[] {tlp2, new TimeLimitedProc2()});
        }
    }

    private static void identifyTest(int test) {
        System.out.println("test " + test + " starting");
    }
    static class TimeLimitedProc1 extends TimeLimitedProc {
        // waits for remaining, so will time out
        @Override
        public int proc(long remaining) throws InterruptedException {
            synchronized (this) {
                wait(remaining);
            }
            return 0;
        }
    }

    static class TimeLimitedProc2 extends TimeLimitedProc {
        // sleeps for half of remaining and then returns a result
        @Override
        public int proc(long remaining) throws InterruptedException {
            synchronized (this) {
                Thread.sleep(remaining / 2);
            }
            return terminate(1);
        }
    }

    static void test(TimeLimitedProc tlp, long timeout) {
        final long start = System.currentTimeMillis();
        tlp.run(timeout);
        final long now = System.currentTimeMillis();
        System.out.println("elapsed: " + (now - start));
    }

    static void testInterrupt(TimeLimitedProc tlp, long timeout) {
        final long start = System.currentTimeMillis();
        final TlpRunner tlpRunner = new TlpRunner(tlp, timeout);
        tlpRunner.start();
        try {
            Thread.sleep(timeout / 2);
            tlpRunner.interrupt();
            tlpRunner.join();
        } catch (InterruptedException ex) {

        }
        final long now = System.currentTimeMillis();
        System.out.println("elapsed: " + (now - start) + ", result " + tlpRunner._result);

    }

    static class TlpRunner extends Thread {
        private TimeLimitedProc _tlp;
        private long _timeout;
        int _result;
        TlpRunner(TimeLimitedProc tlp, long timeout) {
            _tlp = tlp;
            _timeout = timeout;
        }

        @Override
        public void run() {
            _result = _tlp.run(_timeout);
        }
    }

    static void testReusable(long timeout, TimeLimitedProc[] tlps) {
        final int count = tlps.length;
        final Handle handle = new Handle(count);
        final ReusableTlpRunner[] threads = new ReusableTlpRunner[count];
        final long start = System.currentTimeMillis();
        for (int t = 0; t < count; t++) {
            threads[t] = ReusableTlpRunner.getThread();
            threads[t].kick(tlps[t], handle, t, timeout);
        }
        synchronized (handle) {
            try {
                while (handle._waiterCount > 0 && handle._index < 0) {
                    handle.wait();
                    if (handle._index >= 0) {
                        break;
                    }
                }
                final long now = System.currentTimeMillis();
                System.out.println("elapsed: " + (now - start) + " handle.result: " + handle._result + ", handle.index: " + handle._index + ", handle.waitercount: " + handle._waiterCount);
            } catch (InterruptedException ex) {
                System.out.println("coordinator interrupted!");
            } finally {
                if (handle._waiterCount > 0) {
                    System.out.println("cancelling remaining");
                    ReusableTlpRunner.cancelThreads(threads, handle);
                }
            }
        }
    }

    static class Handle {
        int _waiterCount;
        int _index;
        int _result;
        Handle(int count) {
            _waiterCount = count;
            _index = -1;
        }
    }

    static class ReusableTlpRunner extends Thread {
        static List<ReusableTlpRunner> _workers = new ArrayList<ReusableTlpRunner>(0);
        static int _nextWorkerId;
        long _timeout;
        Handle _handle;
        TimeLimitedProc _tlp;
        int _index;
        int _id;

        ReusableTlpRunner(int id) {
            _id = id;
            setDaemon(true);
            setName("Reusable-" + id);
        }

        static synchronized ReusableTlpRunner getThread() {
            ReusableTlpRunner result = null;
            for (int i = 0; i < _workers.size(); i++) {
                final ReusableTlpRunner thread = _workers.get(i);
                if (thread._handle == null) {
                    result = thread;
                    break;
                }
            }
            if (result == null) {
                result = new ReusableTlpRunner(_nextWorkerId++);
                _workers.add(result);
                result.start();
            }
            return result;
        }

        static void cancelThreads(ReusableTlpRunner[] threads, Handle handle) {
            for (ReusableTlpRunner thread : threads) {
                synchronized (thread) {
                    // if it was working for caller, interrupt it
                    if (thread._handle == handle) {
                        thread.interrupt();
                    }
                }
            }
        }

        synchronized void kick(TimeLimitedProc tlp, Handle handle, int index, long timeout) {
            _handle = handle;
            _tlp = tlp;
            _timeout = timeout;
            _index = index;
            notify();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (this) {
                        while (_handle == null) {
                            wait();
                        }
                    }
                    final int result = _tlp.run(_timeout);
                    synchronized (_handle) {
                        if (_handle._index < 0 && result > 0) {
                            _handle._result = result;
                            _handle._index = _index;
                        }
                        // wake up the coordinator - either there was a match or we are the last thread
                        _handle._waiterCount--;
                        _handle.notify();
                    }
                } catch (InterruptedException ex) {

                } finally {
                    // we are done whatever
                    synchronized (this) {
                        _handle = null;
                    }
                }
            }
        }
    }

}
