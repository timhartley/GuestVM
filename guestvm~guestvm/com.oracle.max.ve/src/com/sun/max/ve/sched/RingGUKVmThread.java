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

import com.sun.max.annotate.INLINE;
import com.sun.max.ve.sched.priority.PriorityRingRunQueueEntry;

/**
 * A subclass of GUKVmThread that implements PriorityRingRunQueueEntry and can therefore be inserted into a RingRunQueue
 * or a PriorityRingRunQueue.
 *
 *
 * @author Harald Roeck
 * @author Mick Jordan
 *
 */
public class RingGUKVmThread extends GUKVmThread implements PriorityRingRunQueueEntry {

    private RingRunQueueEntry _next;
    private RingRunQueueEntry _prev;

    public RingGUKVmThread() {
        _next = this;
        _prev = this;
    }

    @INLINE
    @Override
    public final RingRunQueueEntry getNext() {
        return _next;
    }

    @INLINE
    @Override
    public final void setNext(RingRunQueueEntry next) {
        this._next = next;
    }

    @INLINE
    @Override
    public final RingRunQueueEntry getPrev() {
        return _prev;
    }

    @INLINE
    @Override
    public final void setPrev(RingRunQueueEntry prev) {
        this._prev = prev;
    }

    public int getPriority() {
        return javaThread().getPriority();
    }

    public int compareTo(PriorityRingRunQueueEntry entry) {
        return super.compareTo((GUKVmThread) entry);
    }

}
