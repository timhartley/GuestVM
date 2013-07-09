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
package com.sun.max.ve.sched.std;

import com.sun.max.annotate.*;
import com.sun.max.*;
import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.*;
import com.sun.max.ve.sched.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

import static com.sun.max.ve.guk.GUKTrace.*;


/**
 * The default Java thread scheduler.
 *
 * This scheduler is simple round-robin with respect to the RunQueue implementation
 * that it is paired with. I.e., on a schedule it moves the current thread to the end of the
 * queue and schedules the thread from the head of queue.
 *
 * Each CPU has its own run queue. The currently running Java thread on a CPU, if any,
 * is always at the head of the queue, and cached in the _current array.
 *
 * The upcalls can happen concurrently on any CPU, i.e. no CPU is special as far
 * as scheduling is concerned. The only global operations are selecting an initial CPU
 * for a thread and re-balancing the load. The latter operation may need access to
 * the ready queue for any CPU and so logically requires a global lock, which would
 * imply a global lock for all ready queue operations. To avoid this we only run the
 * balancer on CPU 0.
 *
 * @author Harald Roeck
 * @author Mick Jordan
 */

final class StdScheduler extends GUKUpcallHandler {
    private final RunQueue<GUKVmThread>[] _ready; // ready queue
    private GUKVmThread[] _current; // cache of currently running Java thread
    private final int[] _assignCurrent;
    private final int[] _assignNew;
    private volatile boolean _balanced;

    /**
     * N.B. This is called during image build time, at which point we do not know exactly how many CPUs will be available at run time.
     */
    @HOSTED_ONLY
    StdScheduler() {
        _ready = Utils.cast(new RunQueue[MAX_CPU]);
        _current = new GUKVmThread[MAX_CPU];
        _assignCurrent = new int[MAX_CPU];
        _assignNew = new int[MAX_CPU];
        for (int cpu = 0; cpu < MAX_CPU; cpu++) {
            _ready[cpu] = RunQueueFactory.getInstance().createRunQueue();
            _current[cpu] = null;
            _assignCurrent[cpu] = 0;
            _assignNew[cpu] = 0;
        }
    }

    /**
     * Called at run time when we know how many CPUs we actually have.
     */
    @Override
    public void initialize(MaxineVM.Phase phase) {
        _numCpus = GUKScheduler.numCpus();
        for (int cpu = 0; cpu < _numCpus; cpu++) {
            _ready[cpu].runtimeInitialize();

        }
        super.initialize(phase);
    }

    private static class AnalyzeThreadsProcedure implements Pointer.Procedure {
         /*
          * This class finds GC threads and increases their time slice since there is
          * little point in pre-empting a GC thread.
          */

        private static final String GCTHREAD_TIMESLICE_PROPERTY = "max.ve.gcthread.timeslice";
        private static final int DEFAULT_GCTHREAD_TIMESLICE = 1000;

        public void run(Pointer tla) {
            final VmThread vmThread = VmThread.fromTLA(tla);
            if (vmThread.isGCThread()) {
                int timeSlice = DEFAULT_GCTHREAD_TIMESLICE;
                final String p = System.getProperty(GCTHREAD_TIMESLICE_PROPERTY);
                if (p != null) {
                    timeSlice = Integer.parseInt(p);
                }
                GUKScheduler.setThreadTimeSlice(vmThread, timeSlice);
            }
        }
    }

    private static final AnalyzeThreadsProcedure _analyzeThreadsProcedure = new AnalyzeThreadsProcedure();

    @Override
    public void starting() {
        VmThreadMap.ACTIVE.forAllThreadLocals(null, _analyzeThreadsProcedure);
    }

    private static final byte[] BK = "BK".getBytes();
    private static final byte[] WK = "WK".getBytes();

    @Override
    public void wake(GUKVmThread thread) {
        final int cpu = thread.getCpu();
        final RunQueue<GUKVmThread> ready = _ready[cpu];
        if (GUKTrace.getTraceState(Name.SCHED)) {
            GUKTrace.print1L(WK, thread.nativeId());
        }
        ready.lockedInsert(thread);
    }

    @Override
    public void block(GUKVmThread thread) {
        final int cpu = thread.getCpu();
        final RunQueue<GUKVmThread> ready = _ready[cpu];
        if (GUKTrace.getTraceState(Name.SCHED)) {
            GUKTrace.print1L(BK, thread.nativeId());
        }
        ready.lock();
        ready.remove(thread);
        if (getCurrent(cpu) == thread) {
            setCurrent(null, cpu);
        }
        ready.unlock();
    }

    @Override
    public Word scheduleUpcall(int cpu) {
        Word retval = Word.zero();

        final RunQueue<GUKVmThread> ready = _ready[cpu];
        ready.lock();
        final GUKVmThread current = getCurrent(cpu);

        if (current != null && _balanced && ready.size() == 1) {
            // If current is the only thread runnable on this cpu, then no change
            retval = current.nativeThread();
        } else {
            deschedCurrent(cpu);
            if (cpu == 0 && _numCpus > 1 &&  !_balanced) {
                // Since we look at _balanced without a lock we might miss a state change
                // but we'll get it next time around.
                rebalance(cpu);
            }
            final GUKVmThread head = ready.head();
            if (head != null) {
                head.setRunning(true);
                retval = head.nativeThread();
                setCurrent(head, cpu);
            }
        }
        ready.unlock();
        return retval;
    }

    @Override
    public void descheduleUpcall(int cpu) {
        deschedCurrent(cpu);
        setCurrent(null, cpu);
    }

    private static final byte[] CPU_MISMATCH = "Attach CPU differs from assigned CPU".getBytes();

    @Override
    public void attachUpcall(int id, int cpu, int xcpu) {
        final GUKVmThread sthread = (GUKVmThread) VmThreadMap.ACTIVE.getVmThreadForID(id);
        sassert(cpu == xcpu, CPU_MISMATCH);
        final RunQueue<GUKVmThread> ready = _ready[cpu];
        ready.lock();
        deschedCurrent(cpu);
        sthread.setCpu(cpu);
        sthread.setRunning(true);
        ready.insert(sthread);
        setCurrent(sthread, cpu);
        _balanced = false;
        // _assignCurrent[cpu]++;
        ready.unlock();
    }

    @Override
    public void detachUpcall(int id, int xcpu) {
        final GUKVmThread sthread = getCurrent(xcpu);
        final int cpu = sthread.getCpu();
        sassert(cpu == xcpu, CPU_MISMATCH);
        final RunQueue<GUKVmThread> ready = _ready[cpu];
        ready.lock();
        ready.remove(sthread);
        setCurrent(null, cpu);
        _balanced = false;
        // _assignCurrent[cpu]--;
        ready.unlock();
    }

    @Override
    public void blockUpcall(int id, int xcpu) {
        final GUKVmThread sthread = (GUKVmThread) VmThreadMap.ACTIVE.getVmThreadForID(id);
        final int cpu = sthread.getCpu();
        final RunQueue<GUKVmThread> ready = _ready[cpu];
        ready.lock();
        ready.remove(sthread);
        if (getCurrent(cpu) == sthread) {
            setCurrent(null, cpu);
        }
        _balanced = false;
        // _assignCurrent[cpu]--;
        ready.unlock();
    }

    @Override
    public void wakeUpcall(int id, int xcpu) {
        final GUKVmThread sthread = (GUKVmThread) VmThreadMap.ACTIVE.getVmThreadForID(id);
        final int cpu = sthread.getCpu();
        final RunQueue<GUKVmThread> ready = _ready[cpu];
        ready.lockedInsert(sthread);
        // this does not change current
        _balanced = false;
        // _assignCurrent[cpu]++;
    }

    /**
     * Pick a cpu on which to run a newly created thread.
     * If no sleeping CPU we pick the CPU with the smallest ready queue
     * This is only an approximation since the state could change while we
     * run the algorithm. In any event, since the state at creation may differ
     * from (any) steady state, this can still produce unbalanced loads, which
     * needs to be fixed later in @see rebalance.
     * @return cpu to be used
     */
    @Override
    public int pickCpuUpcall() {
        if (_numCpus == 1) {
            return 0;
        }
        int cpu;
        int scpu = 0;
        int minSize = Integer.MAX_VALUE;
        for (cpu = 0; cpu < _numCpus; cpu++) {
            if (GUKScheduler.cpuState(cpu) == CpuState.SLEEPING.ordinal()) {
                return cpu;
            }
            final RunQueue<GUKVmThread> ready = _ready[cpu];
            final int size = ready.lockedSize();
            if (size < minSize) {
                minSize = size;
                scpu = cpu;
            }
        }
        return scpu;
    }

    /**
     * Any runnable threads for given cpu?
     * @param cpu
     * @return 1 if runnable threads, 0 otherwise
     */
    @Override
    public int runnableUpcall(int cpu) {
        final RunQueue<GUKVmThread> ready = _ready[cpu];
        final int result = ready.lockedEmpty() ? 0 : 1;
        return result;
    }

    /*
     * Support methods
     */

    @INLINE
    private static void sassert(boolean condition, byte[] msg) {
        if (!condition) {
            GUK.crash(msg);
        }
    }

    /**
     * Return the Java thread that is currently executing on the given cpu.
     *
     * @param cpu
     * @return
     */
    @INLINE
    private GUKVmThread getCurrent(int cpu) {
        /* N.B. _current[cpu] is not always the same as VmThread.current() in particular
         * after a block. When the thread that called block then calls schedule,
         * following the standard ukernel control flow, VmThread.current would
         * return the caller (instead of null or some other runnable thread) and then schedule
         * could return it unless it also checked whether is was runnable.
         * I.e. using VmThread.current, although slightly more efficient, would violate
         * the invariant that this method always returns a runnable Java thread.
         * Another special case is when schedule is upcalled when a ukernel thread
         * is current. In that case VmThread.current  returns null, which is ok.
         */
        return _current[cpu];
    }

    @INLINE
    private void setCurrent(GUKVmThread thread, int cpu) {
        _current[cpu] = thread;
    }

    /**
     * Deschedule the currently running thread on given cpu
     * and move it to the end of the run queue.
     * Caller must hold lock.
     *
     * @param cpu
     */
    @INLINE
    private void deschedCurrent(int cpu) {
        final GUKVmThread current = getCurrent(cpu);
        if (current != null) {
            current.setRunning(false);
            _ready[cpu].moveHeadToEnd();
        }
    }


    /*
     * Thread re-balancing between CPUs
     */
    private static final byte[] JMR = "JMR".getBytes();
    private static final byte[] JMA = "JMA".getBytes();
    private static final byte[] JMB = "JMB".getBytes();
    private static final byte[] JMM = "JMM".getBytes();

    /**
     * Rebalance assignment of threads to CPUs.
     * Caller holds the lock on _ready[xcpu]
     *
     * @param xcpu cpu on which rebalancing is being performed
     */
    private void rebalance(int xcpu) {
        if (!_balanced) {
            // We may spin briefly if another CPU is in the scheduler
            lockAll(xcpu);
            int numActiveThreads = 0;
            int cpu;
            // Could maintain _assignCurrent on the fly
            for (cpu = 0; cpu < _numCpus; cpu++) {
                final int size = _ready[cpu].size();
                _assignCurrent[cpu] = size;
                numActiveThreads += size;
            }
            int rem = 0;
            final int ideal = numActiveThreads / _numCpus;
            final int x = ideal * _numCpus;
            if (x != numActiveThreads) {
                rem = numActiveThreads - x;
            }
            for (cpu = 0; cpu < _numCpus; cpu++) {
                _assignNew[cpu] = ideal;
            }
            if (GUKTrace.getTraceState(Name.SCHED)) {
                GUKTrace.print5L(JMR, numActiveThreads, _assignCurrent[0], _assignCurrent[1], _assignCurrent[2], _assignCurrent[3]);
                GUKTrace.print4L(JMA, _assignNew[0], _assignNew[1], _assignNew[2], _assignNew[3]);
            }
            // rem cpus have to have one more than ideal.
            // If any are in that state already, then that helps
            // and it would be counterproductive to reduce their
            // load as it would cause needless switching
            int scpu = 0;
            while (rem > 0) {
                cpu = findCpuWithAtLeastN(ideal + 1, scpu);
                if (cpu >= 0) {
                    _assignNew[cpu]++;
                    rem--;
                    scpu = cpu + 1;
                } else {
                    break;
                }
            }
            if (rem > 0) {
                cpu = 0;
                while (rem > 0) {
                    _assignNew[cpu]++;
                    cpu++;
                    rem--;
                }
            }
            if (GUKTrace.getTraceState(Name.SCHED)) {
                GUKTrace.print4L(JMB, _assignNew[0], _assignNew[1], _assignNew[2], _assignNew[3]);
            }
            // now migrate threads to meet new assignment
            for (cpu = 0; cpu < _numCpus; cpu++) {
                while (_assignCurrent[cpu] > _assignNew[cpu]) {
                    final int cpu2 = findMigratee();
                    migrate(cpu, cpu2, xcpu);
                    _assignCurrent[cpu]--;
                }
            }
            unlockAll(xcpu);
            _balanced = true;
        }
    }

    /**
     * Find a cpu whose current assignment is at least n.
     * Start looking at scpu.
     * @param n load to find
     * @param scpu cpu to start search
     */
    private int findCpuWithAtLeastN(int n, int scpu) {
        int cpu = 0;
        for (cpu = scpu; cpu < _numCpus; cpu++) {
            if (_assignCurrent[cpu] >= n) {
                return cpu;
            }
        }
        return -1;
    }

    /**
     * Find a cpu whose current assignment is below new assignment.
     */
    private int findMigratee() {
        for (int cpu = 0; cpu < _numCpus; cpu++) {
            if (_assignCurrent[cpu] < _assignNew[cpu]) {
                return cpu;
            }
        }
        return -1;
    }

    /**
     * Migrate a thread from cpu1's queue to cpu2's queue.
     * However, do not migrate the current thread!
     * Caller holds lock all queues
     * @param cpu1
     * @param cpu2
     * @param xcpu
     */
    private void migrate(int cpu1, int cpu2, int xcpu) {
        final RunQueue<GUKVmThread> ready1 = _ready[cpu1];
        final GUKVmThread t1 = ready1.next();
        sassert(t1 != null, MIGRATE_ERROR);
        ready1.remove(t1);
        t1.setCpu(cpu2);
        GUKScheduler.kickCpu(cpu2); // in case it is sleeping
        final RunQueue<GUKVmThread> ready2 = _ready[cpu2];
        ready2.insert(t1);
        if (GUKTrace.getTraceState(Name.SCHED)) {
            GUKTrace.print3L(JMM, t1.nativeId(), cpu1, cpu2);
        }
    }
    private static final byte[] MIGRATE_ERROR = "migratee is null".getBytes();

    /**
     * Lock all the ready queues except xcpu (already locked).
     * I.e. take the global lock
     * @param xcpu already locked cpu
     */
    private void lockAll(int xcpu) {
        for (int cpu = 0; cpu < _numCpus; cpu++) {
            if (cpu != xcpu) {
                _ready[cpu].lock();
            }
        }
    }

    /**
     * Unock all the ready queues except xcpu (already locked).
     * I.e. remove the global lock
     * @param xcpu already locked cpu
     */
    private void unlockAll(int xcpu) {
        for (int cpu = 0; cpu < _numCpus; cpu++) {
            if (cpu != xcpu) {
                _ready[cpu].unlock();
            }
        }
    }


}
