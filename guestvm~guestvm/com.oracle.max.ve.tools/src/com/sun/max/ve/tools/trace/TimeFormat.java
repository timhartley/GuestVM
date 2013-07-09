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

import java.text.*;

public class TimeFormat {
    private static final String FORMAT = "format=";

    public static enum Kind {
        SECONDS {
            @Override
            public String convert(long nanos) {
                return divNd(nanos, 1000000000, 9);
            }
        },

        MILLIS {
            @Override
            public String convert(long nanos) {
                return divNd(nanos, 1000000, 6);
            }
        },

        MICROS {
            @Override
            public String convert(long nanos) {
                return divNd(nanos, 1000, 3);
            }
        },

        NANOS {
            @Override
            public String convert(long nanos) {
                return Long.toString(nanos);
            }
        };

        public String convert(long nanos) {
            return null;
        }

    }

    public static Kind checkFormat(String[] args) {
        final String s = CommandHelper.stringArgValue(args, FORMAT);
        if (s == null) {
            return Kind.NANOS;
        } else {
            return Kind.valueOf(s);
        }
    }

    public static String byKind(long nanos, Kind kind) {
        return kind.convert(nanos);
    }

    public static String seconds(long nanos) {
        return TimeFormat.byKind(nanos, Kind.SECONDS);
    }

    public static String millis(long nanos) {
        return TimeFormat.byKind(nanos, Kind.MILLIS);
    }

    public static String micros(long nanos) {
        return TimeFormat.byKind(nanos, Kind.MICROS);
    }

    public static String div2d(long a, long b) {
        return divNd(a, b, 2);
    }

    public static String div3d(long a, long b) {
        return divNd(a, b, 3);
    }

    private static final DecimalFormat[] formats = new DecimalFormat[10];

    static {
        final StringBuilder format = new StringBuilder("###,###,###.");
        for (int i = 0; i < formats.length; i++) {
            formats[i] = new DecimalFormat(format.toString());
            format.append('#');
        }
    }

    public static String divNd(long a, long b, int n) {
        return formats[n].format((double) a / (double) b);
    }

}
