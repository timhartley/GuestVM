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
package com.sun.max.ve.test;

import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.*;
import com.sun.max.ve.sched.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.thread.*;

/**
 * Some support classes for tests that involve {@link VmThread}.
 * These must be compiled into the image.
 *
 * @author Mick Jordan
 *
 */
public class VmThreadTestHelper {
    public static VmThread current() {
        return VmThread.current();
    }

    public static long currentAsAddress() {
        return Reference.fromJava(VmThread.current()).toOrigin().asAddress().toLong();
    }

    public static int idLocal() {
        return VmThreadLocal.ID.load(VmThread.currentTLA()).asAddress().toInt();
    }

    public static int idCurrent() {
        return VmThread.current().id();
    }

    public static long nativeCurrent() {
        return VmThread.current().nativeThread().asAddress().toLong();
    }

    public static long nativeUKernel() {
        return GUKScheduler.currentThread().toLong();
    }

    public static int nativeId(Thread t) {
        final GUKVmThread gvm = (GUKVmThread) VmThread.fromJava(t);
        return gvm == null ? -1 : gvm.safeNativeId();
    }


}
