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
/* From adler32.c -- compute the Adler-32 checksum of a data stream
 * Copyright (C) 1995-1998 Mark Adler
 * For conditions of distribution and use, see copyright notice in zlib.h
 */

package com.sun.max.ve.jdk;

import java.util.zip.*;
import com.sun.max.annotate.*;

/**
 * Substitutions for  @see java.util.zip.Adler.
 * @author Mick Jordan
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(Adler32.class)
final class JDK_java_util_zip_Adler32 {

    private static final long BASE = 65521;
    private static final int NMAX = 5552;
    /* NMAX is the largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1 */

    @SUBSTITUTE
    private static int update(int adler, int b) {
        long s1 = adler & 0xFFFF;
        long s2 = (adler >> 16) & 0xFFFF;
        s1 += b;
        s2 += s1;
        s1 %= BASE;
        s2 %= BASE;
        return (int) ((s2 << 16) | s1);
    }

    @SUBSTITUTE
    private static int updateBytes(int adler, byte[] b, int off, int len) {
        if (b == null) {
            return adler;
        }
        long s1 = adler & 0xFFFF;
        long s2 = (adler >> 16) & 0xFFFF;

        int xoff = off;
        while (len > 0) {
            final int k = len < NMAX ? len : NMAX;
            for (int i = 0; i < k; i++) {
                s1 += (int) (b[xoff + i] & 0xFF);
                s2 += s1;
            }
            // CheckStyle: stop parameter assignment check
            len -= k;
            // CheckStyle: resume parameter assignment check
            xoff += k;
            s1 %= BASE;
            s2 %= BASE;
        }
        return (int) ((s2 << 16) | s1);
    }
}
