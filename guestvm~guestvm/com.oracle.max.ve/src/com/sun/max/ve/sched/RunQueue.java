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
 * Basic run queue support for scheduler (but implementation independent for testing).
 * Used by the Java scheduler to organize the runnable threads.
 * The choice of data structure is left to the concrete subclass
 * Access to the queue must be protected by the spin lock associated with the queue.
 * Methods with name lockedXXX acquire and release the lock internally, otherwise
 * it is the callers responsibility to ensure the lock is held (useful for multiple operations).
 *
 * Run queues are typically allocated during image build in the Maxine boot heap,
 * but may require additional initialization at run-time (e.g. native spin lock creation).
 *
 * Therefore, separate initialization methods are provided for use at image build time
 * and runtime.
 *
 * @author Mick Jordan
 * @author Harald Roeck
 *
 * @param <T> Queue entry type
 */

public abstract class RunQueue<T> extends SpinLockedRunQueue implements Iterable<T> {
    /**
     * Image buildtime initialization for the queue implementation.
     */
    public abstract void buildtimeInitialize();

    /*
     * The caller must hold the lock before invoking the following methods.
     */

    /**
     * Insert a thread at the end of the run queue.
     *
     * @param thread thread to insert
     */
    public abstract void insert(T thread);

    /**
     * Remove a thread from the run queue.
     *
     * @param thread thread to remove
     */
    public abstract void remove(T thread);

    /**
     * Return the head of this queue.
     *
     * @return the first thread in the queue
     */
    public abstract T head();

    /**
     * Return the second element of the queue.
     *
     * @return second element of the queue or null if none
     */
    public abstract T next();

    /**
     * Move head to end of queue.
     */
    public abstract void moveHeadToEnd();

    /**
     * Size (length) of the queue.
     */
    public abstract int size();

    /** Is the queue empty?
     * @return true if queue is empty
     */
    public abstract boolean empty();

    /*
     * These variants internally lock/unlock the queue. Therefore, the lock must not already be held.
     */

    /**
     * Insert a thread at the end of the run queue.
     *
     * @param thread
     *                thread to insert
     */
    public void lockedInsert(T thread) {
        try {
            lock();
            insert(thread);
        } finally {
            unlock();
        }
    }

    /**
     * Remove a thread from the run queue. until it invokes the scheduler.
     *
     * @param thread
     *                thread to remove
     */
    public void lockedRemove(T thread) {
        try {
            lock();
            remove(thread);
        } finally {
            unlock();
        }
    }

    /**
     * Return the head of this queue, i.e the thread currently running or next to run (if a ukernel thread is currently
     * executing).
     *
     * @return the first thread in the queue
     */
    public T lockedHead() {
        try {
            lock();
            return head();
        } finally {
            unlock();
        }
    }

    /**
     * Return the second element of the queue, i.e. the thread to run next.
     *
     * @return second element of the queue or null if none
     */
    public T lockedNext() {
        try {
            lock();
            return next();
        } finally {
            unlock();
        }

    }

    /**
     * Move head to end of queue.
     */
    public void lockedMoveHeadToEnd() {
        try {
            lock();
            moveHeadToEnd();
        } finally {
            unlock();
        }
    }

    /**
     * Size (length) of the queue.
     */
    public int lockedSize() {
        try {
            lock();
            return size();
        } finally {
            unlock();
        }
    }

    /**
     * Is the queue empty?
     *
     * @return true if queue is empty
     */
    public boolean lockedEmpty() {
        try {
            lock();
            return empty();
        } finally {
            unlock();
        }
    }


}
