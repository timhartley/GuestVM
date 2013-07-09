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

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

import com.sun.max.ve.fs.VirtualFileSystemId;
import com.sun.max.ve.net.Endpoint;
import com.sun.max.ve.net.EndpointFileSystem;
import com.sun.max.ve.net.tcp.TCPEndpoint;
import com.sun.max.ve.net.udp.UDPEndpoint;

/**
 * Utility class to support network class substitutions.
 * In particular this class generates file descriptors for the network
 * classes and ensure that they are associated with the EndpointFileSystem.
 *
 * @author Mick Jordan
 *
 */


public class JavaNetUtil {

    private static List<Endpoint> _endpoints = new ArrayList<Endpoint>(16);
    private static EndpointFileSystem _endpointFileSystem;

    /**
     * Return a file descriptor id to be associated with the given endpoint.
     * @param u
     * @return
     */
    static int getFreeIndex(Endpoint u) {
        int result;
        synchronized (_endpoints) {
            final int length = _endpoints.size();
            for (int i = 0; i < length; i++) {
                if (_endpoints.get(i) == null) {
                    _endpoints.set(i, u);
                    result = i;
                    break;
                }
            }
            _endpoints.add(u);
            result = length;
        }
        return getUniqueFd(result);
    }

    private static int getUniqueFd(int fd) {
        if (_endpointFileSystem == null) {
            _endpointFileSystem = EndpointFileSystem.create();
        }
        return VirtualFileSystemId.getUniqueFd(_endpointFileSystem, fd);
    }

    static UDPEndpoint getU(int index) {
        return (UDPEndpoint) getFromVfsId(index);
    }

    static TCPEndpoint getT(int index) {
        return (TCPEndpoint) getFromVfsId(index);
    }

    static Endpoint get(FileDescriptor fdObj) {
        return getFromVfsId(JDK_java_io_FileDescriptor.getFd(fdObj));
    }

    public static Endpoint getFromVfsId(int index) {
        return _endpoints.get(VirtualFileSystemId.getFd(index));
    }

    static void set(int index, Endpoint endpoint) {
        _endpoints.set(VirtualFileSystemId.getFd(index), endpoint);
    }

    static void setNull(int index) {
        set(index, null);
    }

}
