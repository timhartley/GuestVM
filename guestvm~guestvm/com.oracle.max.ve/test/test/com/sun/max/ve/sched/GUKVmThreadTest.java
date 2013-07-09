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
package test.com.sun.max.ve.sched;

import com.sun.max.ve.sched.*;
import com.sun.max.ve.test.*;
import com.sun.max.vm.thread.*;
import com.sun.max.lang.*;

import test.util.*;

/**
 * Test the functionality of the {@link GukVMThread} interface.
 *
 * @author Mick Jordan
 *
 */
public class GUKVmThreadTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final ArgsHandler h = ArgsHandler.process(args);
        if (h._opCount == 0) {
            System.out.println("no operations given");
            return;
        }
        for (int j = 0; j < h._opCount; j++) {
            final String opArg1 = h._opArgs1[j];
            final String opArg2 = h._opArgs2[j];
            final String op = h._ops[j];

            try {
                final GUKVmThread vmThread = (GUKVmThread) VmThreadTestHelper.current();
                if (op.equals("runningTime")) {
                    System.out.println("current thread running time is " + vmThread.getRunningTime());
                } else if (op.equals("stack")) {

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }


}
