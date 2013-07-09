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
package com.sun.max.ve.tools.trace.cmds.cpu;

import java.util.*;

import com.sun.max.ve.tools.trace.CPUState;
import com.sun.max.ve.tools.trace.CPUStateDuration;
import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.TimeFormat;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceKind;
import com.sun.max.ve.tools.trace.TraceMain;
import com.sun.max.ve.tools.trace.TraceReader;


public class CPUTimeLineCommand extends CommandHelper implements Command {
    private static final String SUMMARY = "summary";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        final boolean summary = booleanArgValue(args, SUMMARY);
        final int cpu = TraceMain.CPU.getValue();
        checkTimeFormat(args);
        if (cpu < 0) {
            for (int c = 0; c < TraceElement.getCpus(); c++) {
                process(traces, c, summary);
            }
        } else {
            process(traces, cpu, summary);
        }

    }

    private void process(List<TraceElement> traces, int id, boolean summary) {
        final List<CPUStateDuration> history = new ArrayList<CPUStateDuration>();
        CPUStateDuration current = null;
        for (TraceElement trace : traces) {
            if (trace.getTraceKind() == TraceKind.RI) {
                // first execution of the idle thread
                if (trace.getCpu() == id) {
                    current = newCPUStateDuration(current, CPUState.RUNNING, trace, history);
                }
            } else if (trace.getTraceKind() == TraceKind.BI) {
                // block idle thread
                if (trace.getCpu() == id) {
                    current = newCPUStateDuration(current, CPUState.IDLE, trace, history);
                }
            } else if (trace.getTraceKind() == TraceKind.WI) {
                // wake idle thread
                if (trace.getCpu() == id) {
                    current = newCPUStateDuration(current, CPUState.RUNNING, trace, history);
                }
            }
        }
        if (current.getStop() == 0) {
            current.setStop(TraceReader.lastTrace().getTimestamp());
        }

        final long[] totals = new long[CPUState.values().length];
        for (int i = 0; i < totals.length; i++) {
            totals[i] = 0;
        }

        System.out.println("Timeline for CPU " + id);
        for (CPUStateDuration sd : history) {
            if (!summary) {
                System.out.print(sd.getState().name());
                System.out.println(" " + sd.getStart() + " " + sd.getStop() + " (" + (sd.getStop() - sd.getStart()) + ")");
            }
            totals[sd.getState().ordinal()] += sd.getStop() - sd.getStart();
        }
        System.out.print("Summary:");
        System.out.print(" RUNNING " + TimeFormat.byKind(totals[CPUState.RUNNING.ordinal()], _timeFormat));
        System.out.println(" IDLE " + TimeFormat.byKind(totals[CPUState.IDLE.ordinal()], _timeFormat));
    }

    private CPUStateDuration newCPUStateDuration(CPUStateDuration oldCPUStateDuration, CPUState state, TraceElement traceElement, List<CPUStateDuration> history) {
        if (oldCPUStateDuration != null) {
            oldCPUStateDuration.setStop(traceElement.getTimestamp());
        }
        final CPUStateDuration result = new CPUStateDuration(state, traceElement.getTimestamp());
        history.add(result);
        return result;
    }

}
