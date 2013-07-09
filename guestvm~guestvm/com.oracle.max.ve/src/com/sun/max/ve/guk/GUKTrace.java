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
package com.sun.max.ve.guk;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;

/**
 * Low-level access to the GUK tracing mechanism. Permits:
 * <ul>
 * <li>Setting the value of one of the standard microkernel trace options,
 * which are hard-coded into the microkernel code.
 * <li>User defined traces with arguments that use the microkernel
 * tracing mechanism. Enabling/disabling such traces is not supported
 * here, but should be handled by a system property in the client code.
 * </ul>

 *
 * Unfortunately, we can't do the varargs thing (allocation is a no-no).
 * N.B. Trace name strings must be allocated at image build time to allow the
 * associated byte array pointers to be passed to the microkernel without GC concerns.
 * Further, thney mist be explicitly terminated by \0, e.g. "TEST_ENTER\0".
 * All scalar arguments are passed as longs.
 *
 * Trace setting values for the standard microkernel trace options are cached here,
 * so any changes made via back doors won't be observed.
 *
 * @author Mick Jordan
 *
 */

public class GUKTrace {

    /**
     * The order must match that in guk/trace.h.
     *
     */
    public enum  Name {
        SCHED, STARTUP, BLK, DB_BACK, FS_FRONT, GNTTAB, MM, MMPT, NET, SERVICE, SMP, XENBUS, TRAPS;
        boolean _value;
    }

    /**
     * The offset of the byte array data from the byte array object's origin.
     */
    private static final Offset _dataOffset = VMConfiguration.vmConfig().layoutScheme().byteArrayLayout.getElementOffsetFromOrigin(0);

    private static final Name[] _values = Name.values();

    public static boolean setTraceState(Name name, boolean value) {
        return setTraceState(name.ordinal(), value);
    }

    public static boolean setTraceState(int ordinal, boolean value) {
        final int previous = GUK.guk_set_trace_state(ordinal, value ? 1 : 0);
        _values[ordinal]._value = value;
        return previous != 0;
    }

    public static boolean getTraceState(Name name) {
        if (!_cached) {
            populateCache();
        }
        return name._value;
    }

    private static boolean _cached;
    /* This will force class initialization for Name at image build time
     * Important if we do any tracing before the GC is ready!
     */
    private static Name[] _allNames = Name.values();

    private static void populateCache() {
        if (!_cached) {
            for (Name name : _allNames)  {
                final int state =  GUK.guk_get_trace_state(name.ordinal());
                name._value = state != 0;
            }
        }
    }

    @INLINE
    private static Pointer toPointer(byte[] fmt) {
        return Reference.fromJava(fmt).toOrigin().plus(_dataOffset);
    }

    /*
     * The following methods generate traces of the form: "%s %ld %ld ...".
     */

    @INLINE
    public static void print(byte[] fmt) {
        GUK.guk_ttprintk0(toPointer(fmt));
    }

    @INLINE
    public static void print1L(byte[] fmt, long arg) {
        GUK.guk_ttprintk1(toPointer(fmt), arg);
    }

    @INLINE
    public static void print2L(byte[] fmt, long arg1, long arg2) {
        GUK.guk_ttprintk2(toPointer(fmt), arg1, arg2);
    }

    @INLINE
    public static void print3L(byte[] fmt, long arg1, long arg2, long arg3) {
        GUK.guk_ttprintk3(toPointer(fmt), arg1, arg2, arg3);
    }

    @INLINE
    public static void print4L(byte[] fmt, long arg1, long arg2, long arg3, long arg4) {
        GUK.guk_ttprintk4(toPointer(fmt), arg1, arg2, arg3, arg4);
    }

    @INLINE
    public static void print5L(byte[] fmt, long arg1, long arg2, long arg3, long arg4, long arg5) {
        GUK.guk_ttprintk5(toPointer(fmt), arg1, arg2, arg3, arg4, arg5);
    }

    public static final byte[] TEST_TRACE_ENTER = "TEST_ENTER\0".getBytes();
    public static final byte[] TEST_TRACE_EXIT = "TEST_EXIT\0".getBytes();

    public static void xprint(byte[] fmt) {
        print(fmt);
    }

}
