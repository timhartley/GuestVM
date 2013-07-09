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
 * A test for whether spin locks prevent pre-emptive scheduling.
 * The Looper thread is started and waits for the Spinner, then loops asserting that the spinner is not running.
 * The Spinner grabs the spin lock and loops for the given running time and then releases the lock.
 * If the Spinner is pre-empted, the Looper will run and output a message to that effect.
 *
 * N.B. You must run this test with VCPUS == 1!
 *
 * The P variety of spinlocks, e.g., com.sun.max.ve.spinlock.tas.p.PTTASSpinLock
 * should produce the message All others should not.
 *
 * @author Mick Jordan
 *
 */
public class SpinLockPETest {
    private static int _runTime = 10000;
    private static final String SPINLOCK_PKG = "com.sun.max.ve.spinlock.";
    private static volatile boolean _spinnerRunning;
    private static SpinLock _spinLock;


    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String klass = "ukernel.UKernelSpinLock";
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("r")) {
                _runTime = Integer.parseInt(args[++i]) * 1000;
            } else if (arg.equals("c")) {
                klass = args[++i];
            }
        }
        // Checkstyle: resume modified control variable check

        final SpinLockFactory factory = (SpinLockFactory) Class.forName(SPINLOCK_PKG + klass + "Factory").newInstance();
        SpinLockFactory.setInstance(factory);
        _spinLock = SpinLockFactory.createAndInit();

        final Thread looper =  new Looper();
        looper.setDaemon(true);
        looper.start();
        final Spinner spinner = new Spinner();
        spinner.start();
        spinner.join();
    }

    static class Looper extends Thread {

        @Override
        public void run() {
            // wait for spinner to start;
            while (!_spinnerRunning) {
                continue;
            }
            // the only way we could get here is if the spinner is pre-empted or done.
            while (true) {
                if (_spinnerRunning) {
                    System.out.println("Looper running while Spinner holds lock");
                    break;
                }
            }
        }
    }

    static class Spinner extends Thread {
        @Override
        public void run() {
            final long start = System.currentTimeMillis();
            long now = start;
            _spinLock.lock();
            _spinnerRunning = true;
            while (now < start + _runTime) {
                now = System.currentTimeMillis();
            }
            _spinnerRunning = false;
            _spinLock.unlock();
        }

    }

}
