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
package test.java.lang;

import java.util.*;

import test.util.OSSpecific;

/**
 * How many threads can we run?
 * Args:
 * t n     number of threads to (try to) run (default {@value ThreadScaleTest#MAX_THREADS}
 * s n    how long (secs) should each thread sleep between wakeups (default {@value ThreadScaleTest#DEFAULT_SLEEPTIME}
 * r n     how long (decs) should each thread run (default {@value ThreadScaleTest#DEFAULT_RUNTIME}
 *
 * @author Mick Jordan
 *
 */
public final class ThreadScaleTest extends Thread {

    private static final int MAX_THREADS = 65536;
    private static final int DEFAULT_SLEEPTIME = 10;
    private static final int DEFAULT_RUNTIME = 60;
    private static int _sleepTime = DEFAULT_SLEEPTIME;
    private static int _runTime = DEFAULT_RUNTIME;
    private static boolean _verbose;

    /**
     * @param args
     */
    public static void main(String[] args) {
        int n = 0;
        int nmax = MAX_THREADS;

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("t")) {
                nmax = Integer.parseInt(args[++i]);
            } else if (arg.equals("s")) {
                _sleepTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("r")) {
                _runTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("v")) {
                _verbose = true;
            }  else if (arg.equals("gt")) {
                OSSpecific.setTraceState(Integer.parseInt(args[++i]), true);
            }
        }
        // Checkstyle: stop modified control variable check
        _sleepTime *= 1000;
        _runTime *= 1000;
       final List<Thread> threads = new ArrayList<Thread>();

        try {
            while (n < nmax) {
                ThreadScaleTest tst = new ThreadScaleTest(n);
                if (_verbose) {
                    System.out.println("created thread " + n);
                }
                threads.add(tst);
                tst.start();
                if (_verbose) {
                    System.out.println("started thread " + n);
                }
               n++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("created " + n + " threads");
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private int _id;
    private ThreadScaleTest(int t) {
        _id = t;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        final long end = now + _runTime;
        while (now < end) {
            try {
                if (_verbose) {
                    System.out.println("thread " + _id + " running");
                }
                Thread.sleep(_sleepTime);
                now = System.currentTimeMillis();
            } catch (InterruptedException ex) {

            }
        }
        if (_verbose) {
            System.out.println("thread " + _id + " exiting");
        }

    }

}
