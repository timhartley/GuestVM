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

public class ThreadYieldTest extends Thread {

    private static boolean _done;
    private static int _runTime = 60;
    private long _count;


    public static void main(String[] args) throws InterruptedException {

        int nThreads = 2;
        final Timer timer = new Timer(true);
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("t")) {
                nThreads = Integer.parseInt(args[++i]);
            } else if (arg.equals("r")) {
                _runTime = Integer.parseInt(args[++i]) * 1000;
            }
        }
        timer.schedule(new MyTimerTask(), _runTime * 1000);
        final ThreadYieldTest[] threads = new ThreadYieldTest[nThreads];
        for (int t = 0; t < nThreads; t++) {
            threads[t] = new ThreadYieldTest();
            threads[t].setName("T" + t);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        for (ThreadYieldTest thread : threads) {
            System.out.println(thread.getName() + ": " + thread._count);
        }
    }

    @Override
    public void run() {
        while (!_done) {
            _count++;
            Thread.yield();
        }
    }

    static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            _done = true;
        }
    }
}
