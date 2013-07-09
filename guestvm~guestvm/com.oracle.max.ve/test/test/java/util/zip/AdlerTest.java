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
package test.java.util.zip;

import java.util.zip.*;

public class AdlerTest {

    /**
     * @param args
     */

    public static void main(String[] args) {
        final Adler32 a = new Adler32();
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("u")) {
                final int uLimit = Integer.parseInt(args[++i]);
                for (int j = 1; j <= uLimit; j++) {
                    a.update(j & 0xFF);
                    System.out.println("j=" + j + ", av=" + a.getValue());
                }
            } else if (arg.equals("ub")) {
                final int size = Integer.parseInt(args[++i]);
                final byte[] b = new byte[size];
                for (int j = 0; j < size; j++) {
                    b[j] = (byte) (j & 0xFF);
                }
                a.update(b, 0, size);
                System.out.println("av=" + a.getValue());
            }
        }
        // Checkstyle: stop modified control variable check
    }

}
