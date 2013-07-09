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

// Simple test of Server - simply consumes and discards data

import java.lang.reflect.Constructor;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServerMain {

    public static void main(String[] args) {

        SessionData sessionData;
        int blobSize = 1000;
        int nbuffers = 100;
        boolean oneRun = false;
        boolean checkData = false;
        boolean syncCheck = true;
        boolean ack = true;
        boolean verbose = false;
        boolean killThread = false;
        String sdImpl = "test.java.net.cs.Default";
        String protocol = "UDP";

        String serializedDataFile = null;

        int nthreads = 1;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.charAt(0) == '-') {
                arg = arg.substring(1);
            }
            if (arg.equals("bs")) {
                i++;
                blobSize = Integer.parseInt(args[i]);
            } else if (arg.equals("sf")) {
                i++;
                serializedDataFile = args[i];
            } else if (arg.equals("onerun")) {
                oneRun = true;
            } else if (arg.equals("check")) {
                checkData = true;
            } else if (arg.equals("sync")) {
                syncCheck = false;
            } else if (arg.equals("noack")) {
                ack = false;
            } else if (arg.equals("th")) {
                i++;
                nthreads = checkForInt(args, i);
            } else if (arg.equals("buffers")) {
                i++;
                nbuffers = checkForInt(args, i);
            } else if (arg.equals("verbose")) {
                verbose = true;
            } else if (arg.equals("sdimpl")) {
                i++;
                sdImpl = args[i];
            } else if (arg.equals("type")) {
                protocol = args[++i];
            } else if (arg.equals("kt")) {
                killThread = true;
            }
        }
        // Checkstyle: resume modified control variable check
        
        try {
            if (serializedDataFile != null) {
                sessionData = new FileSessionData(serializedDataFile);
            } else {
                sessionData = (SessionData) Class.forName(
                        sdImpl + "SessionData").newInstance();
                sessionData.setDataSize(blobSize);
            }

            final ServerThread[] serverThreads = new ServerThread[nthreads];
            final Consumer[] consumerThreads = new Consumer[nthreads];
            for (int i = 0; i < nthreads; i++) {
                final Class<?> serverClass = Class.forName("test.java.net.cs." + protocol + "Server");
                final Constructor<?> serverCons = serverClass.getConstructor(int.class, SessionData.class, int.class, int.class,
                        boolean.class, boolean.class, boolean.class, boolean.class, boolean.class);
                serverThreads[i] = (ServerThread) serverCons.newInstance(i, sessionData, blobSize,
                        nbuffers, oneRun, checkData, syncCheck, ack, verbose);
                // create a thread to consume data from server
                consumerThreads[i] = new Consumer(serverThreads[i], blobSize);

            }
            
            if (killThread) {
                createKillThread(serverThreads, consumerThreads);
            }


            for (int i = 0; i < nthreads; i++) {
                serverThreads[i].start();
                consumerThreads[i].start();
            }
            for (int i = 0; i < nthreads; i++) {
                serverThreads[i].join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int checkForInt(String[] args, int i) {
        if ((i < args.length)
                && (('0' <= args[i].charAt(0)) && args[i].charAt(0) <= '9')) {
            return Integer.parseInt(args[i]);
        } else {
            System.out.println("usage: integer value expected");
            System.exit(1);
            return -1;
        }
    }
    
    private static void createKillThread(Thread[] serverThreads, Thread[] consumerThreads) {
        KillThread killThread = new KillThread(serverThreads, consumerThreads);
        killThread.setName("Kill Thread");
        killThread.setDaemon(true);
        killThread.start();
    }
    
    static class KillThread extends Thread {
        Thread[] serverThreads;
        Thread[] consumerThreads;
        
        KillThread(Thread[] serverThreads, Thread[] consumerThreads) {
            this.consumerThreads = consumerThreads;
            this.serverThreads = serverThreads;
        }
        
        @Override
        public void run() {
            byte[] data = new byte[1];
            try {
                DatagramSocket killSocket = new DatagramSocket(ServerThread.KILL_PORT);
                DatagramPacket packet = new DatagramPacket(data, 1);
                killSocket.receive(packet);
                for (Thread serverThread : serverThreads) {
                    serverThread.interrupt();
                }
                for (Thread consumerThread : serverThreads) {
                    consumerThread.interrupt();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    static class Consumer extends Thread {
        private ServerThread _server;
        private byte[] _data;

        public Consumer(ServerThread server, int blobSize) {
            _data = new byte[blobSize];
            _server = server;
            setDaemon(true);
        }

        @Override
        public void run() {
            for (;;) {
                _server.getData(_data);
            }
        }
    }
}

