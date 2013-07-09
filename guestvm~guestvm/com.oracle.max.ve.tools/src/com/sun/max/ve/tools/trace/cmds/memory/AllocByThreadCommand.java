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
package com.sun.max.ve.tools.trace.cmds.memory;

import java.util.*;

import com.sun.max.ve.tools.trace.AllocPagesTraceElement;
import com.sun.max.ve.tools.trace.AllocTraceElement;
import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.CreateThreadTraceElement;
import com.sun.max.ve.tools.trace.MAllocTraceElement;
import com.sun.max.ve.tools.trace.TimeFormat;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceKind;


public class AllocByThreadCommand extends CommandHelper implements Command {
    private static final String THREAD_ID = "id=";
    private static final String SUMMARY = "summary";
    private static final String TIMING = "timing";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        final String id = stringArgValue(args, THREAD_ID);
        final boolean summary = booleanArgValue(args, SUMMARY);
        final boolean timing = booleanArgValue(args, TIMING);
        checkTimeFormat(args);
        if (id == null) {
            for (CreateThreadTraceElement te : CreateThreadTraceElement.getThreadIterable(true)) {
                process(traces, te, summary, timing);
            }
        } else {
            process(traces, CreateThreadTraceElement.find(Integer.parseInt(id)), summary, timing);
        }
    }

    private void process(List<TraceElement> traces, CreateThreadTraceElement te, boolean summary, boolean timing) {
        sanitize(traces);
        final int id = te.getId();
        long allocated = 0;
        long adjAllocated = 0;
        long pagesAllocated = 0;
        long mFreedById = 0;
        long pFreedById = 0;
        long mFreedByOther = 0;
        long pFreedByOther = 0;
        long lastAllocation = 0;
        final TimeInfo amTimeInfo = new TimeInfo("AM");
        final TimeInfo apTimeInfo = new TimeInfo("AP");
        final TimeInfo fmTimeInfo = new TimeInfo("FM");
        final TimeInfo fpTimeInfo = new TimeInfo("FP");
        if (!summary) {
            System.out.println("Allocations and Frees");
        }
        final Map<Long, AllocTraceElement> allocMap = new TreeMap<Long, AllocTraceElement>();
        int ix = 0;
        while (ix < traces.size()) {
            final TraceElement trace = traces.get(ix);
            if (trace instanceof AllocTraceElement) {
                final AllocTraceElement atrace = (AllocTraceElement) trace;
                // Looking for allocations by a particular thread
                if (atrace.getThread() == id && (atrace.getTraceKind() == TraceKind.AME || atrace.getTraceKind() == TraceKind.APE)) {
                    lastAllocation = atrace.getTimestamp();
                    if (!summary) {
                        System.out.println(atrace);
                    }
                    if (allocMap.get(atrace.getAddress()) != null) {
                        System.out.println("Address " + Long.toHexString(atrace.getAddress()) + " multiply allocated");
                    } else {
                        allocMap.put(atrace.getAddress(), atrace);
                    }

                    if (atrace.getTraceKind() == TraceKind.AME) {
                        final MAllocTraceElement mtrace = (MAllocTraceElement) atrace;
                        allocated += mtrace.getSize();
                        adjAllocated += mtrace.getAdjSize();
                    } else {
                        final AllocPagesTraceElement aptrace = (AllocPagesTraceElement) atrace;
                        pagesAllocated += aptrace.getPages();
                    }
                    // timing
                    ix = findMatch(traces, atrace.getTraceKind(), ix);
                    final AllocTraceElement etrace = (AllocTraceElement) traces.get(ix);
                    final int duration = (int) (etrace.getTimestamp() - atrace.getTimestamp());
                    if (etrace.getTraceKind() == TraceKind.AMX) {
                        amTimeInfo.add(duration);
                    } else {
                        apTimeInfo.add(duration);
                    }

                } else if (atrace.getTraceKind() == TraceKind.FME || atrace.getTraceKind() == TraceKind.FPE) {
                     // Any thread might free the memory
                    final AllocTraceElement patrace = allocMap.get(atrace.getAddress());
                    if (patrace != null) {
                        if (!summary) {
                            System.out.println(atrace);
                        }
                        if (atrace.getThread() == patrace.getThread()) {
                            if (atrace.getTraceKind() == TraceKind.FME) {
                                mFreedById += patrace.getAdjSize();
                            } else {
                                final AllocPagesTraceElement aptrace = (AllocPagesTraceElement) patrace;
                                pFreedById += aptrace.getPages();
                            }
                        } else {
                            if (atrace.getTraceKind() == TraceKind.FME) {
                                mFreedByOther += patrace.getAdjSize();
                            } else {
                                final AllocPagesTraceElement aptrace = (AllocPagesTraceElement) patrace;
                                pFreedByOther += aptrace.getPages();
                            }

                        }
                        allocMap.remove(patrace.getAddress());
                        // timing
                        ix = findMatch(traces, atrace.getTraceKind(), ix);
                        final AllocTraceElement etrace = (AllocTraceElement) traces.get(ix);
                        final int duration = (int) (etrace.getTimestamp() - atrace.getTimestamp());
                        if (etrace.getTraceKind() == TraceKind.FMX) {
                            fmTimeInfo.add(duration);
                        } else {
                            fpTimeInfo.add(duration);
                        }
                    }
                }
            }
            ix++;
        }

        long remaining = 0;
        if (!summary) {
            System.out.println("Allocations not Freed:");
        }

        for (AllocTraceElement mtrace : allocMap.values()) {
            if (!summary) {
                System.out.println(mtrace);
            }
            remaining += mtrace.getAdjSize();
        }

        if (summary) {
            final long startTime = traces.get(0).getTimestamp();
            final long endTime = traces.get(traces.size() - 1).getTimestamp();
            System.out.println("Thread " + id + " (" + te.getName() + ")");
            System.out.println("  Allocated: AM: " + allocated + " (" + adjAllocated + "), AP: " + pagesAllocated + ", FM: " + mFreedById + ", " + mFreedByOther + ", FP: " + pFreedById + ", " +
                            pFreedByOther + ", remaining: " + remaining + ", lastAlloc: " + TimeFormat.seconds(lastAllocation) + " (" + percent(lastAllocation - startTime, endTime - startTime) + ")");
            // sanity check
            final long totalAllocated = adjAllocated + (pagesAllocated * AllocPagesTraceElement.getPageSize());
            final long totalFreed = mFreedById + mFreedByOther + (pFreedById + pFreedByOther) * AllocPagesTraceElement.getPageSize();
            if (totalAllocated - totalFreed != remaining) {
                System.out.println("MISMATCH!");
            }
            if (timing) {
                System.out.println("  " + amTimeInfo);
                System.out.println("  " + fmTimeInfo);
                System.out.println("  " + apTimeInfo);
                System.out.println("  " + fpTimeInfo);
            }
        }
    }

    private static String percent(long a, long b) {
        return TimeFormat.div2d(a * 100, b);
    }

    public void sanitize(List<TraceElement> traces) {
        int i = 0;
        while (i < traces.size()) {
            final TraceElement trace = traces.get(i);
            if (trace instanceof AllocTraceElement) {
                final AllocTraceElement atrace = (AllocTraceElement) trace;
                if (atrace.getTraceKind() == TraceKind.AME || atrace.getTraceKind() == TraceKind.APE) {
                    final int e = findMatch(traces, atrace.getTraceKind(), i);
                    final AllocTraceElement etrace = (AllocTraceElement) traces.get(e);
                    if (etrace != null) {
                        atrace.setAddress(etrace.getAddress());
                    } else {
                        System.out.println("Failed to match trace: " + atrace);
                    }
                }
            }
            i++;
        }
    }

    /**
     * Returns the index of the matching end trace.
     * @param traces
     * @param traceKind
     * @param ix
     * @return
     */
    public static int findMatch(List<TraceElement> traces, TraceKind traceKind, final int ix) {
        int i = ix;
        final AllocTraceElement atrace = (AllocTraceElement) traces.get(i++);
        while (i < traces.size()) {
            final TraceElement trace = traces.get(i);
            if (trace instanceof AllocTraceElement) {
                final AllocTraceElement atrace2 = (AllocTraceElement) trace;
                if (atrace2.getThread() == atrace.getThread()) {
                    // ok, same thread
                    if (match(atrace.getTraceKind(), atrace2.getTraceKind())) {
                        return i;
                    }
                }
            }
            i++;
        }
        return -1;
    }

    private static boolean match(TraceKind kind1, TraceKind kind2) {
        if (kind1 == TraceKind.AME) {
            return kind2 == TraceKind.AMX;
        } else if (kind1 == TraceKind.FME) {
            return kind2 == TraceKind.FMX;
        } else if (kind1 == TraceKind.APE) {
            return kind2 == TraceKind.APX;
        } else if (kind1 == TraceKind.FPE) {
            return kind2 == TraceKind.FPX;
        } else {
            return false;
        }
    }

    static class TimeInfo {
        int _min = Integer.MAX_VALUE;
        int _max;
        int _total;
        int _count;
        String _name;

        TimeInfo(String name) {
            _name = name;
        }

        int avg() {
            return _total / _count;
        }

        void add(long duration) {
            _total += (int) duration;
            _count++;
            if (duration > _max) {
                _max = (int) duration;
            }
            if (duration < _min) {
                _min = (int) duration;
            }
        }

        @Override
        public String toString() {
            String result = _name;
            if (_count > 0) {
                result  += ": max: " + TimeFormat.byKind(_max, _timeFormat) + ", min: " + TimeFormat.byKind(_min, _timeFormat) + ", avg: " + TimeFormat.byKind(avg(), _timeFormat);
            } else {
                result += ": NA";
            }
            return result;
        }
    }

}
