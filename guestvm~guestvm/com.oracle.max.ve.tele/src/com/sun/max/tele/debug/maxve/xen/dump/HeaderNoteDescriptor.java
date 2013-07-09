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

import com.sun.max.tele.debug.maxve.xen.dump.NotesSection.DescriptorType;


/**
 * @author Puneeet Lakhina
 *
 */
public class HeaderNoteDescriptor extends NotesSectionDescriptor {

    static enum DomainType {
        PARAVIRTUALIZED, FULL_VIRTUALIZED;

        private static final long PVDOMAIN_MAGIC_NUMBER = 0xF00FEBEDL;
        private static final long FVDOMAIN_MAGIC_NUMBER = 0xF00FEBEEL;

        public static DomainType getType(long magicNumber) {
            if (magicNumber == PVDOMAIN_MAGIC_NUMBER) {
                return PARAVIRTUALIZED;
            } else if(magicNumber == FVDOMAIN_MAGIC_NUMBER) {
                return FULL_VIRTUALIZED;
            }else {
                throw new IllegalArgumentException("Improper Magic Number");
            }
        }
    };

    /*
     * The domain type depends on the magic number.
     *
     */
    private long magicnumber;
    //vpus is uint64 but we use int here.
    private int vcpus;
    private long noOfPages;
    private long pageSize;
    public HeaderNoteDescriptor() {
        super(DescriptorType.HEADER);
    }




    /**
     * @return the magicnumber
     */
    public long getMagicnumber() {
        return magicnumber;
    }




    /**
     * @param magicnumber the magicnumber to set
     */
    public void setMagicnumber(long magicnumber) {
        this.magicnumber = magicnumber;
    }




    /**
     * @return the domainType
     */
    public DomainType getDomainType() {
        return DomainType.getType(magicnumber);
    }

    /**
     * @return the vcpus
     */
    public int getVcpus() {
        return vcpus;
    }




    /**
     * @param vcpus the vcpus to set
     */
    public void setVcpus(int vcpus) {
        this.vcpus = vcpus;
    }




    /**
     * @return the noOfPages
     */
    public long getNoOfPages() {
        return noOfPages;
    }




    /**
     * @param noOfPages the noOfPages to set
     */
    public void setNoOfPages(long noOfPages) {
        this.noOfPages = noOfPages;
    }




    /**
     * @return the pageSize
     */
    public long getPageSize() {
        return pageSize;
    }




    /**
     * @param pageSize the pageSize to set
     */
    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }



    @Override
    public String toString() {
        return "Domain Type: [" + getDomainType() +"] , No of Pages: [" + noOfPages + "], Page Size: [" + pageSize + "],  VCpus: [" + vcpus + "]";
    }

}
