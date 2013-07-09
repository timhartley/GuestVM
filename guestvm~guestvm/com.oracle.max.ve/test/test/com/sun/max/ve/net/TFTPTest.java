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
package test.com.sun.max.ve.net;

/**
 * Test for TFTP.
 * Usage: s server f file
 * Prints the content of the file to the standard output.
 * server can be a hostname or an IP address
 *
 * @author Mick Jordan
 */
import java.io.*;

import com.sun.max.ve.net.*;
import com.sun.max.ve.net.dns.DNS;
import com.sun.max.ve.net.ip.IPAddress;
import com.sun.max.ve.net.tftp.*;

public class TFTPTest {
    public static void main(String[] args) {
        String serverName = null;
        String fileName = null;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("s")) {
                i++;
                serverName = args[i];
            } else if (arg.equals("f")) {
                i++;
                fileName = args[i];
            }
        }
        // Checkstyle: resume modified control variable check
        if (serverName == null || fileName == null) {
            System.out.println("usage: s server f filename");
        } else {
            final DNS dns = Init.getDNS();
            IPAddress ipAddress = null;
            try {
                ipAddress = IPAddress.parse(serverName);
            } catch (NumberFormatException ex) {
                ipAddress = dns.lookupOne(serverName);
            }
            if (ipAddress == null) {
                System.out.println("server " + serverName + " not found");
            } else {
                final TFTP.Client tftp = new TFTP.Client(ipAddress);
                final byte[] buffer = new byte[4096];
                try {
                    final int bytesRead = tftp.readFile(fileName, buffer);
                    System.out.println("read " + bytesRead + " bytes");
                    for (int i = 0; i < bytesRead; i++) {
                        System.out.print(buffer[i]);
                    }
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        }
    }
}
