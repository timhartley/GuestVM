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
package test.com.sun.max.ve.sched;

import java.util.*;
import com.sun.max.ve.sched.*;
import com.sun.max.ve.sched.priority.*;

/**
 * This tests the RingRunQueue/PriorityRingRunQueue implementations used by the VM thread scheduler.
 * Args:
 * b        basic functionality test
 * m       multi-thread random mutation test
 * r n      run m test for n seconds
 * t n      use n threads in m test (default 2)
 * p       test PriorityRunRunQueue (default RingRunQueue)
 *
 * @author Mick Jordan
 *
 */

public class QueueTest {

    private static boolean _done;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        boolean doBasicTest = false;
        boolean doMultiTest = false;
        int numThreads = 2;
        int multiRunTime = 5;
        boolean priority = false;

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("b")) {
                doBasicTest = true;
            } else if (arg.equals("m")) {
                doMultiTest = true;
            } else if (arg.equals("t")) {
                numThreads = Integer.parseInt(args[++i]);
            } else if (arg.equals("r")) {
                multiRunTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("p")) {
                priority = true;
            }
        }
        // Checkstyle: resume modified control variable check
        if (doBasicTest) {
            basicTest(priority ? new MyPQueue<ListStringEntry>(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY) : new MyQueue<ListStringEntry>());
        }
        if (doMultiTest) {
            multiThreadTest(numThreads, multiRunTime, priority ? new MyPQueue<ListStringEntry>(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY) : new MyQueue<ListStringEntry>());
        }
    }

    private static void basicTest(RunQueue<ListStringEntry> q) {
        listQueue(q);
        final ListStringEntry one = new ListStringEntry("thread1", 5);
        q.lockedInsert(one);
        listQueue(q);
        q.lockedMoveHeadToEnd();
        listQueue(q);
        final ListStringEntry two = new ListStringEntry("thread2", 5);
        q.lockedInsert(two);
        listQueue(q);
        final ListStringEntry three = new ListStringEntry("thread3", 3);
        q.lockedInsert(three);
        listQueue(q);
        q.lockedMoveHeadToEnd();
        listQueue(q);
        q.lockedRemove(two);
        listQueue(q);
        q.lockedRemove(one);
        listQueue(q);
        q.lockedRemove(three);
        listQueue(q);
    }

    private static void multiThreadTest(int numThreads, int duration, RunQueue<ListStringEntry> q) throws InterruptedException {
        new Timer(true).schedule(new MyTimerTask(), duration * 1000);
        final MultiTester[] threads = new MultiTester[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new MultiTester(q);
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
            System.out.println("Operations for thread " + i + ": " + threads[i].getOps());
        }
    }

    static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            _done = true;
        }
    }

    static class MultiTester extends Thread {
        RunQueue<ListStringEntry> _q;
        Random _rand = new Random(36647);
        long _ops;

        MultiTester(RunQueue<ListStringEntry> q) {
            _q = q;
        }

        long getOps() {
            return _ops;
        }

        @Override
        public void run() {
            while (!_done) {
                final int c = _rand.nextInt(5);
                _ops++;
                try {
                    switch (c) {
                        case 0:
                            _q.lockedInsert(new ListStringEntry("thread" + _ops, priority()));
                            break;
                        case 1:
                            try {
                                _q.lock();
                                final int s = _q.size();
                                if (s > 0) {
                                    int x = _rand.nextInt(s);
                                    ListStringEntry target = null;
                                    for (ListStringEntry e : _q) {
                                        if (x == 0) {
                                            target = e;
                                            break;
                                        }
                                        x--;
                                    }
                                    if (target == null) {
                                        throw new NullPointerException("iterator terminated early");
                                    }
                                    _q.remove(target);
                                }
                            } finally {
                                _q.unlock();
                            }
                            break;
                        case 2:
                            _q.lockedMoveHeadToEnd();
                            break;
                        case 3:
                            _q.lockedSize();
                            break;
                        case 4:
                            _q.lockedEmpty();
                            break;
                        default:
                    }
                } catch (Exception ex) {
                    error(ex, c);
                }
            }
        }

        private void error(Exception ex, int c) {
            System.out.println("Error on operation: " + c);
            ex.printStackTrace();
        }

        private int priority() {
            return _rand.nextInt(Thread.MAX_PRIORITY) + 1;
        }

    }

    private static <T> void listQueue(RunQueue<T> q) {
        System.out.print("Queue contents: ");
        for (T e : q) {
            System.out.print(e + " ");
        }
        System.out.println();
    }

    public static class MyQueue<T extends RingRunQueueEntry> extends RingRunQueue<T> {

        MyQueue() {
            buildtimeInitialize();
            runtimeInitialize();
        }
    }

    static class MyLevelRingQueueCreator<T extends RingRunQueueEntry> implements PriorityRingRunQueue.LevelRingQueueCreator {
        public RingRunQueue<T> create() {
            return new MyQueue<T>();
        }
    }

    static class MyPQueue<T extends PriorityRingRunQueueEntry> extends PriorityRingRunQueue<T>  {

        MyPQueue(int min, int max) {
            super(min, max, new MyLevelRingQueueCreator());
            buildtimeInitialize();
            runtimeInitialize();
        }
    }

    static class StringEntry {
        String _id;
        int _priority;
        StringEntry(String id, int priority) {
            _priority = priority;
            _id = id;
        }

        @Override
        public String toString() {
            return _id + "(" + Integer.toString(_priority) + ")";
        }

        protected int compareTo(StringEntry e) {
            return _priority - e._priority;
        }
    }

    static class ListStringEntry extends StringEntry implements PriorityRingRunQueueEntry {
        private RingRunQueueEntry _next;
        private RingRunQueueEntry _prev;

        public ListStringEntry(String id, int value) {
            super(id, value);
            _next = this;
            _prev = this;
        }

        @Override
        public RingRunQueueEntry getNext() {
            return _next;
        }

        @Override
        public RingRunQueueEntry getPrev() {
            return _prev;
        }

        @Override
        public void setNext(RingRunQueueEntry element) {
            this._next = element;
        }

        @Override
        public void setPrev(RingRunQueueEntry element) {
            this._prev = element;
        }

        public int compareTo(PriorityRingRunQueueEntry e) {
            final StringEntry ie = (StringEntry) e;
            return super.compareTo(ie);
        }

        public int getPriority() {
            return _priority;
        }

    }


}
