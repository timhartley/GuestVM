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
package com.sun.max.ve.jdk;

import java.lang.management.*;
import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.ve.sched.*;
import com.sun.max.vm.Log;
import com.sun.max.vm.management.ThreadManagement;
import com.sun.max.vm.thread.VmThread;

/**
 * Substitutions for the native methods in @see sun.management.ThreadImpl.
 * Many of these are unimplemented as yet.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.management.ThreadImpl")
final class JDK_sun_management_ThreadImpl {
    @SUBSTITUTE
    private static Thread[] getThreads() {
        return ThreadManagement.getThreads();
    }

    @SUBSTITUTE
    private static void getThreadInfo0(long[] ids, int maxDepth, ThreadInfo[] result) {
        ThreadManagement.getThreadInfo(ids, maxDepth, result);
    }

    @SUBSTITUTE
    private static long getThreadTotalCpuTime0(long id) {
        final GUKVmThread gvmThread = (GUKVmThread)  (id == 0 ? VmThread.current() : VmThread.fromJava(ThreadManagement.findThread(id)));
        return gvmThread.getRunningTime();
    }

    @SUBSTITUTE
    private static long getThreadUserCpuTime0(long id) {
        return getThreadTotalCpuTime0(id);
    }

    @SUBSTITUTE
    private static void setThreadCpuTimeEnabled0(boolean enable) {
        ThreadManagement.setThreadCpuTimeEnabled(enable);
    }

    @SUBSTITUTE
    private static void setThreadContentionMonitoringEnabled0(boolean enable) {
        ThreadManagement.setThreadCpuTimeEnabled(enable);
    }

    @SUBSTITUTE
    private static Thread[] findMonitorDeadlockedThreads0() {
        return ThreadManagement.findMonitorDeadlockedThreads();
    }

    @SUBSTITUTE
    private static Thread[] findDeadlockedThreads0() {
        Log.println("findDeadlockedThreads0 not implemented, returning null");
        return null;
    }

    @SUBSTITUTE
    private static void resetPeakThreadCount0() {
        ThreadManagement.resetPeakThreadCount();
    }

    @SUBSTITUTE
    private static ThreadInfo[] dumpThreads0(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers) {
        return ThreadManagement.dumpThreads(ids, lockedMonitors, lockedSynchronizers);
    }

    @SUBSTITUTE
    private static void resetContentionTimes0(long tid) {
        unimplemented("resetContentionTimes0");
    }

    private static void unimplemented(String name) {
        VEError.unimplemented("unimplemented sun.management.ThreadImpl." + name);
    }

}
