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

import java.util.*;

import com.sun.max.program.option.*;


public class TraceMain {
    private static final OptionSet _options = new OptionSet(true);
    private static final Option<String> TRACE_FILE = TraceMain.options().newStringOption("tracefile", "schedtrace",
        "File containing the scheduler trace.");

    public static final Option<Integer> CPU = _options.newIntegerOption("cpu", -1, "cpu number");
    public static final Option<Boolean> ABSTIME = _options.newBooleanOption("abstime", false, "Report timestamps as absolute values");
    private static final Option<String> COMMAND = _options.newStringOption("command", null, "analysis command to execute");

    public static void main(String[] args) {
        final String[] extraArgs = _options.parseArguments(args).getArguments();
        try {
            if (ABSTIME.getValue()) {
                TraceElement.setUseAbsTimestamp(true);
            }
            final List<TraceElement> traces = TraceReader.readTrace();
            doStuff(traces, extraArgs);
        } catch (Exception ex) {
            System.err.println(ex);
        }

    }
    public static OptionSet options() {
        return _options;
    }

    public static int cpuOption() {
        return CPU.getValue();
    }

    public static String getTraceFileName() {
        return TRACE_FILE.getValue();
    }

    private static void doStuff(List<TraceElement> traces, String[] args) throws Exception {
        final String commandName = COMMAND.getValue();
        if (commandName == null) {
            return;
        }
        final Class<?> klass = Class.forName("com.sun.max.ve.tools.trace.cmds." + commandName + "Command");
        final Command command = (Command) klass.newInstance();
        command.doIt(traces, args);
    }


}
