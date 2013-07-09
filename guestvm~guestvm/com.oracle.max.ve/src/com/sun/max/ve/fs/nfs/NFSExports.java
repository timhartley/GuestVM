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

/**
 * Export file systems as NFS exports
 */
package com.sun.max.ve.fs.nfs;

import java.io.File;

import org.acplt.oncrpc.apps.jportmap.jportmap;
import org.openthinclient.mountd.ListExporter;
import org.openthinclient.mountd.MountDaemon;
import org.openthinclient.mountd.NFSExport;
import org.openthinclient.nfsd.NFSServer;
import org.openthinclient.nfsd.PathManager;

import com.sun.max.ve.error.VEError;


/**
 *
 * @author Puneeet Lakhina
 *
 */
public class NFSExports {
    private static final String NFS_EXPORTS_PROPERTY = "max.ve.nfs.exports";
    private static final String NFS_EXPORTS_LOCAL_EXPORT_SEPARATOR = ":";
    private static final String NFS_EXPORTS_SEPARATOR = ";";
    private static boolean _initNFSExports  =  false;
    private static Thread _portMapThread;
    private static Thread _mountdThread;
    private static Thread _nfsServerThread;
    /**Initializes the NFS Exports.
     *
     */
    public static void initNFSExports() {
        if (_initNFSExports) {
            return;
        }
        final String exporttable  =  System.getProperty(NFS_EXPORTS_PROPERTY);
        if (exporttable !=  null) {
            final String[] exportTableEntries  =  exporttable.split(NFS_EXPORTS_SEPARATOR);
            final ListExporter exporter  =  new ListExporter();
            for (String entry : exportTableEntries) {
                final String[] localExportPath  =  entry.split(NFS_EXPORTS_LOCAL_EXPORT_SEPARATOR);
                if (localExportPath.length !=  2) {
                    logBadEntry("Improper entry in the nfs exports. " + entry + " Does not contain both the local and the export path separated by \"" + NFS_EXPORTS_LOCAL_EXPORT_SEPARATOR + "\"");
                }
                final String localPath  =  localExportPath[0];
                final String exportPath  =  localExportPath[1];
                exporter.addExport(new NFSExport(exportPath, new File(localPath)));
            }
            System.out.println("Starting portmap server");
            try {
                final jportmap pm  =  new jportmap();
                _portMapThread  =  new Thread() {
                    @Override
                    public void run() {
                        try {
                            pm.run(pm.transports);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logFailureAndExit("PortMap server error.");
                        }
                    }
                };
                _portMapThread.setDaemon(true);
                _portMapThread.start();
                System.out.println("Starting NFS server");
                final PathManager pathManager  =  new PathManager(new File("nfs-handles.db"), exporter);
                final NFSServer nfs  =  new NFSServer(pathManager, 0, 0);
                _nfsServerThread  =  new Thread() {
                    @Override
                    public void run() {
                        try {
                            nfs.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                            logFailureAndExit("NFS server error.");
                        }
                    }
                };
                _nfsServerThread.setDaemon(true);
                _nfsServerThread.start();
                System.out.println("Starting mount server");
                final MountDaemon mountd  =  new MountDaemon(pathManager, exporter, 0, 0);
                _mountdThread  =  new Thread() {
                    @Override
                    public void run() {
                        try {
                            mountd.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                            logFailureAndExit("NFS server error.");
                        }
                    }
                };
                _mountdThread.setDaemon(true);
                _mountdThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static void logBadEntry(String msg) {
        VEError.exit(msg);
    }
    private static void logFailureAndExit(String msg) {
        VEError.exit(msg);
    }
    /**Forcefully shutdown NFS Server and related threads. Not used currently anywhere. The shutdown in the VM is achieved  by marking the threads daemon
     *
     */
    public static void stopNFSExports() {
        _mountdThread.interrupt();
        _nfsServerThread.interrupt();
        _portMapThread.interrupt();
    }
}
