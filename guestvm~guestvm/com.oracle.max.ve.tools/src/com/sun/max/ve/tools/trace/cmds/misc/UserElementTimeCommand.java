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

import java.util.*;

import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.TimeFormat;
import com.sun.max.ve.tools.trace.TraceElement;
import com.sun.max.ve.tools.trace.TraceKind;
import com.sun.max.ve.tools.trace.UserTraceElement;

/**
 * Command to collect durations between pairs of user traces of the form NAME_ENTER and NAME_EXIT.
 * N.B. Nested or unbalanced pairs are not handled correctly at present.
 *
 * @author Mick Jordan
 *
 */

public class UserElementTimeCommand extends CommandHelper implements Command {
    private static final String USER_TRACE = "user=";

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        final List<Long> durations = new ArrayList<Long>();
        final String userTrace = stringArgValue(args, USER_TRACE);
        if (userTrace == null) {
            throw new Exception("user trace name missing");
        }
        final TimeFormat.Kind kind = TimeFormat.checkFormat(args);
        final String enterName = userTrace + "_ENTER";
        final String exitName = userTrace + "_EXIT";

        // This does not handle nesting.
        long start = 0;
        for (TraceElement trace : traces) {
            if (trace.getTraceKind() == TraceKind.USER) {
                final UserTraceElement utrace = (UserTraceElement) trace;
                if (utrace.getName().equals(enterName)) {
                    start = utrace.getTimestamp();
                } else if (utrace.getName().equals(exitName)) {
                    durations.add(utrace.getTimestamp() - start);
                    start = 0;
                }
            }
        }

        long sum = 0;
        long max = 0;
        long min = Long.MAX_VALUE;
        for (Long duration : durations) {
            sum += duration;
            if (duration > max) {
                max = duration;
            }
            if (duration < min) {
                min = duration;
            }
        }
        System.out.println("max: " + TimeFormat.byKind(max, kind) + ", min: " + TimeFormat.byKind(min, kind) + ", avg: " + TimeFormat.byKind(sum / durations.size(), kind));

    }

}
