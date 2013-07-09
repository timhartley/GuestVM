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
package com.sun.max.ve.monitor;

import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.*;
import com.sun.max.ve.sched.*;
import com.sun.max.ve.spinlock.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.reference.*;

/**
 * MaxVE implementation of a ConditionVariable, in Java.
 * The comments in Mutex.java apply equally to this class.
 *
 * @author Mick Jordan
 * @author Harald Roeck
 */
public final class JavaConditionVariable extends ConditionVariable {

    private static Scheduler _scheduler;
    private SpinLock _spinlock;
    /*
     * A list of  threads that are waiting on this condition.
     */
    private WaitList _waiters;

    static void initialize() {
        assert MaxineVM.isPrimordial();
        GUKScheduler.initialize(MaxineVM.Phase.PRIMORDIAL);
        _scheduler = SchedulerFactory.scheduler();
    }

    public JavaConditionVariable() {
        _spinlock = SpinLockFactory.create();
        _waiters = new WaitList();
    }

    @Override
    public ConditionVariable init() {
        _spinlock.initialize();
        return this;
    }

    @Override
    public boolean threadWait(Mutex mutex, long timeoutMilliSeconds) {
        final JavaMutex spinLockMutex = (JavaMutex) mutex;
        Pointer timer = Pointer.zero();
        final GUKVmThread current = (GUKVmThread) VmThread.current();

        if (timeoutMilliSeconds > 0) {
            timer = GUKScheduler.createTimer();
            GUKScheduler.addTimer(timer, timeoutMilliSeconds);
        }

        _spinlock.lock();
        current.setNotified(false);
        current.setSchedulable(false);
        _waiters.put(current);
        _spinlock.unlock();

        current.setConditionWait(true); // for debugging
        final int rcount = spinLockMutex.unlockAll();

        _scheduler.schedule();

        boolean result = true;
        if (timeoutMilliSeconds > 0) {
            GUKScheduler.removeTimer(timer);
            GUKScheduler.deleteTimer(timer);
        }

        _spinlock.lock();
        current.setConditionWait(false);

        if (current.isNotified()) {
            current.setNotified(false);
        } else { /* on timeout or interrupt remove thread from wait list */
            _waiters.remove(current);
        }
        if (current.isOSInterrupted()) {
            result = false;
            current.clearOSInterrupted();
        }
        _spinlock.unlock();
        /* get the lock before return, and restore recursion count */
        spinLockMutex.lock(rcount);

        return result;
    }

    @Override
    public boolean threadNotify(boolean all) {
        boolean sched = false;
        GUKVmThread next;
        _spinlock.lock();
        next = _waiters.get();
        while (next != null) {
            next.setNotified(true);
            next.setSchedulable(true);
            sched = true;
            if (!all) {
                break;
            }
            next = _waiters.get();
        }
        _spinlock.unlock();
        if (sched) {
            _scheduler.schedule();
        }
        return false;
    }

    @Override
    public long logId() {
        return Reference.fromJava(_spinlock).toOrigin().toLong();
    }


}
