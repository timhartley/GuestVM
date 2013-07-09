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
package com.sun.max.ve.memory;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.GUKBitMap;

/**
 * This is an interface to the pool of virtual memory that is used for thread stacks.
 *
 * @author Mick Jordan
 *
 */
public class StackPool {

    @INLINE
    public static Address getBase() {
        return maxve_stackPoolBase();
    }

    @INLINE
    public static int getSize() {
        return maxve_stackPoolSize();
    }

    public static long getRegionSize() {
        return maxve_stackRegionSize();
    }

    public static boolean isAllocated(int slot) {
        if (_bitMap.isZero()) {
            _bitMap = maxve_stackPoolBitmap();
        }
        return GUKBitMap.isAllocated(_bitMap, slot);
    }

    public static Pointer getSpinLock() {
        return maxve_stackPoolSpinLock();
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static Pointer _bitMap;

    @C_FUNCTION
    private static native Address maxve_stackPoolBase();

    @C_FUNCTION
    private static native int maxve_stackPoolSize();

    @C_FUNCTION
    private static native Pointer maxve_stackPoolBitmap();

    @C_FUNCTION
    private static native long maxve_stackRegionSize();

    @C_FUNCTION
    private static native Pointer maxve_stackPoolSpinLock();

}

