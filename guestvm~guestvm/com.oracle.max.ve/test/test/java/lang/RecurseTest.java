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


public class RecurseTest extends Thread {

    private static long _count;
    private static long _depth;
    private static long _reportDepth = 1000;
   /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        _depth = Long.MAX_VALUE;
        boolean newThread = false;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("d")) {
                _depth = Long.parseLong(args[++i]);
            } else if (arg.equals("t")) {
                newThread = true;
            } else if (arg.equals("r")) {
                _reportDepth = Long.parseLong(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check
        
        if (newThread) {
            final Thread t = new RecurseTest();
            t.start();
            t.join();
        } else {
            new RecurseTest().run();
        }
        System.out.println("max recurse depth: " + _count);

    }
    
    @Override
    public void run()  {
        try {
            recurse();
        } catch (StackOverflowError ex) {
            System.out.println(ex);
        }        
    }

    private static void recurse() {
        if (_count++ <= _depth) {
            if (_count % _reportDepth == 0) {
                System.out.println("recurse depth: " + _count);
            }
            recurse();
        }
    }

}
