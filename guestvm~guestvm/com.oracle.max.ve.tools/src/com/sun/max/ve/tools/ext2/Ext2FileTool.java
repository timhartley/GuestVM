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
package com.sun.max.ve.tools.ext2;

import java.io.*;
import java.nio.*;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jnode.driver.*;
import org.jnode.driver.block.*;
import org.jnode.fs.*;
import org.jnode.fs.ext2.*;

/**
 * Tools for actions, e.g., format, copyin, copyout, mkdir, mkfile, ls, rm, mv, on an ext2 file system stored in a disk image file.
 *
 *<pre>
 * Usage:
 * format -disk imagefile
 * copy[in] -disk imagefile -from file -ext2path tofile
 * copyout -disk imagefile -ext2path fromfile -to file
 * ls -disk imagefile -from file -ext2path dir [-l -a -r]
 * mkdir -disk imagefile -ext2path dir
 * mkfile -disk imagefile -ext2path file
 * rm -disk imagefile -ext2path file
 * mv -disk imagefile -ext2path file -to file
 * cat -disk imagefile -ext2path file
 * </pre>
 *
 * The keyword arguments can be in any order and, if the command is preceded by -c,
 * it can also.
 *
 * @author Mick Jordan
 *
 */
public class Ext2FileTool {

    static boolean _verbose = false;
    static boolean _veryVerbose = false;
    static boolean _recurse = false;
    static boolean _hidden = false;
    static boolean _details = false;
    static SimpleDateFormat _dateFormat = new SimpleDateFormat();
    static String[] _commands = {"format", "copy", "copyin", "copyout", "ls", "mkdir", "mkfile", "rm", "mv", "cat"};

    /**
     * @param args
     */
    public static void main(String[] args) {
        String fromFile = null;
        String diskFile = null;
        String ext2Path = null;
        String command = null;

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (i == 0 && !arg.startsWith("-")) {
                command = args[i];
            } else if (arg.equals("-c")) {
                command = args[++i];
            } else if (arg.equals("-from") || arg.equals("-to")) {
                fromFile = args[++i];
            } else if (arg.equals("-disk")) {
                diskFile = args[++i];
            } else if (arg.equals("-ext2path")) {
                ext2Path = args[++i];
            } else if (arg.equals("-v")) {
                _verbose = true;
            } else if (arg.equals("-vv")) {
                _veryVerbose = true;
            } else if (arg.equals("-r")) {
                _recurse = true;
            } else if (arg.equals("-a")) {
                _hidden = true;
            } else if (arg.equals("-l")) {
                _details = true;
            } else {
                if (arg.startsWith("-")) {
                    System.out.println("unknown option " + arg);
                    usage();
                    return;
                } else {
                    if (fromFile == null) {
                        fromFile = arg;
                    } else {
                        ext2Path = arg;
                    }
                }
            }
        }
        // Checkstyle: resume modified control variable check

        if (!checkCommand(command)) {
            return;
        }

        if (diskFile == null)  {
            usage();
            return;
        }

        if (!(new File(diskFile).exists())) {
            System.out.println("disk file " + diskFile + " does not exist");
            return;
        }

        org.jnode.fs.FileSystem<?> fs = null;
        try {
            final Device device = new Device("fileDevice:0");
            device.registerAPI(FSBlockDeviceAPI.class, new JNodeFSBlockDeviceAPIFileImpl(diskFile));
            final Ext2FileSystemType fsType = new Ext2FileSystemType();

            if (command.equals("format")) {
                final org.jnode.fs.ext2.Ext2FileSystem ext2fs = fsType.create(device, new String[0]);
                ext2fs.create(BlockSize._4Kb);
                return;
            }

            if (ext2Path == null) {
                usage();
                return;
            }

            fs = fsType.create(device, new String[0]);
            final Match m = match(ext2Path, fs.getRootEntry());
            if (m == null) {
                throw new IOException("path " + ext2Path + " not found");
            }
            if (command.equals("copyin") || command.equals("copy") || command.equals("cp")) {
                if (_recurse) {
                    copyInTree(m, fromFile, ext2Path);
                } else {
                    copyIn(m, fromFile, ext2Path);
                }
            } else if (command.equals("copyout")) {
                if (_recurse) {
                    copyOutTree(m, fromFile, ext2Path);
                } else {
                    copyOut(m, fromFile, ext2Path);
                }
            } else if (command.equals("mkdir")) {
                mkdir(m, ext2Path);
            } else if (command.equals("mkfile")) {
                mkfile(m, ext2Path);
            } else if (command.equals("ls")) {
                ls(m, ext2Path);
            } else if (command.equals("rm")) {
                remove(m, ext2Path);
            } else if (command.equals("mv")) {
                rename(ext2Path, fromFile,  fs.getRootEntry());
            } else if (command.equals("cat")) {
                copyOut(m, null, ext2Path);
            }
        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException ex) {
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void usage() {
        System.out.println("usage: ");
        System.out.println("  [-c] copy[in] -disk diskfile -from file -ext2path path");
        System.out.println("  [-c] copy[out -disk diskfile -ext2path path -to file");
        System.out.println("  [-c] mkdir -disk diskfile -ext2path dir");
        System.out.println("  [-c] mkfile -disk diskfile -ext2path file");
        System.out.println("  [-c] ls -disk diskfile -ext2path file [-l] [-a] [-r]");
        System.out.println("  [-c] rm -disk diskfile -ext2path file");
        System.out.println("  [-c] mv -disk diskfile -ext2path file -to ex2path2");
        System.out.println("  [-c] cat -disk diskfile -ext2path file");
    }

    private static boolean checkCommand(String command) {
        for (String c : _commands) {
            if (c.equals(command)) {
                return true;
            }
        }
        System.out.println("unknown command: " + command);
        usage();
        return false;
    }

    static class Match {
        FSEntry _e;
        FSDirectory _d;
        String _tail;

        Match(FSDirectory d, String tail) {
            _d = d;
            _tail = tail;
        }

        Match(FSEntry e, FSDirectory d, String tail) {
            this(d, tail);
            _e = e;
        }

        FSEntry matchTail() throws IOException {
            return _d.getEntry(_tail);
        }
    }

    /**
     * Matches the sequence of names in parts against the directory hierarchy.
     * @param parts
     * @param complete
     * @return Match object or null if no match
     */
    private static Match match(String name, FSEntry root) throws IOException {
        final String[] parts = name.split(File.separator);
        FSDirectory d = root.getDirectory();
        if (parts.length == 0) {
            return new Match(root, d, ".");
        }
        FSEntry fsEntry = root;
        for (int i = 1; i < parts.length - 1; i++) {
            fsEntry = d.getEntry(parts[i]);
            if (fsEntry == null || fsEntry.isFile()) {
                return null;
            }
            d = fsEntry.getDirectory();
        }
        return new Match(fsEntry, d, parts[parts.length - 1]);
    }

    private static void mkdir(Match m, String ext2Path) throws IOException {
        if (_verbose) {
            System.out.println("creating directory " + ext2Path);
        }
        FSEntry fsEntry = m._d.getEntry(m._tail);
        if (fsEntry == null) {
            fsEntry = m._d.addDirectory(m._tail);
        } else {
            throw new IOException(ext2Path + " already exists");
        }
    }

    private static void mkfile(Match m, String ext2Path) throws IOException {
        if (_verbose) {
            System.out.println("creating file " + ext2Path);
        }
        FSEntry fsEntry = m._d.getEntry(m._tail);
        if (fsEntry == null) {
            fsEntry = m._d.addFile(m._tail);
        } else {
            throw new IOException(ext2Path + " already exists");
        }
    }

    private static void copyIn(Match m, String fromFile, String ext2Path) throws IOException {
        FSEntry fsEntry = m.matchTail();
        if (fsEntry != null) {
            if (fsEntry.isDirectory()) {
                final FSDirectory fsEntryDir = fsEntry.getDirectory();
                final String[] parts = fromFile.split(File.separator);
                FSEntry subFsEntry = fsEntryDir.getEntry(parts[parts.length - 1]);
                if (subFsEntry == null) {
                    subFsEntry = fsEntryDir.addFile(parts[parts.length - 1]);
                }
                fsEntry = subFsEntry;
            }
        } else {
            fsEntry = m._d.addFile(m._tail);
        }
        copyInFile(fromFile, fsEntry.getFile());
    }

    /**
     * Copies a file or directory tree from host file system to ext2 filesystem.
     * @param m
     * @param fromFilename path on host file system
     * @param ext2Path path on ext2 file system
     * @throws IOException
     */
    private static void copyInTree(Match m, String fromFilename, String ext2Path) throws IOException {
        final File fromFile = new File(fromFilename);
        if (fromFile.isFile()) {
            copyIn(m, fromFilename, ext2Path);
            return;
        }
        if (!fromFile.exists()) {
            throw new IOException(fromFilename + " does not exist");
        }
        FSEntry fsEntry = m.matchTail();
        if (fsEntry == null) {
            throw new IOException(ext2Path + " does not exist");
        } else  if (fsEntry.isFile()) {
            throw new IOException(ext2Path + " is a file, not a directory");
        }
        final FSDirectory fsDir = fsEntry.getDirectory();
        final String subDirName = fromFile.getName();
        final FSEntry subDir = fsDir.getEntry(subDirName);
        if (subDir == null) {
            fsEntry = fsDir.addDirectory(subDirName);
        }
        copyInDir(fromFile, fsEntry.getDirectory());
    }

    /**
     * Copies a directory tree from host file systemto ext2 file system.
     * @param dir host file system directory handle
     * @param ext2Dir ext2 file systemdirectory handle
     * @throws IOException
     */
    private static void copyInDir(File dir, FSDirectory ext2Dir) throws IOException {
        if (_verbose) {
            System.out.println("copying directory " + dir.getAbsolutePath());
        }
        final File[] files = dir.listFiles();
        for (File file : files) {
            final String name = file.getName();
            if (file.isFile()) {
                final FSEntry fsEntry = ext2Dir.addFile(name);
                copyInFile(file.getAbsolutePath(), fsEntry.getFile());
            } else {
                FSEntry subDir = ext2Dir.getEntry(name);
                if (subDir == null) {
                    subDir = ext2Dir.addDirectory(name);
                }
                // recursively copy subdirectory
                copyInDir(file, subDir.getDirectory());
            }
        }
    }

    /**
     * Copy a single file from host file system to ext2 file system.
     * @param fileName path to host file system file
     * @param ext2File handle of ext2 file system file
     * @throws IOException
     */
    private static void copyInFile(String fileName, FSFile ext2File) throws IOException {
        if (_verbose) {
            System.out.println("copying file " + fileName);
        }
        BufferedInputStream is = null;
        try {
            final byte[] buffer = new byte[4096];
            final java.nio.ByteBuffer ext2Buffer = ByteBuffer.allocateDirect(4096);
            is = new BufferedInputStream(new FileInputStream(fileName));
            int n;
            long fileOffset = 0;
            while ((n = is.read(buffer)) > 0) {
                ext2Buffer.put(buffer, 0, n);
                ext2Buffer.position(0);
                ext2Buffer.limit(n);
                ext2File.write(fileOffset, ext2Buffer);
                fileOffset += n;
                ext2Buffer.position(0);
                ext2Buffer.limit(4096);
            }
            ext2File.flush();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Copies a single file from ext2 file system to host file system or standard output.
     * @param m
     * @param toFileName path to host file system file or null for standard output
     * @param ext2Path path of ext2 file system file or directory
     * @throws IOException
     */
    private static void copyOut(Match m, String toFileName, String ext2Path) throws IOException {
        final FSEntry fsEntry = m.matchTail();
        if (fsEntry != null) {
            if (fsEntry.isDirectory()) {
                throw new IOException("cannot copy a directory");
            } else {
                if (toFileName != null) {
                    final File toFile = new File(toFileName);
                    String copyFileName = toFileName;
                    if (toFile.isDirectory()) {
                        copyFileName = toFileName + File.separator + m._tail;
                    }
                    copyOutFile(copyFileName, fsEntry.getFile());
                } else {
                    copyOutFile(null, fsEntry.getFile());
                }
            }
        } else {
            throw new IOException(ext2Path + " not found");
        }
    }

    /**
     * Copies a single file from ext2 file system to host file system or standard output.
     * @param fileName pathname to file in host file system
     * @param fsFile handle to ext2 file
     * @throws IOException
     */
    private static void copyOutFile(String fileName, FSFile fsFile) throws IOException {
        if (fileName != null) {
            BufferedOutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(fileName));
                copyOutFileToStream(fsFile, os);
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        } else {
            copyOutFileToStream(fsFile, System.out);
        }
    }

    private static void copyOutFileToStream(FSFile fsFile, OutputStream os) throws IOException {
        final byte[] buffer = new byte[4096];
        final java.nio.ByteBuffer ext2Buffer = ByteBuffer.allocateDirect(4096);
        long fileOffset = 0;
        long length = fsFile.getLength();
        while (length > 0) {
            int n = buffer.length;
            if (length < n) {
                n = (int) length;
            }
            ext2Buffer.limit(n);
            fsFile.read(fileOffset, ext2Buffer);
            length -= n;
            fileOffset += n;
            ext2Buffer.position(0);
            ext2Buffer.get(buffer, 0, n);
            os.write(buffer, 0, n);
            ext2Buffer.position(0);
        }
    }

    /**
     * Copies a file or directory tree from ext2 filesystem to host file system.
     * @param m
     * @param fromFilename path on host file system
     * @param ext2Path path on ext2 file system
     * @throws IOException
     */
    private static void copyOutTree(Match m, String toFilename, String ext2Path) throws IOException {
        final FSEntry fsEntry = m.matchTail();
        if (fsEntry != null) {
            if (fsEntry.isDirectory()) {
                final File toDir = new File(toFilename);
                if (!toDir.isDirectory()) {
                    throw new IOException("directory " + toFilename + " does not exist");
                }
                final File toCopyDir = new File(toDir, fsEntry.getName());
                if (!toCopyDir.exists()) {
                    toCopyDir.mkdir();
                }
                copyOutDir(fsEntry.getDirectory(), toCopyDir);
            } else {
                copyOutFile(toFilename, fsEntry.getFile());
            }
        } else {
            throw new IOException(ext2Path + " not found");
        }
    }

    /**
     * Copies all files in ext2 directory to existing host directory.
     * @param fsDir handle to ext2 file system directory
     * @param toDir handle to host file system directory
     */
    private static void copyOutDir(FSDirectory fsDir, File toDir) throws IOException {
        final Iterator<? extends FSEntry> iter = fsDir.iterator();
        while (iter.hasNext()) {
            final FSEntry childEntry = iter.next();
            final File hostChildFile = new File(toDir, childEntry.getName());
            if (childEntry.isFile()) {
                copyOutFile(hostChildFile.getAbsolutePath(), childEntry.getFile());
            } else {
                final String name = childEntry.getName();
                if (name.equals(".") || name.equals("..")) {
                    continue;
                }
                hostChildFile.mkdir();
                copyOutDir(childEntry.getDirectory(), hostChildFile);
            }
        }
    }

    private static void remove(Match m, String ext2Path) throws IOException {
        final FSEntry tailEntry = m.matchTail();
        if (tailEntry == null) {
            throw new IOException(ext2Path + " not found");
        } else if (tailEntry.isFile()) {
            final FSEntry fsEntry = m._d.getEntry(m._tail);
            assert fsEntry != null;
            m._d.remove(m._tail);
        } else {
            final FSEntry fsEntry = m._d.getEntry(m._tail);
            final FSDirectory fsDir = fsEntry.getDirectory();
            final Iterator<? extends FSEntry> iter = fsDir.iterator();
            while (iter.hasNext()) {
                final FSEntry childEntry = iter.next();
                final String name = childEntry.getName();
                if (!(name.equals(".") || name.equals(".."))) {
                    throw new IOException("directory is not empty");
                }
            }
            m._d.remove(m._tail);
        }

    }

    private static void ls(Match m, String ext2Path) throws IOException {
        final FSEntry tailEntry = m.matchTail();
        if (tailEntry == null) {
            throw new IOException(ext2Path + " not found");
        } else if (tailEntry.isDirectory()) {
            ls(tailEntry, ext2Path);
        } else {
            throw new IOException(ext2Path + " is not a directory");
        }
    }

    private static void ls(FSEntry fsDirEntry, String prefix) throws IOException {
        final FSDirectory fsDir = fsDirEntry.getDirectory();
        if (_recurse) {
            System.out.println(prefix + ":");
        }
        Iterator<? extends FSEntry> iter = fsDir.iterator();
        while (iter.hasNext()) {
            final FSEntry fsEntry = iter.next();
            final String name = fsEntry.getName();
            if (_hidden || !name.startsWith(".")) {
                if (_details) {
                    System.out.print(rwx(fsEntry));
                    if (fsEntry.isFile()) {
                        final Ext2File fsFile = (Ext2File) fsEntry.getFile();
                        System.out.print("  " + fsFile.getLength());
                        System.out.print("  " + fsFile.getINode().getINodeNr());
                    }
                    System.out.print("  " + _dateFormat.format(new Date(fsEntry.getLastModified())) + "  ");
                }
                System.out.println(name);
            }
        }
        if (_recurse) {
            iter = fsDir.iterator();
            while (iter.hasNext()) {
                final FSEntry fsEntry = iter.next();
                if (fsEntry.isDirectory()) {
                    final String name = fsEntry.getName();
                    if (name.equals(".") || name.equals("..")) {
                        continue;
                    }
                    if (_hidden || !name.startsWith(".")) {
                        System.out.println();
                        ls(fsEntry, prefix + (prefix.equals(File.separator) ? "" : File.separator) + name);
                    }
                }
            }
        }
    }

    private static String rwx(FSEntry fsEntry) throws IOException {
        final FSAccessRights fsa = fsEntry.getAccessRights();
        String result = fsEntry.isDirectory() ? "d" : "-";
        result += fsa.canRead() ? "r" : "-";
        result += fsa.canWrite() ? "w" : "-";
        result += fsa.canExecute() ? "x" : "-";
        return result + "   ";
    }

    private static void rename(String fromFile, String toFile, FSEntry root) throws IOException {
        final Match m1 = match(fromFile, root);
        final Match m2 = match(toFile, root);
        /* At this point we should have matched up to the last component of both paths */
        if (m1 != null && m2 != null) {
            final FSEntry d1 = m1.matchTail();
            if (d1 == null) {
                /* path1 does not exist */
                throw new IOException(fromFile + " not found");
            }
            final FSEntry d2 = m2.matchTail();
            if (d1 == d2) {
                /* rename to self */
                return;
            }
            if (d2 != null) {
                /* path2 already exists */
                throw new IOException(toFile + " already exists");
            }
            if (m1._d == m2._d) {
                // rename within same directory, this we can do easily
                d1.setName(m2._tail);
            } else {
                final FileSystem<FSEntry> fs = (FileSystem<FSEntry>) d1.getFileSystem();
                fs.rename(d1, m2._e, m2._tail);
            }
        }

    }
}
