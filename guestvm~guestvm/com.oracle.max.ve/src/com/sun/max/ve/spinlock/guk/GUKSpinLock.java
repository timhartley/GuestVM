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
package com.sun.max.ve.spinlock.guk;

import java.lang.ref.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.Pointer;
import com.sun.max.ve.guk.*;
import com.sun.max.ve.spinlock.*;

/**
 * Delegates to ukernel allocated spin locks, so has native state that
 * must be allocated at the appropriate time. To support spinlocks
 * that are created during image build, so obviously can't have the
 * native component created then, the native spinlock is created
 * by the initialize method.
 *
 * @author Mick Jordan
 *
 */
public class GUKSpinLock extends SpinLock {

    private static ReferenceQueue<GUKSpinLock> _refQueue = new ReferenceQueue<GUKSpinLock>();

    static class NativeReference extends WeakReference<GUKSpinLock> {

        @CONSTANT_WHEN_NOT_ZERO
        protected Pointer _spinlock;

        NativeReference(GUKSpinLock m) {
            super(m, _refQueue);
        }

        private void disposeNative() {
            GUKScheduler.destroySpinLock(_spinlock);
        }
    }

    protected NativeReference _native;

    GUKSpinLock() {
        // Create the NativeReference now to avoid heap allocation
        // during initialize, which might be inconvenient, e.g.., during VM startup.
        _native = new NativeReference(this);
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public NativeSpinLockSupport initialize() {
        _native._spinlock = GUKScheduler.createSpinLock();
        return this;
    }

    @Override
    public void cleanup() {
        _native.disposeNative();
    }

    @Override
    public void lock() {
        GUKSpinLock.spinLock(_native._spinlock);
    }

    @Override
    public void unlock() {
        GUKSpinLock.spinUnlock(_native._spinlock);
    }

    /**
     * Release spin lock created elsewhere.
     * @param lock
     */
    @INLINE
    public static void spinUnlock(Pointer lock) {
        GUK.guk_spin_unlock(lock);
    }

    /**
     * Acquire lock created elsewhere.
     * @param lock
     */
    @INLINE
    public static void spinLock(Pointer lock) {
        GUK.guk_spin_lock(lock);
    }

}
