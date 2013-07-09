package org.openthinclient;

/*******************************************************************************
 * openthinclient.org ThinClient suite
 *
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/
/*
 * This code is based on: JNFSD - Free NFSD. Mark Mitchell 2001
 * markmitche11@aol.com http://hometown.aol.com/markmitche11
 */
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.acplt.oncrpc.apps.jportmap.OncRpcEmbeddedPortmap;
import org.acplt.oncrpc.apps.jportmap.jportmap;
import org.openthinclient.mountd.Exporter;
import org.openthinclient.mountd.Group;
import org.openthinclient.mountd.ListExporter;
import org.openthinclient.mountd.MountDaemon;
import org.openthinclient.mountd.NFSExport;
import org.openthinclient.nfsd.NFSServer;
import org.openthinclient.nfsd.PathManager;

public class NFSServerMain {
    public static void main(String[] argv) throws Exception {

        // Check for PORTMAP, if there isn't one, start embedded PM
        System.err.print("Checking for PORTMAP Server...");
        System.err.flush();
        jportmap pm = null;
        if (OncRpcEmbeddedPortmap.isPortmapRunning()) {
            System.err.println("FOUND");
        } else {
            System.err.println("NOT FOUND");
            try {
                System.err.println("Starting PORTMAP Server");
                pm = new jportmap();
                final jportmap p = pm;
                new Thread("portmapper") {
                    @Override
                    public void run() {
                        try {
                            System.err.println("Starting portmapper");
                            p.run(p.transports);
                            System.err.println("portmapper exited");
                        } catch (Throwable th) {
                            th.printStackTrace();
                            System.err.println("portmapper");
                            System.exit(1);
                        }
                    }
                }.start();
            } catch (Throwable th) {
                th.printStackTrace();
                System.err.println("Failed to start PORTMAP Server");
                System.exit(1);
            }
        }

        NFSExport e[] = new NFSExport[1];
        ArrayList<Group> groups = new ArrayList<Group>();
        groups.add(new Group());
        e[0] = new NFSExport("/root", new File("/").getAbsoluteFile(), groups);
        final Exporter exporter = new ListExporter(e);

        final PathManager pathManager = new PathManager(
                                                        new File(
                                                                 "nfs-handles.db"),
                                                        exporter);

        // Start server threads
        final NFSServer nfs = new NFSServer(pathManager, 0, 0);
        new Thread("NFS server") {
            @Override
            public void run() {
                try {
                    System.err.println("Starting NFS Server");
                    nfs.run();
                    System.err.println("NFS Server exited");
                } catch (Throwable th) {
                    th.printStackTrace();
                    System.err.println("NFS Server failed");
                    System.exit(1);
                }
            }
        }.start();

        final MountDaemon mountd = new MountDaemon(pathManager, exporter, 0, 0);
        new Thread("MOUNT daemon") {
            @Override
            public void run() {
                setName("MOUNT Server");
                try {
                    System.err.println("Starting MOUNT Server");
                    mountd.run();
                    System.err.println("MOUNT Server exited");
                } catch (Throwable th) {
                    th.printStackTrace();
                    System.err.println("MOUNT Server failed");
                    System.exit(1);
                }
            }
        }.start();

        // This is a hack to run a main() on some class, for POC only
        if (argv.length != 0) {
            System.out.println();
            String className = argv[0];
            String[] newArgv = new String[argv.length - 1];
            System.arraycopy(argv, 1, newArgv, 0, argv.length - 1);
            Class<?> clazz = NFSServerMain.class.getClassLoader().loadClass(
                                                                            className);
            Method main = clazz.getDeclaredMethod("main", newArgv.getClass());
            main.invoke(null, new Object[] { newArgv });
        }

        // Keep the server running - obviously needs a more sophisticated control system ;)
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // If we are the portmap server, then wait for it to die
        // If it does, kill proc
        // if (pm != null) {
        // try {
        // pm.getEmbeddedPortmapServiceThread().join();
        // } catch (Throwable th) {
        // th.printStackTrace();
        // System.err.println("We were interrupted");
        // }
        // System.err.println("PORTMAP Server exited");
        // }

        nfs.stopRpcProcessing();
        mountd.stopRpcProcessing();
        if (pm != null) {
            pm.stopRpcProcessing();
        }

        System.out.println("I'm gone!");
    }
}
