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

import static com.sun.max.tele.MaxThreadState.JOIN_WAIT;
import static com.sun.max.tele.MaxThreadState.MONITOR_WAIT;
import static com.sun.max.tele.MaxThreadState.NOTIFY_WAIT;
import static com.sun.max.tele.MaxThreadState.RUNNING;
import static com.sun.max.tele.MaxThreadState.SLEEPING;
import static com.sun.max.tele.MaxThreadState.SUSPENDED;
import static com.sun.max.tele.MaxThreadState.WATCHPOINT;
import static com.sun.max.ve.sched.GUKVmThread.AUX1_FLAG;
import static com.sun.max.ve.sched.GUKVmThread.AUX2_FLAG;
import static com.sun.max.ve.sched.GUKVmThread.CPU_OFFSET;
import static com.sun.max.ve.sched.GUKVmThread.FLAGS_OFFSET;
import static com.sun.max.ve.sched.GUKVmThread.ID_OFFSET;
import static com.sun.max.ve.sched.GUKVmThread.IP_OFFSET;
import static com.sun.max.ve.sched.GUKVmThread.JOIN_FLAG;
import static com.sun.max.ve.sched.GUKVmThread.NEXT_OFFSET;
import static com.sun.max.ve.sched.GUKVmThread.RUNNING_FLAG;
import static com.sun.max.ve.sched.GUKVmThread.SLEEP_FLAG;
import static com.sun.max.ve.sched.GUKVmThread.SP_OFFSET;
import static com.sun.max.ve.sched.GUKVmThread.STRUCT_LIST_HEAD_SIZE;
import static com.sun.max.ve.sched.GUKVmThread.STRUCT_THREAD_SIZE;
import static com.sun.max.ve.sched.GUKVmThread.THREAD_LIST_OFFSET;
import static com.sun.max.ve.sched.GUKVmThread.UKERNEL_FLAG;
import static com.sun.max.ve.sched.GUKVmThread.WATCH_FLAG;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import com.sun.max.tele.MaxThreadState;
import com.sun.max.tele.channel.TeleChannelDataIOProtocol;
import com.sun.max.tele.debug.dump.ThreadAccess;
import com.sun.max.tele.debug.maxve.xen.X86_64Registers;

/**
 * Accesses the GUK thread list to support the gathering of threads by the Inspector.
 *
 * @author Mick Jordan
 *
 */
public class GUKThreadListAccess extends ThreadAccess {
    private final static int MAXINE_THREAD_ID = 40;
    private long threadListAddress;

    static class GUKThreadInfo implements ThreadInfo {
        private final int id;
        private final int flags;
        private final int cpu;
        private long rsp;
        private long rip;
        // full register cache, available if regsAvail == true;
        public boolean regsAvail;
        public byte[] integerRegisters = new byte[128];
        public byte[] floatingPointRegisters = new byte[128];
        public byte[] stateRegisters = new byte[16];

        GUKThreadInfo(long threadHandle, int id, int flags, int cpu) {
            this.id = id;
            this.flags = flags;
            this.cpu = cpu;
            Arrays.fill(integerRegisters, (byte) 0);
            Arrays.fill(floatingPointRegisters, (byte) 0);
            Arrays.fill(stateRegisters, (byte) 0);
        }

        public long getStackPointer() {
            return rsp;
        }

        public long getInstructionPointer() {
            return rip;
        }

        public int getId() {
            return id;
        }

        public int getThreadState() {
            return toThreadState(flags).ordinal();
        }
    }

    public GUKThreadListAccess(TeleChannelDataIOProtocol protocol, int threadLocalsAreaSize, long threadListAddress) {
        super(protocol, threadLocalsAreaSize);
        this.threadListAddress = threadListAddress;
    }

    public static MaxThreadState toThreadState(int state) {
        if ((state & AUX1_FLAG) != 0) {
            return MONITOR_WAIT;
        }
        if ((state & AUX2_FLAG) != 0) {
            return NOTIFY_WAIT;
        }
        if ((state & JOIN_FLAG) != 0) {
            return JOIN_WAIT;
        }
        if ((state & SLEEP_FLAG) != 0) {
            return SLEEPING;
        }
        if ((state & WATCH_FLAG) != 0) {
            return WATCHPOINT;
        }
        if ((state & RUNNING_FLAG) != 0) {
            return RUNNING;
        }
        // default
        return SUSPENDED;
    }

    @Override
    public void gatherOSThreads(List<ThreadInfo> threadList) {
        final ByteBuffer listHeadBuffer = ByteBuffer.allocate(STRUCT_LIST_HEAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        int n = protocol.readBytes(threadListAddress, listHeadBuffer.array(), 0, STRUCT_LIST_HEAD_SIZE);
        assert n == STRUCT_LIST_HEAD_SIZE;
        long threadStructAddress = listHeadBuffer.getLong(NEXT_OFFSET);
        final ByteBuffer threadStructBuffer = ByteBuffer.allocate(STRUCT_THREAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        while (threadStructAddress != threadListAddress) {
            threadStructAddress -= THREAD_LIST_OFFSET;
            n = protocol.readBytes(threadStructAddress, threadStructBuffer.array(), 0, STRUCT_THREAD_SIZE);
            assert n == STRUCT_THREAD_SIZE;
            final int flags = threadStructBuffer.getInt(FLAGS_OFFSET);
            final int id = threadStructBuffer.getShort(ID_OFFSET);
            if ((id == MAXINE_THREAD_ID) || ((flags & UKERNEL_FLAG) == 0)) {
                GUKThreadInfo threadInfo = new GUKThreadInfo(threadStructAddress, id, flags, threadStructBuffer.getInt(CPU_OFFSET));
                // must add before calling protocol.readRegisters as it calls back to find cpu
                threadList.add(threadInfo);
                final ByteBuffer stateRegBuffer = ByteBuffer.wrap(threadInfo.stateRegisters).order(ByteOrder.LITTLE_ENDIAN);
                final ByteBuffer intRegBuffer = ByteBuffer.wrap(threadInfo.integerRegisters).order(ByteOrder.LITTLE_ENDIAN);
                long rip;
                long rsp;
                if ((flags & RUNNING_FLAG) != 0) {
                    // full register set is available
                    protocol.readRegisters(id, threadInfo.integerRegisters, threadInfo.integerRegisters.length,
                            threadInfo.floatingPointRegisters, threadInfo.floatingPointRegisters.length,
                            threadInfo.stateRegisters, threadInfo.stateRegisters.length);
                    rip = stateRegBuffer.getLong(0);
                    rsp = intRegBuffer.getLong(X86_64Registers.IntegerRegister.RSP.getCanonicalIndex());
                } else {
                    // at last reschedule, rip and rsp were saved in thread struct
                    rsp = threadStructBuffer.getLong(SP_OFFSET);
                    rip = threadStructBuffer.getLong(IP_OFFSET);
                }
                threadInfo.rip = rip;
                threadInfo.rsp = rsp;
                intRegBuffer.putLong(X86_64Registers.IntegerRegister.RSP.getCanonicalIndex(), rsp);
                stateRegBuffer.putLong(0, rip);
                threadInfo.regsAvail = true;
            }
            threadStructAddress = threadStructBuffer.getLong(THREAD_LIST_OFFSET);
        }
    }

    /**
     * Gets the cpu the thread is currently running on.
     * @param id
     * @return the cpu the thread is currently running on.
     */
    public int getCpu(int id) {
        return ((GUKThreadInfo) getThreadInfo(id)).cpu;
    }


}
