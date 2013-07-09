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
package com.sun.max.tele.debug.maxve;

import java.io.*;

import com.oracle.max.elf.*;
import com.oracle.max.elf.ELFHeader.*;

/**
 * Handles access to the image file.
 *
 * @author Mick Jordan
 * @author Puneeet Lakhina
 */

public final class ImageFileHandler {

    private ELFSymbolLookup symbolLookup;
    private static final String HEAP_SYMBOL_NAME = "theHeap";
    private static final String ALL_THREADS_SYMBOL_NAME = "thread_list";

    public static ImageFileHandler open(File imageFile) throws IOException, FormatError {
        return new ImageFileHandler(imageFile);
    }

    private ImageFileHandler(File imageFile) throws IOException, FormatError {
        symbolLookup = new ELFSymbolLookup(imageFile);
    }

    public void close() {
        // We close the file intially only. Nothing to do
    }

    /**
     * Gets the value of the symbol in the boot image which holds the the address of the boot heap base, see
     * Native/substrate/image.c.
     *
     * @return value of symbol
     */
    public long getBootHeapStartSymbolAddress() {
        return symbolLookup.lookupSymbolValue(HEAP_SYMBOL_NAME).longValue();
    }

    /**
     * Gets the value of the symbol in the boot image which holds the "all threads" list head value, see guk/sched.c.
     *
     * @return
     */
    public long getThreadListSymbolAddress() {
        return symbolLookup.lookupSymbolValue(ALL_THREADS_SYMBOL_NAME).longValue();
    }

    public long getSymbolAddress(String name) {
        final Number number = symbolLookup.lookupSymbolValue(name);
        return number == null ? -1 : number.longValue();
    }
}
