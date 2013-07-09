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

import java.util.List;

import com.sun.max.ve.tools.trace.AllocPagesTraceElement;
import com.sun.max.ve.tools.trace.AllocTraceElement;
import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.CreateThreadTraceElement;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceKind;

/**
 * Show mallocs that allocate pages.
 *
 * @author Mick Jordan
 *
 */

public class MallocPageCommand extends CommandHelper implements Command {
    private static final String THREAD_ID = "id=";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        final String id = stringArgValue(args, THREAD_ID);
        if (id == null) {
            for (CreateThreadTraceElement te : CreateThreadTraceElement.getThreadIterable(true)) {
                process(traces, te.getId());
            }
        } else {
            process(traces, Integer.parseInt(id));
        }

    }

    private void process(List<TraceElement> traces, int id) {
        int ix = 0;
        while (ix < traces.size()) {
            final TraceElement trace = traces.get(ix);
            if (trace instanceof AllocTraceElement) {
                final AllocTraceElement atrace = (AllocTraceElement) trace;
                // Looking for allocations by a particular thread
                if (atrace.getThread() == id && (atrace.getTraceKind() == TraceKind.AME)) {
                    final int ex = AllocByThreadCommand.findMatch(traces, atrace.getTraceKind(), ix);
                    // check for an intervening APE
                    ix++;
                    while (ix < ex) {
                        final TraceElement xtrace = traces.get(ix);
                        if (xtrace.getThread() == id && (xtrace.getTraceKind() == TraceKind.APE)) {
                            final AllocPagesTraceElement aptrace = (AllocPagesTraceElement) xtrace;
                            System.out.println(atrace);
                            System.out.println(aptrace + "(" + aptrace.getPages() * AllocPagesTraceElement.getPageSize() + ")");
                        }
                        ix++;
                    }
                }
            }
            ix++;
        }

    }

}
