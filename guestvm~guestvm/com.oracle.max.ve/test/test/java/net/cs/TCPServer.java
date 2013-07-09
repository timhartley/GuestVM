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

public class TCPServer extends ServerThread {

    public TCPServer(int threadNum, SessionData sessionData, int blobSize, int nbuffers, boolean oneRun, boolean checkData, boolean syncCheck, boolean ack, boolean verbose) {
        super(threadNum, sessionData, blobSize, nbuffers, oneRun, checkData, syncCheck, ack, verbose, "TCP");
    }

    private OutputStream _out;

    @Override
    public void run() {
        super.run();
        try {
            final ServerSocket server = new ServerSocket(PORT + _threadNum);
            for (;;) {
                try {
                    final Socket sock = server.accept();
                    verbose("connection accepted on " + sock.getLocalPort() + " from " + sock.getInetAddress());
                    final InputStream in = sock.getInputStream();
                    _out = sock.getOutputStream();
                    int totalOps = 0;
                    _serverOutOfBuffersCount = 0;
                    for (;;) {
                        final byte[] data = getBuffer();

                        final int ch1 = in.read();
                        final int ch2 = in.read();
                        final int ch3 = in.read();
                        final int ch4 = in.read();
                        if ((ch1 | ch2 | ch3 | ch4) < 0) {
                            break;
                        }

                        final int bytesToRead = (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);

                        in.read(); // id
                        int totalRead = 0;
                        int len;
                        try {
                            // CheckStyle: stop inner assignment check
                            while ((totalRead < bytesToRead) && ((len = in.read(data, totalRead, bytesToRead - totalRead)) > 0)) {
                                totalRead += len;
                            }
                            // CheckStyle: resume inner assignment check
                        } catch (InterruptedIOException e) {
                            System.out.println("interrupted");
                            sock.close();
                            return;
                        }

                        check(data, totalRead);

                        if (totalRead == 0) {
                            break;
                        }
                        totalOps++;
                    }
                    in.close();
                    _out.close();
                    verbose("OPS: " + totalOps + ", out of buffers " + _serverOutOfBuffersCount + " times");
                    if (_oneRun) {
                        break;
                    }
                } catch (InterruptedIOException e) {
                    System.out.println("interrupted");
                    return;
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
        _out.write(ACK_BYTES, 0, ACK_BYTES.length);
    }
}
