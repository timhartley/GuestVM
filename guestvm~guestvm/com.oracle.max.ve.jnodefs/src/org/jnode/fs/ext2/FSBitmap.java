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
/*
 * $Id: FSBitmap.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package org.jnode.fs.ext2;

import java.nio.ByteBuffer;

import org.jnode.fs.FileSystemException;

/**
 * This class provides static methods that operate on the data as a bitmap
 * 
 * @author Andras Nagy
 */
public class FSBitmap {
    
    private static final boolean DEBUG = true;

    /**
     * Check if the block/inode is free according to the bitmap.
     * 
     * @param index the index of the block/inode relative to the block group
     *            (not relative to the whole fs)
     * @return true if the block/inode is free, false otherwise
     */
    protected static boolean isFree(byte[] data, int index) {
        int byteIndex = index / 8;
        byte bitIndex = (byte) (index % 8);
        byte mask = (byte) (1 << bitIndex);

        return ((data[byteIndex] & mask) == 0) ? true : false;
    }

    protected static boolean isFree(ByteBuffer data, int index) {
        int byteIndex = index / 8;
        byte bitIndex = (byte) (index % 8);
        byte mask = (byte) (1 << bitIndex);

        return ((data.get(byteIndex) & mask) == 0) ? true : false;
    }

    protected static boolean isFree(byte data, int index) {
        // byte bitIndex = (byte) (index % 8);

        byte mask = (byte) (1 << index);
        return ((data & mask) == 0) ? true : false;
    }

    protected static void setBit(byte[] data, int index) {
        int byteIndex = index / 8;
        byte bitIndex = (byte) (index % 8);
        byte mask = (byte) (1 << bitIndex);

        data[byteIndex] = (byte) (data[byteIndex] | mask);
    }

    protected static void setBit(ByteBuffer data, int index) {
        int byteIndex = index / 8;
        byte bitIndex = (byte) (index % 8);
        byte mask = (byte) (1 << bitIndex);

        data.put(byteIndex, (byte) (data.get(byteIndex) | mask));
    }

    protected static void setBit(byte[] data, int byteIndex, int bitIndex) {
        byte mask = (byte) (1 << bitIndex);

        data[byteIndex] = (byte) (data[byteIndex] | mask);
    }

    protected static void setBit(ByteBuffer data, int byteIndex, int bitIndex) {
        byte mask = (byte) (1 << bitIndex);

        data.put(byteIndex, (byte) (data.get(byteIndex) | mask));
    }

   protected static void freeBit(ByteBuffer data, int index) throws FileSystemException {
        int byteIndex = index / 8;
        byte bitIndex = (byte) (index % 8);
        byte mask = (byte) ~(1 << bitIndex);

        // filesystem consistency check
        if (DEBUG) {
            if (isFree(data.get(byteIndex), bitIndex))
                throw new FileSystemException("FS consistency error: you are trying "
                        + "to free an unallocated block/inode");
        }

        data.put(byteIndex, (byte) (data.get(byteIndex) & mask));
    }
   
    protected static void freeBit(byte[] data, int index) throws FileSystemException {
        freeBit(ByteBuffer.wrap(data), index);
    }

}
