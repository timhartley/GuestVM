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
package test.java.lang;

import java.util.*;
import com.sun.max.*;

public class OutOfMemoryTest {

    private static final long MB = 1024 * 1024;
    private static boolean _verbose = false;

    /**
     * @param args
     */
    public static void main(String[] args) {

        long maxMem = 8;
        boolean free = false;
        int repeat = 1;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("m")) {
                maxMem = Long.parseLong(args[++i]);
            } else if (arg.equals("v")) {
                _verbose = true;
            } else if (arg.equals("f")) {
                free = true;
            } else if (arg.equals("r")) {
                repeat = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check
        maxMem = maxMem * MB;

        final Object[] lists = new Object[repeat];
        while (repeat > 0) {
            lists[repeat - 1] = new ArrayList<Object[]>();
            List<Object[]> leak = Utils.cast(lists[repeat - 1]);

            try {
                long count = 0;
                while (count < maxMem) {
                    leak.add(allocate());
                    count += 1024 * 8;
                    if (count % MB == 0) {
                        allocated(count);
                    }
                }
            } catch (OutOfMemoryError ex) {
                System.out.println("Out of Memory");
                if (free) {
                    lists[repeat - 1] = null;
                    leak = null;
                }
            }
            repeat--;
        }

    }

    private static Object[] allocate() {
        return new Object[1024];
    }

    private static void allocated(long count) {
        if (_verbose) {
            System.out.println("allocated " + count);
        }
    }

}
