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
package com.sun.max.vm.run.max.ve;

import org.jnode.fs.ext2.cache.ByteBufferFactory;

import sun.nio.ch.BBNativeDispatcher;
import sun.rmi.registry.RegistryImpl;

import com.sun.max.annotate.*;
import com.sun.max.ve.*;
import com.sun.max.ve.attach.AttachListener;
import com.sun.max.ve.error.*;
import com.sun.max.ve.fs.FSTable;
import com.sun.max.ve.fs.nfs.NFSExports;
import com.sun.max.ve.guk.*;
import com.sun.max.ve.logging.Logger;
import com.sun.max.ve.memory.HeapPool;
import com.sun.max.ve.net.guk.*;
import com.sun.max.ve.profiler.*;
import com.sun.max.ve.sched.*;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.run.java.JavaRunScheme;
import com.sun.max.vm.*;


/**
 * This run scheme is used to launch a Maxine VE application.
 * It performs some important initialization prior to the loading
 * of the application's main method.
 * 
 * It also closes down the file systems on VM termination.
 *
 * @author Mick Jordan
 *
 */

public class VERunScheme extends JavaRunScheme {

    private static final String RMIREGISTRY_PROPERTY = "max.ve.rmiregistry";
    private static final String TICK_PROFILER_PROPERTY = "max.ve.profiler";
    private static final String GUK_TRACE_PROPERTY = "max.ve.guktrace";

    @HOSTED_ONLY
    public VERunScheme() {
        super();
    }
    
    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.STARTING) {
            // make sure we have console output in case of exceptions
            FSTable.basicInit();
            // install our custom direct byte buffer factory
            System.setProperty(ByteBufferFactory.BYTEBUFFER_FACTORY_CLASS_PROPERTY_NAME, "org.jnode.fs.ext2.cache.PageByteBufferFactory");
            Logger.resetLogger();
        }
        super.initialize(phase);

        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
            Heap.registerHeapSizeInfo(HeapPool.getHeapSizeInfo());
        }

        if (phase == MaxineVM.Phase.PRIMORDIAL) {
            GUK.initialize();
            GUKScheduler.initialize();
        } else if (phase == MaxineVM.Phase.RUNNING) {
            System.setProperty("max.ve.version", Version.ID);
            System.setProperty("os.version", Version.ID);
            SchedulerFactory.scheduler().starting();
            GUKPagePool.createTargetMemoryThread(GUKPagePool.getCurrentReservation() * 4096);
            BBNativeDispatcher.resetNativeDispatchers();
            NetInit.init();
            NFSExports.initNFSExports();
            checkRmiRegistry();
            AttachListener.create();
            checkGUKTrace();
            checkTickProfiler();
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            FSTable.close();
        }
    }

    public static boolean launcherInit() {
        VEError.unexpected("FIX THIS");
        return false;
    }

    private static void checkRmiRegistry() {
        final String rmiRegistryProperty = System.getProperty(RMIREGISTRY_PROPERTY);
        String portArg = null;
        if (rmiRegistryProperty != null) {
            if (rmiRegistryProperty.length() > 0) {
                portArg = rmiRegistryProperty;
            }
            final String[] args = portArg == null ? new String[0] : new String[] {portArg};
            final Thread registry = new Thread() {
                @Override
                public void run() {
                    try {
                        RegistryImpl.main(args);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        VEError.unexpected("rmiregistry failed");
                    }
                }
            };
            registry.setName("RMIRegistry");
            registry.setDaemon(true);
            registry.start();
        }
    }

    private static void checkTickProfiler() {
        final String prop = System.getProperty(TICK_PROFILER_PROPERTY);
        if (prop != null) {
            int interval = 0;
            int depth = 4;
            int dumpPeriod = 0;
            if (prop.length() > 0) {
                final String[] options = prop.split(",");
                for (String option : options) {
                    if (option.startsWith("interval")) {
                        interval = getTickOption(option);
                    } else if (option.startsWith("depth")) {
                        depth = getTickOption(option);
                    } else if (option.startsWith("dump")) {
                        dumpPeriod = getTickOption(option);
                    } else {
                        tickUsage();
                    }
                }
            }
            if (interval < 0 || depth < 0) {
                tickUsage();
            }
            Tick.create(interval, depth, dumpPeriod);
        }
    }

    private static int getTickOption(String s) {
        final int index = s.indexOf('=');
        if (index < 0) {
            return index;
        }
        return Integer.parseInt(s.substring(index + 1));
    }

    private static void tickUsage() {
        VEError.exit("usage: " + TICK_PROFILER_PROPERTY + "[=interval=i,depth=d,dump=t]");
    }

    private static void checkGUKTrace() {
        final String prop = System.getProperty(GUK_TRACE_PROPERTY);
        if (prop != null) {
            final String[] parts = prop.split(":");
            for (String name : parts) {
                try {
                    GUKTrace.setTraceState(GUKTrace.Name.valueOf(name), true);
                } catch (Exception ex) {
                    System.err.println("no GUK trace element '" + name + "' ignoring");
                }
            }
        }

    }
}
