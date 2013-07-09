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

public class TCPClient extends ClientThread {

    public TCPClient(String host, int threadNum, SessionData sessionData,
            long timeToRun, boolean ack, long delay, boolean verbose) {
        super(host, threadNum, sessionData, timeToRun, ack, delay, verbose, "TCP");
    }

    private OutputStream _out;
    private InputStream _in;
    int _id;

    @Override
    public void run() {
        super.run();
        try {
            final Socket sock = new Socket(_host, ServerThread.PORT + _threadNum);

            if (_verbose) {
                verbose("connected to server");
            }

            _out = sock.getOutputStream();
            _in = sock.getInputStream();

            _id = 0;
            writeLoop();
            _out.close();
            _in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doSend(byte[] data) throws IOException {
        verbose("writing " + data.length + " bytes");
        final int v = data.length;
        _out.write((v >>> 24) & 0xFF);
        _out.write((v >>> 16) & 0xFF);
        _out.write((v >>> 8) & 0xFF);
        _out.write((v >>> 0) & 0xFF);

        _out.write(_id & 0xFF);
        _out.write(data, 0, v);

        _id++;
        int len;
        if (_ack) {
            int totalRead = 0;
            // CheckStyle: stop inner assignment check
            while ((totalRead != ServerThread.ACK_BYTES.length)
                    && ((len = _in.read(_ackBytes, totalRead,
                            ServerThread.ACK_BYTES.length - totalRead)) > 0)) {
                totalRead += len;
            }
            // CheckStyle: resume inner assignment check
            verbose("data ack ok");
        }

    }
}
