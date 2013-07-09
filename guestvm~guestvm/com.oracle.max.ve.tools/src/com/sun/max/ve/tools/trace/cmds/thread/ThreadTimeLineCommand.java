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
package com.sun.max.ve.tools.trace.cmds.thread;

import java.util.*;

import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.CreateThreadTraceElement;
import com.sun.max.ve.tools.trace.ThreadState;
import com.sun.max.ve.tools.trace.ThreadStateInterval;
import com.sun.max.ve.tools.trace.TimeFormat;
import com.sun.max.ve.tools.trace.TraceElement;

/**
 * This command outputs the timeline of a thread's state from creation.
 * E.g. a line of the form :
 *   ONCPU cpu a b
 * means the thread was running on CPU cpu from a to b.
 * A summary of the total time in each state is given at the end.
 * If summary argument is given, only the summary is output.
 * @author Mick Jordan
 *
 */
public class ThreadTimeLineCommand extends CommandHelper implements Command {
    private static final String THREAD_ID = "id=";
    private static final String SUMMARY = "summary";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        final String id = stringArgValue(args, THREAD_ID);
        final boolean summary = booleanArgValue(args, SUMMARY);
        checkTimeFormat(args);
        if (id == null) {
            for (CreateThreadTraceElement te : CreateThreadTraceElement.getThreadIterable()) {
                process(traces, te.getId(), summary);
            }
        } else {
            process(traces, Integer.parseInt(id), summary);
        }
    }

    private void process(List<TraceElement> traces, int id, boolean summary) {
        final List<ThreadStateInterval> history = ThreadStateInterval.createIntervals(traces, id);
        final long[] totals = new long[ThreadState.values().length];
        for (int i = 0; i < totals.length; i++) {
            totals[i] = 0;
        }
        if (!summary) {
            System.out.println("\nTimeline for thread " + id);
        }
        for (ThreadStateInterval threadStateInterval : history) {
            if (!summary) {
                System.out.print(threadStateInterval.getState().name());
                if (threadStateInterval.getState() == ThreadState.ONCPU) {
                    System.out.print(" " + threadStateInterval.getCpu());
                }
                System.out.println(" " + threadStateInterval.getStart() + " " + threadStateInterval.getEnd() + " (" + (threadStateInterval.getEnd() - threadStateInterval.getStart()) + ")");
            }
            totals[threadStateInterval.getState().ordinal()] += threadStateInterval.getEnd() - threadStateInterval.getStart();
        }
        System.out.print("Totals for thread " + id);
        System.out.print(": TOTAL " + TimeFormat.byKind(ThreadStateInterval.intervalLength(history), _timeFormat));
        System.out.print(", ONCPU " + TimeFormat.byKind(totals[ThreadState.ONCPU.ordinal()], _timeFormat));
        System.out.print(", OFFCPU " + TimeFormat.byKind(totals[ThreadState.OFFCPU.ordinal()], _timeFormat));
        System.out.print(", BLOCKED " + TimeFormat.byKind(totals[ThreadState.BLOCKED.ordinal()], _timeFormat));
        System.out.println(", INSCHED " + TimeFormat.byKind(totals[ThreadState.INSCHED.ordinal()], _timeFormat));
        final long extra = unaccounted(totals, ThreadStateInterval.intervalLength(history));
        if (extra != 0) {
            System.out.println("** UNACCOUNTED: " + extra);
        }
    }


    private long unaccounted(long[] totals, long total) {
        final long sum = totals[ThreadState.ONCPU.ordinal()] + totals[ThreadState.OFFCPU.ordinal()] +
            totals[ThreadState.BLOCKED.ordinal()] + totals[ThreadState.INSCHED.ordinal()];
        return total - sum;
    }
}
