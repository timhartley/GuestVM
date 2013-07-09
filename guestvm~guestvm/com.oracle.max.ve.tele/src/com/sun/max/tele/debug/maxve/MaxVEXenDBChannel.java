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

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import com.sun.max.tele.MaxWatchpoint.WatchpointSettings;
import com.sun.max.tele.TeleVM;
import com.sun.max.tele.debug.ProcessState;
import com.sun.max.tele.debug.TeleNativeThread;
import com.sun.max.tele.debug.TeleWatchpoint;
import com.sun.max.tele.memory.TeleFixedMemoryRegion;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.Pointer;
import com.sun.max.vm.runtime.FatalError;

/**
 * This class encapsulates all interaction with the Xen db communication channel and ensures
 * that access is single-threaded.
 *
 * @author Mick Jordan
 *
 */
public final class MaxVEXenDBChannel {
    private static MaxVETeleDomain teleDomain;
    private static MaxVETeleChannelProtocol channelProtocol;
    private static int maxByteBufferSize;

    public static synchronized void attach(MaxVETeleDomain teleDomain, int domId) {
        MaxVEXenDBChannel.teleDomain = teleDomain;
        channelProtocol = (MaxVETeleChannelProtocol) TeleVM.teleChannelProtocol();
        channelProtocol.initialize(teleDomain.vm().bootImage().header.tlaSize, false);
        // To avoid having to replicate the DB/XG sub-variant on this side of the channel protocol,
        // we always call setNativeAddresses, even though the DB/Dump modes don't need it.
        final File maxvm = new File(teleDomain.vm().vmDirectory(), "maxvm");
        try {
            final ImageFileHandler fh  = ImageFileHandler.open(maxvm);
            channelProtocol.setNativeAddresses(fh.getThreadListSymbolAddress(), fh.getBootHeapStartSymbolAddress(), fh.getSymbolAddress("xg_resume_flag"));
        } catch (Exception ex) {
            FatalError.unexpected("failed to open maxvm image file", ex);
        }
        channelProtocol.attach(domId);
        maxByteBufferSize = channelProtocol.maxByteBufferSize();
    }

    public static synchronized Pointer getBootHeapStart() {
        return Pointer.fromLong(channelProtocol.getBootHeapStart());
    }

    public static synchronized void setTransportDebugLevel(int level) {
        channelProtocol.setTransportDebugLevel(level);
    }

    private static int readBytes0(long src, ByteBuffer dst, int dstOffset, int length) {
        assert dst.limit() - dstOffset >= length;
        if (dst.isDirect()) {
            return channelProtocol.readBytes(src, dst, dstOffset, length);
        }
        assert dst.array() != null;
        return channelProtocol.readBytes(src, dst.array(), dst.arrayOffset() + dstOffset, length);
    }

    public static synchronized int readBytes(Address src, ByteBuffer dst, int dstOffset, int length) {
        int lengthLeft = length;
        int localOffset = dstOffset;
        long localAddress = src.toLong();
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > maxByteBufferSize ? maxByteBufferSize : lengthLeft;
            final int r = readBytes0(localAddress, dst, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }

    private static int writeBytes0(long dst, ByteBuffer src, int srcOffset, int length) {
        assert src.limit() - srcOffset >= length;
        if (src.isDirect()) {
            return channelProtocol.writeBytes(dst, src, srcOffset, length);
        }
        assert src.array() != null;
        return channelProtocol.writeBytes(dst, src.array(), src.arrayOffset() + srcOffset, length);

    }

    public static synchronized int writeBytes(ByteBuffer buffer, int offset, int length, Address address) {
        int lengthLeft = length;
        int localOffset = offset;
        long localAddress = address.toLong();
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > maxByteBufferSize ? maxByteBufferSize : lengthLeft;
            final int r = writeBytes0(localAddress, buffer, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }

    public static synchronized void gatherThreads(List<TeleNativeThread> threads, long threadLocalsList) {
        channelProtocol.gatherThreads(teleDomain, threads, threadLocalsList);
    }

    public static synchronized boolean resume(int domainId) {
        return channelProtocol.resume(0);
    }

    public static synchronized ProcessState waitUntilStopped() {
        return channelProtocol.waitUntilStopped();
    }

    public static synchronized boolean setInstructionPointer(int threadId, long ip) {
        return channelProtocol.setInstructionPointer(threadId, ip);
    }

    public static synchronized boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize) {
        return channelProtocol.readRegisters(threadId, integerRegisters, integerRegistersSize, floatingPointRegisters, floatingPointRegistersSize, stateRegisters, stateRegistersSize);
    }

    public static synchronized boolean singleStep(int threadId) {
        return channelProtocol.singleStep(threadId);
    }

    /**
     * This is not synchronized because it is used to interrupt a resume that already holds the lock.
     *
     * @return
     */
    public static boolean suspendAll() {
        return channelProtocol.suspendAll();
    }

    public static synchronized boolean suspend(int threadId) {
        return channelProtocol.suspend(threadId);
    }

    public static synchronized boolean activateWatchpoint(int domainId, TeleWatchpoint teleWatchpoint) {
        final WatchpointSettings settings = teleWatchpoint.getSettings();
        return channelProtocol.activateWatchpoint(teleWatchpoint.memoryRegion().start().toLong(), teleWatchpoint.memoryRegion().nBytes(), true, settings.trapOnRead, settings.trapOnWrite, settings.trapOnExec);
    }

    public static synchronized boolean deactivateWatchpoint(int domainId, TeleFixedMemoryRegion memoryRegion) {
        return channelProtocol.deactivateWatchpoint(memoryRegion.start().toLong(), memoryRegion.nBytes());
    }

    public static synchronized long readWatchpointAddress(int domainId) {
        return channelProtocol.readWatchpointAddress();
    }

    public static synchronized int readWatchpointAccessCode(int domainId) {
        return channelProtocol.readWatchpointAccessCode();
    }

}
