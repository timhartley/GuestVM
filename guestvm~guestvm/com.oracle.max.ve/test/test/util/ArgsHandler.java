/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.util;

/**
 * Utterly trivial argument processing for simple test programs.
 * The following keywords are recognised:
 *
 * a1 val   set argument a1
 * a2 val   set argument a2
 * a3 val   set argument a3
 * a4 val   set argument a4
 * op c      set command c
 * v           set verbose
 * gt ord    set GUK tracing flag "ord" to true (see {@link GUKTrace.Name}.
 *
 * Up to ten sets of command/arguments area support.
 * Arguments must precede the command and are available in the corresponding array, e.g. {@link _opArgs1}.
 * Unchanged arguments propagate to subsequent commands.
 *
 * The verbose and GUK trace arguments are global. The former sets {@link _verbose} and the latter
 * is acted upon immediately (and may be repeated with different trace ordinals).
 *
 * @author Mick Jordan
 *
 */
public final class ArgsHandler {

    public final String[] _ops = new String[10];
    public final String[] _opArgs1 = new String[10];
    public final String[] _opArgs2 = new String[10];
    public final String[] _opArgs3 = new String[10];
    public final String[] _opArgs4 = new String[10];
    public int _opCount = 0;
    public boolean _verbose;

    public static ArgsHandler process(String[] args) {
        return new ArgsHandler(args);
    }

    private ArgsHandler(String[] args) {
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("a") || arg.equals("a1")) {
                _opArgs1[_opCount] = args[++i];
            } else if (arg.equals("a2")) {
                _opArgs2[_opCount] = args[++i];
            } else if (arg.equals("a3")) {
                _opArgs3[_opCount] = args[++i];
            } else if (arg.equals("a4")) {
                _opArgs4[_opCount] = args[++i];
            } else if (arg.equals("op")) {
                _ops[_opCount++] = args[++i];
                _opArgs1[_opCount] = _opArgs1[_opCount - 1];
                _opArgs2[_opCount] = _opArgs2[_opCount - 1];
                _opArgs3[_opCount] = _opArgs3[_opCount - 1];
                _opArgs4[_opCount] = _opArgs4[_opCount - 1];
            } else if (arg.equals("v")) {
                _verbose = true;
            } else if (arg.equals("gt")) {
                final int ord = Integer.parseInt(args[++i]);
                OSSpecific.setTraceState(ord, true);
            }
        }
        // Checkstyle: resume modified control variable check

    }
}
