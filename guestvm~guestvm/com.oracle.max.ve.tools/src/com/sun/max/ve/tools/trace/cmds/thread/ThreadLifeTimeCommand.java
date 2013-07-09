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

import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.CreateThreadTraceElement;
import com.sun.max.ve.tools.trace.ThreadIdTraceElement;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceKind;

/**
 * This command lists the time a thread was created and the state it is in at a given time
 * (currently end of trace). If the thread has exited, its lifetime and info on whether the state
 * has been recovered (reaped) is also output.
 *
 * @author Mick Jordan
 *
 */

public class ThreadLifeTimeCommand extends CommandHelper implements Command {
    private static final String THREAD_ID = "id=";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        final String id = stringArgValue(args, THREAD_ID);
        if (id == null) {
            for (CreateThreadTraceElement te : CreateThreadTraceElement.getThreadIterable()) {
                process(traces, te.getId());
            }
        } else {
            process(traces, Integer.parseInt(id));
        }
    }

    private void process(List<TraceElement> traces, int id) {
        long createTime = -1;
        long exitTime = -1;
        long destroyTime = -1;
        int cpu = -1;
        String name = null;
        for (TraceElement trace : traces) {
            if (trace.getTraceKind() == TraceKind.CT) {
                final CreateThreadTraceElement ctrace = (CreateThreadTraceElement) trace;
                if (ctrace.getId() == id) {
                    createTime = trace.getTimestamp();
                    name = ctrace.getName();
                    cpu = ctrace.getInitialCpu();
                }
            } else if (trace.getTraceKind() == TraceKind.TX) {
                final ThreadIdTraceElement ctrace = (ThreadIdTraceElement) trace;
                if (ctrace.getId() == id) {
                    exitTime = trace.getTimestamp();
                }
            } else if (trace.getTraceKind() == TraceKind.DT) {
                final ThreadIdTraceElement ctrace = (ThreadIdTraceElement) trace;
                if (ctrace.getId() == id) {
                    destroyTime = trace.getTimestamp();
                    break;
                }
            }
        }

        if (createTime >= 0) {
            System.out.print("Thread " + id + " (" + name + ") created at " + createTime + ", on cpu " + cpu);
            if (exitTime < 0) {
                System.out.println(" not exited");
            } else {
                System.out.print(" exited at " + exitTime + ", lifetime " + (exitTime - createTime));
                if (destroyTime > 0) {
                    System.out.print(", reaped at " + destroyTime);
                } else {
                    System.out.print(", not reaped");
                }
                System.out.println();
            }
        } else {
            System.out.println("thread " + id + " not found");
        }
    }
}
