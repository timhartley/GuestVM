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

/**
 * Trace kind definition and parsing methods.
 * Every trace is of the form:
 *   Time CPU Thread TraceKind [args]
 *
 * @author Mick Jordan
 *
 */

public enum TraceKind {

    IS, RI, BI, WI, TI, US,

    IT {
        @Override
        public TraceElement process(String[] args) {
            final InitialTraceElement traceElement = new InitialTraceElement();
            processPrefix(this, args, traceElement);
            return traceElement;
        }
    },

    CT {

        @Override
        public TraceElement process(String[] args) {
            final CreateThreadTraceElement traceElement = new CreateThreadTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setId(Integer.parseInt(args[UAX]));
            traceElement.setName(args[UAX + 1]).setInitialCpu(Integer.parseInt(args[UAX + 2], 16)).setFlags(Integer.parseInt(args[UAX + 3])).setStack(Long.parseLong(args[UAX + 4], 16));
            return traceElement;
        }
    },

    SS {

        @Override
        public TraceElement process(String[] args) {
            final SetTimerTraceElement traceElement = new SetTimerTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setTime(Long.parseLong(args[UAX]));
            return traceElement;
        }
    },

    ST {

        @Override
        public TraceElement process(String[] args) {
            final SetTimerTraceElement traceElement = new SetTimerTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setTime(Long.parseLong(args[UAX]));
            return traceElement;
        }
    },

    TS {

        @Override
        public TraceElement process(String[] args) {
            final ThreadSwitchTraceElement traceElement = new ThreadSwitchTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setFromId(Integer.parseInt(args[UAX])).setToId(Integer.parseInt(args[UAX + 1])).setRunningTime(Long.parseLong(args[UAX + 2])).setSchedStartTime(Long.parseLong(args[UAX + 3]));
            return traceElement;
        }
    },

    PS {

        @Override
        public TraceElement process(String[] args) {
            return processThreadId(this, args);
        }

    },

    DT {

        @Override
        public TraceElement process(String[] args) {
            return processThreadId(this, args);
        }

    },

    WK {

        @Override
        public TraceElement process(String[] args) {
            return processThreadId(this, args);
        }

    },

    TX {

        @Override
        public TraceElement process(String[] args) {
            return processThreadId(this, args);
        }

    },

    BK {

        @Override
        public TraceElement process(String[] args) {
            return processThreadId(this, args);
        }

    },

    KC {

        @Override
        public TraceElement process(String[] args) {
            final SMPTraceElement traceElement = new SMPTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setCpuCount(Integer.parseInt(args[UAX]));
            return traceElement;
        }
    },

    SMP {

        @Override
        public TraceElement process(String[] args) {
            final SMPTraceElement traceElement = new SMPTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setCpuCount(Integer.parseInt(args[UAX]));
            return traceElement;
        }

    },

    // Allocation tracing

    AME {
        @Override
        public TraceElement process(String[] args) {
            final MAllocTraceElement traceElement = new MAllocTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setSize(Integer.parseInt(args[UAX]));
            traceElement.setAdjSize(Integer.parseInt(args[UAX + 1]));
            return traceElement;
        }
    },

    AMX {
        @Override
        public TraceElement process(String[] args) {
            final MAllocTraceElement traceElement = new MAllocTraceElement();
            processAllocPrefix(this, args, traceElement);
            traceElement.setSize(Integer.parseInt(args[UAX + 1]));
            traceElement.setAdjSize(Integer.parseInt(args[UAX + 2]));
            return traceElement;
        }
    },

    API {
        @Override
        public TraceElement process(String[] args) {
            final PagePoolTraceElement traceElement = new PagePoolTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setStart(Integer.parseInt(args[UAX]));
            traceElement.setEnd(Integer.parseInt(args[UAX + 1]));
            traceElement.setMax(Integer.parseInt(args[UAX + 2]));
            return traceElement;
        }
    },

    APE {
        @Override
        public TraceElement process(String[] args) {
            final AllocPagesTraceElement traceElement = new AllocPagesTraceElement();
            processPrefix(this, args, traceElement);
            traceElement.setPages(Integer.parseInt(args[UAX]));
            traceElement.setType(Integer.parseInt(args[UAX + 1]));
            return traceElement;
        }
    },

    APX {
        @Override
        public TraceElement process(String[] args) {
            final AllocPagesTraceElement traceElement = new AllocPagesTraceElement();
            processAllocPrefix(this, args, traceElement);
            traceElement.setPages(Integer.parseInt(args[UAX + 1]));
            traceElement.setFirstFreePage(Integer.parseInt(args[UAX + 2]));
            traceElement.setHwmAllocPage(Integer.parseInt(args[UAX + 3]));
            return traceElement;
        }
    },

    FME {
        @Override
        public TraceElement process(String[] args) {
            final FreeTraceElement traceElement = new FreeTraceElement();
            processAllocPrefix(this, args, traceElement);
            return traceElement;
        }

     },

     FMX {
        @Override
        public TraceElement process(String[] args) {
            final FreeTraceElement traceElement = new FreeTraceElement();
            processAllocPrefix(this, args, traceElement);
            return traceElement;
        }

     },

     FPE {
        @Override
        public TraceElement process(String[] args) {
            final AllocPagesTraceElement traceElement = new AllocPagesTraceElement();
            processAllocPrefix(this, args, traceElement);
            traceElement.setPages(Integer.parseInt(args[UAX + 1]));
            return traceElement;
        }

     },

     FPX {
        @Override
        public TraceElement process(String[] args) {
            final AllocPagesTraceElement traceElement = new AllocPagesTraceElement();
            processAllocPrefix(this, args, traceElement);
            traceElement.setPages(Integer.parseInt(args[UAX + 1]));
            return traceElement;
        }

     },

     USER {
        @Override
        public TraceElement process(String[] args) {
            final UserTraceElement traceElement = new UserTraceElement(args[TKX]);
            processPrefix(this, args, traceElement);
            final String[] userArgs = new String[args.length - UAX];
            if (userArgs.length > 0) {
                System.arraycopy(args, UAX, userArgs, 0, userArgs.length);
            }
            traceElement.setArgs(userArgs);
            return traceElement;
        }
     };

    public static final int TKX = 3;
    private static final int UAX = 4;

    public TraceElement process(String[] args) {
        return processPrefix(this, args, new TraceElement());
    }

    private static TraceElement processPrefix(TraceKind traceKind, String[] args, TraceElement traceElement) {
        traceElement.init(traceKind, Long.parseLong(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        return traceElement;
    }

    private static TraceElement processThreadId(TraceKind traceKind, String[] args) {
        final ThreadIdTraceElement traceElement = new ThreadIdTraceElement();
        processPrefix(traceKind, args, traceElement);
        traceElement.setId(Integer.parseInt(args[UAX]));
        return traceElement;
    }

    private static TraceElement processAllocPrefix(TraceKind traceKind, String[] args, AllocTraceElement traceElement) {
        processPrefix(traceKind, args, traceElement);
        traceElement.setAddress(Long.parseLong(args[UAX], 16));
        return traceElement;
    }

}
