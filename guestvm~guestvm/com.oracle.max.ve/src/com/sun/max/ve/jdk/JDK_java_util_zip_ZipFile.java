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
package com.sun.max.ve.jdk;

/**
 * Substitutions for the native methods in java.util.ZipFile.
 * Unfortunately, ZipFile traffics in long values that are, in traditional VMs, addresses of structures allocated on the C heap.
 * We can't change this without changing the implementation of ZipFile itself. So we have to  convert the real objects
 * that we use into small integers to comply with this interface.
 *
 * @author Mick Jordan
 */

import java.util.zip.*;

import com.sun.max.annotate.*;
import com.sun.max.ve.zip.*;

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(ZipFile.class)
public class JDK_java_util_zip_ZipFile {

    @SUBSTITUTE
    private static long open(String name, int mode, long lastModified) throws ZipException {
        return ZZipFile.create(name, mode, lastModified).getId();
    }

    @SUBSTITUTE(optional=true) // appeared sometime after 1.6.0_20
    private static long open(String name, int mode, long lastModified, boolean usemmap) throws ZipException {
        return ZZipFile.create(name, mode, lastModified).getId();
    }

    @SUBSTITUTE
    private static int getTotal(long jzfile) {
        return ZZipFile.get(jzfile).getTotal();
    }

    @SUBSTITUTE
    private static long getEntry(long jzfile, String name, boolean addSlash) {
        return ZZipFile.get(jzfile).getEntry(name, addSlash);
    }

    @SUBSTITUTE
    private static void freeEntry(long jzfile, long jzentry) {
        // nothing to do as we don't allocate jzentry
    }

    @SUBSTITUTE
    private static int getMethod(long jzentry) {
        return ZZipFile.getMethod(jzentry);
    }

    @SUBSTITUTE
    private static long getNextEntry(long jzfile, int i) {
        return ZZipFile.get(jzfile).getNextEntry(i);
    }

    @SUBSTITUTE
    private static void close(long jzfile) {
        ZZipFile.get(jzfile).close(jzfile);
    }

    @SUBSTITUTE
    private static int read(long jzfile, long jzentry, long pos, byte[] b, int off, int len)  throws ZipException {
        return ZZipFile.read(jzfile, jzentry, pos, b, off, len);
    }

    @SUBSTITUTE
    private static long getCSize(long jzentry) {
        return ZZipFile.getCSize(jzentry);
    }

    @SUBSTITUTE
    private static long getSize(long jzentry) {
        return ZZipFile.getSize(jzentry);
    }

    @SUBSTITUTE
    private static String getZipMessage(long jzfile) {
        return null;
    }

    @SUBSTITUTE
    private static void initIDs() {

    }

}
