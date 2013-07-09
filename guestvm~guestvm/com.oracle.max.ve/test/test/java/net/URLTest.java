/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.java.net;

import java.net.*;
import test.util.*;

public class URLTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final ArgsHandler h = ArgsHandler.process(args);
        for (int j = 0; j < h._opCount; j++) {
            final String opArg1 = h._opArgs1[j];
            final String opArg2 = h._opArgs2[j];
            final String op = h._ops[j];

            try {
                if (op.equals("url")) {
                    final URL url = new URL(opArg1);
                    System.out.println("pr: " + url.getProtocol() + " p: " + url.getPath());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

}
