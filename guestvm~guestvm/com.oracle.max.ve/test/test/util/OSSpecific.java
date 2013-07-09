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
package test.util;

/**
 * Some methods that can be called in any OS environment but call GUK via reflection if that's where we are running.
 * @author Mick Jordan
 */

import java.lang.reflect.*;

public final class OSSpecific implements Runnable {

    private static boolean _initialized;
    private static Method _threadStatsMethod;
    private static Method _setTimeSliceMethod;
    private static Method _setTraceStateMethod;
    private static Object[] _nullArgs = new Object[0];
    private int _period;

    public static void printThreadStats() {
        if (!_initialized) {
            initialize();
        }
        if (_threadStatsMethod != null) {
            try {
                _threadStatsMethod.invoke(null, _nullArgs);
            } catch (Throwable t) {

            }
        }
    }

    public static void setTimeSlice(Thread thread, int time) {
        if (!_initialized) {
            initialize();
        }
        if (_setTimeSliceMethod != null) {
            try {
                _setTimeSliceMethod.invoke(null, new Object[] {thread, time});
            } catch (Throwable t) {

            }
        }
    }

    public static boolean setTraceState(int ordinal, boolean value) {
        if (!_initialized) {
            initialize();
        }
        if (_setTraceStateMethod != null) {
            try {
                final Boolean result = (Boolean) _setTraceStateMethod.invoke(null, new Object[] {ordinal, value});
                return result.booleanValue();
            } catch (Throwable t) {

            }
        }
        return false;
    }

    private OSSpecific(int period) {
        _period = period;
    }

    /**
     * Periodically print thread statistics.
     *
     * @param period
     *            in millisecs
     */
    public static void periodicThreadStats(int period) {
        if (!_initialized) {
            initialize();
        }
        if (_threadStatsMethod != null) {
            final Thread t = new Thread(new OSSpecific(period), "ThreadStats");
            t.setDaemon(true);
            t.start();
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(_period);
            } catch (InterruptedException ex) {

            }
            printThreadStats();
        }
    }

    private static void initialize() {
        final String os = System.getProperty("os.name");
        if (os.equals("VE")) {
            try {
                final Class<?> schedClass = Class.forName("com.sun.max.ve.guk.GUKScheduler");
                final Class<?> traceClass = Class.forName("com.sun.max.ve.guk.GUKTrace");
                final Class<?> vmThreadClass = Class.forName("com.sun.max.vm.thread.VmThread");
                _threadStatsMethod = schedClass.getMethod("printRunQueue", (Class<?>[]) null);
                _setTimeSliceMethod = schedClass.getMethod("setThreadTimeSlice", new Class<?>[] {vmThreadClass, int.class});
                _setTraceStateMethod = traceClass.getMethod("setTraceState", new Class<?>[] {int.class, boolean.class});

            } catch (Throwable t) {
                System.err.println("OSSpecific: error finding GUK methods: " + t);
            }
        }
        _initialized = true;
    }
}
