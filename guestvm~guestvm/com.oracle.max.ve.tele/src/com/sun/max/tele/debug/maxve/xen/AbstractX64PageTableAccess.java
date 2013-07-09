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
package com.sun.max.tele.debug.maxve.xen;

import java.io.*;

import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.x64.*;


/**
 * @author Mick Jordan
 * @author Puneeet Lakhina
 *
 */
public abstract class AbstractX64PageTableAccess implements PageTableAccess {

    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getMfnForAddress(com.sun.max.unsafe.Address)
     */

    protected Address basePageTableAddress = null;
    @Override
    public long getMfnForAddress(Address address)throws IOException {
        return getPfnForMfn(address.toLong() >> X64VM.L1_SHIFT);
    }

    @Override
    public int getNumPTEntries(int level) {
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
            throw new IllegalArgumentException("illegal page table level: "+ level);
        }
    }

    @Override
    public Address getAddressForPfn(long pfn) {
        return Address.fromLong(pfn << X64VM.L1_SHIFT);
    }
    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getPTIndex(com.sun.max.unsafe.Address, int)
     */
    @Override
    public final int getPTIndex(Address address, int level) {
        final long a = address.toLong();
        long result;
        switch (level) {
        case 1:
            result = (a >> X64VM.L1_SHIFT) & (X64VM.L1_ENTRIES - 1);
            break;
        case 2:
            result = (a >> X64VM.L2_SHIFT) & (X64VM.L2_ENTRIES - 1);
            break;
        case 3:
            result = (a >> X64VM.L3_SHIFT) & (X64VM.L3_ENTRIES - 1);
            break;
        case 4:
            result = (a >> X64VM.L4_SHIFT) & (X64VM.L4_ENTRIES - 1);
            break;
        default:
            throw new IllegalArgumentException("illegal page table level: "
                    + level);
        }
        return (int) result;
    }

//    public final long getPhysicalAddressForVirtualAddress(Address address)throws IOException {
//      long pte = getPteForAddress(address);
//      Address l0PageTableAddress = getAddressForPte(pte);
//      final int index = getPTIndex(address, 0);
//      return getPTEntryAtIndex(l0PageTableAddress, index);
//    }
    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getPteForAddress(com.sun.max.unsafe.Address)
     */
    @Override
    public final long getPteForAddress(Address address)throws IOException {
        if(basePageTableAddress == null) {
            basePageTableAddress = getPageTableBase();
        }
        Address table = basePageTableAddress;
        long pte = 0;
        int level = 4;
        while (level > 0) {
            final int index = getPTIndex(address, level);
            pte = getPTEntryAtIndex(table, index);
            if (!PageTableUtil.isPresent(pte)) {
                throw new PteNotPresentException("page table entry at index " + index + " in level " + level + " is not present");
            }
            table = getAddressForPte(pte);
            level--;
        }
        return pte;
    }

    @Override
    public Address getAddressForPte(long pte) throws IOException {
        return Address.fromLong(getPfnForPte(pte) << X64VM.PAGE_SHIFT);
    }

    @Override
    public long getPfnForPte(long pte)throws IOException {
        return getPfnForMfn((pte & X64VM.PADDR_MASK & X64VM.PAGE_MASK) >> X64VM.PAGE_SHIFT);
    }

    @Override
    public long getPfnForAddress(Address address) {
        return address.toLong() >> X64VM.L1_SHIFT;
    }

}
