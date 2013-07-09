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

import test.util.ArgsHandler;

public class SocketOptsTest {

    private static Socket _socket = null;
    private static ServerSocket _serverSocket = null;
    private static DatagramSocket _datagramSocket = null;
    /**
     * @param args
     */
    public static void main(String[] args) {

        final ArgsHandler h = ArgsHandler.process(args);
        if (h._opCount == 0) {
            System.out.println("no operations given");
            return;
        }
        for (int j = 0; j < h._opCount; j++) {
            final String opArg1 = h._opArgs1[j];
            final String opArg2 = h._opArgs2[j];
            final String op = h._ops[j];

            try {
                if (op.equals("css")) {
                    _serverSocket = new ServerSocket(Integer.parseInt(opArg1));
                } else if (op.equals("cds")) {
                    _datagramSocket = new DatagramSocket(Integer.parseInt(opArg1));
                } else if (op.equals("cs")) {
                    _socket = new Socket();
                } else if (op.equals("getReceiveBufferSize")) {
                    System.out.println("getReceiveBufferSize=" + getReceiveBufferSize(opArg1));
                } else if (op.equals("setReceiveBufferSize")) {
                    setReceiveBufferSize(opArg1, Integer.parseInt(opArg2));
                    System.out.println("setReceiveBufferSize:" + opArg1 + " ok");
                } else if (op.equals("getSendBufferSize")) {
                    System.out.println("getSendBufferSize=" + getSendBufferSize(opArg1));
                } else if (op.equals("setSendBufferSize")) {
                    setSendBufferSize(opArg1, Integer.parseInt(opArg2));
                    System.out.println("setSendBufferSize:" + opArg1 + " ok");
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }

    }

    private static int getReceiveBufferSize(String type) throws SocketException {
        if (type.equals("s")) {
            return _socket.getReceiveBufferSize();
        } else if (type.equals("ss")) {
            return _serverSocket.getReceiveBufferSize();
        } else if (type.equals("ds")) {
            return _datagramSocket.getReceiveBufferSize();
        } else {
            throw new SocketException("unknown socket type: " + type);
        }
    }

    private static void setReceiveBufferSize(String type, int size) throws SocketException {
        if (type.equals("s")) {
            _socket.setReceiveBufferSize(size);
        } else if (type.equals("ss")) {
            _serverSocket.setReceiveBufferSize(size);
        } else if (type.equals("ds")) {
            _datagramSocket.setReceiveBufferSize(size);
        } else {
            throw new SocketException("unknown socket type: " + type);
        }
    }

    private static int getSendBufferSize(String type) throws SocketException {
        if (type.equals("s")) {
            return _socket.getSendBufferSize();
        } else if (type.equals("ss")) {
            throw new SocketException("getSendBufferSize on ss unavailable operation");
        } else if (type.equals("ds")) {
            return _datagramSocket.getSendBufferSize();
        } else {
            throw new SocketException("unknown socket type: " + type);
        }
    }

    private static void setSendBufferSize(String type, int size) throws SocketException {
        if (type.equals("s")) {
            _socket.setSendBufferSize(size);
        } else if (type.equals("ss")) {
            throw new SocketException("setSendBufferSize on ss unavailable operation");
        } else if (type.equals("ds")) {
            _datagramSocket.setSendBufferSize(size);
        } else {
            throw new SocketException("unknown socket type: " + type);
        }
    }
}


