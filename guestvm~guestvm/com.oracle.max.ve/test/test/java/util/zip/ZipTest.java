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
package test.java.util.zip;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;

import test.util.OSSpecific;

public class ZipTest {

    private static Map<String, ZipEntry> _zipMap = new HashMap<String, ZipEntry>();
    private static boolean _quiet = false;
    private static int _threads = 1;
    private static int _randomSeed = 467377;
    private static boolean _traceMM;

    /**
     * @param args
     */
    public static void main(String[] args) {
        final String[] fileNames = new String[10];
        final String[] fileNames2 = new String[10];
        final String[] ops = new String[10];
        int opCount = 0;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("f")) {
                fileNames[opCount] = args[++i];
            } else  if (arg.equals("f2")) {
                fileNames2[opCount] = args[++i];
            } else if (arg.equals("op")) {
                ops[opCount++] = args[++i];
                fileNames[opCount] = fileNames[opCount - 1];
                fileNames2[opCount] = fileNames2[opCount - 1];
            } else if (arg.equals("-tmm")) {
                _traceMM = true;
            }
        }
        // Checkstyle: resume modified control variable check

        ZipFile zipFile = null;
        JarFile jarFile = null;
        ZipInputStream zipStream = null;
        for (int j = 0; j < opCount; j++) {
            try {
                final String fileName = fileNames[j];
                final String op = ops[j];
                if (op.equals("open")) {
                    zipFile = new ZipFile(fileName);
                } else if (op.equals("openStream")) {
                    zipStream = openZipStream(fileName);
                } else if (op.equals("openJar")) {
                    jarFile = new JarFile(fileName);
                    zipFile = jarFile;
                } else if (op.equals("close")) {
                    zipFile.close();
                } else if (op.equals("getEntry")) {
                    doGetEntry(checkOpen(zipFile), fileNames[j]);
                } else if (op.equals("entries")) {
                    doEntries(checkOpen(zipFile), false);
                } else if (op.equals("entriesDetails")) {
                    doEntries(checkOpen(zipFile), true);
                } else if (op.equals("entriesStream")) {
                    doEntriesStream(checkOpen(zipStream), false);
                } else if (op.equals("entriesStreamDetails")) {
                    doEntriesStream(checkOpen(zipStream), true);
                } else if (op.equals("readEntry")) {
                    doReadEntry(checkOpen(zipFile), fileNames[j], null, fileNames2[j] != null && fileNames2[j].equals("print"));
                } else if (op.equals("copyEntry")) {
                    doReadEntry(checkOpen(zipFile), fileNames[j], fileNames2[j], false);
                } else if (op.equals("metaNames")) {
                    checkOpen(zipFile);
                    doMetaNames(jarFile);
                } else if (op.equals("randomSeed")) {
                    _randomSeed = Integer.parseInt(fileNames2[j]);
                } else if (op.equals("randomRead")) {
                    doRandomRead(checkOpen(zipFile), Integer.parseInt(fileNames2[j]));
                } else if (op.equals("q")) {
                    _quiet = true;
                } else if (op.equals("v")) {
                    _quiet = false;
                } else if (op.equals("t")) {
                    _threads = Integer.parseInt(fileNames2[j]);
                }
            } catch  (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private static ZipInputStream openZipStream(String fileName) throws Exception {
        return new ZipInputStream(new BufferedInputStream(new FileInputStream(fileName)));
    }

    private static ZipFile checkOpen(ZipFile zipFile) throws Exception {
        if (zipFile == null) {
            throw new Exception("zip file not opened");
        }
        return zipFile;
    }

    private static ZipInputStream checkOpen(ZipInputStream zipStream) throws Exception {
        if (zipStream == null) {
            throw new Exception("zip file not opened");
        }
        return zipStream;
    }

    private static void doGetEntry(ZipFile zipFile, String entryName) {
        final ZipEntry zipEntry = zipFile.getEntry(entryName);
        if (zipEntry == null) {
            System.out.println("entry " + entryName + "  not found");
        } else {
            displayEntry(zipEntry);
            System.out.println("");
        }
    }

    private static void displayEntry(ZipEntry zipEntry) {
        System.out.print(" size: " + zipEntry.getSize() +
                        " csize: " + zipEntry.getCompressedSize() +
                        " crc: " + Long.toHexString(zipEntry.getCrc()) +
                        " method: " + zipEntry.getMethod() +
                        " time: " + zipEntry.getTime() +
                        " comment: " + zipEntry.getComment() +
                        " extra: " + zipEntry.getExtra());
    }

    private static void doEntries(ZipFile zipFile, boolean verbose) {
        final Enumeration<? extends ZipEntry> iter = zipFile.entries();
        while (iter.hasMoreElements()) {
            final ZipEntry zipEntry = iter.nextElement();
            handleEntry(zipEntry, verbose);
        }
    }

    private static void handleEntry(ZipEntry zipEntry, boolean verbose) {
        if (!_quiet) {
            System.out.print(zipEntry.getName());
            if (verbose) {
                displayEntry(zipEntry);
            }
        }
        _zipMap.put(zipEntry.getName(), zipEntry);
        if (!_quiet) {
            System.out.println("");
        }

    }

    private static void doEntriesStream(ZipInputStream zipStream, boolean verbose) throws IOException {
        ZipEntry zipEntry;
        while((zipEntry = zipStream.getNextEntry()) != null) {
            handleEntry(zipEntry, verbose);
        }
    }

    private static void doReadEntry(ZipFile zipFile, String entryName, String outFile, boolean print) {
        System.out.println("readEntry " + entryName + " outFile " + outFile + " print " + print);
        final ZipEntry zipEntry = zipFile.getEntry(entryName);
        final long size = zipEntry.getSize();
        if (zipEntry == null) {
            System.out.println("entry " + entryName + " not found");
            return;
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            if (outFile != null) {
                os = new FileOutputStream(outFile);
            }
            is = zipFile.getInputStream(zipEntry);
            int n = 0;
            int pc = 0;
            long rsize = 0;
            final byte[] buf = new byte[128];
            while ((n = is.read(buf)) > 0) {
                rsize += n;
                if (outFile == null) {
                    for (int i = 0; i < n; i++) {
                        if (print) {
                            final int d1 = (buf[i] >> 4) & 0xF;
                            final int d2 = buf[i] & 0xF;
                            System.out.print(Integer.toHexString(d1));
                            System.out.print(Integer.toHexString(d2));
                            pc++;
                            if (pc % 32  == 0) {
                                System.out.println();
                            } else {
                                System.out.print(" ");
                            }
                        } else {
                            System.out.write(buf[i]);
                        }
                    }
                } else {
                    os.write(buf, 0, n);
                }
            }
            if (rsize != size) {
                System.out.println("size mismatch: " + size + ", read " + rsize);
            }
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private static void doMetaNames(JarFile jarFile) throws IOException {
        final Manifest manifest = jarFile.getManifest();
        for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private static void doRandomRead(ZipFile zipFile, int count)  throws Exception {
        if (_zipMap.size() == 0) {
            throw new Exception("entries not read");
        }
        final Thread[] threads = new Thread[_threads];
        for (int t = 0; t < _threads; t++) {
            threads[t] = new RandomReader(t, zipFile, count);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    static class RandomReader extends Thread {
        private ZipFile _zipFile;
        private int _count;
        private int _thread;

        RandomReader(int thread, ZipFile zipFile, int count) {
            _thread = thread;
            _zipFile = zipFile;
            _count = count;
        }

        @Override
        public void run() {
            final int size = _zipMap.size();
            final ZipEntry[] entries = new ZipEntry[size];
            _zipMap.values().toArray(entries);
            final Random random = new Random(_randomSeed + _thread * 119);
            final byte[] buf = new byte[128];
            int ccount = _count;
            try {
                while (ccount-- > 0) {
                    final int entryIndex = random.nextInt(size);
                    final ZipEntry zipEntry = entries[entryIndex];
                    final long entrySize = zipEntry.getSize();
                    if (!_quiet) {
                        tprintln("reading entry " + zipEntry.getName());
                    }
                    InputStream is = null;
                    try {
                        is = _zipFile.getInputStream(zipEntry);
                        long n = 0;
                        long totalRead = 0;
                        while ((n = is.read(buf)) > 0) {
                            totalRead += n;
                        }
                        if (totalRead != entrySize) {
                            throw new Exception("size mismatch, read " + totalRead + " size " + size);
                        }
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException ex) {
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            tprintln("randomRead read " + _count + " entries");
        }

        private static void tprintln(String msg) {
            System.out.println(Thread.currentThread() + ": " + msg);
        }
    }
}
