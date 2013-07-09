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
package com.sun.max.ve.net.tcp;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author Puneeet Lakhina
 * @author Mick Jordan
 *
 */
public class ThreadPoolScheduledTimer extends ScheduledThreadPoolExecutor {

    static final int DEFAULT_THREAD_POOL_SIZE = 10;

    private String _name;
    private Runnable _command;
    private ScheduledFuture<?> _future;

    ThreadPoolScheduledTimer(String name) {
        this(name, DEFAULT_THREAD_POOL_SIZE);
    }
    
    ThreadPoolScheduledTimer(String name, int corePoolSize) {
        super(corePoolSize, new DaemonThreadFactory(name));
        _name = name;
    }
    
    synchronized void scheduleTask(TCP.TCPTimerTask task, long delay) {
        if (task != null) {
            if (_future == null) {
                schedule(task, delay);
            }
        }
    }

    public void schedule(Runnable command, long delay) {
            _future = schedule(command, delay, TimeUnit.MILLISECONDS);
            _command = command;
            if (TCP._debug){ 
                TCP.sdprint("scheduled " + command + " on " + this._name + " with delay " + delay);
            }
    }

    String getName() {
        return _name;
    }

    synchronized void cancelTask() {
        if (_future != null) {
            if (TCP._debug){ 
                TCP.sdprint("cancelling " + _command + " done: " + _future.isDone() + " cancelled: " + _future.isCancelled());
            }
            _future.cancel(false);
            _future = null;
        }
    }

}
