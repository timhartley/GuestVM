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

import test.util.OSSpecific;

public class ThreadDeathTest implements Runnable {

    /**
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {
        int numThreads = 10;
        int lifeTime = 5;
        long sleep = 0;
        int interval = 1;
        boolean trace = false;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("n")) {
                numThreads = Integer.parseInt(args[++i]);
            } else if (arg.equals("t")) {
                lifeTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("s")) {
                sleep = Long.parseLong(args[++i]);
            } else if (arg.equals("r")) {
                trace = true;
            } else if (arg.equals("i")) {
                interval = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check

        if (sleep > 0) {
            System.out.println("Sleeping for " + sleep + " seconds");
            Thread.sleep(sleep * 1000);
        }

        if (trace) {
            OSSpecific.setTraceState(0, true);
        }

        System.out.println("Starting");
        final Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new ThreadDeathTest(i, lifeTime));
            threads[i].setName("AppThread-" + i);
            threads[i].start();
            Thread.sleep(interval * 1000);
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        if (sleep > 0) {
            System.out.println("Sleeping for " + sleep + " seconds");
            Thread.sleep(sleep * 1000);
        }
        System.out.println("Exiting");
    }

    private int _id;
    private int _lifeTime;
    ThreadDeathTest(int i, int lifeTime) {
        _id = i;
        _lifeTime = lifeTime;
    }

    public void run() {
        try {
            System.out.println("Thread " + _id + " running");
            Thread.sleep(_lifeTime * 1000);
            System.out.println("Thread " + _id + " terminating");
        } catch (InterruptedException ex) {
        }
    }

}
