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

import java.io.*;

public abstract class ClientThread extends Thread {
    ClientThread(String host, int threadNum, SessionData sessionData,
            long timeToRun, boolean ack, long delay, boolean verbose, String type) {
        super("Client-" + threadNum);
        _host = host;
        _threadNum = threadNum;
        _sessionData = sessionData;
        _ack = ack;
        if (timeToRun < 0) {
            _iterations = -timeToRun;
            _timeToRun = Long.MAX_VALUE / 10000;
        } else {
            _timeToRun = timeToRun;
            _iterations = Long.MAX_VALUE;
        }
        _delay = delay;
        _verbose = verbose;
        _type = type;
        _ackBytes = new byte[ServerThread.ACK_BYTES.length];
    }
    /**
     * Total number of operations for his client.
     */
    long _totalOps;
    /**
     * The minimum latency measured for an operation.
     */
    long _minLatency = Long.MAX_VALUE;
    /**
     * The maximum latency measured for an operation.
     */
    long _maxLatency = 0;
    /**
     * The total time the client was active in the test.
     */
    long _totalTime = 0;

    protected SessionData _sessionData;
    protected  boolean _ack;
    protected byte[] _ackBytes;
    protected long _timeToRun;
    protected long _iterations;
    protected int _threadNum;
    protected String _host;
    protected long _delay;
    protected boolean _verbose;
    protected String _type;

    protected abstract void doSend(byte[] data) throws IOException;

    /**
     * Write the data to the server repeatedly until the run time is reached.
     * Optionally delay between sends. Record min/max latency.
     * @throws IOException
     */
    protected void writeLoop()  throws IOException {
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + _timeToRun * 1000;
        long startWriteTime;
        byte[] sessionDataBytes = null;
        while ((startWriteTime = System.currentTimeMillis()) < endTime && _iterations > 0) {
            sessionDataBytes = _sessionData.getSessionData();

            doSend(sessionDataBytes);

            final long endWriteTime = System.currentTimeMillis();
            final long durTime = endWriteTime - startWriteTime;
            if (durTime < _minLatency) {
                _minLatency = durTime;
            }
            if (durTime > _maxLatency) {
                _maxLatency = durTime;
            }
            _totalTime += durTime;
            _totalOps++;
            if (_delay > 0) {
                try {
                    Thread.sleep(_delay);
                } catch (InterruptedException ex) {

                }
            }
            _iterations--;
        }
    }

    @Override
    public void run() {
        info("connecting to " + _type + " server at " + _host + " on port " + ServerThread.PORT + _threadNum);
    }

    protected void info(String msg) {
        System.out.println(Thread.currentThread() + ": " + msg);
    }

    protected void verbose(String msg) {
        if (_verbose) {
            System.out.println(Thread.currentThread() + ": " + msg);
        }
    }

}
