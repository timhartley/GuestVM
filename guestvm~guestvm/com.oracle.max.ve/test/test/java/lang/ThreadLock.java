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

/**
 * A class that can be used to understand, using the Inspector, exactly what happens
 * when multiple threads contend for a lock.
 *
 * @author Mick Jordan
 *
 */

public class ThreadLock {

    private static int _sleeptime = 0;
    public static void main(String[] args) {
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("s")) {
                _sleeptime = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check
        final Thread t1 = new Thread(new Locker(1));
        final Thread t2 = new Thread(new Locker(2));
        final Thread t3 = new Thread(new Locker(3));
        t1.start();
        t2.start();
        t3.start();
    }

    static class Locker  implements Runnable {
        private static Object _lock = new Object();
        private int _id;
        Locker(int i) {
            _id = i;
        }
        public void run() {
            System.out.println("Locker " + _id + " going for lock");
            synchronized (_lock) {
                System.out.println("Locker " + _id + " got lock");
                try {
                    Thread.sleep(_sleeptime * 1000);
                } catch (InterruptedException ex) {

                }
            }
        }

    }

}
