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
package com.sun.max.tele.debug.maxve.xen.dump;

import java.io.IOException;

import com.sun.max.tele.debug.maxve.xen.AbstractX64PageTableAccess;
import com.sun.max.unsafe.Address;


/**
 * @author Puneeet Lakhina
 *
 */
public class CoreDumpPageTableAccess extends AbstractX64PageTableAccess {

    private XenCoreDumpELFReader dumpReader;
    public CoreDumpPageTableAccess(XenCoreDumpELFReader dumpReader) {
        this.dumpReader = dumpReader;
    }


    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getMfnForPfn(long)
     */
    @Override
    public long getMfnForPfn(long pfn) throws IOException {
        // Pfn starts at 0
        if (pfn < getNoOfPages()) {
            return dumpReader.getPagesSection().getPageInfoForPfn(pfn).getGmfn();
        } else {
            throw new IndexOutOfBoundsException("page frame index " + pfn + " is out of range");
        }
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getNoOfPages()
     */
    @Override
    public long getNoOfPages() throws IOException {
        return dumpReader.getNoOfPages();
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getPTEntryAtIndex(com.sun.max.unsafe.Address, int)
     */
    @Override
    public long getPTEntryAtIndex(Address table, int index)throws IOException {
        return dumpReader.getPagesSection().getX64WordAtOffset(table.toLong() + (index * 8));
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getPageTableBase()
     */
    @Override
    public Address getPageTableBase()throws IOException {
        return getAddressForPte((dumpReader.getGuestContext(0).getCtrlreg()[3]));
    }


    /* (non-Javadoc)
     * @see com.sun.max.tele.page.PageTableAccess#getPfnForMfn(long)
     */
    @Override
    public long getPfnForMfn(long mfn)throws IOException {
        return dumpReader.getPagesSection().getPageInfoForMfn(mfn).getPfn();
    }

}
