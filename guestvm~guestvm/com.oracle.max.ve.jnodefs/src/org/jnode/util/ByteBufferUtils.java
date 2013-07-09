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
 * $Id: ByteBufferUtils.java 4973 2009-02-02 07:52:47Z lsantha $
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
 
package org.jnode.util;

import java.nio.ByteBuffer;

public class ByteBufferUtils {
    /**
     * This method is the equivalent of System.arraycopy
     * But, instead of 2 arrays, it takes 2 ByteBuffers.
     * The position/limit are saved/restored for the src buffer.
     * The position of the dest buffer is set to destStart and will advance by len,
     * unless saveDestPos == true, in which case it is restored to the incoming value.
     * The dest limit is unchanged.
     *
     * @param src
     * @param srcStart
     * @param dest
     * @param destStart
     * @param len
     * @param saveDestPos 
     */
    public static void buffercopy(ByteBuffer src, int srcStart,
                                  ByteBuffer dest, int destStart, int len, boolean saveDestPos) {
        final int srcPos = src.position();
        final int srcLimit = src.limit();
        final int destPos = dest.position();
        // set copy bounds
        src.position(srcStart);
        src.limit(srcStart + len);
        // set destination start position
        dest.position(destStart);
        // copy
        dest.put(src);
        // reset src buffer
        src.position(srcPos);
        src.limit(srcLimit);
        // optionally reset dest position
        if (saveDestPos) {
            dest.position(destPos);
        }
    }
    
    /**
     * ByteBuffer to byte array copy, with reset of position for src.
     * @param src
     * @param srcStart
     * @param dest
     * @param destStart
     * @param len
     */
    public static void buffercopy(ByteBuffer src, int srcStart, byte[] dest, int destStart, int len) {
        final int srcPos = src.position();
        src.position(srcStart);
        src.get(dest, destStart, len);
        src.position(srcPos);
    }

    /**
     * byte array to ByteBuffer copy with reset of position for dest
     * @param src
     * @param srcStart
     * @param dest
     * @param destStart
     * @param len
     */
    public static void buffercopy(byte[] src, int srcStart, ByteBuffer dest, int destStart, int len) {
        final int destPos = dest.position();
        dest.position(destStart);
        dest.put(src, srcStart, len);
        dest.position(destPos);
    }

}
