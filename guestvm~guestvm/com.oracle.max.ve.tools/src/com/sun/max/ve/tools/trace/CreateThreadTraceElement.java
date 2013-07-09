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
package com.sun.max.ve.tools.trace;

/**
 * Thread ids are kernel ids:
 * -1: no thread (kernel startup)
 * 0-31: idle threads for CPUs 0-31
 * 32: debug handler thread
 * 33 - : kernel and Java threads
 *
 * @author Mick Jordan
 *
 */

import java.util.*;

public class CreateThreadTraceElement extends ThreadIdTraceElement {
    private String _name;
    private int _cpu;
    private long _stack;
    private int  _flags;
    private static ThreadIterable _myIterable = new ThreadIterable();

    static {
        final CreateThreadTraceElement startUp = new CreateThreadTraceElement();
        startUp.setId(-1);
        startUp.setName("kernel-startup");
        startUp.setInitialCpu(0);
    }


    public CreateThreadTraceElement() {
        _myIterable.addElement(this);
    }

    public CreateThreadTraceElement setName(String name) {
        _name = name;
        return this;
    }

    public CreateThreadTraceElement setInitialCpu(int cpu) {
        _cpu = cpu;
        return this;
    }

    public CreateThreadTraceElement setStack(long stack) {
        _stack = stack;
        return this;
    }

    public CreateThreadTraceElement setFlags(int flags) {
        _flags = flags;
        return this;
    }

    public String getName() {
        return _name;
    }

    public int getInitialCpu() {
        return _cpu;
    }

    @Override
    public String toString() {
        return super.toString() + " " + _name + " " + _cpu + " " + _flags + " " + Long.toHexString(_stack);
    }

    public static Iterable<CreateThreadTraceElement> getThreadIterable(boolean includeKernel) {
        _myIterable.includeKernel(includeKernel);
        return _myIterable;
    }

    public static Iterable<CreateThreadTraceElement> getThreadIterable() {
        return getThreadIterable(false);
    }

    public static CreateThreadTraceElement find(int id) {
        for (CreateThreadTraceElement t : _myIterable) {
            if (t.getId() == id) {
                return t;
            }
        }
        throw new RuntimeException("thread id " + id + " not found");
    }

    public static final class ThreadIterable implements Iterable<CreateThreadTraceElement> {
        private boolean _includeKernel = false;
        public static List<CreateThreadTraceElement> _list = new ArrayList<CreateThreadTraceElement>();

        private ThreadIterable() {
        }

        private void includeKernel(boolean includeKernel) {
            _includeKernel = includeKernel;
        }

        public Iterator<CreateThreadTraceElement> iterator() {
            return new ThreadIterator(_list.listIterator(), _includeKernel);
        }

        private void addElement(CreateThreadTraceElement t) {
            _list.add(t);
        }
    }

    private static class ThreadIterator implements Iterator<CreateThreadTraceElement> {

        ListIterator<CreateThreadTraceElement> _iter;

        ThreadIterator(ListIterator<CreateThreadTraceElement> iter, boolean includeKernel) {
            _iter = iter;
            if (!includeKernel) {
                _iter.next();
            }
        }

        public boolean hasNext() {
            return _iter.hasNext();
        }

        public void remove() {

        }

        public CreateThreadTraceElement next() {
            return _iter.next();
        }
    }

}
