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
package com.sun.max.ve.sched;

import java.util.*;

import com.sun.max.ve.error.*;
import com.sun.max.vm.thread.*;

/**
 * MaxVE implementation of the VMThreadFactory.
 * Although we don't know the exact subclass that will be created, we build
 * a cache of instances at image build time for runtime use by newVmThread.
 * This is to ensure that all VmThread instances are in the boot heap
 * and therefore avoid issues with entry to the scheduler while GC is taking place.
 * N.B. Even if the GC threads are non pre-emptable entry to the scheduler
 * can still occur due to an external event which typically requires touching
 * a VmThread instance.
 *
 * @author Mick Jordan
 *
 */

public class VEVmThreadFactory extends VmThreadFactory {

    private static final int MAX_THREADS = 16384;
    private static List<VmThread> _vmThreadCache;
    private static int _vmThreadCacheSize;

    static void populateVmThreadCache() {
        _vmThreadCache = new LinkedList<VmThread>();
        for (int i = 0; i < MAX_THREADS; i++) {
            _vmThreadCache.add(RunQueueFactory.getInstance().newVmThread());
        }
    }

    @Override
    protected VmThread newVmThread(Thread javaThread) {
        if (_vmThreadCache == null) {
            populateVmThreadCache();
        }
        if (_vmThreadCacheSize >= MAX_THREADS) {
            VEError.unexpected("thread limit exceeded");
        }
        final VmThread vmThread = _vmThreadCache.remove(0);
        _vmThreadCacheSize++;
        if (javaThread != null) {
            vmThread.setJavaThread(javaThread, javaThread.getName());
        }
        return vmThread;
    }

}
