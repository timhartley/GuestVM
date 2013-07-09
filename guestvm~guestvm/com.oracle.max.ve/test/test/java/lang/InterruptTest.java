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


public class InterruptTest  {

    /**
     * @param args
     */
    public static void main(String[] args) {
        boolean waitTest = false;
        boolean sleepTest = false;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("s")) {
                sleepTest = true;
            } else if (arg.equals("w")) {
                waitTest = true;
            }
        }
        if (waitTest) {
            final Thread waitInterruptee = new WaitInterruptee();
            waitInterruptee.setName("interruptee");
            waitInterruptee.start();
            waitInterruptee.interrupt();
            try {
                waitInterruptee.join();
            } catch (InterruptedException ex) {
                System.out.println("[" + Thread.currentThread().getName() + "] caught InterruptedException on join, status " + Thread.currentThread().isInterrupted());
            }
        }

        if (sleepTest) {
            final Thread sleepInterruptee = new SleepInterruptee();
            sleepInterruptee.setName("interruptee");
            sleepInterruptee.start();
            sleepInterruptee.interrupt();
            try {
                sleepInterruptee.join();
            } catch (InterruptedException ex) {
                System.out.println("[" + Thread.currentThread().getName() + "] caught InterruptedException on join, status " + Thread.currentThread().isInterrupted());
            }
        }
    }

    static class WaitInterruptee extends Thread {
        @Override
        public void run() {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    System.out.println("[" + Thread.currentThread().getName() + "] caught InterruptedException on wait, status " + Thread.currentThread().isInterrupted());
                }
            }
        }
    }

    static class SleepInterruptee extends Thread {
        @Override
        public void run() {
            synchronized (this) {
                try {
                    sleep(1000);
                } catch (InterruptedException ex) {
                    System.out.println("[" + Thread.currentThread().getName() + "] caught InterruptedException on sleep, status " + Thread.currentThread().isInterrupted());
                }
            }
        }
    }

}
