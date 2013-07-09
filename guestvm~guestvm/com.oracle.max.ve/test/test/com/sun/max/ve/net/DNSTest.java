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
 * Simple test of DNS lookup.
 * Usage: [r] name
 * If r absent lookup host name "name, else find hostname for ip address "name".
 *
 * @author Mick Jordan
 */

import com.sun.max.ve.net.dns.*;
import com.sun.max.ve.net.ip.*;

public class DNSTest {
    public static void main(String[] args) throws InterruptedException {
        final DNS dns = DNS.getDNS();
        if (dns == null) {
            System.out.println("no DNS: is network configured correctly?");
            System.exit(1);
        }
        String name = null;
        boolean reverse = false;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("r")) {
                reverse = true;
            } else {
                name = args[i];
            }
        }
        if (name != null) {
            if (reverse) {
                final String hostname = dns.reverseLookup(name);
                System.out.println("hostname for " + name + " is " + (hostname == null ? "not found" : hostname));
            } else {
                final IPAddress[] ipAddresses = dns.lookup(name);
                if (ipAddresses == null) {
                    System.out.println("host " + name + " not found");
                } else {
                    for (int i = 0; i < ipAddresses.length; i++) {
                        final IPAddress ipAddress = ipAddresses[i];
                        System.out.println(name + " has address " + ipAddress);
                    }
                }
            }
        }
    }
}
