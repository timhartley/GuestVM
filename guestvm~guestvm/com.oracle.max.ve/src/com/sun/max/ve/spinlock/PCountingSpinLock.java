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

/**
 * N.B. Code is identical to NPCountingSpinLock except for superclass.
 *
 * Provides a field to count the number of times a caller spins to get the lock.
 *
 * @author Mick Jordan
 *
 */

public abstract class PCountingSpinLock extends PFieldSpinLock implements CountingSpinLock {
    private long _maxCount;
    protected volatile long _count;

    @Override
    public void unlock() {
        if (_count > _maxCount) {
            _maxCount = _count;
        }
        _count = 0;
        super.unlock();
    }

    public long getMaxSpinCount() {
        return _maxCount;
    }

}
