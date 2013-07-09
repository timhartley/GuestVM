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
package com.sun.max.ve.tools.trace.cmds.misc;

import java.io.*;
import java.util.List;

import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceMain;

public class SplitCommand extends CommandHelper implements Command {
    private static final String OUT_TRACE_FILE = "outfile=";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        final int cpu = TraceMain.cpuOption();
        if (cpu < 0) {
            // TODO split all CPUs
        } else {
            PrintStream wr = null;
            final String outFile = outFile(args);
            try {
                if (outFile == null) {
                    wr = System.out;
                } else {
                    wr = new PrintStream(new FileOutputStream(outFile));
                }
                for (TraceElement traceElement : traces) {
                    if (cpu == traceElement.getCpu()) {
                        wr.println(traceElement);
                    }
                }
            } catch (Exception ex) {
                System.err.println(ex);
            } finally {
                if (wr != null && wr != System.out) {
                    wr.close();
                }
            }
        }
    }

    private String outFile(String[] args) {
        return stringArgValue(args, OUT_TRACE_FILE);
    }
}

