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
package com.sun.max.ve.fs;

import java.util.*;
import com.sun.max.annotate.*;

/**
 * Common support for handling the fileoffset value in an open file.
 *
 * @author Mick Jordan
 *
 */
public class VirtualFileSystemOffset {

    private static Map<Integer, Value> _table = new HashMap<Integer, Value>();

    public static long get(int fd) {
        return getValue(fd)._offset;
    }

    public static void inc(int fd) {
        getValue(fd)._offset++;
    }

    // Checkstyle: stop indentation check
    public static void add(int fd, long incr) {
        getValue(fd)._offset += incr;
    }

    public static void set(int fd, long offset) {
        getValue(fd)._offset = offset;
    }
    // Checkstyle: resume indentation check

    public static void remove(int fd) {
        _table.remove(fd);
    }

    @INLINE
    static Value getValue(int fd) {
        Value v = _table.get(fd);
        if (v == null) {
            v = new Value();
            _table.put(fd, v);
        }
        return v;
    }

    static class Value {

        long _offset;
    }
}
