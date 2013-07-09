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
package com.sun.max.ve.tools.trace;


public class AllocPagesTraceElement extends AllocTraceElement {
    // Must match com.sun.max.memory.VirtualMemory.Type
    public enum MemType {
        HEAP,               // for the garbage collected heap
        STACK,             // for thread stacks
        CODE,               // for compiled code
        DATA,               // for miscellaneous data
        PAGE_FRAME  // page table frame
    }

    private static int _pageSize = 4096;
    private int _pages;
    private MemType _memType;
    private int _firstFreePage;
    private int _hwmAllocPage;

    public void setPages(int order) {
        _pages = order;
    }

    public int getPages() {
        return _pages;
    }

    @Override
    public int getAdjSize() {
        return getPages() * _pageSize;
    }

    public static int getPageSize() {
        return _pageSize;
    }

    public void setType(int memTypeOrdinal) {
        switch (memTypeOrdinal) {
            case 0:
                _memType = MemType.HEAP;
                break;
            case 1:
                _memType = MemType.STACK;
                break;
            case 2:
                _memType = MemType.CODE;
                break;
            case 3:
                _memType = MemType.DATA;
                break;
            case 4:
                _memType = MemType.PAGE_FRAME;
                break;
            default:
                throw new IllegalArgumentException("invalid MemType ordinal " + memTypeOrdinal);
        }
    }

    public MemType getType() {
        return _memType;
    }

    public void setFirstFreePage(int firstFreePage) {
        _firstFreePage = firstFreePage;
    }

    public int getFirstFreePage() {
        return _firstFreePage;
    }

    public void setHwmAllocPage(int hwmAllocPage) {
        _hwmAllocPage = hwmAllocPage;
    }

    public int getHwmAllocPage() {
        return _hwmAllocPage;
    }

    @Override
    public String toString() {
        return super.toString() + " " + _pages + " " + _memType.ordinal();
    }

}
