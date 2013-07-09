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

import java.util.*;
import java.io.*;

/**
 * Server thread abstract class.
 * @author Mick Jordan
 *
 */
public abstract class ServerThread extends Thread {
    /**
     * Base port number for first server thread.
     */
    public static final int PORT = 10000;
    public static final int KILL_PORT = 20000;
    /**
     * The data that is returned on an ackowledgement.
     */
    public static final byte[] ACK_BYTES = {0, 1, 2, 3 };

    protected int _threadNum;
    protected SessionData _sessionData;
    protected int _blobSize;
    protected int _nbuffers;
    protected boolean _oneRun = false;
    protected boolean _checkData = false;
    protected boolean _syncCheck = true;
    protected boolean _ack = true;
    protected List<byte[]> _buffer;
    protected int _bufferPut = 0;
    protected int _bufferGet = 0;
    protected int _bufferCount = 0;
    protected boolean _verbose;
    protected int _serverOutOfBuffersCount;
    protected String _type;

    protected ServerThread(int threadNum, SessionData sessionData, int blobSize,
            int nbuffers, boolean oneRun, boolean checkData, boolean syncCheck,
            boolean ack, boolean verbose, String type) {
        super("Server-" + threadNum);
        _threadNum = threadNum;
        _sessionData = sessionData;
        _blobSize = blobSize;
        _nbuffers = nbuffers;
        _oneRun = oneRun;
        _checkData = checkData;
        _syncCheck = syncCheck;
        _ack = ack;
        _verbose = verbose;
        _type = type;
        _nbuffers = nbuffers;
        _buffer = new ArrayList<byte[]>(nbuffers);
        for (int i = 0; i < nbuffers; i++) {
            _buffer.add(new byte[blobSize]);
        }
    }

    public int getData(byte[] data) {
        synchronized (_buffer) {
            while (_bufferCount <= 0) {
                try {
                    _buffer.wait();
                } catch (InterruptedException e) {
                    if (_verbose) {
                        System.out.print("Server: interrupted");
                    }
                }
            }
            System.arraycopy((byte[]) _buffer.get(_bufferGet), 0, data, 0,
                    _blobSize);
            _bufferGet++;
            if (_bufferGet >= _nbuffers) {
                _bufferGet = 0; // wrap
            }
            _bufferCount--;
            _buffer.notifyAll();
        }
        return 1;
    }

    protected abstract void doAck() throws IOException;

    protected byte[] getBuffer() {
        byte[] data = null;

        synchronized (_buffer) {
            while (_bufferCount >= _nbuffers) {
                try {
                    _serverOutOfBuffersCount++;
                    verbose("waiting for buffer");
                    _buffer.wait();
                } catch (InterruptedException e) {
                }
            }
            data = (byte[]) _buffer.get(_bufferPut);
            _bufferPut++;
            if (_bufferPut >= _nbuffers) {
                _bufferPut = 0;
            }
            _bufferCount++;
        }
        return data;
    }

    protected void check(byte[] data, int totalRead)  throws IOException {
        verbose("read " + totalRead + " bytes");

        if (_syncCheck) {
            if (_checkData) {
                if (!_sessionData.compare(data)) {
                    verbose("session data mismatch");
                }
            }
        }
        if (_ack) {
            doAck();
        }

        synchronized (_buffer) {
            _buffer.notifyAll();
        }

        if (!_syncCheck) {
            if (_checkData) {
                if (!_sessionData.compare(data)) {
                    verbose("session data mismatch");
                }
            }
        }

    }

    protected void info(String msg) {
        System.out.println(Thread.currentThread() + ": " + msg);
    }

    protected void verbose(String msg) {
        if (_verbose) {
            System.out.println(Thread.currentThread() + ": " + msg);
        }
    }

    @Override
    public void run() {
        info("starting " + _type + " server on port " + PORT + _threadNum);
    }

}
