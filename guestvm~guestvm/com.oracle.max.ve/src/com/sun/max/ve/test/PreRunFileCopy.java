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
package com.sun.max.ve.test;

import java.io.*;


public class PreRunFileCopy {

    private static boolean _verbose;

    public PreRunFileCopy() {

    }

    public static void premain(String agentArgs) throws IOException {
        final String[] args = agentArgs.split(",");
        if (args.length < 2) {
            usage();
        }
        final String from = args[0];
        final String to = args[1];
        _verbose = args.length > 2 && args[2].equals("verbose");
        copyFiles(new File(from), new File(to));
    }

    private static void usage() throws IOException {
        throw new IOException("usage: from,to[,verbose]");
    }

    private static void copyFiles(File dir1, File dir2) throws IOException {
        final File[] files = dir1.listFiles();
        if (files == null) {
            throw new IOException(dir1 + "  not found");
        }
        for (File f : files) {
            final File dir2File = new File(dir2, f.getName());
            if (f.isDirectory()) {
                if (!dir2File.exists()) {
                    if (!dir2File.mkdir()) {
                        throw new IOException("cannot create directory " + dir2File.getAbsolutePath());
                    }
                } else {
                    if (dir2File.isFile()) {
                        throw new IOException(dir2File.getAbsolutePath() + " exists as a file");
                    }
                }
                copyFiles(f, dir2File);
            } else {
                copyFile(f.getAbsolutePath(), dir2File.getAbsolutePath());
            }
        }
    }

    private static void copyFile(String fileName, String toFile) {
        if (_verbose) {
            System.out.println("copyFile  " + fileName + " " + toFile);
        }
        FileInputStream fsIn = null;
        FileOutputStream fsOut = null;
        try {
            fsIn = new FileInputStream(fileName);
            fsOut = new FileOutputStream(toFile);
            int n;
            final byte[] buf = new byte[512];
            while ((n = fsIn.read(buf)) != -1) {
                fsOut.write(buf, 0, n);
            }
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (fsIn != null) {
                try {
                    fsIn.close();
                } catch (Exception ex) {
                }
            }
            if (fsOut != null) {
                try {
                    fsOut.close();
                } catch (Exception ex) {
                }
            }
        }
    }
}
