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
package com.sun.max.ve.tools.trace;


public class TraceElement {
    protected long _absTimestamp;
    private long _relTimestamp;
    private int _cpu;
    private int _thread;
    private TraceKind _traceKind;

    private static long _absStartTime;
    private static boolean _useRelTimestamp = true;
    private static int _numCpus;
    private static boolean _firstTrace = true;

    public TraceElement init(TraceKind traceKind, long timestamp, int cpu, int thread) {
        _traceKind = traceKind;
        _absTimestamp = timestamp;
        _cpu = cpu;
        _thread = thread;
        if (_firstTrace) {
            _absStartTime = timestamp;
            _firstTrace = false;
        }
        if (traceKind == TraceKind.RI) {
            _numCpus++;
        }
        _relTimestamp = _absTimestamp - _absStartTime;
        return this;
    }

    public static void setUseAbsTimestamp(boolean on) {
        _useRelTimestamp = !on;
    }

    public static int getCpus() {
        return _numCpus;
    }

    public long getTimestamp() {
        return _useRelTimestamp  ? _relTimestamp : _absTimestamp;
    }

    public int getCpu() {
        return _cpu;
    }

    public int getThread() {
        return _thread;
    }

    public TraceKind getTraceKind() {
        return _traceKind;
    }

    @Override
    public String toString() {
        return getTimestamp() + " " + _cpu + " " + _thread + " " + _traceKind.name();
    }

}
