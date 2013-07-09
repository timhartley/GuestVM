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
package test.com.sun.max.ve.spinlock;

import com.sun.max.ve.spinlock.*;

/**
 * Simple test to measure spinlock performance.
 * A set of threads contend for a shared spinlock.
 * The work is represented by incrementing a per-thread counter
 * a variable number of times while the lock is held.
 *
 * Args: [t nt] [r nr] [f nf] [k klass]
 *
 * t: number of threads
 * r: runtime in seconds
 * f: number of times counter is incremented while lock is held
 * k: spin lock class to instantiate (omitting com.sun.max.ve.spinlock prefix)
 *
 * @author Mick Jordan
 *
 */
public class SpinLockTest implements Runnable {

    private static int _runTime = 10000;
    private static final String SPINLOCK_PKG = "com.sun.max.ve.spinlock.";
    private static boolean _done;
    private static SpinLock _spinLock;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        int nThreads = 2;
        int factor = 1;
        String klass = "ukernel.UKernelSpinLock";
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("t")) {
                nThreads = Integer.parseInt(args[++i]);
            } else if (arg.equals("r")) {
                _runTime = Integer.parseInt(args[++i]) * 1000;
            } else if (arg.equals("c")) {
                klass = args[++i];
            } else if (arg.equals("f")) {
                factor = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check

        final SpinLockFactory factory = (SpinLockFactory) Class.forName(SPINLOCK_PKG + klass + "Factory").newInstance();
        SpinLockFactory.setInstance(factory);
        _spinLock = SpinLockFactory.createAndInit();

        final Spinner[] threads = new Spinner[nThreads];
        for (int t = 0; t < nThreads; t++) {
            threads[t] = new Spinner(factor);
        }

        new Thread(new SpinLockTest()).start();

        for (int t = 0; t < nThreads; t++) {
            threads[t].start();
        }

        for (int t = 0; t < nThreads; t++) {
            threads[t].join();
        }
        long totalCount = 0;

        for (int t = 0; t < nThreads; t++) {
            System.out.println("thread " + t + ": " + threads[t].counter());
            totalCount += threads[t].counter();
        }
        System.out.println("total count: " + totalCount);

        if (_spinLock instanceof CountingSpinLock) {
            final CountingSpinLock cSpinLock = (CountingSpinLock) _spinLock;
            System.out.println("max spin count: " + cSpinLock.getMaxSpinCount());
        }

        _spinLock.cleanup();
    }

    public void run() {
        try {
            Thread.sleep(_runTime);
        } catch (InterruptedException ex) {

        }
        _done = true;
    }

    static class Spinner extends Thread {
        private long _counter;
        private int _factor;

        Spinner(int factor) {
            _factor = factor;
        }

        @Override
        public void run() {
            while (!_done) {
                _spinLock.lock();
                int f = _factor;
                while (f > 0) {
                    _counter++;
                    f--;
                }
                _spinLock.unlock();
            }
        }

        public long counter() {
            return _counter;
        }

    }

}
