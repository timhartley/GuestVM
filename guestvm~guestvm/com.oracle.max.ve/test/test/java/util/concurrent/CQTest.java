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
package test.java.util.concurrent;

import java.util.*;
import com.sun.max.*;

public class CQTest {

    static MyQueue<Long> _q;
    static boolean _running = true;
    static Long[] _elements;
    private static final int ELEMENTS_SIZE = 1024;
    static int _delay = 0;
    static boolean _verbose = false;

    public static void main(String[] args) {
        int producerCount = 1;
        int consumerCount = 1;
        int runTime = 5;
        boolean needSync = false;

        String qImpl = "java.util.LinkedList";
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("p")) {
                producerCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("c")) {
                consumerCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("t")) {
                runTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("q")) {
                qImpl = args[++i];
            } else if (arg.equals("s")) {
                needSync = true;
            } else if (arg.equals("d")) {
                _delay = Integer.parseInt(args[++i]);
            } else if (arg.equals("v")) {
                _verbose = true;
            }
        }
        // Checkstyle: resume modified control variable check

        _elements = new Long[ELEMENTS_SIZE];
        for (int i = 0; i < ELEMENTS_SIZE; i++) {
            _elements[i] = new Long(i);
        }

        System.out.println("Instantiating " + qImpl);
        Queue<Long> queue = null;
        try {
            final Class< ? > qClass = Class.forName(qImpl);
            queue = Utils.cast(qClass.newInstance());
        } catch (Exception ex) {
            System.out.println("failed to instantiate " + qImpl);
        }

        if (needSync) {
            _q = synchronizedQueue(queue);
        } else {
            _q = unSynchronizedQueue(queue);
        }

        final Consumer[] consumers = new Consumer[consumerCount];
        final Producer[] producers = new Producer[producerCount];

        System.out.println("Running for " + runTime + " seconds");
        for (int p = 0; p < producerCount; p++) {
            producers[p] = new Producer();
            producers[p].setName("Producer[" + p + "]");
            producers[p].start();
        }
        for (int c = 0; c < consumerCount; c++) {
            consumers[c] = new Consumer();
            consumers[c].setName("Consumer[" + c + "]");
            consumers[c].start();
        }
        try {
            for (int i = 0; i < runTime; i++) {
                Thread.sleep(1000);
                if (_verbose) {
                    for (Producer producer : producers) {
                        System.out.println(producer.getName() + ", count: " + producer.count());
                    }
                    for (Consumer consumer : consumers) {
                        System.out.println(consumer.getName() + ", polls: " + consumer.polls() + ", count: " + consumer.count());
                    }
                }
            }
            _running = false;
            for (Producer producer : producers) {
                producer.join();
                System.out.println(producer.getName() + ", count: " + producer.count());
            }
            for (Consumer consumer : consumers) {
                consumer.join();
                System.out.println(consumer.getName() + ", polls: " + consumer.polls() + ", count: " + consumer.count());
            }
        } catch (InterruptedException ex) {
        }

    }

    static MyQueue<Long> synchronizedQueue(Queue<Long> q) {
        return new SynchronizedQueue<Long>(q);
    }

    static MyQueue<Long> unSynchronizedQueue(Queue<Long> q) {
        return new UnSynchronizedQueue<Long>(q);
    }

    interface MyQueue<E> {
        void add(E elem);
        E poll();
    }

    static class SynchronizedQueue<E> implements MyQueue<E> {
        private Queue<E> _queue;
        SynchronizedQueue(Queue<E> q) {
            _queue = q;
        }

        public synchronized void add(E elem) {
            _queue.add(elem);
        }

        public synchronized E poll() {
            return _queue.poll();
        }
    }

    static class UnSynchronizedQueue<E> implements MyQueue<E> {
        private Queue<E> _queue;
        UnSynchronizedQueue(Queue<E> q) {
            _queue = q;
        }

        public void add(E elem) {
            _queue.add(elem);
        }

        public E poll() {
            return _queue.poll();
        }
    }

    static class Consumer extends Thread {
        long _p;
        long _c;
        @Override
        public void run() {
            while (_running) {
                _p++;
                final Long l = _q.poll();
                if (l != null) {
                    _c++;
                }
            }
        }

        long polls() {
            return _p;
        }

        long count() {
            return _c;
        }
    }

    static class Producer extends Thread {
        long _c = 0;
        @Override
        public void run() {
            while (_running) {
                _q.add(_elements[(int) _c % ELEMENTS_SIZE]);
                _c++;
                if (_delay > 0) {
                    try {
                        Thread.sleep(_delay);
                    } catch (InterruptedException ex) {

                    }
                }
            }
        }

        long count() {
            return _c;
        }
    }

}
