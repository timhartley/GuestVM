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
package com.sun.max.ve.fs.image;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.io.Files;
import com.sun.max.program.ProgramError;
import com.sun.max.program.Trace;
import com.sun.max.ve.error.VEError;
import com.sun.max.ve.fs.*;
import com.sun.max.vm.MaxineVM;

/**
 * A (read-only) file system that is pre-loaded into a Maxine VE image.
 *
 * @author Mick Jordan
 *
 */
public class BootImageFileSystem extends UnimplementedFileSystemImpl implements VirtualFileSystem {

    public static final String BOOTIMAGE_FILESYSTEM_PROPERTY = "max.ve.bootimage.fs.contents";
    private static BootImageFileSystem _singleton = new BootImageFileSystem();
    private static final int S_IFREG = 0x8000;
    private static final int S_IFDIR = 0x4000;
    @HOSTED_ONLY
    private static Set<File> includeSet = new HashSet<File>();

    private static byte[][] _openFiles = new byte[64][];
    
    private static String imageFSPrefix = "";
    private static Map<String, byte[]> _fileSystem = new HashMap<String, byte[]>();

    static {
        final String prop = System.getProperty(BOOTIMAGE_FILESYSTEM_PROPERTY);
        if (prop != null) {
            readBootImageFileSystemSpec(new File(prop));
        }
    }
    
    @HOSTED_ONLY
    private static void readBootImageFileSystemSpec(File specFile) {
        final File parent = specFile.getParentFile();
        includeSet.add(specFile);
        BufferedReader bs = null;
        try {
            bs = new BufferedReader(new FileReader(specFile));
            while (true) {
                final String line = bs.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                String argument = null;
                String command;
                final int ix = line.indexOf(' ');
                if (ix > 0) {
                    command = line.substring(0, ix);
                    argument = line.substring(ix + 1).trim();
                } else {
                    command = line;
                }
                if (command.equals("path")) {
                    doImageFile(argument);
                } else if (command.equals("prefix")) {
                    doImagefsPrefix(argument);
                } else if (command.equals("include")) {
                    if (argument != null) {
                        argument = processVars(argument);
                        File f = new File(argument);
                        if (!f.isAbsolute()) {
                            f = new File(parent, argument);
                        }
                        if (!includeSet.contains(f)) {
                            readBootImageFileSystemSpec(f);
                        }
                    }
                } else {
                    error("unknown command: " + command + " in spec file" + specFile);
                }
            }
        } catch (IOException ex) {
            error("error reading spec file: " + specFile + ": " + ex);
        } finally {
            if (bs != null) {
                try {
                    bs.close();
                } catch (IOException ex) {
                }
            }
        }
        
    }
    
    @HOSTED_ONLY
    private static void error(String m) {
        VEError.unexpected("BootImageFileSystem: " + m);
    }
       
    @HOSTED_ONLY
    public static void putImageFile(File file, File asFile) {
        try {
            String path = asFile.getPath();
            if (asFile.isAbsolute() || imageFSPrefix.equals("")) {
                path = imageFSPrefix + path;
            } else {
                path = imageFSPrefix + File.separator + path;
            }
            _fileSystem.put(path, Files.toBytes(file));
            Trace.line(1, "added file " + file.getPath() + " to boot image file system as path " + path);
        } catch (IOException ioException) {
            ProgramError.unexpected("Error reading from " + file + ": " + ioException);
        }
    }

    @HOSTED_ONLY
    protected static void putDirectory(File dir, File asFile) {
        final String[] files = dir.list();
        for (String path : files) {
            final File child = new File(dir, path);
            final File asChild = new File(asFile, path);
            if (child.isDirectory()) {
                putDirectory(child, asChild);
            } else {
                putImageFile(child, asChild);
            }
        }
    }
    
    @HOSTED_ONLY
    protected static void doImageFile(String argument) {
        String pathName = argument;
        String asName = argument;
        final int ix = argument.lastIndexOf(' ');
        if (ix > 0) {
            pathName = argument.substring(0, ix);
            asName = argument.substring(ix + 1);
        }
        pathName = processVars(pathName);
        asName = processVars(asName);
        final File file = new File(pathName);
        final File asFile = new File(asName);
        if (file.exists()) {
            if (file.isDirectory()) {
                putDirectory(file, asFile);
            } else {
                putImageFile(file, asFile);
            }
        } else {
            ProgramError.unexpected("failed to find file: " + argument);
        }
    }
    
    @HOSTED_ONLY
    private static String processVars(String s) {
        StringBuilder sb = null;
        int jx = 0;
        int ix = 0;
        while ((ix = s.indexOf("${", jx)) >= 0) {
            if (sb == null) {
                sb = new StringBuilder(s.length());
            }
            sb.append(s.substring(jx, ix));
            jx = s.indexOf('}');
            if (jx < 0) {
                error("malformed variable");
            }
            final String var = s.substring(ix + 2, jx);
            String pvar = System.getProperty(var);
            if (pvar == null) {
                pvar = System.getenv(var);
            }
            if (pvar == null) {
                error("no system property or environment variable: " + var);
            }
            sb.append(pvar);
            jx++;
            ix = jx;
        }
        if (sb == null) {
            return s;
        } else {
            sb.append(s.substring(jx));
            return sb.toString();
        }
    }

    @HOSTED_ONLY
    protected static void doImagefsPrefix(String argument) {
        final int last = argument.length() - 1;
        final String argImageFSPrefix = argument.charAt(last) == File.separatorChar ? argument.substring(0, last) : argument;
        if (imageFSPrefix.length() != 0) {
            if (!imageFSPrefix.equals(argImageFSPrefix)) {
                ProgramError.unexpected("inconsistent definitions of image file system prefix: " + imageFSPrefix + ", " + argImageFSPrefix);
            } else {
                return;
            }
        }
        imageFSPrefix = argImageFSPrefix;
        Trace.line(1, "setting boot image file system prefix to " + imageFSPrefix);
    }

    public static String getPath() {
        return imageFSPrefix;
    }

    @Override
    public void close() {

    }

    private static synchronized int getFd(byte[] data) {
        for (int i = 0; i < _openFiles.length; i++) {
            if (_openFiles[i] == null) {
                _openFiles[i] = data;
                return i;
            }
        }
        return -1;
    }

    public static BootImageFileSystem create() {
        return _singleton;
    }

    @Override
    public String canonicalize0(String path) throws IOException {
        // TODO correct implementation
        return path;
    }

    @Override
    public boolean checkAccess(String path, int access) {
        if (!(_fileSystem.containsKey(path) || isDir(path))) {
            return false;
        }
        switch (access) {
            case ACCESS_READ:
                return true;
            case ACCESS_WRITE:
                return false;
            case ACCESS_EXECUTE:
                return false;
        }
        return false;
    }

    @Override
    public boolean createDirectory(String path) {
        return false;
    }

    @Override
    public boolean createFileExclusively(String path) throws IOException {
        if (MaxineVM.isHosted()) {
            if (_fileSystem.containsKey(path)) {
                return false;
            }
            _fileSystem.put(path, null);
        }

        return false;
    }

    @Override
    public boolean delete0(String path) {
        return false;
    }

    @Override
    public long getLastModifiedTime(String path) {
        // Not recorded
        return 0;
    }

    @Override
    public long getLength(String path) {
        final byte[] data = _fileSystem.get(path);
        return data == null ? 0 : data.length;
    }

    private boolean isDir(String path) {
        final int dirLength = path.length();
        final Set<String> keys = _fileSystem.keySet();
        for (String entry : keys) {
            if (entry.startsWith(path)) {
                if (entry.charAt(dirLength) == File.separatorChar) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getMode(String path) {
        if (_fileSystem.containsKey(path)) {
            return S_IFREG | S_IREAD;
        }
        // maybe a directory?
        if (isDir(path)) {
            return S_IFDIR | S_IREAD | S_IEXEC;
        }
        return -1;
    }

    @Override
    public long getSpace(String path, int t) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String[] list(String path) {
        final String dir = path;
        final List<String> results = new ArrayList<String>();
        final int dirLength = dir.length();
        final Set<String> keys = _fileSystem.keySet();
        for (String keyPath : keys) {
            if (keyPath.startsWith(dir)) {
                if (keyPath.charAt(dirLength) == File.separatorChar) {
                    // matches dir exactly
                    final int ix = keyPath.indexOf(File.separatorChar, dirLength + 1);
                    if (ix < 0) {
                        // leaf file
                        results.add(keyPath.substring(dirLength + 1));
                    } else {
                        // subdirectory
                        final String subDir = keyPath.substring(dirLength + 1, ix);
                        boolean isNew = true;
                        for (String s : results) {
                            if (s.equals(subDir)) {
                                isNew = false;
                                break;
                            }
                        }
                        if (isNew) {
                            results.add(subDir);
                        }
                    }
                }
            }
        }
        return results.toArray(new String[results.size()]);
    }

    @Override
    public boolean rename0(String path1, String path2) {
        return false;
    }

    @Override
    public boolean setLastModifiedTime(String path, long time) {
        return false;
    }

    @Override
    public int setMode(String path, int mode) {
        return -ErrorDecoder.Code.EROFS.getCode();
    }

    // FileInputStream, FileOutputStream

    @Override
    public int read(int fd, long fileOffset) {
        final byte[] data = _openFiles[fd];
        if ((int) fileOffset >= data.length) {
            return -1;
        }
        return data[(int) fileOffset] & 0xFF;
    }

    @Override
    public int readBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        final byte[] data = _openFiles[fd];
        if ((int) fileOffset >= data.length) {
            return -1;
        }
        if (length > data.length - (int) fileOffset) {
            // CheckStyle: stop parameter assignment check
            length = data.length - (int) fileOffset;
            // CheckStyle: resume parameter assignment check
        }
        System.arraycopy(data, (int) fileOffset, bytes, offset, length);
        return length;

    }

    @Override
    public int write(int fd, int b, long fileOffset) {
        return -ErrorDecoder.Code.EROFS.getCode();

    }

    @Override
    public int writeBytes(int fd, byte[] bytes, int offset, int length, long fileOffset) {
        return -ErrorDecoder.Code.EROFS.getCode();
    }

    @Override
    public int open(String name, int flags) {
        if (flags == VirtualFileSystem.O_RDONLY) {
            final byte[] data = _fileSystem.get(name);
            if (data == null) {
                return -ErrorDecoder.Code.ENOENT.getCode();
            }
            return getFd(data);
        } else {
            return -ErrorDecoder.Code.EROFS.getCode();
        }
    }

    @Override
    public int close0(int fd) {
        _openFiles[fd] = null;
        return 0;
    }

    @Override
    public long getLength(int fd) {
        return _openFiles[fd].length;
    }

    @Override
    public int setLength(int fd, long length) {
        return -ErrorDecoder.Code.EROFS.getCode();
    }

    @Override
    public int available(int fd, long fileOffset) {
        final byte[] data = _openFiles[fd];
        if ((int) fileOffset >= data.length) {
            return 0;
        }
        return data.length - (int) fileOffset;
    }

    @Override
    public long skip(int fd, long n, long fileOffset) {
        // spec allows skip to go past end of file
        return n;
    }

}
