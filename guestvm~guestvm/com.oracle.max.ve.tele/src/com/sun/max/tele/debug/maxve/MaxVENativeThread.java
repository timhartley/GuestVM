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

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

public class MaxVENativeThread extends TeleNativeThread {

    @Override
    public MaxVETeleDomain teleProcess() {
        return (MaxVETeleDomain) super.teleProcess();
    }

    protected MaxVENativeThread(MaxVETeleDomain teleDomain, Params params) {
        super(teleDomain, params);
    }

    @Override
    protected boolean readRegisters(byte[] integerRegisters, byte[] floatingPointRegisters, byte[] stateRegisters) {
        return MaxVEXenDBChannel.readRegisters((int) localHandle(),
                        integerRegisters, integerRegisters.length,
                        floatingPointRegisters, floatingPointRegisters.length,
                        stateRegisters, stateRegisters.length);
    }

    @Override
    protected boolean updateInstructionPointer(Address address) {
        return MaxVEXenDBChannel.setInstructionPointer((int) localHandle(), address.toLong());
    }

    @Override
    protected boolean singleStep() {
        return MaxVEXenDBChannel.singleStep((int) localHandle());
    }

    // In the current synchronous connection with the target domain, we only ever stop at a breakpoint
    // and control does not return to the inspector until that happens.

    @Override
    public boolean threadSuspend() {
        return MaxVEXenDBChannel.suspend((int) localHandle());
    }

    @Override
    protected boolean threadResume() {
        throw FatalError.unimplemented();
    }
}
