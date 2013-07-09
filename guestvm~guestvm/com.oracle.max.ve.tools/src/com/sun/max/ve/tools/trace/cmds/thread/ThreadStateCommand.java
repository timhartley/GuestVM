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

import java.util.List;

import com.sun.max.ve.tools.trace.CPUState;
import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.CreateThreadTraceElement;
import com.sun.max.ve.tools.trace.ThreadState;
import com.sun.max.ve.tools.trace.ThreadStateInterval;
import com.sun.max.ve.tools.trace.TimeFormat;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceKind;

/**
 * This command reports on the state of a thread at a given time (default is end of trace)
 * and other misc queries.
 *
 * @see ThreadState
 *
 * @author Mick Jordan
 *
 */

public class ThreadStateCommand extends CommandHelper implements Command {
    private static final String THREAD_ID = "id=";
    private static final String TIME = "time=";
    private static final String LASTONCPU = "lastoncpu";
    private static final String CREATEINFO = "create";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        long endTime = Long.MAX_VALUE;
        final String time = stringArgValue(args, TIME);
        if (time != null) {
            endTime = Long.parseLong(time);
        }
        final boolean lastoncpu = booleanArgValue(args, LASTONCPU);
        final boolean create = booleanArgValue(args, CREATEINFO);
        final String id = stringArgValue(args, THREAD_ID);
        checkTimeFormat(args);

        if (id == null) {
            for (CreateThreadTraceElement te : CreateThreadTraceElement.getThreadIterable()) {
                if (lastoncpu) {
                    processLastOnCpu(traces, te.getId());
                } else if (create) {
                    processCreateInfo(traces, te.getId());
                } else {
                    processTime(traces, te.getId(), endTime);
                }
            }
        } else {
            if (lastoncpu) {
                processLastOnCpu(traces, Integer.parseInt(id));
            } else if (create) {
                processCreateInfo(traces, Integer.parseInt(id));
            } else {
                processTime(traces, Integer.parseInt(id), endTime);
            }
        }

    }

    private void processTime(List<TraceElement> traces, int id, long endTime) {
        final List<ThreadStateInterval> history = ThreadStateInterval.createIntervals(traces, id);
        final Match match = findMatch(history, id, endTime);
        ThreadStateInterval tsi = match._match;
        if (tsi == null) {
            if (endTime == Long.MAX_VALUE && match._last != null) {
                tsi = match._last;
            }
        }
        System.out.println("Thread " + id + " state at " + endTime + " is " + (tsi == null ? "NON_EXISTENT" : tsi));
    }

    static class Match {
        ThreadStateInterval _match;
        ThreadStateInterval _last;
    }

    private Match findMatch(List<ThreadStateInterval> history, int id, long endTime) {
        final Match result = new Match();
        for (ThreadStateInterval tsi : history) {
            if (endTime >= tsi.getStart() && endTime <= tsi.getEnd()) {
                result._match = tsi;
                break;
            }
            result._last = tsi;
        }
        return result;
    }

    private void processLastOnCpu(List<TraceElement> traces, int id) {
        final List<ThreadStateInterval> history = ThreadStateInterval.createIntervals(traces, id);
        final int endIndex = history.size() - 1;
        for (int i = endIndex; i >= 0; i--) {
            final ThreadStateInterval tsi = history.get(i);
            if (tsi.getState() == ThreadState.ONCPU) {
                System.out.println("Thread " + id + " last ONCPU at " + TimeFormat.byKind(tsi.getEnd(), _timeFormat));
                return;
            }
        }
        System.out.println("Thread " + id + " never ONCPU");
    }

    private void processCreateInfo(List<TraceElement> traces, int id) {
        for (int i = 0; i < traces.size(); i++) {
            final TraceElement t = traces.get(i);
            if (t.getTraceKind() == TraceKind.CT) {
                final CreateThreadTraceElement ct = (CreateThreadTraceElement) t;
                if (ct.getId() == id) {
                    System.out.println(ct);
                    // what was the state of the CPUs and other threads at this point?
                    for (int cpu = 0; cpu < TraceElement.getCpus(); cpu++) {
                        System.out.println("CPU " + cpu + " " + getCPUState(traces, cpu, i).name());
                    }
                    for (CreateThreadTraceElement ctx : CreateThreadTraceElement.getThreadIterable()) {
                        if (ctx != ct) {
                            final List<ThreadStateInterval> history = ThreadStateInterval.createIntervals(traces, ctx.getId());
                            final Match match = findMatch(history, ctx.getId(), ct.getTimestamp());
                            if (match._match != null) {
                                System.out.println("Thread " + ctx.getId() + " is " + match._match.getState());
                            }
                        }
                    }

                }
            }
        }
    }

    private CPUState getCPUState(List<TraceElement> traces, int cpu, int index) {
        for (int i = index; i > 0; i--) {
            final TraceElement t = traces.get(i);
            if ((t.getTraceKind() == TraceKind.WI || t.getTraceKind() == TraceKind.RI) && t.getCpu() == cpu) {
                return CPUState.RUNNING;

            } else if (t.getTraceKind() == TraceKind.BI && t.getCpu() == cpu) {
                return CPUState.IDLE;
            }
        }
        return CPUState.UNKNOWN;
    }

}
