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

import java.util.ArrayList;
import java.util.List;


public class ThreadStateInterval {
    private ThreadState _state;
    private long _start;
    private long _end;
    private int _cpu;

    public ThreadStateInterval(ThreadState state, long start) {
        _state = state;
        _start = start;
    }

    public ThreadState getState() {
        return _state;
    }

    public long getStart() {
        return _start;
    }

    public long getEnd() {
        return _end;
    }

    public void setEnd(long end) {
        _end = end;
    }

    public int getCpu() {
        return _cpu;
    }

    public void setCpu(int cpu) {
        _cpu = cpu;
    }

    @Override
    public String toString() {
        String result = _state.name();
        if (_state == ThreadState.ONCPU) {
            result += " " + _cpu;
        }
        return result;
    }

    public static long intervalLength(List<ThreadStateInterval> intervals) {
        final ThreadStateInterval te = intervals.get(intervals.size() - 1);
        long end;
        if (te.getState() == ThreadState.REAPED) {
            end = intervals.get(intervals.size() - 2).getStart();
        } else if (te.getState() == ThreadState.EXITED) {
            end = te.getStart();
        } else {
            end = te.getEnd();
        }
        return end - intervals.get(0).getStart();
    }

    public static List<ThreadStateInterval> createIntervals(List<TraceElement> traces, int id) {
        final List<ThreadStateInterval> history = new ArrayList<ThreadStateInterval>();
        ThreadStateInterval current = null;
        for (TraceElement trace : traces) {
            if (trace.getTraceKind() == TraceKind.BK) {
                final ThreadIdTraceElement ctrace = (ThreadIdTraceElement) trace;
                if (ctrace.getId() == id) {
                    if (current.getState() != ThreadState.EXITED && current.getState() != ThreadState.INSCHED) {
                        current = newThreadStateInterval(current, ThreadState.INSCHED, ctrace, history);
                    }
                }
            } else if (trace.getTraceKind() == TraceKind.WK) {
                final ThreadIdTraceElement ctrace = (ThreadIdTraceElement) trace;
                if (ctrace.getId() == id) {
                    if (current.getState() != ThreadState.INSCHED) {
                        current = newThreadStateInterval(current, ThreadState.INSCHED, ctrace, history);
                    }
                }
            } else if (trace.getTraceKind() == TraceKind.TS) {
                final ThreadSwitchTraceElement ctrace = (ThreadSwitchTraceElement) trace;
                if (ctrace.getFromId() == id && (ctrace.getToId() != ctrace.getFromId())) {
                    // switching off cpu
                    ThreadState newState = current.getState();
                    switch (current.getState()) {
                        case ONCPU:
                            newState = ThreadState.OFFCPU;
                            break;
                        case EXITED:
                            continue;
                        case INSCHED:
                            newState = ThreadState.BLOCKED;
                            break;
                        default:
                            System.err.println("Unexpected state in TS: " + current.getState() + ", @ " + ctrace.getTimestamp());
                    }
                    current = newThreadStateInterval(current, newState, ctrace, history);
                } else if (ctrace.getToId() == id) {
                    // switching on cpu
                    current = newThreadStateInterval(current, ThreadState.ONCPU, ctrace, history);
                    current.setCpu(ctrace.getCpu());
                }
            } else if (trace.getTraceKind() == TraceKind.CT) {
                final CreateThreadTraceElement ctrace = (CreateThreadTraceElement) trace;
                if (ctrace.getId() == id) {
                    current = newThreadStateInterval(current, ThreadState.INSCHED, ctrace, history);
                }
            } else if (trace.getTraceKind() == TraceKind.TX) {
                final ThreadIdTraceElement ctrace = (ThreadIdTraceElement) trace;
                if (ctrace.getId() == id) {
                    current = newThreadStateInterval(current, ThreadState.EXITED, ctrace, history);
                }
            } else if (trace.getTraceKind() == TraceKind.DT) {
                final ThreadIdTraceElement ctrace = (ThreadIdTraceElement) trace;
                if (ctrace.getId() == id) {
                    current = newThreadStateInterval(current, ThreadState.REAPED, ctrace, history);
                    break;
                }
            } else if (trace.getTraceKind() == TraceKind.RI) {
                // special variant of TS, first execution of the idle thread
                if (trace.getCpu() == id) {
                    current = newThreadStateInterval(current, ThreadState.ONCPU, trace, history);
                    current.setCpu(trace.getCpu());
                }
            } else if (trace.getTraceKind() == TraceKind.BI) {
                // block idle thread
                if (trace.getCpu() == id) {
                    current = newThreadStateInterval(current, ThreadState.BLOCKED, trace, history);
                }
            } else if (trace.getTraceKind() == TraceKind.WI) {
                // wake idle thread
                if (trace.getCpu() == id) {
                    current = newThreadStateInterval(current, ThreadState.INSCHED, trace, history);
                }
            }
        }
        if (current.getEnd() == 0) {
            current.setEnd(TraceReader.lastTrace().getTimestamp());
        }
        return history;
    }

    private static ThreadStateInterval newThreadStateInterval(ThreadStateInterval oldThreadStateInterval, ThreadState state, TraceElement traceElement, List<ThreadStateInterval> history) {
        if (oldThreadStateInterval != null) {
            oldThreadStateInterval.setEnd(traceElement.getTimestamp());
        }
        final ThreadStateInterval result = new ThreadStateInterval(state, traceElement.getTimestamp());
        history.add(result);
        return result;
    }
}
