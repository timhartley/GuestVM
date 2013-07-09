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
package com.sun.max.ve.sched.priority;

import java.util.*;

import com.sun.max.*;
import com.sun.max.ve.sched.RingRunQueue;
import com.sun.max.ve.sched.RingRunQueueEntry;
import com.sun.max.ve.sched.RunQueue;

/**
 * This class supports O(1) prioritized insertion of elements into the logical queue.
 * It is implemented as an array of @see RingRunQueue objects.
 *
 * @author Mick Jordan
 *
 * @param <T>
 */

public class PriorityRingRunQueue<T extends PriorityRingRunQueueEntry> extends RunQueue<T> {

    private RingRunQueue<T>[] _queues;
    private int _min;
    private int _max;
    private int _entries;  // total number of entries
    private int _lwm;      // low water mark of non-empty queue
    private int _hwm;     // high water mark of non-empty queue
    private LevelRingQueueCreator<T> _creator;

    public interface LevelRingQueueCreator<T extends PriorityRingRunQueueEntry> {
        RingRunQueue<T> create();
    }

    public PriorityRingRunQueue(int min, int max, LevelRingQueueCreator<T> creator) {
        _min = min;
        _max = max;
        _hwm = _min;
        _lwm = _max;
        _creator = creator;
    }

    @Override
    public void buildtimeInitialize() {
        // allocate a queue for each priority level
        _queues = Utils.cast(new RingRunQueue[_max - _min + 1]);
        for (int q = 0; q < _queues.length; q++) {
            _queues[q] = _creator.create();
        }
    }

    @Override
    public void insert(T entry) {
        final int p = entry.getPriority();
        final RingRunQueue<T> queue = _queues[p - _min];
        queue.insert(entry);
        _entries++;
        if (p > _hwm) {
            _hwm = p;
        }
        if (p < _lwm) {
            _lwm = p;
        }
    }

    @Override
    public T head() {
        // return head of highest priority queue
        for (int i = _hwm; i >= _lwm; i--) {
            final RingRunQueue<T> queue = _queues[i - _min];
            final T head = queue.head();
            if (head != null) {
                return head;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T next() {
        // this spans priorities
        T theHead = null;
        for (int i = _hwm; i >= _lwm; i--) {
            final RingRunQueue<T> queue = _queues[i - _min];
            final T head = queue.head();
            if (head != null) {
                if (theHead != null) {
                    return head;
                }
                theHead = head;
                final RingRunQueueEntry next = head.getNext();
                if (next != head)  {
                    return (T) next;
                }
                // otherwise keep on looking at lower priority
            }
        }
        return null;
    }

    @Override
    public boolean empty() {
        return _entries == 0;
    }

    @Override
    public int size() {
        return _entries;
    }

    @Override
    public void moveHeadToEnd() {
        // only moves entries in the same priority queue
        for (int i = _hwm; i >= _lwm; i--) {
            final RingRunQueue<T> queue = _queues[i - _min];
            final T head = queue.head();
            if (head != null) {
                queue.moveHeadToEnd();
                return;
            }
        }
    }

    @Override
    public void remove(T entry) {
        final int p = entry.getPriority();
        RingRunQueue<T> queue = _queues[p - _min];
        queue.remove(entry);
        _entries--;
        final T head = queue.head();
        if (head == null) {
            // queue became empty, adjust high/low watermarks
            // if we find nothing, we reset
            if (p == _hwm) {
                _hwm = _min;
                for (int i = p; i >= _lwm; i--) {
                    queue = _queues[i - _min];
                    if (queue.head() != null) {
                        _hwm = i;
                        break;
                    }
                }
            }
            if (p == _lwm) {
                _lwm = _max;
                for (int i = p; i <= _hwm; i++) {
                    queue = _queues[i - _min];
                    if (queue.head() != null) {
                        _lwm = i;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new QIterator();
    }

    class QIterator implements Iterator<T> {
        private RingRunQueueEntry _next;
        private RingRunQueueEntry _head;
        private int _level;

        QIterator() {
            for (int i = _hwm; i >= _lwm; i--) {
                final RingRunQueue<T> queue = _queues[i - _min];
                final T head = queue.head();
                if (head != null) {
                    _next = head;
                    _head = head;
                    _level = i;
                    return;
                }
            }
            _next = null;
        }

        public boolean hasNext() {
            return _next != null;
        }

        public T next() {
            @SuppressWarnings("unchecked")
            final T result = (T) _next;
            _next = _next.getNext();
            if (_next == _head) {
                // exhausted this level, look lower
                _head = null;
                for (int i = _level - 1; i >= _lwm; i--) {
                    final RingRunQueue<T> queue = _queues[i - _min];
                    final T head = queue.head();
                    if (head != null) {
                        _next = head;
                        _head = head;
                        _level = i;
                        break;
                    }
                }
                if (_head == null) {
                    // done
                    _next = null;
                }
            }
            return result;
        }

        public void remove() {

        }
    }

}
