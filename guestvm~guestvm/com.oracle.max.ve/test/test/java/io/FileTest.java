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
package test.java.io;

import java.io.*;

/**
 * A test program for the java.io.File and related classes.
 *
 * @author Mick Jordan
 *
 */

public class FileTest {

    private static RandomAccessFile _raFile;
    private static int _bufSize = 128;
    private static boolean _binary;
    private static boolean _reflect;
    
    enum ReadMode {
        SINGLE,
        ARRAY,
        BUFFERED,
        ALL
    }

    public static void main(String[] args) {
        final String[] fileNames = new String[10];
        final String[] fileNames2 = new String[10];
        final String[] ops = new String[10];
        int opCount = 0;
        boolean echo = false;
        boolean append = false;

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("f")) {
                fileNames[opCount] = args[++i];
            } else if (arg.equals("f2")) {
                fileNames2[opCount] = args[++i];
            } else if (arg.equals("op")) {
                ops[opCount++] = args[++i];
                fileNames[opCount] = fileNames[opCount - 1];
            } else if (arg.equals("echo")) {
                echo = true;
            } else if (arg.equals("bs")) {
                _bufSize = Integer.parseInt(args[++i]);
            } else if (arg.equals("b")) {
                _binary = true;
            } else if (arg.equals("a")) {
                append = true;
            } else if (arg.equals("r")) {
                _reflect = true;
            }
        }
        // Checkstyle: resume modified control variable check

        if (opCount == 0) {
            System.out.println("no operations given");
            return;
        }
        for (int j = 0; j < opCount; j++) {
            try {
                final String fileName = fileNames[j];
                final File file = fileName == null ? null : new File(fileName);
                final String op = ops[j];
                if (echo) {
                    System.out.println("command: " + op + " " + fileName);
                }
                if (op.equals("canRead")) {
                    System.out.println("canRead " + fileName + " returned "
                            + file.canRead());
                } else if (op.equals("canWrite")) {
                    System.out.println("canWrite " + fileName + " returned "
                            + file.canWrite());
                } else if (op.equals("canExecute")) {
                    System.out.println("canExecute " + fileName + " returned "
                            + file.canExecute());
                } else if (op.equals("exists")) {
                    System.out.println("exists " + fileName + " returned "
                            + file.exists());
                } else if (op.equals("isFile")) {
                    System.out.println("isFile " + fileName + " returned "
                            + file.isFile());
                } else if (op.equals("isDirectory")) {
                    System.out.println("isDirectory " + fileName + " returned "
                            + file.isDirectory());
                } else if (op.equals("getLength")) {
                    System.out.println("length of " + fileName + " is "
                            + file.length());
                } else if (op.equals("setLength")) {
                    final RandomAccessFile ra = new RandomAccessFile(fileName, "rw");
                    try {
                        ra.setLength(Long.parseLong(fileNames2[j]));
                        System.out.println("setLength of " + fileName + " ok ");
                        ra.close();
                    } catch (IOException ex) {
                        System.out.println("setLength of " + fileName + " failed: " + ex.toString());
                    }
                } else if (op.equals("setReadOnly")) {
                    System.out.println("setReadOnly " + fileName + " returned "
                            + file.setReadOnly());
                } else if (op.equals("setWritable")) {
                    System.out.println("setWritable " + fileName + " returned "
                            + file.setWritable(true));
                } else if (op.equals("setExecutable")) {
                    System.out.println("setExecutable " + fileName
                            + " returned " + file.setExecutable(true));
                } else if (op.equals("unsetExecutable")) {
                    System.out.println("unsetExecutable " + fileName
                            + " returned " + file.setExecutable(false));
                } else if (op.equals("list")) {
                    listFiles(file, false);
                } else if (op.equals("listr")) {
                    listFiles(file, true);
                } else if (op.equals("delete")) {
                    final boolean rc = file.delete();
                    System.out.println("file delete of " + fileName
                            + checkRc(rc));
                } else if (op.equals("lastModified")) {
                    System.out.println("mtime of " + fileName + " is "
                            + file.lastModified());
                } else if (op.equals("isDirectory")) {
                    System.out.println("isDirectory of " + fileName + " is "
                            + file.isDirectory());
                } else if (op.equals("mkdir")) {
                    final boolean rc = file.mkdir();
                    System.out.println("mkdir of " + fileName + checkRc(rc));
                } else if (op.equals("mkdirs")) {
                    final boolean rc = file.mkdirs();
                    System.out.println("mkdirs of " + fileName + checkRc(rc));
                } else if (op.equals("rename")) {
                    final boolean rc = file.renameTo(new File(fileNames2[j]));
                    System.out.println("rename of " + fileName + " to "
                            + fileNames2[j] + checkRc(rc));
                } else if (op.equals("mkfile")) {
                    try {
                        final boolean rc = file.createNewFile();
                        System.out.println("mkfile of " + fileName
                                + checkRc(rc));
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                } else if (op.equals("readStdin")) {
                    readStdin();
                } else if (op.equals("readFile")) {                    
                    readFile(fileName, ReadMode.ARRAY);
                } else if (op.equals("readFileSingle")) {
                    readFile(fileName, ReadMode.SINGLE);
                } else if (op.equals("readFileAll")) {
                    readFile(fileName, ReadMode.ALL);
                } else if (op.equals("readFileBuf")) {
                    readFile(fileName, ReadMode.BUFFERED);
                } else if (op.equals("copyFile")) {
                    copyFile(fileName, fileNames2[j]);
                } else if (op.equals("copyFiles")) {
                    copyFiles(new File(fileName), new File(fileNames2[j]));
                } else if (op.equals("compareFile")) {
                    compareFile(fileName, fileNames2[j]);
                } else if (op.equals("writeFile")) {
                    writeFile(fileName, true, append);
                } else if (op.equals("writeFileSingle")) {
                    writeFile(fileName, false, append);
                } else if (op.equals("openRA")) {
                    _raFile = openRA(fileName, fileNames2[j]);
                } else if (op.equals("readRAFile")) {
                    readRAFile(true);
                } else if (op.equals("readRAFileSingle")) {
                    readRAFile(false);
                } else if (op.equals("getRAPtr")) {
                    System.out.println("getFilePointer returned " + _raFile.getFilePointer());
                } else if (op.equals("seekRA")) {
                    final long offset = Long.parseLong(fileNames2[j]);
                    _raFile.seek(offset);
                } else if (op.equals("getAbsolutePath")) {
                    System.out.println("getAbsolutePath of " + fileName + " returned " + file.getAbsolutePath());
                } else if (op.equals("getCanonicalPath")) {
                    System.out.println("getCanonicalPath of " + fileName + " returned " + file.getCanonicalPath());
                } else if (op.equals("createTempFile")) {
                    System.out.println("createTempFile of " + fileName + " returned " + File.createTempFile(fileName, null, null));
                } else if (op.equals("available")) {
                    System.out.println("available returned " + available(file));
                } else {
                    System.out.println("unknown command: " + op);
                }
            } catch (Exception ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
    }

    private static String checkRc(boolean rc) {
        return rc ? " ok" : " not ok";
    }


    private static void listFiles(File dir, boolean recurse) {
        System.out.println("Contents of " + dir.getAbsolutePath());
        doListFiles(dir, recurse, 0);
    }

    private static void doListFiles(File dir, boolean recurse, int indent) {
        final File[] files = dir.listFiles();
        if (files == null) {
            System.out.println(dir + "  not found");
            return;
        }
        for (File f : files) {
            for (int i = 0; i < indent; i++) {
                System.out.print(" ");
            }
            System.out.println("  " + rwx(f) + f.length() + "  " + f.lastModified() + "  " + f.getName());
            if (recurse && f.isDirectory()) {
                doListFiles(f, true, indent + 2);
            }
        }
    }

    private static String rwx(File file) {
        String result = file.isDirectory() ? "d" : "-";
        result += file.canRead() ? "r" : "-";
        result += file.canWrite() ? "w" : "-";
        result += file.canExecute() ? "x" : "-";
        return result + "   ";
    }

    public static void copyFiles(File dir1, File dir2) throws IOException {
        final File[] files = dir1.listFiles();
        if (files == null) {
            System.out.println(dir1 + "  not found");
            return;
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

    private static void readFile(String fileName, ReadMode readMode) {
        System.out.println("readFile  " + fileName + " " + readMode);
        InputStream is = null;
        try {
            FileInputStream fs = new FileInputStream(fileName);
            is = readMode == ReadMode.BUFFERED ? new BufferedInputStream(fs) : fs;
            readStream(fileName, is, readMode);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private static void readStdin() {
        try {
            readStream(null, System.in, ReadMode.SINGLE);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    private static int available(File file) throws IOException {
        if (file == null) {
            return System.in.available();
        } else {
            FileInputStream fs = null;
            try {
                fs = new FileInputStream(file);
                return fs.available();
            } finally {
                if (fs != null) {
                    try {
                        fs.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    private static void readStream(String fileName, InputStream fs, ReadMode readMode) throws IOException {
        long now = System.currentTimeMillis();
        int totalRead = 0;
        if (readMode == ReadMode.ARRAY || readMode == ReadMode.BUFFERED) {
            int n;
            final byte[] buf = new byte[_bufSize];
            while ((n = fs.read(buf)) != -1) {
                if (_reflect) {
                    System.out.write(buf, 0, n);
                }
                totalRead += n;
            }
        } else if (readMode == ReadMode.SINGLE) {
            int b;
            int i = 0;
            while ((b = fs.read()) != -1) {
                if (_reflect) {
                    if (_binary) {
                        System.out.print(b);
                        if ((i++ % 32) == 0) {
                            System.out.println();
                        } else {
                            System.out.write(' ');
                        }
                    } else {
                        System.out.write(b);
                    }
                }
                totalRead++;
            }
        } else if (readMode == ReadMode.ALL) {
            final File file = new File(fileName);
            final byte[] buf = new byte[(int) file.length()];
            totalRead = fs.read(buf);
        }
        System.out.println("read " + totalRead + " bytes in " + (System.currentTimeMillis() - now) + "ms");
    }

    private static void copyFile(String fileName, String toFile) {
        System.out.println("copyFile  " + fileName + " " + toFile);
        FileInputStream fsIn = null;
        FileOutputStream fsOut = null;
        try {
            fsIn = new FileInputStream(fileName);
            fsOut = new FileOutputStream(toFile);
            int n;
            final byte[] buf = new byte[_bufSize];
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

    private static void compareFile(String fileName1, String fileName2) {
        System.out.println("compareFile  " + fileName1 + " " + fileName2);
        FileInputStream fsIn1 = null;
        FileInputStream fsIn2 = null;
        final long l1 = new File(fileName1).length();
        final long l2 = new File(fileName2).length();
        if (l1 != l2) {
            System.out.println("files are differerent lengths " + l1 + ", " + l2);
        }
        try {
            fsIn1 = new FileInputStream(fileName1);
            fsIn2 = new FileInputStream(fileName2);
            int n1;
            int c1 = 0;
            int c2 = 0;
            final byte[] buf1 = new byte[_bufSize];
            final byte[] buf2 = new byte[_bufSize];
            while ((n1 = fsIn1.read(buf1)) != -1) {
                final int n2 = fsIn2.read(buf2);
                if (n1 != n2) {
                    throw new IOException("file read length mismatch n1 " + n1 + " n2 " + n2 + " after " + c1 + " bytes");
                }
                c1 += n1;
                c2 += n2;
                for (int i = 0; i < n1; i++) {
                    if (buf1[i] != buf2[i]) {
                        throw new IOException("bytes differ at offset " + (n1 + i) + " b1 " + Integer.toHexString(buf1[i] & 0xFF) + " b2 " + Integer.toHexString(buf2[i] & 0xFF));
                    }
                }
            }
            System.out.println("files are equal");
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (fsIn1 != null) {
                try {
                    fsIn1.close();
                } catch (Exception ex) {
                }
            }
            if (fsIn2 != null) {
                try {
                    fsIn2.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private static void writeFile(String fileName, boolean array, boolean append) {
        System.out.println("writeFile  " + fileName + " "
                + (array ? "multiple" : "single"));
        final String data = "The Quick Brown Fox Jumps Over The Lazy Dog\n";
        final byte[] byteData = data.getBytes();
        final int dataLength = data.length();
        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream(fileName, append);
            for (int i = 0; i < 100; i++) {
                if (array) {
                    fs.write(byteData);
                } else {
                    for (int j = 0; j < dataLength; j++) {
                        fs.write(data.charAt(j));
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (Exception ex) {
                }
            }
        }

    }

    private static RandomAccessFile openRA(String path, String mode) throws FileNotFoundException {
        return new RandomAccessFile(path, mode);
    }

    private static void readRAFile(boolean array) {
        System.out.println("readRAFile  "  + (array ? "multiple" : "single"));
        try {
            if (array) {
                int n;
                final byte[] buf = new byte[_bufSize];
                while ((n = _raFile.read(buf)) != -1) {
                    System.out.write(buf, 0, n);
                }
            } else {
                int b;
                int i = 0;
                while ((b = _raFile.read()) != -1) {
                    if (_binary) {
                        System.out.print(b);
                        if ((i++ % 32) == 0) {
                            System.out.println();
                        } else {
                            System.out.write(' ');
                        }
                    } else {
                        System.out.write(b);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }


}
