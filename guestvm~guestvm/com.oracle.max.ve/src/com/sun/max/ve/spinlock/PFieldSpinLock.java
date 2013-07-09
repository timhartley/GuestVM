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
package com.sun.max.ve.spinlock;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.ve.spinlock.tas.inline.ISpinLock;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.FieldActor;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;

/**
 * N.B. This code is identical to NPFieldSpinLock except for the parent class to get the pre-emption calls inlined.
 *
 * Holds the lock as a field and uses Maxine's Reference.compareAndSwapInt method to grab the lock.
 * Also handles pre-emption, abstractly, depending on the superclass chosen.
 *
 * In principle a globally optimizing compiler could duplicate the code for this class with the appropriate
 * inlined code and we could delay the choice of parent until later in the subclass hierarchy.
 *
 * @author Mick Jordan
 *
 */
public abstract class PFieldSpinLock extends PSpinLock {
    protected volatile int _lock;
    private static final Offset _lockOffset = Offset.fromInt(ClassActor.fromJava(PFieldSpinLock.class).findFieldActor(SymbolTable.makeSymbol("_lock"), null).offset());

    /**
     * Unlocks and re-enables pre-emption.
     */
    @Override
    public void unlock() {
        _lock = 0;
        enablePreemption();
    }

    /**
     * Tries to acquire the lock, disabling pre-emption first.
     * If acquisition fails re-enables pre-emption.
     * @return true iff the lock is acquire, false otherwise.
     */
    @INLINE
    protected final boolean canLock() {
        disablePreemption();
        final boolean r = Reference.fromJava(this).compareAndSwapInt(_lockOffset, 0, 1) == 0;
        if (!r) {
            enablePreemption();
        }
        return r;
    }
}
