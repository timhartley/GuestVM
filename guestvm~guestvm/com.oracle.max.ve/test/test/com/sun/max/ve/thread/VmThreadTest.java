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
package test.com.sun.max.ve.thread;

import com.sun.max.ve.sched.*;
import com.sun.max.ve.test.VmThreadTestHelper;

/**
 * A class to test the various ways to get the current VmThread and the current native thread.
 *
 * @author Mick Jordan
 *
 */

public class VmThreadTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        long count = 1000000;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("c")) {
                count = Long.parseLong(args[++i]);
            }
        }

        System.out.println("current: " + VmThreadTestHelper.current());
        System.out.println("currentAsAddress: " + VmThreadTestHelper.currentAsAddress());
        System.out.println("idLocal: " + VmThreadTestHelper.idLocal());
        System.out.println("idCurrent: " + VmThreadTestHelper.idCurrent());
        System.out.println("nativeCurrent: " + Long.toHexString(VmThreadTestHelper.nativeCurrent()));
        System.out.println("nativeUKernel: " + Long.toHexString(VmThreadTestHelper.nativeUKernel()));

        System.out.println("timed current: " + new CurrentProcedure().timedRun(count) + "ns");
        System.out.println("timed nativeCurrent: " + new NativeCurrentProcedure().timedRun(count) + "ns");
        System.out.println("timed nativeUKernel: " + new NativeUKernelProcedure().timedRun(count) + "ns");
    }

    abstract static class Procedure {
        long timedRun(long count) {
            final long start = System.nanoTime();
            for (long i = 0; i < count; i++) {
                run();
            }
            return System.nanoTime() - start;
        }
        protected abstract void run();
    }

    static class CurrentProcedure extends Procedure {
        @Override
        protected void run() {
            VmThreadTestHelper.current();
        }
    }

     static class NativeCurrentProcedure extends Procedure {
        @Override
        protected void run() {
            VmThreadTestHelper.nativeCurrent();
        }
    }

    static class NativeUKernelProcedure extends Procedure {
        @Override
        protected void run() {
            VmThreadTestHelper.nativeUKernel();
        }
    }
}
