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
package com.sun.max.ve.jdk;

import static com.sun.max.ve.jdk.AliasCast.*;

import java.net.*;
import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.dns.*;
import com.sun.max.ve.net.icmp.ICMP;
import com.sun.max.ve.net.ip.IPAddress;
import com.sun.max.ve.unsafe.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.heap.Heap;

/**
 * MaxVE specific substitutions for @see java.net.Inet4AddressImpl.
 *
 * @author Mick Jordan
*/

@METHOD_SUBSTITUTIONS(className = "java.net.Inet4AddressImpl")
final class JDK_java_net_Inet4AddressImpl {

    @ALIAS(declaringClass = java.net.Inet4Address.class, name="<init>")
    private native void init(String hostname, int ipAddr);
    
    static Inet4Address createInet4Address(String hostname, int ipAddr) {
        // Use the ALIAS mechanism to avoid reflection
        final Inet4Address inet4Address = UnsafeCast.asInet4Address(Heap.createTuple(ClassActor.fromJava(Inet4Address.class).dynamicHub()));
        JDK_java_net_Inet4AddressImpl thisInet4Address = asJDK_java_net_Inet4AddressImpl(inet4Address);
        thisInet4Address.init(hostname, ipAddr);
        return inet4Address;
    }

    @SUBSTITUTE
    InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException {
        final DNS dns = getDNS();
        final IPAddress[] ipAddresses = dns.lookup(hostname);
        if (ipAddresses == null) {
            throw new UnknownHostException();
        }
        final InetAddress[] result = new InetAddress[ipAddresses.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = createInet4Address(hostname, ipAddresses[i].addressAsInt());
        }
        return result;
    }

    @SUBSTITUTE
    String getLocalHostName() throws UnknownHostException {
        return getHostByIPAddress(Init.getLocalAddress());
    }

    @SUBSTITUTE
    String getHostByAddr(byte[] addr) throws UnknownHostException {
        return getHostByIPAddress(new IPAddress(addr));
    }

    @SUBSTITUTE
    boolean isReachable0(byte[] addr, int timeout, byte[] ifaddr, int ttl) throws IOException {
        if (ttl == 0) {
            // CheckStyle: stop parameter assignment check
            ttl = ICMP.defaultTimeout();
            // Checkstyle: resume final variable check
        }
        return ICMP.doSeqMatchingICMPEchoReq(new IPAddress(addr), timeout, ttl, ICMP.nextId(), 0) == 0;
    }

    @INLINE
    private String getHostByIPAddress(IPAddress ipAddress) throws UnknownHostException {
        final String result = getDNS().reverseLookup(ipAddress);
        if (result == null) {
            throw new UnknownHostException("host " + ipAddress + " not found");
        }
        return result;
    }

    private static DNS getDNS()  throws UnknownHostException {
        final DNS dns = DNS.getDNS();
        if (dns == null) {
            throw new UnknownHostException("network is unavailable");
        }
        return dns;
    }
}
