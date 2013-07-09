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
package test.com.sun.max.ve.net;

/**
 * A test to demonstrate the impact of slow response network packet handlers.
 * Usage: [d runtime] [s sleeptime] [r] [c] [t]
 * The test, which runs for "runtime" seconds (default 30), registers a network packet handler which, by default, sleeps for
 * "sleeptime" milliseconds (default 5000) before returning.  This scenario will typically result in dropped packets by
 * the network driver as not enough handlers will be available for all incoming packets.
 * If r is set, the sleep is for a random time up to sleeptime.
 * If c is set a compute-bound thread is run in parallel, which can be used to see the handler thread schedule latency.
 * If t is set, scheduler tracing is turned on (also requires -XX:GUKTrace to be set on guest launch).
 *
 * The main thread reports the dropped packet count every second.
 *
 * @author Mick Jordan
 */

import java.util.*;

import com.sun.max.ve.guk.GUKTrace;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.device.*;
import com.sun.max.ve.net.guk.*;

public class SlowNetDeviceHandlerTest implements NetDevice.Handler, Runnable {
    static int _sleepTime = 5000;
    static long _startTime;
    static Object _lock = new Object();
    static boolean _waiting = true;
    static boolean _randomSleep = false;
    static Random _random = new Random();
    static long _endTime;
    static boolean _done;
    private static final long SEC_TO_NANO = 1000 * 1000 * 1000;

    public static void main(String[] args) throws InterruptedException {
        boolean compute = false;
        boolean trace = false;
        long duration  = 30;
        // Checkstyle: stop modified control variable check
       for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("s")) {
                _sleepTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("r")) {
                _randomSleep = true;
            } else if (arg.equals("c")) {
                compute = true;
            } else if (arg.equals("t")) {
                trace = true;
            } else if (arg.equals("d")) {
                duration = Integer.parseInt(args[++i]);
            }
        }
       // Checkstyle: resume modified control variable check

        if (trace) {
            GUKTrace.setTraceState(GUKTrace.Name.SCHED, true);
        }
        _startTime = System.nanoTime();
        _endTime = _startTime + duration * SEC_TO_NANO;
        System.out.println("starting: handler sleep time: " + _sleepTime);
        final NetDevice device = GUKNetDevice.create();
        final SlowNetDeviceHandlerTest self = new SlowNetDeviceHandlerTest();
        device.registerHandler(self);

        if (compute) {
            final Thread spinner = new Thread(new SlowNetDeviceHandlerTest());
            spinner.setName("Spinner");
            spinner.setDaemon(true);
            spinner.start();
        }

        while (!_done) {
            Thread.sleep(1000);
            tprintln("drop counter: " + device.dropCount());
        }
    }

    static long relTime() {
        final long now = System.nanoTime();
        if (now >= _endTime) {
            _done = true;
        }
        return now - _startTime;
    }

    public void handle(Packet packet) {
        final int sleepTime = _randomSleep ? _random.nextInt(_sleepTime) : _sleepTime;
        tprintln("handler entry: ttp: " + (System.nanoTime() - packet.getTimeStamp()) + " : len: " + packet.length() + " sleep: " + sleepTime);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
            tprintln("handler interrupted");
        }
        tprintln("handler exit");
    }

    public void run() {
        long x = 0;
        while (true && !_done) {
            x++;
        }
    }

    static void tprintln(String msg) {
        System.out.println(Thread.currentThread().getName() + ": @" + relTime() + ": " + msg);
    }

}
