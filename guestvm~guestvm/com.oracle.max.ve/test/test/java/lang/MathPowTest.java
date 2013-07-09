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


public class MathPowTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final long[] op1 = new long[10];
        final long[] op2 = new long[10];
        final String[] ops = new String[10];
        int opCount = 0;

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("op1")) {
                op1[opCount] = Long.parseLong(args[++i]);
            } else if (arg.equals("op2")) {
                op2[opCount] = Long.parseLong(args[++i]);
            } else if (arg.equals("op")) {
                ops[opCount++] = args[++i];
                op1[opCount] = op1[opCount - 1];
                op2[opCount] = op2[opCount - 1];
            }
        }
        // Checkstyle: resume modified control variable check

        for (int j = 0; j < opCount; j++) {
            final String op = ops[j];
            if (op.equals("pow")) {
                System.out.println("pow(" + op1[j] + ", " + op2[j] + ") = " + (long) Math.pow((double) op1[j], (double) op2[j]));
            } else if  (op.equals("lpow")) {
                System.out.println("lpow(" + op1[j] + ", " + op2[j] + ") = " + pow(op1[j], op2[j]));
            }
        }
    }

    private static long pow(long a, long b) {
        if (b == 0) {
            return 1;
        } else if (b == 1) {
            return a;
        } else {
            long result = a;
            for (long i = 1; i < b; i++) {
                result *= a;
            }
            return result;
        }
    }


}
