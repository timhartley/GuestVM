/*
        @Override
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


import com.sun.max.ve.guk.*;
import com.sun.max.ve.sched.*;
import com.sun.max.ve.spinlock.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.reference.*;


/**
 * MaxVE implementation of a Mutex, in Java, using spin locks.
 * This class is optionally used in MaxVE over Maxine's SpinLockMutex
 * implementation by setting the max.mutex.factory.class=SpinLockMutexFactory
 *
 * This code will inter-operate with the Java version of the thread scheduler because it
 * changes thread state by calling the SchedThread methods (which simply forward to the
 * uKernel scheduler if Java scheduling is not active).
 *
 * @author Mick Jordan
 * @author Harald Roeck
 */
public final class JavaMutex extends Mutex {

    private int _rcount;
    private GUKVmThread _holder;
    private SpinLock _spinlock;
    private static Scheduler _scheduler;
    /*
     * A list of threads that are waiting on this monitor.
     */
    private WaitList _waiters;

    static void initialize() {
        assert MaxineVM.isPrimordial();
        GUKScheduler.initialize(MaxineVM.Phase.PRIMORDIAL);
        _scheduler = SchedulerFactory.scheduler();
    }

    public JavaMutex() {
        _spinlock = SpinLockFactory.create();
        _rcount = 0;
        _waiters = new WaitList();
    }

    @Override
    public Mutex init() {
        _spinlock.initialize();
        return this;
    }

    @Override
    public void cleanup() {
        _spinlock.cleanup();
    }

    @Override
    public boolean lock() {
        final GUKVmThread current = (GUKVmThread) VmThread.current();

        if (_holder == current) {
            ++_rcount;
        } else {
            _spinlock.lock();
            current.setMutexWait(true); // for debugging
            while (_holder != null) {
                _waiters.put(current);
                current.setSchedulable(false);
                _spinlock.unlock();
                _scheduler.schedule();
                _spinlock.lock();
            }
            _holder = current;
            _rcount = 1;
            current.setMutexWait(false);
            _spinlock.unlock();
        }
        return true;
    }

    @Override
    public boolean unlock() {
        final GUKVmThread current = (GUKVmThread) VmThread.current();
        assert current == _holder;

        if (--_rcount == 0) {
            _spinlock.lock();
            _holder = null;
            final GUKVmThread next = _waiters.get();
            _spinlock.unlock();
            if (next != null) {
                next.setSchedulable(true);
                _scheduler.schedule();
            }
        }
        return true;
    }

    boolean lock(int rcount) {
        final boolean retval = lock();
        _rcount = rcount;
        return retval;
    }

    int unlockAll() {
        final int rcount = _rcount;
        _rcount = 1;
        unlock();
        return rcount;
    }

    @Override
    public long logId() {
        return Reference.fromJava(_spinlock).toOrigin().toLong();
    }

}
