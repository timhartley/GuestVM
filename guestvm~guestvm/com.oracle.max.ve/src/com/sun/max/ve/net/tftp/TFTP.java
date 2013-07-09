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
package com.sun.max.ve.net.tftp;

import java.io.IOException;

import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.ip.*;
import com.sun.max.ve.net.udp.*;


public class TFTP {
    private static final int RRQ = 1;
    private static final int WRQ = 2;
    private static final int DATA = 3;
    private static final int ACK = 4;
    private static final int ERROR = 5;

    private static final int OPCODE_OFFSET = 0;
    private static final int FILENAME_OFFSET = 2;
    private static final int BLOCK_NUMBER_OFFSET = 2;
    private static final int ERROR_NUMBER_OFFSET = 2;
    private static final int DATA_OFFSET = 4;
    private static final int ERROR_MESSAGE_OFFSET = 4;

    private static final String NETASCII = "netascii";
    private static final String OCTET = "octet";

    private static final int IP_HDR_SIZE = 20;
    private static final int UDP_HDR_SIZE = 8;
    private static final int ETHER_HDR_SIZE = 14;
    private static final int TFTOP_SIZE = 2;
    private static final int AGG_HDR_SIZE = IP_HDR_SIZE + UDP_HDR_SIZE
            + ETHER_HDR_SIZE;

    private static final int SERVER_PORT = 69;

    private static final int READ_STATE = 1;
    private static final int COMPLETE_STATE = 2;
    private static final int ERROR_STATE = 3;

    private static boolean debug = System.getProperty("max.ve.net.tftp.debug") != null;

    private static final int DEFAULT_TIMEOUT = 4000;
    private static int timeout = DEFAULT_TIMEOUT;

    static {
        String ts = System.getProperty("max.ve.net.tftp.timeout");
        if (ts != null) {
            try {
                timeout = Integer.parseInt(ts);
            } catch (Exception ex) {
                System.out.println("TFTP: error parsing timeout value: " + ts);
            }
        }
    }

    private static void putString(Packet mp, String s, int offset) {
        int sl = s.length();
        for (int i = 0; i < sl; i++)
            mp.putByte((byte) s.charAt(i), offset + i);
        mp.putByte((byte) 0, offset + sl);
    }

    private static void dprintln(String m) {
        if (debug)
            System.out.println("TFTP: " + m);
    }

    public static class Client implements UDPUpcall {
        private int local_port;
        private int server_port = SERVER_PORT;
        private IPAddress server;
        private int state = READ_STATE;
        private int blockNumber = 1; // unsigned short
        private byte[] buffer;
        private int bytesRead = 0;

        public Client(IPAddress server) {
            this.server = server;
            local_port = UDP.register(this, getLocalPort(), false);
        }

        public synchronized int readFile(String name, byte[] buffer)
                throws IOException {
            this.buffer = buffer;
            Packet mp = null;
            try {
                int size = TFTOP_SIZE + name.length() + 1 + OCTET.length() + 1;
                mp = Packet.get(AGG_HDR_SIZE, size);
                mp.putShort(RRQ, OPCODE_OFFSET);
                putString(mp, name, FILENAME_OFFSET);
                putString(mp, OCTET, FILENAME_OFFSET + name.length() + 1);
                UDP.output(mp, local_port, server.addressAsInt(), server_port,
                        size, 0);
                do {
                    waitForStateChange();
                } while (state != COMPLETE_STATE && state != ERROR_STATE);
            } catch (InterruptedException ex) {
                return 0;
            }
            if (state == ERROR_STATE)
                throw new IOException("error reading file: " + name);
            else
                return bytesRead;
        }

        private boolean waitForStateChange() throws InterruptedException {
            long currentState = state;
            long start = System.currentTimeMillis();
            long now = start;
            long end = start + timeout;
            while (state == currentState && now < end) {
                wait(2000);
                now = System.currentTimeMillis();
            }
            if (state == currentState) {
                // we timed out, try again
                // wait(delay);
                // delay = delay * 2;
                // xid++;
                dprintln("timeout");
                state = ERROR_STATE;
                return false;
            } else
                return true;
        }

        private int getLocalPort() {
            // should be randomized
            return 0;
        }

        private  void ack() {
            Packet mp = Packet.get(AGG_HDR_SIZE, 4);
            mp.putShort(ACK, OPCODE_OFFSET);
            mp.putShort(blockNumber, BLOCK_NUMBER_OFFSET);
            UDP.output(mp, local_port, server.addressAsInt(), server_port, 4, 0);
        }

        public synchronized void input(Packet pkt) {
            int newState = state;
            dprintln("input");

            Packet mp = (Packet) pkt;
            int opcode = mp.getShort(OPCODE_OFFSET);

            if (opcode == DATA) {
                int pktBlockNumber = mp.getShort(BLOCK_NUMBER_OFFSET);
                if (state == READ_STATE) {
                    if (pktBlockNumber != blockNumber) {
                            dprintln("out of sequence block: " + pktBlockNumber);
                            newState = ERROR_STATE;
                    } else {
                        int dataLength = mp.length() - mp.getHeaderOffset() - DATA_OFFSET;
                        dprintln("received block " + pktBlockNumber + " length " + dataLength);
                        mp.getBytes(DATA_OFFSET, buffer, bytesRead, dataLength);
                        bytesRead += dataLength;
                        if (dataLength < 512) {
                            // last block
                            newState = COMPLETE_STATE;;
                        }
                        if (blockNumber == 1) {
                            // first reply gives new port to continue comversation on
                            server_port = mp.getShort(-UDP_HDR_SIZE);
                            dprintln("new server port: " + server_port);
                        }
                        ack();
                        blockNumber++;
                    }
                }
            } else if (opcode == ERROR) {
                newState = ERROR_STATE;
                dprintln("error packet");
            } else {
                dprintln("unexpected packet code:" + opcode);
            }
            int oldState = state;
            state = newState;
            if (state != oldState) notify();
        }
    }

    public static class Server {

    }
}
