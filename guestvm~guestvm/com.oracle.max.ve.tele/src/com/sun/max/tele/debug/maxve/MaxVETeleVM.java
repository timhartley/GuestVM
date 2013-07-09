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
package com.sun.max.tele.debug.maxve;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.hosted.BootImage;
import com.sun.max.vm.hosted.BootImageException;

public class MaxVETeleVM extends TeleVM {

    public MaxVETeleVM(BootImage bootImage, Classpath sourcepath, String[] commandlineArguments) throws BootImageException {
        super(bootImage, sourcepath, commandlineArguments);
    }

    @Override
    protected TeleProcess createTeleProcess(String[] commandLineArguments) throws BootImageException {
        throw new BootImageException("domain creation not supported from the Inspector");
    }

    @Override
    protected TeleProcess attachToTeleProcess() {
        return new MaxVETeleDomain(this, Platform.platform(), targetLocation().id);
    }

    @Override
    protected Pointer loadBootImage() throws BootImageException {
        // the only reason we override this is to ensure we go via MaxVEDBChannel to get the lock
        return MaxVEXenDBChannel.getBootHeapStart();
    }

}
