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
package com.sun.max.ve.profiler;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.unsafe.*;

/**
 * Tick profiler. Runs a thread that periodically wakes up, stops all the threads, and records their stack.
 * There is a singleton instance per VM and most state is static. Note that the stack is gathered regardless
 * of the state of the thread, e.g., it may be blocked.
 *
 * Attempts to allocate minimal heap memory to limit interfering with the application.
 *
 * A shutdown hook is registered which outputs the data at VM shutdown, but it can also be dumped
 * periodically to the standard output.
 *
 * @author Mick Jordan
 *
 */
public final class Tick extends Thread {

    private static final int DEFAULT_FREQUENCY = 50;
    private static final int DEFAULT_DEPTH = 4;
    private static final Random rand = new Random();
    /**
     * The base period in milliseconds between activations of the profiler.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static int _period;

    /**
     * To avoid strobe effects the profiler activations are randomized around {@link _period}.
     * The actual activation period is in the range {@code _period - _jiggle <-> -period + _jiggle}.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static int _jiggle;

    /**
     * The maximum stack depth the profiler will ever gather.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static int _maxDepth;

    /**
     * Used as a scratch object for the working stack being analyzed, to avoid excessive heap allocation.
     * Stacks are analyzed serially and the (working) stack being analyzed is built up in this object,
     * which is reset prior to the analysis. The map lookup uses this object and only if the stack has not
     * been seen before is a new {@link StackInfo} object allocated and the contents copied in.
     * Therefore, once an application reaches a steady-state, no further allocation should occur.
     *
     */
    private static StackInfo _workingStackInfo;
    /**
     * Records the depth for the working stack being analyzed.
     */

    private static int _workingDepth;

    /**
     * Allows profiling to be turned off temporarily.
     */
    private static volatile boolean _profiling;

    /**
     * Count of how many times the profiler has actived.
     */
    private static int _profileCount;

    /**
     * Period in milliseconds between dumping the traces to the standard output.
     */
    private static long _dumpPeriod;

    /**
     * The profiler thread itself.
     */
    private static VmThread _profiler;

    /**
     * Create a tick profiler with given measurement period, stack depth and dump period.
     * @param period base period for measurements in millisecs, 0 implies {@value DEFAULT_FREQUENCY}
     * @param depth stack depth to record, 0 implies {@value DEFAULT_DEPTH}, < 0 implies no limit
     * @param dumpPeriod time in seconds between dumps to standard output, 0 implies never (default)
     */
    public static void create(int period, int depth, int dumpPeriod) {
        _period = period == 0 ? DEFAULT_FREQUENCY : period;
        _jiggle = _period / 8;
        _maxDepth = depth == 0 ? DEFAULT_DEPTH : (depth < 0 ? Integer.MAX_VALUE : depth);
        _dumpPeriod = dumpPeriod * 1000000000L;
        _workingStackInfo = new StackInfo(_maxDepth);
        Runtime.getRuntime().addShutdownHook(new InfoOutput());
        final Thread profileThread = new Tick();
        profileThread.setName("Tick-Profiler");
        profileThread.setDaemon(true);
        _profiling = true;
        profileThread.start();
    }

    @Override
    public void run() {
        _profiler = VmThread.fromJava(this);
        long lastDump = System.nanoTime();
        while (true) {
            try {
                final int thisJiggle = rand.nextInt(_jiggle);
                final int thisPeriod = _period + (rand.nextBoolean() ? thisJiggle : -thisJiggle);
                Thread.sleep(thisPeriod);
                final long now = System.nanoTime();
                if (_profiling) {
                    stackTraceGatherer.submit();
                    if (_dumpPeriod > 0 && now > lastDump + _dumpPeriod) {
                        InfoOutput.dumpTraces();
                        lastDump = now;
                    }
                    _profileCount++;
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Encapsulates the basic logic of handling one thread after all threads are frozen.
     */
    private static final class StackTraceGatherer extends VmOperation {
        StackTraceGatherer() {
            super("Tick Profiler", null, Mode.Safepoint);
        }

        @Override
        protected boolean operateOnThread(VmThread thread) {
            return thread != _profiler;
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            TickStackTraceVisitor stv = new TickStackTraceVisitor();
            final VmStackFrameWalker stackFrameWalker = vmThread.stackDumpStackFrameWalker();
            _workingStackInfo.reset();
            _workingDepth = 0;
            stv.walk(stackFrameWalker, ip, sp, fp);
            // Have we seen this stack before?
            List<ThreadInfo> threadInfoList = stackInfoMap.get(_workingStackInfo);
            if (threadInfoList == null) {
                threadInfoList = new ArrayList<ThreadInfo>();
                final StackInfo copy = _workingStackInfo.copy(_workingDepth);
                stackInfoMap.put(copy, threadInfoList);
            }
            // Check if this thread had this stack trace before, allocating a new {@link ThreadInfo} instance if not
            final ThreadInfo threadInfo = getThreadInfo(threadInfoList, vmThread);
            // bump the number of times the given threads has been in this state
            threadInfo.count++;
        }
    }

    private static final StackTraceGatherer stackTraceGatherer = new StackTraceGatherer();

    /**
     * Allocation free stack frame analyzer that builds up the {@StackInfo} in {@link Tick#_workingStackInfo}.
     */
    private static class TickStackTraceVisitor extends StackTraceVisitor {

        TickStackTraceVisitor() {
            super(null, _maxDepth);
        }
        @Override
        public boolean add(ClassMethodActor classMethodActor, int sourceLineNumber) {
            _workingStackInfo.stack[_workingDepth].classMethodActor = classMethodActor;
            _workingStackInfo.stack[_workingDepth].lineNumber = sourceLineNumber;
            _workingDepth++;
            return _workingDepth < _maxDepth;
        }

        @Override
        public void clear() {
            _workingDepth = 0;
        }

        @Override
        public StackTraceElement[] getTrace() {
            return null;
        }
    }

    /**
     * For each unique stack trace, we record the list of threads with that trace.
     */
    private static Map<StackInfo, List<ThreadInfo>> stackInfoMap = new HashMap<StackInfo, List<ThreadInfo>>();

    private static ThreadInfo getThreadInfo(List<ThreadInfo> threadInfoList, VmThread vmThread) {
        for (ThreadInfo  threadInfo : threadInfoList) {
            if (threadInfo.vmThread == vmThread) {
                return threadInfo;
            }
        }
        final ThreadInfo  threadInfo = new ThreadInfo(vmThread);
        threadInfoList.add(threadInfo);
        return threadInfo;
    }

    /**
     * Value class that records a thread and a hit count.
     */
    static class ThreadInfo {
        long count;
        VmThread vmThread;
        ThreadInfo(VmThread vmThread) {
            this.vmThread = vmThread;
        }
    }

    /**
     * Value class that captures the essential information on a stack frame element.
     */
    static class StackElement {
        ClassMethodActor classMethodActor;
        int lineNumber;  // < 0 if unknown
    }

    /**
     * The essential information on a sequence of frames, with support for comparison and hashing.
     */
    static class StackInfo {
        StackElement[] stack;

        StackInfo(int depth) {
            stack = new StackElement[depth];
            for (int i = 0; i < depth; i++) {
                stack[i] = new StackElement();
            }
        }

        void reset() {
            for (int i = 0; i < stack.length; i++) {
                stack[i].classMethodActor = null;
                stack[i].lineNumber = -1;
            }
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (StackElement s : stack) {
                if (s.classMethodActor == null) {
                    break;
                } else {
                    result ^= s.lineNumber ^ s.classMethodActor.hashCode();
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            // array lengths may be different, but can still compare equal
            final StackInfo osi = (StackInfo) o;
            int min = stack.length;
            StackInfo longer = null;
            if (stack.length < osi.stack.length) {
                longer = osi;
            } else if (stack.length > osi.stack.length) {
                min = osi.stack.length;
                longer = this;
            }
            // compare up to min
            for (int i = 0; i < min; i++) {
                if ((osi.stack[i].classMethodActor != stack[i].classMethodActor) ||
                                osi.stack[i].lineNumber != stack[i].lineNumber) {
                    return false;
                }
            }
            // same length or if longer is empty after min
            return  (longer == null) || longer.stack[min].classMethodActor == null;
        }

        /**
         * Shallow copy of this object, truncating length.
         * @return copied object
         */
        StackInfo copy(int depth) {
            final StackInfo result = new StackInfo(depth);
            for (int i = 0; i < result.stack.length; i++) {
                result.stack[i].classMethodActor = this.stack[i].classMethodActor;
                result.stack[i].lineNumber = this.stack[i].lineNumber;
            }
            return result;
        }
    }

    /**
     * Shutdown hook thread that outputs the data.
     */
    static class InfoOutput extends Thread {
        @Override
        public void run() {
            _profiling = false;
            dumpTraces();
        }

        static void dumpTraces() {
            System.out.println("Tick Profiler Stack Traces (profiler activations: " + _profileCount + ")\n");
            for (Map.Entry<StackInfo, List<ThreadInfo>> entry : stackInfoMap.entrySet()) {
                final StackInfo stackInfo = entry.getKey();
                final List<ThreadInfo> threadInfoList = entry.getValue();
                for (StackElement se : stackInfo.stack) {
                    if (se.classMethodActor != null) {
                        final ClassActor holder = se.classMethodActor.holder();
                        System.out.println(holder.name.toString() + "." + se.classMethodActor.name().toString() +
                                        "(" + (se.lineNumber > 0 ? holder.sourceFileName + ": " + se.lineNumber : "Native Method") + ")");
                    }
                }
                for (ThreadInfo ti : threadInfoList) {
                    final Thread t = ti.vmThread.javaThread();
                    System.out.println("  Thead id=" + t.getId() + ", name=\"" + t.getName() + "\" count: " + ti.count);
                }
                System.out.println();
            }
        }
    }
}
