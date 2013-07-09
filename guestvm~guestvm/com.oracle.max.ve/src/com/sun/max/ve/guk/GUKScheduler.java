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
package com.sun.max.ve.guk;

import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Word;
import com.sun.max.ve.sched.SchedulerFactory;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.thread.VmThread;

/**
 * Interface to the Guest VM microkernel scheduler.
 *
 * @author Mick Jordan
 *
 */

public class GUKScheduler {
    private static boolean _initialized = false;

    public static void initialize() {
        initialize(MaxineVM.Phase.PRIMORDIAL);
    }

    public static void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PRIMORDIAL) {
            if (!_initialized) {
                SchedulerFactory.scheduler().initialize(phase);
                _initialized = true;
            }
        }
    }

    @INLINE
    public static Pointer currentThread() {
        return GUK.guk_current();
    }

    @INLINE
    public static long getCPURunningTime(int cpu) {
        return GUK.guk_get_cpu_running_time(cpu);
    }

    @INLINE
    public static void schedule() {
        GUK.guk_schedule();
    }

    @INLINE
    public static void preemptSchedule() {
        GUK.guk_preempt_schedule();
    }

    @INLINE
    public static void block(Word thread) {
        GUK.guk_block(thread);
    }

    @INLINE
    public static void wake(Word thread) {
        GUK.guk_wake(thread);
    }

    @INLINE
    public static void attachThread(Word thread, int id) {
        GUK.guk_attach_to_appsched(thread, id);
    }

    @INLINE
    public static void detachThread(Word thread) {
        GUK.guk_detach_from_appsched(thread);
    }

    @INLINE
    /**
     * Set the timeslice for given thread to t millisecs.
     * @param thread the thread
     * @param t the new timeslice
     * @return the previous value of the timeslice
     */
    public static int setThreadTimeSlice(VmThread thread, int t) {
        return GUK.guk_set_timeslice(thread.nativeThread(), t);
    }

    @INLINE
    public static Pointer createSpinLock() {
        return GUK.guk_create_spin_lock();
    }

    @INLINE
    public static void destroySpinLock(Pointer lock) {
        GUK.guk_delete_spin_lock(lock);
    }

    /**
     * Acquire lock created by createSpinlock and disable interrupts (events).
     * @param lock
     */
    @INLINE
    public static long spinLockDisableInterrupts(Pointer lock) {
        return GUK.guk_spin_lock_irqsave(lock);
    }

    /**
     * Release lock created by createSpinlock and enable interrupts (events).
     * @param lock
     */
    @INLINE
    public static void spinUnlockEnableInterrupts(Pointer lock, long flags) {
        GUK.guk_spin_unlock_irqrestore(lock, flags);
    }

    /**
     * Disable interrupts (events).
     * @return flags to pass to enableInterrupts
     */
    @INLINE
    public static long disableInterrupts() {
        return GUK.guk_local_irqsave();
    }

    /**
     * Enable interrupts (events).
     * @param flags previously returned by disableInterrupts
     */
    @INLINE
    public static void enableInterrupts(long flags) {
        GUK.guk_local_irqrestore(flags);
    }

    @INLINE
    public static void complete(Pointer comp) {
        GUK.guk_complete(comp);
    }

    @INLINE
    public static Pointer createCompletion() {
        return GUK.guk_create_completion();
    }

    @INLINE
    public static void deleteCompletion(Pointer completion) {
        GUK.guk_delete_completion(completion);
    }

    @INLINE
    public static void waitCompletion(Pointer comp) {
        GUK.guk_wait_completion(comp);
    }

    /**
     * Allocate a native timer structure.
     * @return the address of the new timer
     */
    @INLINE
    public static Pointer createTimer() {
        return GUK.guk_create_timer();
    }

    /**
     * Deallocate the native timer structure.
     * @param timer
     */
    @INLINE
    public static void deleteTimer(Pointer timer) {
        GUK.guk_delete_timer(timer);
    }

    /**
     * Add a timer to the timer queue.
     * @param timer
     * @param timeout when the timer should go off in ms.
     */
    @INLINE
    public static void addTimer(Pointer timer, long timeout) {
        GUK.guk_add_timer(timer, timeout);
    }

    /**
     * Remove timer from timer queue.
     * @param timer
     * @return true if the timer expired, false otherwise
     */
    @INLINE
    public static boolean removeTimer(Pointer timer) {
        return GUK.guk_remove_timer(timer) != 0;
    }

    public static void printRunQueue() {
        GUK.guk_print_runqueue();
    }

    @INLINE
    public static int registerUpcalls(Address schedCall, Address deschedCall, Address wakeCall, Address blockCall,
                    Address attachCall, Address detachCall, Address pickCpuCall, Address runnableUpcall) {
        return GUK.guk_register_upcalls(schedCall, deschedCall, wakeCall, blockCall, attachCall, detachCall, pickCpuCall, runnableUpcall);
    }

    @INLINE
    /**
     * Return the number of CPUs available for scheduling threads on.
     * N.B. This may be less than the number of threads available to the guest.
     * @return number of CPUs available for scheduling threads
     */
    public static int numCpus() {
        return GUK.guk_sched_num_cpus();
    }

    @INLINE
    public static int cpuState(int cpu) {
        return GUK.guk_smp_cpu_state(cpu);
    }

    @INLINE
    public static void kickCpu(int cpu) {
        GUK.guk_kick_cpu(cpu);
    }


}
