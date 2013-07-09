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
package test.java.lang;

import java.io.*;
import java.util.*;

public class RuntimeTest {

    private static boolean _reflectImmediate = true;
    private static List<String> _stdOutLines = new ArrayList<String>();
    private static List<String> _stdErrLines = new ArrayList<String>();
    private static StringBuffer _stdOutBuffer = new StringBuffer();
    private static StringBuffer _stdErrBuffer = new StringBuffer();
    /**
     * @param args
     */
    public static void main(String[] args) {
        final Runtime runtime = Runtime.getRuntime();
        File wdir = null;
        boolean lines = true;
        int execCount = 1;
        String execInput = null;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                arg = arg.substring(1);
            }
            if (arg.equals("ap")) {
                System.out.println("availableProcessors=" + runtime.availableProcessors());
            } else if (arg.equals("quiet")) {
                _reflectImmediate = false;
            } else if (arg.equals("chars")) {
                lines = false;
            } else if (arg.equals("wdir")) {
                wdir = new File(args[++i]);
            } else if (arg.equals("ec")) {
                execCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("input")) {
                execInput = args[++i];
           } else if (arg.equals("exec")) {
                List<String> execArgsList = new ArrayList<String>();
                i++;
                while (i < args.length) {
                    final String execArg = args[i++];
                    execArgsList.add(execArg);
                }
                final String[] execArgs = new String[execArgsList.size()];
                execArgsList.toArray(execArgs);
                for (int e = 0; e < execCount; e++) {
                    Process p = null;
                    BufferedReader stdOut = null;
                    BufferedReader stdErr = null;
                    BufferedWriter stdIn = null;
                    try {
                        p = runtime.exec(execArgs, null, wdir);
                        if (execInput != null) {
                            stdIn = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                            stdIn.write(execInput);
                            stdIn.newLine();
                            stdIn.flush();
                            stdIn.close();
                            stdIn = null;
                        }
                        stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        readFully(stdOut, true, lines);
                        stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        readFully(stdErr, false, lines);
                        System.out.println("waitFor returned " + p.waitFor());
                        if (!_reflectImmediate) {
                            delayedOutput(true, lines);
                            delayedOutput(false, lines);
                        }
                    } catch (IOException ex) {
                        System.err.println(ex);
                    } catch (InterruptedException ex) {
                        System.err.println(ex);
                    } finally {
                        if (p != null) {
                            try {
                                stdOut.close();
                                stdErr.close();
                                if (stdIn != null) {
                                    stdIn.close();
                                }
                                p.destroy();
                            } catch (IOException ex) {

                            }
                        }
                    }
                }
            }
        }
        // Checkstyle: resume modified control variable check

    }

    private static void readFully(BufferedReader in, boolean isStdOut, boolean lines) throws IOException {
        if (_reflectImmediate) {
            System.out.println(isStdOut ? "stdout: " : "stderr: ");
        }
        if (lines) {
            readFullyLines(in, isStdOut);
        } else {
            readFullyChars(in, isStdOut);
        }
    }

    private static void readFullyLines(BufferedReader in, boolean isStdOut) throws IOException {
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            if (_reflectImmediate) {
                System.out.println(line);
            } else {
                if (isStdOut) {
                    _stdOutLines.add(line);
                } else {
                    _stdErrLines.add(line);
                }
            }
        }
    }

    private static void readFullyChars(BufferedReader in, boolean isStdOut) throws IOException {
        char[] buf = new char[512];
        int nRead;
        while ((nRead = in.read(buf, 0, buf.length)) > 0) {
            if (isStdOut) {
                _stdOutBuffer.append(buf, 0, nRead);
            } else {
                _stdErrBuffer.append(buf, 0, nRead);
            }
            if (_reflectImmediate) {
                for (int i = 0; i < nRead; i++) {
                    System.out.print(buf[i]);
                }
            }
        }
    }

    private static void delayedOutput(boolean isStdOut, boolean lines) {
        System.out.println(isStdOut ? "stdout: " : "stderr: ");
        if (lines) {
            delayedOutputLines(isStdOut);
        } else {
            delayedOutputChars(isStdOut);
        }
    }

    private static void delayedOutputLines(boolean isStdOut) {
        List<String> lines = isStdOut ? _stdOutLines : _stdErrLines;
        for (String line : lines) {
            System.out.println(line);
        }
    }

    private static void delayedOutputChars(boolean isStdOut) {
        StringBuffer buf = isStdOut ? _stdOutBuffer : _stdErrBuffer;
            System.out.println(buf);
    }
}
