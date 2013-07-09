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
 * $Id: Ext2Utils.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author epr
 * (every method copied from DosUtils)
 */
public class Ext2Utils {
    /**
     * ceil(a/b) if a>=0
     *
     * @param a
     * @param b
     * @return
     */
    public static long ceilDiv(long a, long b) {
        return (int) ((a + b - 1) / b);
    }

    /**
     * Gets an unsigned 8-bit byte from a given offset
     *
     * @param offset
     * @return int
     */
    public static short get8(byte[] data, int offset) {
        return (short) (data[offset] & 0xFF);
    }

    /**
     * Sets an unsigned 8-bit byte at a given offset
     *
     * @param offset
     */
    public static void set8(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
    }

    /**
     * Gets an unsigned 16-bit word from a given offset
     *
     * @param offset
     * @return int
     */
    public static int get16(byte[] data, int offset) {
        int b1 = data[offset] & 0xFF;
        int b2 = data[offset + 1] & 0xFF;
        return (b2 << 8) | b1;
    }

    /**
     * Sets an unsigned 16-bit word at a given offset
     *
     * @param offset
     */
    public static void set16(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * Gets an unsigned 32-bit word from a given offset Can't read from blocks
     * bigger in size than 2GB (32bit signed int)
     *
     * @param offset
     * @return long
     */
    public static long get32(byte[] data, int offset) {
        int b1 = data[offset] & 0xFF;
        int b2 = data[offset + 1] & 0xFF;
        int b3 = data[offset + 2] & 0xFF;
        int b4 = data[offset + 3] & 0xFF;
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    public static long get32(ByteBuffer data, int offset) {
        int b1 = data.get(offset) & 0xFF;
        int b2 = data.get(offset + 1) & 0xFF;
        int b3 = data.get(offset + 2) & 0xFF;
        int b4 = data.get(offset + 3) & 0xFF;
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }
    
    /**
     * Sets an unsigned 32-bit word at a given offset
     *
     * @param offset
     */
    public static void set32(byte[] data, int offset, long value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    public static void set32(ByteBuffer data, int offset, long value) {
        data.put(offset, (byte) (value & 0xFF));
        data.put(offset + 1, (byte) ((value >> 8) & 0xFF));
        data.put(offset + 2, (byte) ((value >> 16) & 0xFF));
        data.put(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
    /* Creating this statically in the VM image avoids a circularity when starting up, as TimeZone otherwise
     * wants to read information from the file system defining java.home, which is typically Ext2.
     */
    private static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");

    /**
     *
     * @param time
     * @return
     */
    public static Calendar decodeDate(long time) {
        Calendar ref = Calendar.getInstance();
        ref.setTimeZone(GMT_TIMEZONE);
        ref.set(1970, 0, 1, 0, 0, 0);
        ref.add(Calendar.SECOND, (int) time);
        return ref;
    }

    /**
     *
     * @param time
     * @return
     */
    public static long encodeDate(Date date) {
        Calendar ref = Calendar.getInstance();
        ref.setTimeZone(GMT_TIMEZONE);
        ref.setTime(date);
        return ref.getTimeInMillis() / 1000;
    }

}
