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

import static com.sun.max.platform.Platform.*;

import java.nio.*;
import java.util.*;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.DataAccess;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;

public class MaxVETeleDomain extends TeleProcess {

    private int domainId;
    private final DataAccess dataAccess;

    protected MaxVETeleDomain(TeleVM teleVM, Platform platform, int id) {
        super(teleVM, platform, ProcessState.STOPPED);
        this.domainId = id;
        dataAccess = new PageDataAccess(this, platform.dataModel);
        MaxVEXenDBChannel.attach(this, id);
    }

    @Override
    public DataAccess dataAccess() {
        return dataAccess;
    }

    @Override
    protected TeleNativeThread createTeleNativeThread(Params params) {
        /* Need to align and skip over the guard page at the base of the stack.
         * N.B. "base" is low address (i.e., actually the end of the stack!).
         */
        final int pageSize = platform().pageSize;
        final long stackBottom = pageAlign(params.stackRegion.start().toLong(), pageSize) + pageSize;
        final long adjStackSize = params.stackRegion.nBytes() - (stackBottom - params.stackRegion.start().toLong());
        final TeleFixedMemoryRegion adjStack = new TeleFixedMemoryRegion(vm(), params.stackRegion.regionName(), Address.fromLong(stackBottom), adjStackSize);
        params.stackRegion = adjStack;
        return new MaxVENativeThread(this, params);
    }

    private static long pageAlign(long address, int pageSize) {
        final long alignment = pageSize - 1;
        return (long) (address + alignment) & ~alignment;

    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        if (!TeleVM.isDump()) {
            ProgramWarning.message("unimplemented: " + "cannot kill target domain from Inspector");
        }
    }

    // In the current synchronous connection with the target domain, we only ever stop at a breakpoint
    // and control does not return to the inspector until that happens.

    @Override
    protected ProcessState waitUntilStopped() {
        return MaxVEXenDBChannel.waitUntilStopped();
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        MaxVEXenDBChannel.resume(domainId);
    }

    @Override
    public void suspend() throws OSExecutionRequestException {
        if (!MaxVEXenDBChannel.suspendAll()) {
            throw new OSExecutionRequestException("Could not suspend the VM");
        }
    }

    @Override
    public void setTransportDebugLevel(int level) {
        MaxVEXenDBChannel.setTransportDebugLevel(level);
        super.setTransportDebugLevel(level);
    }

    @Override
    protected int read0(Address address, ByteBuffer buffer, int offset, int length) {
        return MaxVEXenDBChannel.readBytes(address, buffer, offset, length);
    }

    @Override
    protected int write0(ByteBuffer buffer, int offset, int length, Address address) {
        return MaxVEXenDBChannel.writeBytes(buffer, offset, length, address);
    }

    @Override
    protected void gatherThreads(List<TeleNativeThread> threads) {
        final Word threadLocalsList = dataAccess().readWord(vm().bootImageStart().plus(vm().bootImage().header.tlaListHeadOffset));
        MaxVEXenDBChannel.gatherThreads(threads, threadLocalsList.asAddress().toLong());
    }

    @Override
    public int platformWatchpointCount() {
        // not sure how many are supported; we'll try this
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean activateWatchpoint(TeleWatchpoint teleWatchpoint) {
        return MaxVEXenDBChannel.activateWatchpoint(domainId, teleWatchpoint);
    }

    @Override
    protected boolean deactivateWatchpoint(TeleWatchpoint teleWatchpoint) {
        return MaxVEXenDBChannel.deactivateWatchpoint(domainId, teleWatchpoint.memoryRegion());
    }

    @Override
    protected long readWatchpointAddress() {
        return MaxVEXenDBChannel.readWatchpointAddress(domainId);
    }

    @Override
    protected int readWatchpointAccessCode() {
        int code = MaxVEXenDBChannel.readWatchpointAccessCode(domainId);
        if (code == 1) {
            return 1;
        } else if (code == 2) {
            return 2;
        } else if (code == 4) {
            return 3;
        }
        return 0;
    }
}
