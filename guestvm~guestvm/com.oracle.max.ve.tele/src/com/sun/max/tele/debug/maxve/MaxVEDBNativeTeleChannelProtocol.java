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

import java.nio.ByteBuffer;

import com.sun.max.program.*;
import com.sun.max.tele.channel.TeleChannelProtocol;
import com.sun.max.tele.debug.ProcessState;

/**
 * An implementation of {@link TeleChannelProtocol} that links directly to native code
 * that communicates directly through JNI to  the Xen ring mechanism to the target Guest VM domain.
 * This requires that the Inspector or Inspector Agent run with root privileges in dom0.
 *
 * @author Mick Jordan
 *
 */

public class MaxVEDBNativeTeleChannelProtocol extends MaxVENativeTeleChannelProtocolAdaptor {

    public MaxVEDBNativeTeleChannelProtocol() {

    }

    @Override
    public boolean attach(int id) {
        Trace.line(1, "attaching to domain " + id);
        natives.teleInitialize(threadLocalsAreaSize);
        return nativeAttach(id);
    }

    @Override
    public boolean detach() {
        Trace.line(1, "detaching from domain");
        return nativeDetach();
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        return nativeActivateWatchpoint(start, size, after, read, write, exec);
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        return nativeDeactivateWatchpoint(start, size);
    }

    @Override
    public boolean gatherThreads(Object teleDomain, Object threadSequence, long threadLocalsList) {
        return nativeGatherThreads(teleDomain, threadSequence, threadLocalsList, 0);
    }

    @Override
    public long getBootHeapStart() {
        return nativeGetBootHeapStart();
    }

    @Override
    public int maxByteBufferSize() {
        return nativeMaxByteBufferSize();
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        return nativeReadBytes(src, dst, false, 0, length);
    }

    @Override
    public int readBytes(long src, ByteBuffer dst, int dstOffset, int length) {
        return nativeReadBytes(src, dst, dst.isDirect(), dstOffset, length);
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        return nativeReadRegisters((int) threadId, integerRegisters, integerRegistersSize, floatingPointRegisters, floatingPointRegistersSize, stateRegisters, stateRegistersSize);
    }

    @Override
    public int readWatchpointAccessCode() {
        return nativeReadWatchpointAccessCode();
    }

    @Override
    public long readWatchpointAddress() {
        return nativeReadWatchpointAddress();
    }

    private boolean terminated;

    /*
     * nativeResume returns true if the target domain has terminated, false if it stopped.
     * Since the resume always succeeds we always return true.
     * There is no per-thread resumption.
     */

    @Override
    public boolean resume(long threadId) {
        terminated = nativeResume();
        return true;
    }

    @Override
    public boolean resumeAll() {
        terminated = nativeResume();
        return true;
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        return nativeSetInstructionPointer((int) threadId, ip) ==.0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        return nativeSetTransportDebugLevel(level);
    }

    @Override
    public boolean singleStep(long threadId) {
        return nativeSingleStep((int) threadId);
    }

    @Override
    public boolean suspend(long threadId) {
        return nativeSuspend((int) threadId);
    }

    @Override
    public boolean suspendAll() {
        return nativeSuspendAll();
    }

    /*
     * Note that resume does not return until the domain is stopped or terminated.
     */
    @Override
    public ProcessState waitUntilStopped() {
        return terminated ? ProcessState.TERMINATED : ProcessState.STOPPED;
    }

    @Override
    public int waitUntilStoppedAsInt() {
        return terminated ? ProcessState.TERMINATED.ordinal() : ProcessState.STOPPED.ordinal();
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        return nativeWriteBytes(dst, src, false, 0, length);
    }

    @Override
    public int writeBytes(long dst, ByteBuffer src, int srcOffset, int length) {
        return nativeWriteBytes(dst, src, src.isDirect(), srcOffset, length);
    }

    private static native boolean nativeAttach(int domId);
    private static native boolean nativeDetach();
    private static native long nativeGetBootHeapStart();
    private static native int nativeSetTransportDebugLevel(int level);
    private static native int nativeReadBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length);
    private static native int nativeWriteBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length);
    private static native int nativeMaxByteBufferSize();
    private static native boolean nativeGatherThreads(Object teleDomain, Object threadSequence, long threadLocalsList, long primordialThreadLocals);
    private static native boolean nativeResume();
    private static native int nativeSetInstructionPointer(int threadId, long ip);
    private static native boolean nativeSingleStep(int threadId);
    private static native boolean nativeSuspendAll();
    private static native boolean nativeSuspend(int threadId);
    private static native boolean nativeActivateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec);
    private static native boolean nativeDeactivateWatchpoint(long start, long size);
    private static native long nativeReadWatchpointAddress();
    private static native int nativeReadWatchpointAccessCode();

    private static native boolean nativeReadRegisters(int threadId,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

}
