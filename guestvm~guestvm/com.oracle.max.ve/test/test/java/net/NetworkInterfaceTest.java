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

import java.util.*;
import java.net.*;

public class NetworkInterfaceTest {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
       // Checkstyle: stop modified control variable check
       for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("getNetworkInterfaces")) {
                getNetworkInterfaces();
            } else if (arg.equals("getByInetAddress")) {
                getByInetAddress(args[++i]);
            } else if (arg.equals("isUp")) {
                isUp(args[++i]);
            } else if (arg.equals("getByName")) {
                getByName(args[++i]);
            } else if (arg.equals("getHardwareAddress")) {
                getHardwareAddress(args[++i]);
            }
        }
       // Checkstyle: resume modified control variable check
    }

    private static void getNetworkInterfaces() throws Exception {
        final Enumeration<NetworkInterface> intfsEnum = NetworkInterface.getNetworkInterfaces();
        System.out.println("Network interfaces:");
        while (intfsEnum.hasMoreElements()) {
            final NetworkInterface intf = intfsEnum.nextElement();
            System.out.println("  " + intf.getName() + ", " + intf.getDisplayName());
            final Enumeration<InetAddress> addrEnum = intf.getInetAddresses();
            System.out.println("  InetAddresses:");
            while (addrEnum.hasMoreElements()) {
                final InetAddress inetAddress = addrEnum.nextElement();
                System.out.println("    " + inetAddress.getHostAddress() + ", " + inetAddress.getHostName());
            }
        }
    }

    private static void getByInetAddress(String addr) throws Exception {
        final InetAddress inetAddress = InetAddress.getByName(addr);
        System.out.println("getByInetAddress(" + addr + ") = " + NetworkInterface.getByInetAddress(inetAddress));
    }

    private static void getByName(String arg) throws Exception {
        System.out.println("getByName(" + arg + ") = " + NetworkInterface.getByName(arg));
    }

    private static void isUp(String arg) throws Exception {
        final NetworkInterface networkInterface = NetworkInterface.getByName(arg);
        System.out.println("isUp(" + arg + ") = " + networkInterface.isUp());
    }

    private static void getHardwareAddress(String arg) throws Exception {
        System.out.print("getHardwareAddress(" + arg + ") = ");
        final byte[] b = NetworkInterface.getByName(arg).getHardwareAddress();
        for (int i = 0; i < b.length; i++) {
            if (i != 0) {
                System.out.print(":");
            }
            System.out.print(Integer.toHexString(b[i] & 0xFF));
        }
        System.out.println("");
    }

}
