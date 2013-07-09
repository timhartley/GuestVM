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
 * @author Puneeet Lakhina
 *
 */
public class TrapInfo {

    private short vector;
    private short flags;
    private int codeSelector;
    private long codeOffset;
    /**
     * @return the vector
     */
    public short getVector() {
        return vector;
    }

    /**
     * @param vector the vector to set
     */
    public void setVector(short vector) {
        this.vector = vector;
    }

    /**
     * @return the flags
     */
    public short getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(short flags) {
        this.flags = flags;
    }

    /**
     * @return the codeSelector
     */
    public int getCodeSelector() {
        return codeSelector;
    }

    /**
     * @param codeSelector the codeSelector to set
     */
    public void setCodeSelector(int codeSelector) {
        this.codeSelector = codeSelector;
    }

    /**
     * @return the codeOffset
     */
    public long getCodeOffset() {
        return codeOffset;
    }

    /**
     * @param codeOffset the codeOffset to set
     */
    public void setCodeOffset(long codeOffset) {
        this.codeOffset = codeOffset;
    }



}
