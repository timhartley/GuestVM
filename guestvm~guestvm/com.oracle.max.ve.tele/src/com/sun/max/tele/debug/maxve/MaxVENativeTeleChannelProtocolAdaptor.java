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

import com.sun.max.program.ProgramError;
import com.sun.max.tele.channel.natives.TeleChannelNatives;

public abstract class MaxVENativeTeleChannelProtocolAdaptor implements MaxVETeleChannelProtocol {
    protected TeleChannelNatives natives;
    protected int threadLocalsAreaSize;

    public MaxVENativeTeleChannelProtocolAdaptor() {
        natives = new TeleChannelNatives();
    }

    @Override
    public boolean initialize(int threadLocalsAreaSize, boolean bigEndian) {
        this.threadLocalsAreaSize = threadLocalsAreaSize;
        return true;
    }

    @Override
    public void setNativeAddresses(long threadListAddress, long bootHeapStartAddress, long resumeAddress) {
        // by default, nothing to do.
    }

    @Override
    public long create(String programFile, String[] commandLineArguments) {
        return -1;
    }

    @Override
    public boolean kill() {
        return false;
    }

    @Override
    public int gatherThreads(long threadLocalsList) {
        ProgramError.unexpected("TeleChannelProtocol.gatherThreads(int, int) should not be called in this configuration");
        return 0;
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadsData) {
        ProgramError.unexpected("TeleChannelProtocol.readThreads should not be called in this configuration");
        return 0;
    }


}
