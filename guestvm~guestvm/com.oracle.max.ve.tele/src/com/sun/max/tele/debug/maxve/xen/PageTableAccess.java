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


/**
 * @author Puneeet Lakhina
 *
 */
public interface PageTableAccess {

    /**
     * Get Total No of current pages.
     *
     * @return
     */
    long getNoOfPages()throws IOException;

    /**
     * Get machine frame number for pseudo physical frame number.
     *
     * @param pfn
     * @return
     */
    long getMfnForPfn(long pfn)throws IOException;

    /**
     * Return the machine frame corresponding to the given virtual address.
     *
     * @param address
     * @return machine frame
     */
    long getMfnForAddress(Address address)throws IOException;

    /**
     * Get Pseudo Physical Frame Number for this address.
     *
     * @param address
     * @return
     */
    long getPfnForAddress(Address address)throws IOException;

    Address getAddressForPfn(long pfn)throws IOException;

    /**
     * Get the entry at a given index in a given page table.
     *
     * @param table virtual address of table base
     * @param index index into table
     * @return
     */
    long getPTEntryAtIndex(Address table, int index)throws IOException;

    /**
     * Get number of page tables entries in a page frame at a given level.
     *
     * @param level
     * @return
     */
    int getNumPTEntries(int level)throws IOException;

    /**
     * Return the page table entry for a given address. Requires walking the page table structure.
     *
     * @param address
     * @return
     */
    long getPteForAddress(Address address)throws IOException;

    /**
     * Return the index into the given page table for given address.
     *
     * @param address virtual address
     * @param level the page table level
     */
    int getPTIndex(Address address, int level)throws IOException;

    /**
     * Return the physical frame that is mapped to the given machine frame.
     *
     * @param mfn machine frame number
     * @return physical frame number
     */
    long getPfnForMfn(long mfn)throws IOException;

    /**
     * Get base address of Level 1 page table.
     *
     * @return
     */
    Address getPageTableBase()throws IOException;

    Address getAddressForPte(long pte)throws IOException;

    long getPfnForPte(long pte)throws IOException;


}
