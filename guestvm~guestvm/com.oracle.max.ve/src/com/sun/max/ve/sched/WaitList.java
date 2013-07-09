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

/**
 * provides a FIFO list of SchedThreads; no locking in here, users of this class have to do the locking.
 *
 * @author Harald Roeck
 *
 */
public class WaitList {

    private GUKVmThread _first;
    private GUKVmThread _last;

    /**
     * Insert a thread into the list.
     *
     * @param thread the thread to insert
     */
    public void put(GUKVmThread thread) {
        if (_first == null) { /* list is empty */
            // CheckStyle: stop inner assignment check
            _first = _last = thread;
            // CheckStyle: resume inner assignment check
        } else {
            _last.setNextWaiting(thread);
            _last = thread;
        }

    }

    /**
     * Removes and returns the first thread in the list.
     *
     * @return the first thread in the list or null if the list is empty
     */
    public GUKVmThread get() {
        GUKVmThread retval;
        retval = _first;
        if (retval != null) {
            _first = retval.getNextWaiting();
            if (_first == null) {
                _last = null;
            }
            retval.setNextWaiting(null);
        }
        return retval;
    }

    /**
     * Removes a thread from the list.
     *
     * @param thread the thread to remove from the list
     */
    public void remove(GUKVmThread thread) {
        if (_first == null || thread == null) {
            return;
        } else if (thread == _first) {
            get();
        } else {
            GUKVmThread iterator = _first;
            GUKVmThread prev = null;
            while (iterator != null) {
                if (iterator == thread) {
                    break;
                }
                prev = iterator;
                iterator = iterator.getNextWaiting();
            }

            if (iterator == thread) { /* found the thread */
                prev.setNextWaiting(thread.getNextWaiting());
                if (thread == _last) {
                    _last = prev;
                }
            }
        }
    }
}
