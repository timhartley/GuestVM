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
package com.sun.max.ve.fs;

import java.io.File;
import java.util.*;

import com.sun.max.ve.error.VEError;
import com.sun.max.ve.fs.console.ConsoleFileSystem;
import com.sun.max.ve.fs.ext2.Ext2FileSystem;
import com.sun.max.ve.fs.heap.HeapFileSystem;
import com.sun.max.ve.fs.image.BootImageFileSystem;
import com.sun.max.ve.fs.nfs.NfsFileSystem;
import com.sun.max.ve.fs.sg.SiblingFileSystem;

/**
 * Stores information on (mounted) file systems.
 * The max.ve.fs.table property holds a list of specifications modelled on /etc/fstab
 * N.B. we do not unify the file system tree up to the root, aka /; each file system
 * must use a unique mount path that is not a prefix of another and path searches
 * always start from one of these mount paths. I.e,, you cannot do the equivalent of "ls /".
 * This could be fixed but it is not clear that it is necessary (since we are not building
 * an operating system).
 *
 * @author Mick Jordan
 *
 */

public class FSTable {
    private static Map<Info, VirtualFileSystem> _fsTable = new HashMap<Info, VirtualFileSystem>();
    private static final String FS_TABLE_PROPERTY = "max.ve.fs.table";
    private static final String TMPDIR_PROPERTY = "max.ve.tmpdir";
    private static final String DEFAULT_TMPDIR = "/tmp";
    private static final String FS_INFO_SEPARATOR = ":";
    private static final String FS_TABLE_SEPARATOR = ";";
    private static final String FS_OPTIONS_SEPARATOR = ",";
    private static final String READ_ONLY = "ro";
    public static final String AUTO = "auto";
    private static final String SEP_AUTO = FS_INFO_SEPARATOR + AUTO ;
    private static final String SEP_AUTO_SEP = SEP_AUTO + FS_TABLE_SEPARATOR;
    private static final String DEFAULT_FS_TABLE_PROPERTY = "ext2" + FS_INFO_SEPARATOR + "/blk/0" + FS_INFO_SEPARATOR + "/maxve/java" + 
                                           FS_INFO_SEPARATOR + READ_ONLY + FS_OPTIONS_SEPARATOR + AUTO;
    public static final String TMP_FS_INFO = "heap" + FS_INFO_SEPARATOR + "/heap/0" + FS_INFO_SEPARATOR;
    public static final String IMG_FS_INFO = "img" + FS_INFO_SEPARATOR + "/img/0" + FS_INFO_SEPARATOR;
    private static final int TYPE_INDEX = 0;
    private static final int DEV_INDEX = 1;
    private static final int MOUNT_INDEX = 2;
    private static final int OPTIONS_INDEX = 3;

    private static boolean _initFSTable;
    private static boolean _initImageAndHeap; // image and heap file systems initialized
    private static boolean _basicInit;
    private static RootFileSystem _rootFS;
    private static Info[] _infoArray;

    public static class Info {
        private String _type;
        private String _devPath;
        private String _mountPath;
        private String[] _options;
        private VirtualFileSystem _fs;

        public static enum Parts {
            TYPE, DEVPATH, MOUNTPATH, OPTIONS, DUMP, ORDER;
        }

        private static final int PARTS_LENGTH = Parts.values().length;

        Info(String type, String devPath, String mountPath, String[] options) {
            _type = type;
            _devPath = devPath;
            _mountPath = mountPath;
            _options = options == null ? new String[0] : options;
        }

        boolean readOnly() {
            for (String option : _options) {
                if (option.equals("ro")) {
                    return true;
                }
            }
            return false;
        }

        boolean autoMount() {
            for (String option : _options) {
                if (option.equals("auto")) {
                    return true;
                }
            }
            return false;
        }
        
        public String mountPath() {
            return _mountPath;
        }

        @Override
        public boolean equals(Object other) {
            return _mountPath.equals((Info) other);
        }

        @Override
        public int hashCode() {
            return _mountPath.hashCode();
        }

        @Override
        public String toString() {
            String result =  _type + FS_INFO_SEPARATOR + _devPath + FS_INFO_SEPARATOR + _mountPath + FS_INFO_SEPARATOR;
            for (int i = 0; i < _options.length; i++) {
                result += _options[i];
                if (i != _options.length - 1) {
                    result += FS_OPTIONS_SEPARATOR;
                }
            }
            return result;
        }
    }

    private static void logBadEntry(String msg) {
        VEError.exit(msg);
    }

    /**
     * Basic initialization, ensures that console output is set up and the RootFileSystem.
     */
    public static void basicInit() {
        if (!_basicInit) {
            // This call guarantees that file descriptors 0,1,2 map to the ConsoleFileSystem
            VirtualFileSystemId.getUniqueFd(new ConsoleFileSystem(), 0);

            _rootFS = RootFileSystem.create();
            _basicInit = true;
        }
    }

    private static void initFSTable(boolean all) {
        if (!_initFSTable) {
            if (!_initImageAndHeap) {
                basicInit();
                String fsTableProperty = System.getProperty(FS_TABLE_PROPERTY);
                if (fsTableProperty == null) {
                    fsTableProperty = DEFAULT_FS_TABLE_PROPERTY;
                }

                String tmpDir = System.getProperty(TMPDIR_PROPERTY);
                if (tmpDir == null) {
                    tmpDir = DEFAULT_TMPDIR;
                }
                String imageAndTmpTable = IMG_FS_INFO + BootImageFileSystem.getPath() + SEP_AUTO_SEP + TMP_FS_INFO + tmpDir + SEP_AUTO;

                /* prepend the image and default heap fs */
                fsTableProperty = imageAndTmpTable + FS_TABLE_SEPARATOR + fsTableProperty;

                // Check that the mount paths are globally consistent
                final String[] entries = fsTableProperty.split(FS_TABLE_SEPARATOR);
                _infoArray = new Info[entries.length];
                int infoArrayIndex = 0;
                for (String entry : entries) {
                    final String[] info = fixupNfs(entry.split(FS_INFO_SEPARATOR, Info.PARTS_LENGTH));
                    if (info.length < MOUNT_INDEX || info.length > OPTIONS_INDEX + 1) {
                        logBadEntry("fs.table entry " + entry + " is malformed");
                        continue;
                    }
                    final String type = info[TYPE_INDEX];
                    final String devPath = info[DEV_INDEX];
                    final String mountPath = (info.length <= MOUNT_INDEX || info[MOUNT_INDEX].length() == 0) ? devPath : info[MOUNT_INDEX];
                    if (!mountPath.startsWith("/")) {
                        logBadEntry("mountpath " + mountPath + " is not absolute");
                        continue;
                    }
                    // check unique mount paths
                    for (Info fsInfo : _fsTable.keySet()) {
                        if (fsInfo._mountPath.startsWith(mountPath) || mountPath.startsWith(fsInfo._mountPath)) {
                            logBadEntry("mountpath " + mountPath + " is not unique");
                            continue;
                        }
                    }
                    String[] options = null;
                    if (info.length > OPTIONS_INDEX) {
                        options = info[OPTIONS_INDEX].split(FS_OPTIONS_SEPARATOR);

                    }
                    _infoArray[infoArrayIndex++] = new Info(type, devPath, mountPath, options);
                }

                // now mount the image and heap file systems and record that
                initPartialFSTable(_infoArray, 0, 2);
                _initImageAndHeap = true;
            }
            if (all) {
                initPartialFSTable(_infoArray, 2, _infoArray.length);
                _initFSTable = true;
            }
        }
    }

    /**
     * Process a range off the file systems to be made available
     * @param infoArray
     * @param start
     * @param end
     */
    private static void initPartialFSTable(Info[] infoArray, int start, int end) {
       for (int i = start; i < end; i++) {
            final Info fsInfo = infoArray[i];
            VirtualFileSystem vfs = null;
            if (fsInfo.autoMount()) {
                vfs = initFS(fsInfo);
                // this is considered fatal
                if (vfs == null) {
                    logBadEntry("file system " + fsInfo + " cannot be mounted");
                }
            } else {
                // record the info so that a future path lookup will match,
                // the fs will be mounted at that point.
                _fsTable.put(fsInfo, vfs);
            }
        }        
    }

    /*
     * Nfs is irregular in that a ":" is used internally in the device path, so we recover from that here.
     */
    private static String[] fixupNfs(String[] parts) {
        String[] result = parts;
        if (parts[TYPE_INDEX].equals("nfs")) {
            result = new String[parts.length - 1];
            result[TYPE_INDEX] = parts[TYPE_INDEX];
            result[DEV_INDEX] = parts[DEV_INDEX] + ":" + parts[DEV_INDEX + 1];
            if (parts.length > MOUNT_INDEX + 1) {
                result[MOUNT_INDEX] = parts[MOUNT_INDEX + 1];
                if (parts.length > OPTIONS_INDEX + 1) {
                    result[OPTIONS_INDEX] = parts[OPTIONS_INDEX + 1];
                }
            }
        }
        return result;
    }

    /**
     * Create the fileystem instance.
     *
     * @param info fstable info
     * @return VirtualFileSystem instance
     */
    private static VirtualFileSystem initFS(Info fsInfo) {
        VirtualFileSystem  result = null;
        if (fsInfo._type.equals("ext2")) {
            result =  Ext2FileSystem.create(fsInfo._devPath, fsInfo._mountPath, fsInfo._options);
        } else if (fsInfo._type.equals("nfs")) {
            result =  NfsFileSystem.create(fsInfo._devPath, fsInfo._mountPath);
        } else if (fsInfo._type.equals("sg")) {
            result = SiblingFileSystem.create(fsInfo._devPath, fsInfo._mountPath);
        } else if (fsInfo._type.equals("img")) {
            result = BootImageFileSystem.create();
        } else if (fsInfo._type.equals("heap")) {
            result = HeapFileSystem.create(fsInfo._devPath, fsInfo._mountPath);
        }
        return checkedPut(fsInfo, result);
    }


    private static VirtualFileSystem checkedPut(Info fsInfo, VirtualFileSystem fs) {
        if (fs != null) {
            fsInfo._fs = fs;
            _fsTable.put(fsInfo, fs);
            RootFileSystem.mount(fsInfo);
        }
        return fs;
    }

    public static Info getInfo(VirtualFileSystem vfs) {
        for (Map.Entry<Info, VirtualFileSystem> entry : _fsTable.entrySet()) {
            if (entry.getValue() == vfs) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void close() {
        for (VirtualFileSystem fs :  _fsTable.values()) {
            if (fs != null) {
                fs.close();
            }
        }
    }

    /**
     * Return the file system that exports file or null if none do. This is carefully coded to allow resolution of the
     * image and heap file systems before everything is initialized. Why? The primary reason is to
     * allow the system to boot out of the image file system without initializing any ext2 file systems.
     * This allows, for example, {@link Ext2FileSystem} to be traced with AspectJ.
     * 
     * @param path to match in a file system
     * @return the {@link VirtualFileSystem} that contains this path or {@code null} if none.
     */
    public static VirtualFileSystem exports(String path) {
        VirtualFileSystem vfs = null;
        if (_initFSTable) {
            vfs = partialExports(path);
        } else {
            // try heap and image first
            if (!_initImageAndHeap) {
                initFSTable(false);
            }
            vfs = partialExports(path);
            if (vfs != null) {
                return vfs;
            }
            // full initialization and retry
            initFSTable(true);
            vfs = partialExports(path);
        }
        if (vfs == null) {
            /* We may have been given a path that is above the mount points */
            if (_rootFS.getMode(path) > 0) {
                vfs = _rootFS;
            }
        }
        return vfs;
    }
        
   private static VirtualFileSystem partialExports(String path) {
        assert path != null;
        for (Info fsInfo : _fsTable.keySet()) {
            final int mountLength = fsInfo._mountPath.length();
            if (path.startsWith(fsInfo._mountPath) && (path.length() == mountLength || path.charAt(mountLength) == File.separatorChar)) {
                if (fsInfo._fs == null) {
                    initFS(fsInfo);
                }
                return fsInfo._fs;
            }
        }
        return null;
    }


}
