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

import java.io.*;
import java.util.*;

import test.util.OSSpecific;

/**
 * A test to show that fp registers are preserved across thread switches.
 *
 * @author Mick Jordan
 *
 */
public class ThreadFloat extends Thread {

    private static volatile boolean _done;
    private static boolean _yield;
    /**
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {
        int nThreads = 2;
        int runtime = 10;
        int timeSlice = 0;
        boolean fsThread = false;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("t")) {
                nThreads = Integer.parseInt(args[++i]);
            } else if (arg.equals("r")) {
                runtime = Integer.parseInt(args[++i]);
            } else if (arg.equals("y")) {
                _yield = true;
            } else if (arg.equals("ts")) {
                timeSlice = Integer.parseInt(args[++i]);
            } else if (arg.equals("fs")) {
                fsThread = true;
            }
        }

        FSThread fst = null;
        if (fsThread) {
            fst = new FSThread();
            fst.start();
        }
        final Timer timer = new Timer(true);
        timer.schedule(new MyTimerTask(), runtime *1000);
        final ThreadFloat[] threads = new ThreadFloat[nThreads];
        for (int t = 0; t < nThreads; t++) {
            threads[t] = new ThreadFloat(t);
            threads[t].setName("T" + t);
            if (timeSlice > 0) {
                OSSpecific.setTimeSlice(threads[t], timeSlice);
            }
            threads[t].start();
        }
        for (int t = 0; t < nThreads; t++) {
            threads[t].join();
            System.out.println(threads[t] + ": " + threads[t]._count);
        }
        if (fsThread) {
            System.out.println("read " + fst.read() + " files");
        }
    }

    @Override
    public void run() {
        while (!_done) {
            double d1 = D1 * _id;
            double d2 = D2 * _id;
            double d3 = D3 * _id;
            double d4 = D4 * _id;
            double d5 = D5 * _id;
            double d6 = D6 * _id;
            double d7 = D7 * _id;
            double d8 = D8 * _id;
            if (_yield) {
                Thread.yield();
            }
            call(d1, d2, d3, d4, d5, d6, d7, d8);
        }
    }

    ThreadFloat(int id) {
        _id = id;
    }

    private static final double D1 = 1.0;
    private static final double D2 = 2.0;
    private static final double D3 = 3.0;
    private static final double D4 = 4.0;
    private static final double D5 = 5.0;
    private static final double D6 = 6.0;
    private static final double D7 = 7.0;
    private static final double D8 = 8.0;
    private int _id;
    private long _count;

    public void call(double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        if (d1 != D1 * _id || d2 != D2 * _id || d3 != D3 * _id || d4 != D4 * _id || d5 != D5 * _id || d6 != D6 * _id || d7 != D7 * _id || d8 != D8 * _id) {
            throw new IllegalArgumentException("thread arguments mismatch");
        }
        _count++;
    }

    static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            _done = true;
        }
    }

    static class FSThread extends Thread {
        private List<File> _files = new ArrayList<File>();
        private Random _random = new Random();
        private int _read;

        FSThread() {
            setDaemon(true);
            final File root = new File("/max.ve/ext2");
            addDir(root);
            System.out.println("found " + _files.size() + " files");
        }

        private void addDir(File dir) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    addDir(file);
                } else {
                    _files.add(file);
                }
            }
        }

        int read() {
            return _read;
        }

        @Override
        public void run() {
            while (true) {
                final int x = _random.nextInt(_files.size());
                final File file = _files.get(x);
                FileInputStream fs = null;
                try {
                    fs = new FileInputStream(file);
                    final byte[] buf = new byte[1024];
                    while (fs.read(buf) != -1) {
                    }
                    _read++;
                } catch (IOException ex) {
                    System.out.println(ex);
                } finally {
                    if (fs != null) {
                        try {
                            fs.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        }
    }

}
