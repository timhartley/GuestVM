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

public class UDPServer extends ServerThread {
    public UDPServer(int threadNum, SessionData sessionData, int blobSize,
            int nbuffers, boolean oneRun, boolean checkData, boolean syncCheck,
            boolean ack, boolean verbose) {
        super(threadNum, sessionData, blobSize, nbuffers, oneRun, checkData, syncCheck, ack, verbose, "UDP");
    }


    private DatagramSocket _socket;
    private DatagramPacket _packet;

    @Override
    public void run() {
        super.run();
        try {
            _socket = new DatagramSocket(PORT + _threadNum);
            _packet = new DatagramPacket((byte[]) _buffer.get(0), _blobSize);
            /* TODO
            if (_socket.getReceiveBufferSize() < blobSize) {
                int bufSize = nextPowerOf2(blobSize);
                _socket.setReceiveBufferSize(bufSize);
                System.out.println("setting receive buffer to " + bufSize);
            }
            */

            for (;;) {
                try {
                    int totalOps = 0;
                    _serverOutOfBuffersCount = 0;
                    for (;;) {

                        final byte[] data = getBuffer();
                        _packet.setData(data);
                        int totalRead = 0;
                        while (totalRead < _blobSize) {
                            _socket.receive(_packet);
                            final int len = _packet.getLength();
                            if (len == 0) {
                                break;
                            }
                            totalRead += len;
                        }

                        if (totalRead == 0) {
                            break;
                        }

                        check(data, totalRead);
                        totalOps++;
                    }
                    // Done with this session
                    verbose("OPS: " + totalOps + ", out of buffers " + _serverOutOfBuffersCount + " times");
                    if (_oneRun) {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doAck() throws IOException {
        _packet.setData(ACK_BYTES);
        _socket.send(_packet);
    }

    @SuppressWarnings("unused")
    private int nextPowerOf2(int size) {
        int s = 2;
        for (int i = 1; i < 32; i++) {
            if (s >= size) {
                return s;
            }
            s = s * 2;
        }
        throw new RuntimeException("nextPowerOf2!!");
    }

}
