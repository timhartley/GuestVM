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
 * A tool to read and process a trace from the GUK microkernel tracing system..
 * The general form of a trace line is:
 *
 * Timestamp CPU Thread Command args
 *
 * @author Mick Jordan
 *
 */

import java.io.*;
import java.util.*;

public class TraceReader {

    private static final Map<String, TraceKind> _traceKindMap = new HashMap<String, TraceKind>();
    private static TraceElement _last;
     /**
     * @param args
     */
    public static List<TraceElement> readTrace() throws Exception {
        java.io.BufferedReader reader = null;
        try {
            reader =  new BufferedReader(new FileReader(TraceMain.getTraceFileName()));
            initialize();
            return processTrace(reader);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private static List<TraceElement> processTrace(BufferedReader reader) throws Exception {
        final List<TraceElement> result = new ArrayList<TraceElement>();
        int lineCount = 0;
        while (true) {
            final String line = reader.readLine();
            lineCount++;
            if (line == null) {
                break;
            }
            if (line.length() == 0) {
                continue;
            }
            final String[] parts = line.split(" ");
            final String traceName = parts[TraceKind.TKX];
            final TraceKind traceKind = _traceKindMap.get(traceName);

            TraceElement traceElement;
            if (traceKind == null) {
                traceElement = TraceKind.USER.process(parts);
            } else {
                traceElement = traceKind.process(parts);
            }
            result.add(traceElement);
            _last = traceElement;
        }
        return result;
    }

    public static TraceElement lastTrace() {
        return _last;
    }

    private static void initialize() {
        for (TraceKind traceKind : TraceKind.values()) {
            _traceKindMap.put(traceKind.name(), traceKind);
        }

    }

}
