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
package com.sun.max.ve.guk.x64;

/**
 * Interface to the virtual memory features of the x64.
 *
 * @author Mick Jordan
 *
 */

public class X64VM {

    public static final int L1_SHIFT = 12;
    public static final int L2_SHIFT = 21;
    public static final int L3_SHIFT = 30;
    public static final int L4_SHIFT = 39;

    public static final long L1_MASK = (1L << L2_SHIFT) - 1;
    public static final long L2_MASK = (1L << L3_SHIFT) - 1;
    public static final long L3_MASK = (1L << L4_SHIFT) - 1;

    public static final int L0_ENTRIES = 4096;
    public static final int L1_ENTRIES = 512;
    public static final int L2_ENTRIES = 512;
    public static final int L3_ENTRIES = 512;
    public static final int L4_ENTRIES = 512;

    public static final int PAGE_SHIFT = L1_SHIFT;
    public static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    public static final int PAGE_OFFSET_MASK = PAGE_SIZE - 1;
    public static final int PAGE_MASK = ~PAGE_OFFSET_MASK;

    public static final int PAGE_PRESENT  = 0x001;
    public static final int PAGE_RW            = 0x002;
    public static final int PAGE_USER         = 0x004;
    public static final int PAGE_PWT          = 0x008;
    public static final int PAGE_PCD           = 0x010;
    public static final int PAGE_ACCESSED = 0x020;
    public static final int PAGE_DIRTY         = 0x040;
    public static final int PAGE_PAT            = 0x080;
    public static final int PAGE_PSE            = 0x080;
    public static final int PAGE_GLOBAL      = 0x100;

    public static final int PADDR_BITS = 52;
    public static final long PADDR_MASK = (1L << PADDR_BITS) - 1;

}
