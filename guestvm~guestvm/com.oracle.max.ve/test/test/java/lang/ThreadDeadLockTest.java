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

import java.lang.management.*;


public class ThreadDeadLockTest {

    private static Lock lock1 = new Lock(1);
    private static Lock lock2 = new Lock(2);
    private static boolean verbose;

    private static class Lock {
        int id;
        Lock(int id) {
            this.id = id;
        }
        @Override
        public String toString() {
            return "lock-" + id;
        }
    }

    public static void main(String[] args) throws Exception {
        boolean deadLock = false;
        boolean dump = false;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("dl")) {
                deadLock = true;
            } else if (arg.equals("dump")) {
                dump = true;
            } else if (arg.equals("v")) {
                verbose = true;
            }
        }
        final DThread thread1 = new DThread(lock1, lock2);
        final DThread thread2 = new DThread(thread1, lock2, lock1);
        thread1.setOtherThread(thread2);
        thread1.start();
        thread2.start();
        System.out.println("waiting for threads to deadlock");
        Thread.sleep(1000);
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if (dump) {
            final long[] ids = new long[2];
            ids[0] = thread1.getId();
            ids[1] = thread2.getId();
            final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ids, true, false);
            for (int i = 0; i < ids.length; i++) {
                System.out.println("Stack for thread " + threadInfos[i].getThreadName());
                final StackTraceElement[] stackTrace = threadInfos[i].getStackTrace();
                for (int j = 0; j < stackTrace.length; j++) {
                    System.out.println("\tat " + stackTrace[j]);
                }
                final MonitorInfo[] monitorInfos = threadInfos[i].getLockedMonitors();
                System.out.println("Thread id: " + ids[i] + " locked monitors");
                for (MonitorInfo monitorInfo : monitorInfos) {
                    System.out.println("  hc: " + monitorInfo.getIdentityHashCode() + " class: " + monitorInfo.getClassName() + " depth: " + monitorInfo.getLockedStackDepth());
                }
            }
        }
        if (deadLock) {
            final long[] ids = threadMXBean.findMonitorDeadlockedThreads();
            if (ids == null) {
                System.out.println("findMonitorDeadlockedThreads returned null");
            } else {
                for (int i = 0; i < ids.length; i++) {
                    System.out.println("Thread id: " + ids[i] + " is monitor deadlocked");
                }
            }
        }
    }

    static class DThread extends Thread {
        private static int id = 1;
        protected boolean locked;
        protected DThread otherThread;
        private Lock lock1;
        private Lock lock2;

        DThread(Lock lock1, Lock lock2) {
            this(null,lock1, lock2);
        }

        DThread(DThread otherThread, Lock lock1, Lock lock2) {
            this.otherThread = otherThread;
            this.lock1 = lock1;
            this.lock2 = lock2;
            setDaemon(true);
            setName("Locker-" + id++);
        }

        void setOtherThread(DThread otherThread) {
            this.otherThread = otherThread;
        }

        @Override
        public void run() {
            assert otherThread != null;
            synchronized (lock1) {
                if (verbose) {
                    log(" acquired " + lock1);
                }
                synchronized (this) {
                    locked = true;
                    notify();
                }
                // wait for thread2 to acquire lock2
                synchronized (otherThread) {
                    while (!otherThread.locked) {
                        try {
                            otherThread.wait(1000);
                        } catch (InterruptedException ex) {

                        }
                    }
                }
                // now deadlock
                if (verbose) {
                    log(" other thread acquired " + lock1 + ", trying for " + lock2);
                }
                synchronized (lock2) {
                    if (verbose) {
                        log(" acquired " + lock2);
                    }
                }
            }
        }

        private void log(String m) {
            System.out.println(System.nanoTime() + ": " + getName() + m);
        }
    }


 }
