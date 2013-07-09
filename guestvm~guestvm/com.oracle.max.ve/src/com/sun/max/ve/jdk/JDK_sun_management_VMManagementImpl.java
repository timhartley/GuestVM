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

import java.lang.reflect.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.FieldActor;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.management.*;
import com.sun.max.ve.error.*;

/**
 * Substitutions for the native methods in @see sun.management.VMManagementImpl.
 * Most of these are unimplemented as yet.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.management.VMManagementImpl")

final class JDK_sun_management_VMManagementImpl {

    private static String[] _supportedOptions = {"currentThreadCpuTimeSupport", "otherThreadCpuTimeSupport", "objectMonitorUsageSupport"};

    private static boolean isSupported(String name) {
        for (String s : _supportedOptions) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }

    @SUBSTITUTE
    private static String getVersion0() {
        return "0.0";
    }

    @SUBSTITUTE
    private static void initOptionalSupportFields() {
        try {
            final Class<?> klass = Class.forName("sun.management.VMManagementImpl");
            final Object staticTuple = ClassActor.fromJava(klass).staticTuple();
            final Field[] fields = klass.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                final Field field = fields[i];
                final String fieldName = field.getName();
                if (fieldName.endsWith("Support")) {
                    FieldActor.fromJava(field).setBoolean(staticTuple, isSupported(fieldName));
                }
            }
        } catch (Exception ex) {
            VEError.unexpected("problem initializing sun.management.VMManagementImpl " + ex);
        }
    }

    @SUBSTITUTE
    private boolean isThreadContentionMonitoringEnabled() {
        return false;
    }

    @SUBSTITUTE
    private boolean isThreadCpuTimeEnabled() {
        return true;
    }

    @SUBSTITUTE
    private long getTotalClassCount() {
        return ClassLoadingManagement.getTotalClassCount();
    }

    @SUBSTITUTE
    private long getUnloadedClassCount() {
        return ClassLoadingManagement.getUnloadedClassCount();
    }

    @SUBSTITUTE
    private boolean getVerboseClass() {
        return VMOptions.verboseOption.verboseClass;
    }

    @SUBSTITUTE
    private boolean getVerboseGC() {
        return Heap.verbose();
    }

    @SUBSTITUTE
    private int getProcessId() {
        final String pid = System.getProperty("guestvm.pid");
        return pid == null ? 0 : Integer.parseInt(pid);
    }

    @SUBSTITUTE
    private String getVmArguments0() {
        return RuntimeManagement.getVmArguments();
    }

    @SUBSTITUTE
    private long getStartupTime() {
        return RuntimeManagement.getStartupTime();
    }

    @SUBSTITUTE
    private int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    @SUBSTITUTE
    private long getTotalCompileTime() {
        unimplemented("getTotalCompileTime");
        return 0;
    }

    @SUBSTITUTE
    private long getTotalThreadCount() {
        return ThreadManagement.getTotalThreadCount();
    }

    @SUBSTITUTE
    private int  getLiveThreadCount() {
        return ThreadManagement.getThreads().length;
    }

    @SUBSTITUTE
    private int  getPeakThreadCount() {
        return ThreadManagement.getPeakThreadCount();
    }

    @SUBSTITUTE
    private int  getDaemonThreadCount() {
        return ThreadManagement.getDaemonThreadCount();
    }

    @SUBSTITUTE
    private long getSafepointCount() {
        unimplemented("getSafepointCount");
        return 0;
    }

    @SUBSTITUTE
    private long getTotalSafepointTime() {
        unimplemented("getTotalSafepointTime");
        return 0;
    }

    @SUBSTITUTE
    private long getSafepointSyncTime() {
        unimplemented("getSafepointSyncTime");
        return 0;
    }

    @SUBSTITUTE
    private long getTotalApplicationNonStoppedTime() {
        unimplemented("getTotalApplicationNonStoppedTime");
        return 0;
    }

    @SUBSTITUTE
    private long getLoadedClassSize() {
        unimplemented("getLoadedClassSize");
        return 0;
    }

    @SUBSTITUTE
    private long getUnloadedClassSize() {
        unimplemented("getUnloadedClassSize");
        return 0;
    }

    @SUBSTITUTE
    private long getClassLoadingTime() {
        unimplemented("getClassLoadingTime");
        return 0;
    }

    @SUBSTITUTE
    private long getMethodDataSize() {
        unimplemented("getMethodDataSize");
        return 0;
    }

    @SUBSTITUTE
    private long getInitializedClassCount() {
        unimplemented("getInitializedClassCount");
        return 0;
    }

    @SUBSTITUTE
    private long getClassInitializationTime() {
        unimplemented("getClassInitializationTime");
        return 0;
    }

    @SUBSTITUTE
    private long getClassVerificationTime() {
        unimplemented("getClassVerificationTime");
        return 0;
    }

    private static void unimplemented(String name) {
        VEError.unimplemented("unimplemented sun.management.VMManagementImpl." + name);
    }
}
