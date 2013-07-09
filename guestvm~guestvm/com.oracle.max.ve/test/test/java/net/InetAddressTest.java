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
package test.java.net;

import java.net.InetAddress;

public class InetAddressTest {

    public static void main(String[] args) {
        try {
            // Checkstyle: stop modified control variable check
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("getAddress")) {

                } else if (arg.equals("getByName")) {
                    i++;
                    String addr = args[i];
                    InetAddress a = InetAddress.getByName(addr);
                    System.out.println("hostname " + a.getHostName() + ", IP address " + a.getHostAddress());
                } else if (arg.equals("getAllByName")) {
                    i++;
                    String addr = args[i];
                    InetAddress[] a = InetAddress.getAllByName(addr);
                    for (int j = 0; j < a.length; j++) {
                        System.out.println("hostname " + a[j].getHostName() + ", IP address " + a[j].getHostAddress());
                    }

                } else if (arg.equals("isReachable")) {
                    i++;
                    String addr = args[i];
                    InetAddress a = InetAddress.getByName(addr);
                    boolean b = a.isReachable(20000);
                    System.out.println("hostname " + a.getHostName() + " is " + (b ? "reachable" : "not reachable"));
                }
            }
            // Checkstyle: resume modified control variable check
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }



}
