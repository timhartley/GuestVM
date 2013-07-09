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
package com.sun.max.ve.vmargs;

import com.sun.max.annotate.*;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.*;

/**
 * This class permits additional MaxVE-specific command line arguments.
 *
 *
 * @author Mick Jordan
 */

public final class VEVMProgramArguments {

    @SuppressWarnings({"unused"})
    private static final VMOption _debugOption = VMOptions.register(new VMOption("-XX:GUKDebug", "Enables MaxVE debug mode."), MaxineVM.Phase.PRISTINE);
    @SuppressWarnings({"unused"})
    private static final VMOption _xenTraceOption = VMOptions.register(new VMOption("-XX:GUKTrace", "Enables MaxVE microkernel tracing") {
        @Override
        public void printHelp() {
            VMOptions.printHelpForOption(Category.IMPLEMENTATION_SPECIFIC, "-XX:GUKTrace[:subsys1:subsys2:...[:buffer][:toring]]", "", help);
        }
    }, MaxineVM.Phase.PRISTINE);

    private static final VMOption _upcallOption = VMOptions.register(new VMOption("-XX:GUKAS", "Enables the MaxVE Java thread scheduler"), MaxineVM.Phase.PRISTINE);
    @SuppressWarnings({"unused"})
    private static final VMOption _timesliceOption = VMOptions.register(new VMIntOption("-XX:GUKTS=", 10, "Set Scheduler Time Slice (ms)"), MaxineVM.Phase.PRISTINE);
    @SuppressWarnings({"unused"})
    private static final VMOption _traceCpuOption = VMOptions.register(new VMIntOption("-XX:GUKCT=", -1, "Reserves a CPU for tracing"), MaxineVM.Phase.PRISTINE);
    @SuppressWarnings({"unused"})
    private static final VMOption _memPartitionOption = VMOptions.register(new VMIntOption("-XX:GUKMS=", 2, "Set percentage of memory allocated to small page partition"), MaxineVM.Phase.PRISTINE);
    /*
    @SuppressWarnings({"unused"})
    private static final VMStringOption _argVarOption = VMOptions.register(new VMStringOption("-XX:GVMArgVar", false, "", "Define a command line variable for use in other arguments") {
        @Override
        public void printHelp() {
            VMOptions.printHelpForOption(Category.IMPLEMENTATION_SPECIFIC, "-XX:GVMArgVar:name=value", "", help);
        }
    }, MaxineVM.Phase.PRISTINE);
    */

    @SuppressWarnings({"unused"})
    private static final VMStringOption _ramArgsOption = VMOptions.register(new VMStringOption("-XX:GVMRamArgs", false, "", "Command line arguments are in ramdisk"), MaxineVM.Phase.PRISTINE);

    private VEVMProgramArguments() {
    }

    @INLINE
    public static boolean upcallsActive() {
        return _upcallOption.isPresent();
    }

}
