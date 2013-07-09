/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

public class ThreadsDumpTest {

    private static boolean all = true;
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("one")) {
                all = false;
            } else if (args[i].equals("all")) {
                all = true;
            }
        }
        final Thread myThread = new MyThread();
        myThread.start();
        Thread.sleep(1000);
        if (all) {
            final Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
            for (Map.Entry<Thread, StackTraceElement[]> entry : traces.entrySet()) {
                System.out.println("Stack for thread " + entry.getKey().getName());
                final StackTraceElement[] trace = entry.getValue();
                for (int i = 0; i < trace.length; i++) {
                    System.out.println("\tat " + trace[i]);
                }
            }
        }
    }

    private static class MyThread extends Thread {
        MyThread() {
            setDaemon(all);
        }

        @Override
        public void run() {
            while (true) {
                if (!all) {
                    final StackTraceElement[] trace = this.getStackTrace();
                    System.out.println("Stack for thread " + this.getName());
                    for (int i = 0; i < trace.length; i++) {
                        System.out.println("\tat " + trace[i]);
                    }
                    return;
                }
            }
        }
    }

}
