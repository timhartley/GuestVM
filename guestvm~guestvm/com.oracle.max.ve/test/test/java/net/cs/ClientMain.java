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

import java.lang.reflect.Constructor;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientMain {

    public static void main(String[] args) {
        String host = null;
        SessionData sessionData;
        boolean ack = true;
        int blobSize = 1000;
        int timeToRun = 10;
        String serializedDataFile = null;
        int nthreads = 1;
        boolean verbose = false;
        boolean killServer = false;
        String sdImpl = "test.java.net.cs.Default";
        String protocol = "UDP";
        long delay = 0;

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.charAt(0) == '-') {
                arg = arg.substring(1);
            }
            if (arg.equals("host")) {
                i++;
                host = args[i];
            } else if (arg.equals("bs")) {
                i++;
                blobSize = Integer.parseInt(args[i]);
            } else if (arg.equals("time")) {
                i++;
                timeToRun = checkForInt(args, i);
            } else if (arg.equals("iter")) {
                i++;
                timeToRun = - checkForInt(args, i);
            } else if (arg.equals("sf")) {
                i++;
                serializedDataFile = args[i];
            } else if (arg.equals("noack")) {
                ack = false;
            } else if (arg.equals("th")) {
                i++;
                nthreads = checkForInt(args, i);
            } else if (arg.equals("verbose")) {
                verbose = true;
            } else if (arg.equals("sdimpl")) {
                i++;
                sdImpl = args[i];
            } else if (arg.equals("delay")) {
                delay = Long.parseLong(args[++i]);
            } else if (arg.equals("type")) {
                protocol = args[++i];
            } else if (arg.equals("kill")) {
                killServer = true;
            }
        }
        // Checkstyle: resume modified control variable check

        if (host == null) {
            System.err.println("No server host specified");
            System.exit(1);
        }
        
        if (killServer) {
            killServer(host);
            return;
        }

        try {
            if (serializedDataFile != null) {
                sessionData = new FileSessionData(serializedDataFile);
            } else {
                sessionData = (SessionData) Class.forName(
                        sdImpl + "SessionData").newInstance();
                sessionData.setDataSize(blobSize);
            }

            final ClientThread[] clientThreads = new ClientThread[nthreads];
            for (int i = 0; i < nthreads; i++) {
                final Class<?> clientClass = Class.forName("test.java.net.cs." + protocol + "Client");
                final Constructor<?> clientCons = clientClass.getConstructor(String.class, int.class, SessionData.class, long.class, boolean.class, long.class, boolean.class);
                clientThreads[i] = (ClientThread) clientCons.newInstance(host, i, sessionData, timeToRun, ack, delay, verbose);
            }
            for (int i = 0; i < nthreads; i++) {
                clientThreads[i].start();
            }
            long minLatency = Long.MAX_VALUE;
            long maxLatency = 0;
            long totalOps = 0;
            long totalTime = 0;

            for (int i = 0; i < nthreads; i++) {
                final ClientThread ti = clientThreads[i];
                ti.join();
                System.out.println("Thread\tTPS\t\tOPC\t\tKBPS\t\tAVGL\tMINL\tMAXL");
                System.out.println(i + "\t" + div2pl(ti._totalOps, timeToRun)
                        + "\t\t" + ti._totalOps + "\t\t"
                        + div2pl(ti._totalOps * blobSize, timeToRun * 1000)
                        + "\t\t" + div2pl(ti._totalTime, ti._totalOps) + "\t"
                        + ti._minLatency + "\t" + ti._maxLatency);
                totalTime += ti._totalTime;
                totalOps += ti._totalOps;
                if (ti._minLatency < minLatency) {
                    minLatency = ti._minLatency;
                }
                if (ti._maxLatency > maxLatency) {
                    maxLatency = ti._maxLatency;
                }
            }
            System.out.print("All\t");
            System.out.println(div2pl(totalOps, timeToRun) + "\t\t" + totalOps
                    + "\t\t" + div2pl(totalOps * blobSize, timeToRun * 1000)
                    + "\t\t" + div2pl(totalTime, totalOps) + "\t" + minLatency
                    + "\t" + maxLatency);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    private static void killServer(String host) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = new byte[1];
            DatagramPacket packet = new DatagramPacket(data, 1, InetAddress.getByName(host), ServerThread.KILL_PORT);
            socket.send(packet);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * a/b as decimal rounded to two digits of precision.
     */
    private static String div2pl(long a, long b) {
        if (a == 0) {
            return "0";
        }
        long d = a * 1000 / b;
        final long dRem10 = d - (10 * (d / 10));
        d = d / 10;
        if (dRem10 >= 5) {
            d = d + 1;
        }
        String result = (d / 100) + ".";
        final long dRem100 = d - (100 * (d / 100));
        if (dRem100 < 10) {
            result += "0";
        }
        return result + dRem100;
    }

    private static int checkForInt(String[] args, int i) {
        try {
            if (i >= args.length) {
                throw new IllegalArgumentException("insufficient arguments");
            } else {
                return Integer.parseInt(args[i]);
            }
        } catch (Exception ex) {
            System.out.println("usage: integer value expected: " + ex);
            System.exit(1);
            return -1;
        }
    }

}

