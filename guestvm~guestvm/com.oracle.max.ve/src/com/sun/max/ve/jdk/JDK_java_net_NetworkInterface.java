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

import com.sun.max.annotate.*;
import com.sun.max.program.ProgramError;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.device.*;
import com.sun.max.ve.net.ip.*;
import com.sun.max.ve.unsafe.UnsafeCast;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.heap.Heap;

/**
 * Substitutions for @see java.net.NetworkInterface.
 * @author Mick Jordan
 *
 */

@METHOD_SUBSTITUTIONS(NetworkInterface.class)
public class JDK_java_net_NetworkInterface {

    @ALIAS(declaringClass = NetworkInterface.class, name="<init>")
    private native void init(String name, int index, InetAddress[] addrs);
    
    static NetworkInterface createNetworkInterface(String name, int index, InetAddress[] addrs) {
        // Use the ALIAS mechanism to avoid reflection
        final NetworkInterface networkInterface = UnsafeCast.asNetworkInterface(Heap.createTuple(ClassActor.fromJava(NetworkInterface.class).dynamicHub()));
        JDK_java_net_NetworkInterface thisNetworkInterface = asJDK_java_net_NetworkInterface(networkInterface);
        thisNetworkInterface.init(name, index, addrs);
        return networkInterface;
    }

    static NetDevice findDevice(String name) {
        final NetDevice[] netDevices = Init.getNetDevices();
        for (int i = 0; i < netDevices.length; i++) {
            if (netDevices[i].getNICName().equals(name)) {
                return netDevices[i];
            }
        }
        return null;
    }

    @SUBSTITUTE
    private static NetworkInterface[] getAll() throws SocketException {
        final NetDevice[] netDevices = Init.getNetDevices();
        final NetworkInterface[] result = new NetworkInterface[netDevices.length];
        for (int i = 0; i < netDevices.length; i++) {
            final InetAddress[] inetAddresses = new InetAddress[1];
            inetAddresses[0] = JDK_java_net_Inet4AddressImpl .createInet4Address(Init.hostName(), Init.getLocalAddress(netDevices[i]).addressAsInt());
            final NetworkInterface networkInterface = createNetworkInterface(netDevices[i].getNICName(), i, inetAddresses);
            result[i] = networkInterface;
        }
        return result;
    }

    @SUBSTITUTE
    private static NetworkInterface getByName0(String name) throws SocketException {
        final NetDevice netDevice = findDevice(name);
        if (netDevice != null) {
            final InetAddress[] inetAddresses = new InetAddress[1];
            inetAddresses[0] = JDK_java_net_Inet4AddressImpl.createInet4Address(Init.hostName(), Init.getLocalAddress(netDevice).addressAsInt());
            return createNetworkInterface(name, 0, inetAddresses);
        }
        return null;
    }

    @SUBSTITUTE
    private static NetworkInterface getByInetAddress0(InetAddress inetAddress) throws SocketException {
        final NetDevice[] netDevices = Init.getNetDevices();
        final IPAddress addrToCheck = new IPAddress(inetAddress.getAddress());
        for (int i = 0; i < netDevices.length; i++) {
            final NetDevice netDevice = netDevices[i];
            final IPAddress ipAddress = Init.getLocalAddress(netDevice);
            if (addrToCheck.addressAsInt() == ipAddress.addressAsInt()) {
                return createNetworkInterface(netDevices[i].getNICName(), 0, new InetAddress[] {inetAddress});
            }
        }
        return null;
    }

    @SUBSTITUTE
    private static NetworkInterface getByIndex(int index) {
        //final NetDevice[] netDevices = Init.getNetDevices();
        return null;
    }

    @SUBSTITUTE
    private static long getSubnet0(String name, int ind) throws SocketException {
        ProgramError.unexpected("getSubnet0 not implemented");
        return 0;
    }

    @SUBSTITUTE
    private static Inet4Address getBroadcast0(String name, int ind) throws SocketException {
        ProgramError.unexpected("getBroadcast0 not implemented");
        return null;
    }

    @SUBSTITUTE
    private static boolean isUp0(String name, int ind) throws SocketException {
        final NetDevice netDevice = findDevice(name);
        return netDevice != null;
    }

    @SUBSTITUTE
    private static boolean isLoopback0(String name, int ind) throws SocketException {
        final NetDevice netDevice = findDevice(name);
        return netDevice == Init.getLoopbackDevice();
    }

    @SUBSTITUTE
    private static boolean supportsMulticast0(String name, int ind) throws SocketException {
        ProgramError.unexpected("supportsMulticast0 not implemented");
        return false;
    }

    @SUBSTITUTE
    private static boolean isP2P0(String name, int ind) throws SocketException {
        ProgramError.unexpected("isP2P0 not implemented");
        return false;
    }

    @SUBSTITUTE
    private static byte[] getMacAddr0(byte[] inAddr, String name, int ind) throws SocketException {
        final NetDevice netDevice = findDevice(name);
        final byte[] macBytes = netDevice.getMACAddress();
        final byte[] result = new byte[macBytes.length];
        System.arraycopy(macBytes, 0, result, 0, macBytes.length);
        return result;
    }

    @SUBSTITUTE
    private static int getMTU0(String name, int ind) throws SocketException {
        ProgramError.unexpected("getMTU0 not implemented");
        return 0;
    }

    @SUBSTITUTE
    private static void init() {

    }
}
