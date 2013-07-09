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

import java.io.*;

public class MathTest {

    private static PrintStream out;

    /**
     * @param args
     */
    public static void main(String[] args) {
        out = System.out;
        final String result = checkMathFcts();
        if (result != null) {
            out.println(result);
        } else {
            out.println("OK");
        }
    }

    static boolean checkClose(String exprStr, double v, double r) {

        double m, av = v, ar = r;

        if (av < 0.0)
            av = -av;

        if (ar < 0.0)
            ar = -ar;

        if (av > ar)

            m = av;

        else

            m = ar;

        if (m == 0.0)
            m = 1.0;

        if ((v - r) / m > 0.0001) {

            out.println(exprStr + " evaluated to: " + v + ", expected: " + r);

            return false;

        }

        return true;

    }

    static String checkMathFcts() {

        out.print("checkMathFcts: ");

        boolean ok = true;

        if (!checkClose("log(0.7)", Math.log(0.7), -0.356675))
            ok = false;

        if (!checkClose("sin(0.7)", Math.sin(0.7), 0.644218))
            ok = false;

        if (!checkClose("cos(0.7)", Math.cos(0.7), 0.764842))
            ok = false;

        if (!checkClose("tan(0.7)", Math.tan(0.7), 0.842288))
            ok = false;

        if (!checkClose("asin(0.7)", Math.asin(0.7), 0.775397))
            ok = false;

        if (!checkClose("acos(0.7)", Math.acos(0.7), 0.795399))
            ok = false;

        if (!checkClose("atan(0.7)", Math.atan(0.7), 0.610726))
            ok = false;

        if (!ok)
            return "Some math function failed";

        return null;

    }

}
