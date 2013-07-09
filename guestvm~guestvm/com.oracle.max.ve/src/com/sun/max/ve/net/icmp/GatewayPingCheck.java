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
package com.sun.max.ve.net.icmp;

import java.util.*;

import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.ip.*;


/**
* GatewayPingCheck is used to build a list of ICMP echo requests of gateways
* for which we are waiting for replies. This is used for dead gateway detection
* as per the Host Requirements RFC.
*/

public class GatewayPingCheck implements ICMPHandler {

    private static long PING_TIME = 60000L;    // time in milliseconds we wait for an ICMP_ECHOREQ to return.
    private static Map<Integer, GatewayPingCheck> _gatewayPingMap = Collections.synchronizedMap(new HashMap<Integer, GatewayPingCheck>());
    private int _router;                                         // the router being pinged
    private int _ident;                                           // the ident in the ICMP echo message
    private boolean _dead;                                 // true iff no reply before timeout
    private long _time;                                         // time ping sent
    private PingTimerTask _timerTask;
    private static Timer _timer;                          // handles all timeout tasks

    private GatewayPingCheck(int router) {
         if (_timer == null) {
            // not initialized yet
            _timer = new Timer("GatewayPingCheck", true);
        }

        _router = router;
        _dead = false;

        ICMP.registerHandler(router, this);
        sendAndScheduleTimer();
    }

    public static GatewayPingCheck create(int router) {
        return new GatewayPingCheck(router);
    }

    private class PingTimerTask extends TimerTask {
        @Override
        public void run() {
            Route.markRouterDead(_router);

             _dead = true;
             // send out another ping.
             sendAndScheduleTimer();
        }
    }

    private void sendAndScheduleTimer() {
        _ident = ICMP.nextId();
        _time = System.currentTimeMillis();
        _gatewayPingMap.put(_router, this);
        // Start timer for PING_TIME milliseconds. If we don't get a reply in
        // that time we mark the gateway dead.
        _timerTask = new PingTimerTask();
        _timer.schedule(_timerTask, PING_TIME);
        ICMP.rawSendICMPEchoReq(_router, ICMP.defaultTimeout(), ICMP.defaultTTL(), _ident, 0);
    }

    public void handle(Packet pkt, int src_ip, int id, int seq) {
        GatewayPingCheck g = _gatewayPingMap.get(src_ip);
        if (g != null) {
            // Do we care that the id matches? I say not.
            // If the router was marked dead, then see if we can mark
            // it alive again.
            if (g._dead == true) {
                Route.markRouterAlive(g._router);
            }

            // Stop this ping's timer.
            _timerTask.cancel();

            // Remove from the map
            _gatewayPingMap.remove(src_ip);
        }
    }

    public static boolean isInList(int rtr) {
        return _gatewayPingMap.containsKey(rtr);
    }

    public static void dump() {
        Debug.println("Gateway Ping Requests");
        synchronized (_gatewayPingMap) {
            if (_gatewayPingMap.isEmpty())
                Debug.println(" < No Entries >");
            else {
                Iterator<?> iter = _gatewayPingMap.values().iterator();
                while (iter.hasNext()) {
                    GatewayPingCheck g = (GatewayPingCheck) iter.next();
                    Debug.println("       " + IPAddress.toString(g._router)
                            + ", id " + g._ident + ", time " + g._time
                            + ", dead " + g._dead);
                }
            }
        }
    }
}


