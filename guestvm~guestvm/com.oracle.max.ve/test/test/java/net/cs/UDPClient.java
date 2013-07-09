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
package test.java.net.cs;

import java.net.*;
import java.io.*;

public class UDPClient extends ClientThread {

    public UDPClient(String host, int threadNum, SessionData sessionData,
            long timeToRun, boolean ack, long delay, boolean verbose) {
        super(host, threadNum, sessionData, timeToRun, ack, delay, verbose, "UDP");
    }

    private DatagramSocket _socket;
    private DatagramPacket _packet;

    @Override
    public void run() {
        super.run();
        try {
            _socket = new DatagramSocket();
            _packet = new DatagramPacket(_ackBytes,
                    _ackBytes.length, InetAddress.getByName(_host), ServerThread.PORT
                            + _threadNum);
            writeLoop();
            // sign off with a zero length packet
            _ack = false;
            doSend(new byte[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doSend(byte[] data) throws IOException {
        verbose("writing " + data.length + " bytes");
        _packet.setData(data);
        _socket.send(_packet);
        if (_ack) {
            int totalRead = 0;
            while (totalRead != ServerThread.ACK_BYTES.length) {
                _packet.setData(_ackBytes);
                _socket.receive(_packet);
                totalRead += _packet.getLength();
            }
            verbose("data ack ok");
        }

    }

}

