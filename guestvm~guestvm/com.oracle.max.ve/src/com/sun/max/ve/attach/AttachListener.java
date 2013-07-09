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
package com.sun.max.ve.attach;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;

import sun.misc.Launcher;

import com.sun.max.ve.error.VEError;
import com.sun.max.vm.run.java.JavaRunScheme;

/**
 * Support for attaching to this VM.
 * We run a thread that listens on a socket.
 *
 * @author Mick Jordan
 *
 */
public final class AttachListener implements Runnable {
    private static AttachListener _attachListener;
    private static final String DEBUG_PROPERTY = "max.ve.attach.debug";
    private static boolean _debug;

    private AttachListener() {
        _debug = System.getProperty(DEBUG_PROPERTY) != null;
        final Thread attachListenerThread = new Thread(this, "Attach Listener");
        attachListenerThread.setDaemon(true);
        attachListenerThread.start();
    }

    public static void create() {
        if (_attachListener == null) {
            _attachListener = new AttachListener();
        }
    }

    public void run() {
        try {
            final ServerSocket attachSocket = new ServerSocket(AttachPort.getPort());
            // each command is a separate accept, as the close is the signal to the client
            for (;;) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    final Socket sock = attachSocket.accept();
                    System.out.println("connection accepted on " + sock.getLocalPort() + " from " + sock.getInetAddress());
                    in = sock.getInputStream();
                    out = sock.getOutputStream();
                    // read a command
                    final int numArgs = in.read();
                    final String[] args = new String[numArgs];
                    for (int i = 0; i < numArgs; i++) {
                        args[i] = readString(in);
                    }
                    debug(args);
                    final String cmd = args[0];
                    if (cmd.equals("properties")) {
                        final Map<String, String> sysProps = ManagementFactory.getRuntimeMXBean().getSystemProperties();
                        for (Map.Entry<String, String> entry : sysProps.entrySet()) {
                            writeString(out, entry.getKey());
                            out.write('=');
                            writeString(out, entry.getValue());
                            out.write('\n');
                        }
                    } else if (cmd.equals("load")) {
                        // load jarpath isabsolute options
                        final String jarPath = args[1];
                        final String isAbsolute = args[2];
                        final String options = args[3];
                        JarFile jarFile = null;
                        char error = '0';
                        try {
                            jarFile = new JarFile(jarPath);
                            final String agentClassName = JavaRunScheme.findClassAttributeInJarFile(jarFile, "Agent-Class");
                            if (agentClassName == null) {
                                error = '1';
                            } else {
                                final URL url = new URL("file://" + jarPath);
                                Launcher.addURLToAppClassLoader(url);
                                JavaRunScheme.invokeAgentMethod(url, agentClassName, "agentmain", options);
                            }
                        } catch (Exception ex) {
                            System.out.println(ex);
                            ex.printStackTrace();
                            error = '2';
                        } finally {
                            if (jarFile != null) {
                                jarFile.close();
                            }
                        }
                        out.write(error);
                    }
                } catch (IOException ex) {
                    // just abandon this command
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }
            }
        } catch (IOException ex) {
            VEError.unexpected("attach listener failed");
        }
    }

    private static String readString(InputStream in) throws IOException {
        final int length = in.read();
        if (length == 0) {
            return null;
        }
        final byte[] b = new byte[length];
        in.read(b);
        return new String(b, "UTF-8");
    }

    private static void writeString(OutputStream out, String s)  throws IOException {
        final byte[] b = s.getBytes("UTF-8");
        out.write(b);
    }

    private static void debug(String[] args) {
        if (_debug) {
            System.out.print("execute:");
            for (Object arg : args) {
                System.out.print(" ");
                System.out.print(arg);
            }
            System.out.println();
        }
    }
}
