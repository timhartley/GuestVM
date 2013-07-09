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

import java.util.*;

/**
 * A ring implementation for the run queue. We implement our own to make sure no allocation
 * and no synchronization is done. This works with objects that implement the RingEntry interface,
 * which is a doubly-linked list abstraction, i.e., the queue is actually a ring, with one entry
 * specially designated as the head. Moving down the queue involves following the "next"
 * link, moving up the queue involves following the "link". The last entry of the queue
 * can be accessed simply by following the "prev" link from the head entry.
 *
 * The empty queue is denoted by _head == null;
 * A queue of one entry has the next and prev links pointing to itself.
 *
 * @author Harald Roeck
 * @suthor Mick Jordan
 *
 */

public class RingRunQueue<T extends RingRunQueueEntry> extends  RunQueue<T> {

    private T _head;
    private int _entries;

    @Override
    public void buildtimeInitialize() {

    }

    public RingRunQueue() {
        _head = null;
    }

    @Override
    public void insert(T entry) {
        if (_head == null) {
            _head = entry;
            entry.setNext(entry); // point next at ourself
            entry.setPrev(entry); // point prev at ourself
        } else {
            entry.setPrev(_head.getPrev()); // old last is prev to entry
            entry.setNext(_head); // next for entry is head
            _head.getPrev().setNext(entry); // old last now points at entry
            _head.setPrev(entry); // entry is now the last
        }
        _entries++;
    }

    @Override
    public T head() {
        return (T) _head;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T next() {
        if (_head == null) {
            return null;
        } else {
            final RingRunQueueEntry next = _head.getNext();
            if (next == _head) {
                return null;
            } else {
                return (T) next;
            }
        }
    }

    @Override
    public boolean empty() {
        return _head == null;
    }

    @Override
    public int size() {
        return _entries;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void remove(T entry) {
        if (_head == entry) {
            _head = (T) entry.getNext();
        }
        if (_head == entry) {
            _head = null;
        } else {
            entry.getNext().setPrev(entry.getPrev());
            entry.getPrev().setNext(entry.getNext());
            entry.setNext(entry);
            entry.setPrev(entry);
        }
        _entries--;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void moveHeadToEnd() {
        if (_head != null) {
            _head = (T) _head.getNext();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new QIterator();
    }

    class QIterator implements Iterator<T> {
        private RingRunQueueEntry _next;

        QIterator() {
            _next = _head;
        }

        public boolean hasNext() {
            return _next != null;
        }

        @SuppressWarnings("unchecked")
        public T next() {
            final T result = (T) _next;
            _next = _next.getNext();
            if (_next == _head) {
                _next = null;
            }
            return result;
        }

        public void remove() {

        }
    }
}
