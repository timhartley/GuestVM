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
import com.sun.max.ve.tools.trace.Command;
import com.sun.max.ve.tools.trace.CommandHelper;
import com.sun.max.ve.tools.trace.TraceElement;


public class PagePoolCommand extends CommandHelper implements Command {

    @Override
    public void doIt(List<TraceElement> traces, String[] args) throws Exception {
        int maxHwm = 0;
        for (TraceElement te : traces) {
            if (te instanceof AllocPagesTraceElement) {
                final AllocPagesTraceElement apte = (AllocPagesTraceElement) te;
                if (apte.getHwmAllocPage() > maxHwm) {
                    maxHwm = apte.getHwmAllocPage();
                }
            }
        }
        System.out.println("max hwm " + maxHwm);
    }

}
