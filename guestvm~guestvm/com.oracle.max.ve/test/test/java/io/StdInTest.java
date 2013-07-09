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
package test.java.io;

import java.io.*;

public class StdInTest {
    public static void main(String[] args) throws IOException {
        boolean echo = false;
        boolean lineReader = false;
        String prompt = null;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("e")) {
                echo = true;
            } else if (arg.equals("l")) {
                lineReader = true;
            } else if (arg.equals("p")) {
                prompt = args[++i];
            }
        }
        if (lineReader) {
            final BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                if (prompt != null) {
                    System.out.print(prompt);
                }
                final String line = r.readLine();
                if (line == null || (line.length() > 0 && line.charAt(0) == 'q')) {
                    break;
                }
                if (echo) {
                    System.out.println(line);
                }
            }
        } else {
            boolean needPrompt = true;
            while (true) {
                if (prompt != null && needPrompt) {
                    System.out.print(prompt);
                }
                final int b = (char) System.in.read();
                if (b < 0 || b == 'q') {
                    break;
                }
                needPrompt = b == '\n';
                if (echo) {
                    System.out.write(b);
                }
            }
        }
    }

}