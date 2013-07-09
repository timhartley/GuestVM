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
import java.util.*;
import java.util.logging.Level;

import com.sun.max.ve.logging.*;

/**
 * A stress test for file I/O.
 * First we create a tree of files of known sizes and content.
 * Each level in the tree has a random number number of sub-directories
 * limited by {@link #_maxDirs} and a random number of files, limited by
 * {@link #_maxFiles}, or a random size, limited by {@link _maxFileSize}.
 * The tree has depth given by {@link _depth}.
 * 
 * Then we spawn a given number of threads to read {@link #_numReads}  files at random,
 * checking the content. Optionally, we can also create writer threads that
 * create and write an additional {@link FileStressTest#_numWriteTrees} trees.
 * 
 * Args:
 * <pre>
 * r n        number of readers (default 1)
 * w n       number of writers (default 0)
 * d n       maximum directory tree depth (default 5)
 * mf n      maximum number of files per directory (default 10)
 * mfs n    maximum file size (default 64k)
 * md n    maximum number of sub-directories per directory
 * nr n      number of files read by reader thread
 * nw n     number of trees created by writer thread
 * root p   path name for root of tree (default /scratch)
 * ff f         file filler, f=r for random, f=n for pathname (default random)
 * nc        no create step, recover file tree from file system (ff=n required on original create)
 * ss         same random number seed for readers (default different)
 * nm       do not try to match file content (useful when operating on arbitrary file systems)
 * v          verbose output
 * </pre>
 *
 * @author Mick Jordan
 *
 */
public class FileStressTest {

    private static int _maxFileSize = 64 * 1024;
    private static Map<String, byte[]> _fileTable = new HashMap<String, byte[]>();
    private static String[] _fileList;
    private static Random _rand;
    private static int _createSeed = 467349;
    private static int _readSeed = 756433;
    private static boolean sameReaderSeed;
    private static boolean _verbose;
    private static boolean _veryVerbose;
    private static String _rootName = "/scratch";
    private static int _maxFiles = 10;
    private static int _maxDirs = 3;
    private static int _depth = 5;
    private static int _numReads = 1000;
    private static int _numWriteTrees = 1;
    private static FileFiller fileFiller;
    private static boolean noMatch;
    private static Logger logger = Logger.getLogger(FileStressTest.class.getName());
    
    public static void main(String[] args) throws Exception {
        int numReaders = 1;
        int numWriters = 0;
        String fileFillerOption = "r";
        boolean create = true;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("r")) {
                numReaders = Integer.parseInt(args[++i]);
            } else if (arg.equals("w")) {
                numWriters = Integer.parseInt(args[++i]);
            } else if (arg.equals("d")) {
                _depth = Integer.parseInt(args[++i]);
            } else if (arg.equals("mf")) {
                _maxFiles = Integer.parseInt(args[++i]);
            } else if (arg.equals("ms")) {
                _maxFileSize = Integer.parseInt(args[++i]);
            } else if (arg.equals("md")) {
                _maxDirs = Integer.parseInt(args[++i]);
            } else if (arg.equals("nr")) {
                _numReads = Integer.parseInt(args[++i]);
            } else if (arg.equals("nw")) {
                _numWriteTrees = Integer.parseInt(args[++i]);
            } else if (arg.equals("root")) {
                _rootName = args[++i];
            } else if (arg.equals("v")) {
                _verbose = true;
            } else if (arg.equals("vv")) {
                _verbose = true;
                _veryVerbose = true;
            } else if (arg.equals("ff")) {
                fileFillerOption = args[++i];
            } else if (arg.equals("nc")) {
                create = false;
                fileFillerOption = "n";
            } else if (arg.equals("nm")) {
                noMatch = true;
            } else if (arg.equals("ss")) {
                sameReaderSeed = true;
            } else {
                System.out.println("unknown option: " + arg);
                System.exit(1);
            }
        }
        _rand = new Random(_createSeed);
        if (fileFillerOption.equals("n")) {
            fileFiller = new PathnameFileFiller();
        } else if (fileFillerOption.equals("r")) {
            fileFiller = new RandomFileFiller(_rand);
        } else {
            System.out.println("unknown file filler option: " + fileFillerOption);
            System.exit(1);
        }

        if (create) {
            final File root = new File(_rootName);
            if (!root.exists()) {
                root.mkdir();
            }
            final long beforeCreate = System.currentTimeMillis();
            createTree(root, _depth, _maxDirs, _maxFiles);
            final long afterCreate = System.currentTimeMillis();
            if (_verbose) {
                System.out.println("created " + _fileTable.size() + " files in " + (afterCreate - beforeCreate) + "ms");
            }
        } else {
            recoverFileList();
        }
        _fileList = new String[_fileTable.size()];
        _fileTable.keySet().toArray(_fileList);

        final Thread[] readers = new Thread[numReaders];
        final Thread[] writers = new Thread[numWriters];
        for (int r = 0; r < numReaders; r++) {
            readers[r] = new Reader(r);
            readers[r].setName("Reader-" + r);
            readers[r].start();
        }
        for (int w = 0; w < numWriters; w++) {
            writers[w] = new Writer(w);
            writers[w].setName("Writer-" + w);
            writers[w].start();
        }
        for (int r = 0; r < numReaders; r++) {
            readers[r].join();
        }
        for (int w = 0; w < numWriters; w++) {
            writers[w].join();
        }
    }

    private static void createTree(File parent, int depth, int maxDirs, int maxFiles) throws IOException {
        int filesToDo = _rand.nextInt(maxFiles + 1);
        int dirsToDo = (depth > 0) ? _rand.nextInt(maxDirs + 1) : 0;
        if (dirsToDo == 0) {
            dirsToDo = 1;
        }
        final File[] dirs = new File[dirsToDo];
        if (_verbose) {
            System.out.println("createTree " + parent.getAbsolutePath() + ", subdirs " + dirsToDo + ", files " + filesToDo);
        }
        while (dirsToDo > 0 || filesToDo > 0) {
            final boolean isFile = _rand.nextBoolean();
            if (isFile && filesToDo > 0) {
                createFile(parent, filesToDo);
                filesToDo--;
            } else if (dirsToDo > 0) {
                dirs[dirsToDo - 1] = new File(parent,  "d" + dirsToDo);
                dirs[dirsToDo - 1].mkdir();
                dirsToDo--;
            }
        }
        if (depth > 0) {
            final int nDepth = depth - 1;
            for (int d = 0; d < dirs.length; d++) {
                createTree(dirs[d], nDepth, maxDirs, maxFiles);
            }
        }
    }
    
    private static void recoverFileList() {
        File root = new File(_rootName);
        if (!root.exists()) {
            System.out.println("root " + root + " does not exist");
            System.exit(1);
        }
        readDirs(root);
    }
    
    private static void readDirs(File dir) {
        if (_veryVerbose) {
            System.out.println("reading directory " + dir.getAbsolutePath());
        }
        final File[] children = dir.listFiles();
        for (File child : children) {
            if (child.isDirectory()) {
                readDirs(child);
            } else {
                final byte[] data = new byte[(int) child.length()];
                _fileTable.put(child.getAbsolutePath(), fileFiller.fillData(child, data));
            }
        }
    }

    private static void createFile(File parent, int key) throws IOException {
        final File file = new File(parent, "f" + key);
        final FileOutputStream wr = new FileOutputStream(file);
        final int size = _rand.nextInt(_maxFileSize);
        final byte[] data = new byte[size];
        fileFiller.fillData(file, data);
        if (_veryVerbose) {
            System.out.println("createFile " + file.getAbsolutePath());
        }
        try {
            wr.write(data);
            _fileTable.put(file.getAbsolutePath(), data);
        } finally {
            wr.close();
        }
    }
    
    static abstract class FileFiller {
        abstract byte[] fillData(Object obj, byte[] data);
    }
    
    static class RandomFileFiller extends FileFiller {
        private Random rand;
        
        RandomFileFiller(Random rand) {
            this.rand = rand;
        }
        
        @Override
        byte[] fillData(Object obj, byte[] data) {
            rand.nextBytes(data);
            return data;
        }
    }
    
    static class PathnameFileFiller extends FileFiller {
        @Override
        byte[] fillData(Object obj, byte[] data) {
            File file = (File) obj;
            final byte[] bytes = file.getAbsolutePath().getBytes();
            final int bytesLength = bytes.length;
            for (int i = 0; i < data.length; i++) {
                data[i] = bytes[i % bytesLength];
            }
            return data;
        }
    }

    static class Reader extends Thread {
        private int _id;
        Reader(int id) {
            _id = id;
        }
        @Override
        public void run() {
            int numReads = _numReads;
            Random rand = new Random(sameReaderSeed ? _readSeed : _readSeed + _id *17);
            final long start = System.currentTimeMillis();
            while (numReads > 0) {
                final int index = rand.nextInt(_fileList.length);
                FileInputStream in = null;
                final String name = _fileList[index];
                if (_veryVerbose) {
                    final String msg = Thread.currentThread().getName() + ": reading " + name;
                    System.out.println(msg);
                    logger.log(Level.INFO,msg);
                }
                try {
                    in = new FileInputStream(name);
                    final byte[] writtenData = _fileTable.get(name);
                    final byte[] readData = new byte[writtenData.length];
                    final long beforeRead = System.nanoTime();
                    final int n = in.read(readData);
                    final long afterRead = System.nanoTime();
                    if (_veryVerbose) {
                        System.out.println(Thread.currentThread().getName() + ": read in " + (afterRead - beforeRead) + "ns");
                    }
                    if (n != writtenData.length) {
                        throw new IOException("length mismatch on file " + name + ", size " + writtenData.length + ", read " + n);
                    }
                    if (!noMatch) {
                        for (int i = 0; i < n; i++) {
                            if (readData[i] != writtenData[i]) {
                                throw new IOException("read mismatch on file " + name + ", wrote " + writtenData[i] + ", read " + readData[i]);
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(Thread.currentThread().getName() + ": " + ex);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ex) {
                        }
                    }
                }
                numReads--;
            }
            if (_verbose) {
                System.out.println(Thread.currentThread().getName() + ": total read time: " + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

    static class Writer extends Thread {
        private int me;
        
        Writer(int me) {
            this.me = me;
        }
        
        @Override
        public void run() {
            int numWrites = _numWriteTrees;
            while (numWrites > 0) {
                File root = new File(_rootName + "/w" + me + "_" + numWrites);
                try {
                    if (_verbose) {
                        System.out.println(Thread.currentThread().getName() + " creating tree at " + root.getAbsolutePath());
                    }
                    root.mkdir();
                    createTree(root, _depth, _maxDirs, _maxFiles);
                } catch (IOException ex) {
                    System.out.println(Thread.currentThread().getName() + ": " + ex);
                }
                numWrites--;
            }
        }
    }

}
