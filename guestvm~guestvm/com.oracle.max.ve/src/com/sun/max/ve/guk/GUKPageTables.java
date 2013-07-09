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
package com.sun.max.ve.guk;

import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.x64.*;

/**
 * Interface to virtual (and physical) memory, pages tables etc.
 *
 * @author Mick Jordan
 *
 */

public class GUKPageTables {
    // cache of getCurrentReservation
    private static long _numPages = 0;

    /*
     * Update the cache of _numPages
     */
    static void updateNumPages() {
        _numPages = GUKPagePool.getCurrentReservation();
    }

    /**
     * Return the machine frame mapped to the given physical frame.
     * @param pfn physical frame number
     * @return machine frame number
     */
    public static long getMfnForPfn(long pfn) {
        if (_numPages == 0) {
            _numPages = GUKPagePool.getCurrentReservation();
        }
        if (pfn < _numPages) {
            return GUK.guk_pfn_to_mfn(pfn);
        } else {
            throw new IndexOutOfBoundsException("page frame index " + pfn + " is out of range, max: " + _numPages);
        }
    }

    /**
     * Return the machine frame corresponding to the given virtual address.
     * @param address
     * @return machine frame
     */
    public static long getMfnForAddress(Address address) {
        return  getMfnForPfn(address.toLong() >> X64VM.L1_SHIFT);
    }

    public static long getPfnForAddress(Address address) {
        return address.toLong() >> X64VM.L1_SHIFT;
    }

    public static Address getAddressForPfn(long pfn) {
        return Address.fromLong(pfn << X64VM.L1_SHIFT);
    }

   /**
     * Get the entry at a given index in a given page table.
     * @param table virtual address of table base
     * @param index index into table
     * @return
     */
    public static long getPTEntryAtIndex(Address table, int index) {
        return table.asPointer().getLong(index);
    }

    /**
     * Get number of page tables entries in a page frame at a given level.
     * @param level
     * @return
     */
    public static int getNumPTEntries(int level) {
        switch (level) {
            case 1:
                return X64VM.L1_ENTRIES;
            case 2:
                return X64VM.L2_ENTRIES;
            case 3:
                return X64VM.L3_ENTRIES;
            case 4:
                return X64VM.L4_ENTRIES;
            default:
                throw new IllegalArgumentException("illegal page table level: " + level);
        }
    }

    public static int getState(long pte) {
        return (int) (pte & ~X64VM.PAGE_MASK);
    }

    public static boolean isPresent(long pte) {
        return (pte & X64VM.PAGE_PRESENT) != 0;
    }

    public static boolean isWritable(long pte) {
        return (pte & X64VM.PAGE_RW) != 0;
    }

    public static boolean isUser(long pte) {
        return (pte & X64VM.PAGE_USER) != 0;
    }

    public static boolean isAccessed(long pte) {
        return (pte & X64VM.PAGE_ACCESSED) != 0;
    }

    public static boolean isDirty(long pte) {
        return (pte & X64VM.PAGE_DIRTY) != 0;
    }

    public static long getMfnForPte(long pte) {
        return (pte & X64VM.PADDR_MASK & X64VM.PAGE_MASK) >> X64VM.PAGE_SHIFT;
    }

    public static long getPfnForPte(long pte) {
        return getPfnForMfn((pte & X64VM.PADDR_MASK & X64VM.PAGE_MASK) >> X64VM.PAGE_SHIFT);
    }

    public static Address getAddressForPte(long pte) {
        return Address.fromLong(getPfnForPte(pte) << X64VM.PAGE_SHIFT);
    }

    /**
     * Return the page table entry for a given address.
     * Requires walking the page table structure.
     * @param address
     * @return
     */
    public static long getPteForAddress(Address address) {
        Address table = getPageTableBase(); // level 4 table
        long pte = 0;
        int level = 4;
        while (level > 0) {
            final int index = GUKPageTables.getPTIndex(address, level);
            pte = GUKPageTables.getPTEntryAtIndex(table, index);
            if (!GUKPageTables.isPresent(pte)) {
                throw new PteNotPresentException(pte, level, index);
            }
            table = GUKPageTables.getAddressForPte(pte);
            level--;
        }
        return pte;
    }

  /**
     * Return the index into the given page table for given address.
     * @param address virtual address
     * @param level the page table level
     */
    public static int getPTIndex(Address address, int level) {
        final long a = address.toLong();
        long result;
        switch (level) {
            case 1:
                result =  (a >> X64VM.L1_SHIFT) & (X64VM.L1_ENTRIES - 1);
                break;
            case 2:
                result =  (a >> X64VM.L2_SHIFT) & (X64VM.L2_ENTRIES - 1);
                break;
            case 3:
                result =  (a >> X64VM.L3_SHIFT) & (X64VM.L3_ENTRIES - 1);
                break;
            case 4:
                result =  (a >> X64VM.L4_SHIFT) & (X64VM.L4_ENTRIES - 1);
                break;
            default:
                throw new IllegalArgumentException("illegal page table level: " + level);
        }
        return (int) result;
    }

    /**
     * Return the physical frame that is mapped to the given machine frame.
     * @param mfn machine frame number
     * @return physical frame number
     */
    public static long getPfnForMfn(long mfn) {
        return GUK.guk_mfn_to_pfn(mfn);
    }

    public static Address getPageTableBase() {
        return Address.fromLong(GUK.guk_pagetable_base());
    }

    public static long allocate_2mb_machine_pages(int n, int type) {
        return GUK.guk_allocate_2mb_machine_pages(n, type);
    }

    @SuppressWarnings("serial")
    public static class PteNotPresentException extends RuntimeException {
        public final long _pte;
        public final int _level;
        public final int _index;
        PteNotPresentException(long pte, int level, int index) {
            super();
            this._pte = pte;
            this._level = level;
            this._index = index;
        }

    }
}
