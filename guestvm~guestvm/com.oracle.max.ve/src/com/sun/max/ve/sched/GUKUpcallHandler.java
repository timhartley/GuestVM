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
package com.sun.max.ve.sched;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.*;

/**
 * This abstract class handles the connection with the microkernel upcalls.
 * It invokes the actual Scheduler method in response to the upcall.
 * Safepoints are disabled while in the scheduler.
 *
 * @author Mick Jordan
 *
 */
public abstract class GUKUpcallHandler extends Scheduler {
    public enum CpuState{
        DOWN,
        UP,
        SUSPENDING,
        RESUMING,
        SLEEPING
    }

    protected static final int MAX_CPU = 32;
    protected static int _numCpus;  // actual number of CPUs
    protected static boolean _upcallsActive = false; // true once the upcalls have been registered with the uKernel

    private static Scheduler _sched;
    private static final CriticalMethod scheduleUpcall = new CriticalMethod(GUKUpcallHandler.class, "schedule_upcall", null, CallEntryPoint.C_ENTRY_POINT);
    private static final CriticalMethod descheduleUpcall = new CriticalMethod(GUKUpcallHandler.class, "deschedule_upcall", null, CallEntryPoint.C_ENTRY_POINT);
    private static final CriticalMethod wakeUpcall = new CriticalMethod(GUKUpcallHandler.class, "wake_upcall", null, CallEntryPoint.C_ENTRY_POINT);
    private static final CriticalMethod blockUpcall = new CriticalMethod(GUKUpcallHandler.class, "block_upcall", null, CallEntryPoint.C_ENTRY_POINT);
    private static final CriticalMethod attachUpcall = new CriticalMethod(GUKUpcallHandler.class, "attach_upcall", null, CallEntryPoint.C_ENTRY_POINT);
    private static final CriticalMethod detachUpcall = new CriticalMethod(GUKUpcallHandler.class, "detach_upcall", null, CallEntryPoint.C_ENTRY_POINT);
    private static final CriticalMethod pickCpuUpcall = new CriticalMethod(GUKUpcallHandler.class, "pick_cpu_upcall", null, CallEntryPoint.C_ENTRY_POINT);
    private static final CriticalMethod runnableUpcall = new CriticalMethod(GUKUpcallHandler.class, "runnable_upcall", null, CallEntryPoint.C_ENTRY_POINT);

    @Override
    public void initialize(MaxineVM.Phase phase) {
        registerWithMicrokernel();
    }

    @Override
    public void schedule() {
        GUKScheduler.schedule();
    }

    private void registerWithMicrokernel() {
        _upcallsActive = GUKScheduler.registerUpcalls(
                        scheduleUpcall.address(), descheduleUpcall.address(),
                        wakeUpcall.address(), blockUpcall.address(),
                        attachUpcall.address(), detachUpcall.address(),
                        pickCpuUpcall.address(), runnableUpcall.address()) == 0;
    }

    @HOSTED_ONLY
    protected GUKUpcallHandler() {
        _sched = this;
    }

    @Override
    public boolean active() {
        return _upcallsActive;
    }

    /**
     * Unless already disabled, disable safepoints.
     * @return true iff this method disabled safepoints
     */
    @INLINE
    private static boolean disableSafepoints() {
        final boolean safepointsDisabled = SafepointPoll.isDisabled();
        if (!safepointsDisabled) {
            SafepointPoll.disable();
        }
        return !safepointsDisabled;
    }

    @INLINE
    private static void enableSafepoints(boolean safepointsDisabled) {
        if (safepointsDisabled) {
            SafepointPoll.enable();
        }
    }

    /*
     * Normally we would use try/finally for the safepoint disable/enable but if the
     * scheduler throws an exception we are dead anyway.
     */

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static Word schedule_upcall(int cpu) {
        final boolean safepointsDisabled = disableSafepoints();
        final Word result = _sched.scheduleUpcall(cpu);
        enableSafepoints(safepointsDisabled);
        return result;
    }

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static void deschedule_upcall(int cpu) {
        final boolean safepointsDisabled = disableSafepoints();
        _sched.descheduleUpcall(cpu);
        enableSafepoints(safepointsDisabled);
    }

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static void wake_upcall(int id, int cpu) {
        final boolean safepointsDisabled = disableSafepoints();
        _sched.wakeUpcall(id, cpu);
        enableSafepoints(safepointsDisabled);
    }

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static void block_upcall(int id, int cpu) {
        final boolean safepointsDisabled = disableSafepoints();
        _sched.blockUpcall(id, cpu);
        enableSafepoints(safepointsDisabled);
    }

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static void attach_upcall(int id, int tcpu, int xcpu) {
        final boolean safepointsDisabled = disableSafepoints();
        _sched.attachUpcall(id, tcpu, xcpu);
        enableSafepoints(safepointsDisabled);
    }

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static void detach_upcall(int id, int cpu) {
        final boolean safepointsDisabled = disableSafepoints();
        _sched.detachUpcall(id, cpu);
        enableSafepoints(safepointsDisabled);
    }

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static int pick_cpu_upcall() {
        final boolean safepointsDisabled = disableSafepoints();
        final int result = _sched.pickCpuUpcall();
        enableSafepoints(safepointsDisabled);
        return result;
    }

    @SuppressWarnings({ "unused"})
    @VM_ENTRY_POINT
    private static int runnable_upcall(int cpu) {
        final boolean safepointsDisabled = disableSafepoints();
        final int result = _sched.runnableUpcall(cpu);
        enableSafepoints(safepointsDisabled);
        return result;
    }

}
