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
package com.sun.max.ve.jdk;

import com.sun.max.annotate.*;
import com.sun.max.vm.jni.JVMFunctions;

/**
 * Substitutions for the @see sun.misc.VM class.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(sun.misc.VM.class)
final class JDK_sun_misc_VM {
    @SUBSTITUTE
    private static void getThreadStateValues(int[][] vmThreadStateValues,
                    String[][] vmThreadStateNames) {
        /*
         * As I understand the purpose of this method, it is to allow for a finer grain
         * set of thread status values than provided by Thread.State and to map those
         * fine grain values to Thread.State values. E.g., there could be several reasons for
         * WAITING, each with their own integer thread status, all of which would be mapped
         * to (the ordinal value of) WAITING.
         *
         * Currently, our map is 1-1.
         */
        final Thread.State[] ts = Thread.State.values();
        assert ts.length == vmThreadStateValues.length;
        for (int i = 0; i < vmThreadStateValues.length; i++) {
            vmThreadStateValues[i] = JVMFunctions.GetThreadStateValues(i);
            vmThreadStateNames[i] = JVMFunctions.GetThreadStateNames(i, vmThreadStateValues[i]);
        }
    }
}
