/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ve.jdk;

import com.sun.max.annotate.*;
import com.sun.max.ve.guk.GUKPagePool;

/**
 * Substitutions for @see com.sun.management.UnixOperatingSystem.
 *
 * @author Mick Jordan
 *
 */
@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "com.sun.management.UnixOperatingSystem")

final class JDK_com_sun_management_UnixOperatingSystem {

    @SUBSTITUTE
    private long getCommittedVirtualMemorySize() {
        // TODO: need to add up all the VM in use
        return 0;
    }

    @SUBSTITUTE
    private long getTotalSwapSpaceSize() {
        // no swap space
        return 0;
    }

    @SUBSTITUTE
    private long getFreeSwapSpaceSize() {
        // no swap space
        return 0;
    }

    @SUBSTITUTE
    private long getProcessCpuTime() {
        // TODO implement this
        return 0;
    }

    @SUBSTITUTE
    private long getFreePhysicalMemorySize() {
        return GUKPagePool.getFreePages() * GUKPagePool.PAGE_SIZE;
    }

    @SUBSTITUTE
    private long getTotalPhysicalMemorySize() {
        return GUKPagePool.getCurrentReservation() * GUKPagePool.PAGE_SIZE;
    }

    @SUBSTITUTE
    private long getOpenFileDescriptorCount() {
        // TODO we could implement this
        return 0;
    }

    @SUBSTITUTE
    private long getMaxFileDescriptorCount() {
        // no limit
        return Long.MAX_VALUE;
    }

    @SUBSTITUTE
    private void initialize() {
    }


}
