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
package com.sun.max.ve.tools.trace.cmds.sched;

import java.util.List;

import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.ThreadSwitchTraceElement;
import com.sun.max.ve.tools.trace.TimeFormat;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceKind;


public class SchedStatsCommand extends CommandHelper implements Command {

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        checkTimeFormat(args);
        long max = 0;
        long min = Long.MAX_VALUE;
        long sum = 0;
        int calls = 0;
        ThreadSwitchTraceElement mts = null;
        for (TraceElement t : traces) {
            if (t.getTraceKind() == TraceKind.TS) {
                final ThreadSwitchTraceElement ts = (ThreadSwitchTraceElement) t;
                calls++;
                final long schedTime = ts.getSchedTime();
                sum += schedTime;
                if (schedTime > max) {
                    max = schedTime;
                    mts = ts;
                } else if (schedTime < min) {
                    min = schedTime;
                }
            }
        }

        System.out.println("Schedule stats: calls: " + calls + ", avg: " + TimeFormat.byKind(sum / calls, _timeFormat) +
                        ", min: " + TimeFormat.byKind(min, _timeFormat) + ", max: " + TimeFormat.byKind(max, _timeFormat));
        System.out.println("max element: " + mts);
    }

}
