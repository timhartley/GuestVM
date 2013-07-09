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
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import com.oracle.max.elf.ELFDataInputStream;
import com.oracle.max.elf.ELFHeader;
import com.oracle.max.elf.ELFSectionHeaderTable;


/**
 * @author Puneeet Lakhina
 *
 */
public class PagesSection {

    private RandomAccessFile raf;
    private ELFSectionHeaderTable.Entry pageSectionHeader;
    private ELFSectionHeaderTable.Entry p2mSectionHeader;
    private ELFHeader elfHeader;
    private long noOfPages;
    private Map<Long, PageInfo> mfnPageInfoMap = new HashMap<Long, PageInfo>();
    private long pageSize;

    public PagesSection(RandomAccessFile raf,ELFSectionHeaderTable.Entry pageSectionHeader,ELFSectionHeaderTable.Entry p2mSectionHeader, ELFHeader elfHeader,long noOfPages,long pageSize) {
        this.raf = raf;
        this.pageSectionHeader = pageSectionHeader;
        this.p2mSectionHeader = p2mSectionHeader;
        this.elfHeader=elfHeader;
        this.noOfPages = noOfPages;
        this.pageSize = pageSize;
    }

    public long getX64WordAtOffset(long sectionLocalOffset)throws IOException {
        return getDataInputStream(sectionLocalOffset).read_Elf64_XWord();
    }
    private ELFDataInputStream getDataInputStream(long sectionLocalOffset) throws IOException {
        raf.seek(pageSectionHeader.getOffset()+sectionLocalOffset);
        return new ELFDataInputStream(elfHeader, raf);
    }

    public int readBytes(long address,byte[] dst, int dstOffset,int length)throws IOException {
        if(address > pageSectionHeader.getSize()) {
            throw new IllegalArgumentException("Improper address:" + address + " Size is:" + pageSectionHeader.getSize());
        }
        raf.seek(pageSectionHeader.getOffset() + address);
        return raf.read(dst, dstOffset, length);
    }
    /**
     * Get the page info corresponding to this pseudo physical pfn.
     * @param pfn
     * @return
     */
    public PageInfo getPageInfoForPfn(long pfn)throws IOException {
        raf.seek(p2mSectionHeader.getOffset()+pfn * 16);
        PageInfo pageInfo = new PageInfo();
        ELFDataInputStream dataInputStream = new ELFDataInputStream(elfHeader,raf);
        pageInfo.setPfn(dataInputStream.read_Elf64_Addr());
        pageInfo.setGmfn(dataInputStream.read_Elf64_Addr());
        if(!pageInfo.isValid()) {
            return null;
        }
        if(pageInfo.getPfn() != pfn) {
            throw new RuntimeException("Improper read.The pfn at the offset doesnt match.");
        }
        return pageInfo;
    }

    /**
     * Get the page info corresponding to this pseudo physical pfn.
     * @param pfn
     * @return
     */
    public PageInfo getPageInfoForMfn(long mfn)throws IOException {
        if(mfnPageInfoMap.get(mfn) != null ) {
            return mfnPageInfoMap.get(mfn);
        }
        raf.seek(p2mSectionHeader.getOffset() + mfnPageInfoMap.size() * 16);
        ELFDataInputStream dataInputStream = new ELFDataInputStream(elfHeader,raf);
        for(int i=mfnPageInfoMap.size();i<noOfPages;i++) {
            PageInfo pageInfo = new PageInfo();
            pageInfo.setPfn(dataInputStream.read_Elf64_XWord());
            pageInfo.setGmfn(dataInputStream.read_Elf64_XWord());
            if(!pageInfo.isValid()) {
                continue;
            }
            mfnPageInfoMap.put(pageInfo.getGmfn(), pageInfo);
            if(pageInfo.getGmfn() == mfn) {
                return pageInfo;
            }
        }
        throw new RuntimeException("Mfn "+Long.toHexString(mfn) + " not found");
    }
}
