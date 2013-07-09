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



/**
 * This maps to to the xen_cumpcore_p2m struct. An array of this struct (array length = no of pages) is contained in the {@link XenCoreDumpELFReader#P2M_SECTION_NAME} section of the core dump. The {@link XenCoreDumpELFReader#PFN_SECTION_NAME} is for fully virtualized or ia64 domain both of which we done support.
 * @author Puneeet Lakhina
 *
 */
public class PageInfo {

    private long pfn;
    private long gmfn;

    /**
     * @return the pfn
     */
    public long getPfn() {
        return pfn;
    }

    /**
     * @param pfn the pfn to set
     */
    public void setPfn(long pfn) {
        this.pfn = pfn;
    }

    /**
     * @return the gmfn
     */
    public long getGmfn() {
        return gmfn;
    }

    /**
     * @param gmfn the gmfn to set
     */
    public void setGmfn(long gmfn) {
        this.gmfn = gmfn;
    }

    @Override
    public String toString() {
        return String.format("[pfn=%s , mfn = %s ",Long.toHexString(pfn),Long.toHexString(gmfn)+"]");
    }

    @Override
    public int hashCode() {
        //This is based on Long.hashCode + Iconstant and iTotal from apache commons-lang
        return (17*37 + ((int)(this.gmfn ^ (this.gmfn >>> 32)))) + (17*37 + ((int)(this.pfn ^ (this.pfn >>> 32))));
    }

    @Override
    public boolean equals(Object other) {
        if(other != null && other instanceof PageInfo) {
            PageInfo otherPage = (PageInfo)other;
            return this.isValid() && otherPage.isValid() && this.pfn == otherPage.pfn && this.gmfn == otherPage.gmfn;
        }
        return false;
    }

    public boolean isValid() {
      return !(this.gmfn == (~0) || this.pfn == (~0));
    }

}
