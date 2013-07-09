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
package test.com.sun.max.ve.mm;

/**
 * This is a specific test for the page allocation algorithm in the kernel.
 * Assumes we are running with a 16MB semispace heap and 64MB of domain memory.
 * The large allocation should fail, but the small should succeed.
 *
 * @author Mick Jordan
 *
 */

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

public class VirtualMemoryAllocTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final Pointer p = VirtualMemory.allocate(Size.fromLong(32 * 1024 * 1024), VirtualMemory.Type.HEAP);
        System.out.println("Large allocation " + check(p));
        final Pointer q = VirtualMemory.allocate(Size.fromInt(1024 * 1024), VirtualMemory.Type.STACK);
        System.out.println("Small allocation " + check(q));
    }

    private static String check(Pointer p) {
        if (p.isZero()) {
            return "failed";
        } else {
            return "ok";
        }
    }

}
