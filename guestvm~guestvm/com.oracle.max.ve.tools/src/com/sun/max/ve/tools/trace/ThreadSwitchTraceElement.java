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


public class ThreadSwitchTraceElement extends TraceElement {

    private int _fromId;
    private int _toId;
    private long _runningTime;
    private long _schedStartTime;

    public ThreadSwitchTraceElement setFromId(int id) {
        _fromId = id;
        return this;
    }

    public ThreadSwitchTraceElement setToId(int id) {
        _toId = id;
        return this;
    }

    public ThreadSwitchTraceElement setRunningTime(long t) {
        _runningTime = t;
        return this;
    }

    public ThreadSwitchTraceElement setSchedStartTime(long t) {
        _schedStartTime = t;
        return this;
    }

    public int getFromId() {
        return _fromId;
    }

    public int getToId() {
        return _toId;
    }

    public long getRunningTime() {
        return _runningTime;
    }

    public long getSchedTime() {
        return _absTimestamp - _schedStartTime;
    }

    @Override
    public String toString() {
        return super.toString() + " " + _fromId + " " + _toId + " " + _runningTime + " " + _schedStartTime;
    }

}
