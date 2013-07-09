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
package com.sun.max.ve.igmp;

import java.util.*;

/**
 * Class representing each active multicast group
 *
 * One of these is created for every group we're a member of
 * (except "all-hosts")
 * When created, we send out an unsolicited membership report to announce
 * to local routers that we exist.
 */

public class GroupEntry extends TimerTask {
    private static final long IGMP_MAX_DELAY = 10000; // 10 seconds

    private static java.util.Random     rand;

    int                 group;          // 28-bit group number
    Timer     timer;          // non-null indicates "delaying" mode
    int                 useCount;       // Keep track of # of clients

    /**
     * Constructor.
     *
     * @param         g        Group number
     */
    public GroupEntry(int g) {
        group = g;
        useCount = 1;
        timer = createTimer();
        rand = new java.util.Random(group);
        startReport();
    }

    private Timer createTimer() {
        return new Timer("IGMP_Report", true);
    }

    /**
     * Called when report timer times out.
     */
    @Override
    public void run() {
        timer.cancel();                   // destroy timer
        IGMP.sendReport(group);
    }

    /**
     * Set up so that we send a membership report some random
     * amount of time in the future (less than 10 seconds)
     * If we can't allocate a timer or random # generator,
     * then use default times.
     */
    void startReport() {
        if (timer != null) {              // In delaying state already, return
            return;
        }

        timer = createTimer();
        timer.schedule(this, rand.nextLong() % IGMP_MAX_DELAY);
    }

    /**
     * Clean up so that timer doesn't fire in the future and do
     * bad things.
     */
    void cancelReport() {               // Called to clean up outstanding
        if (timer != null) {
            // timer.stop();
            timer.cancel();
            timer = null;
        }
    }
}
