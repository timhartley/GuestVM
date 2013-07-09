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
package com.sun.max.ve.util;

import com.sun.max.ve.fs.ErrorDecoder;
import com.sun.max.vm.Log;

/**
 * A class that handles timeouts while running a given procedure.
 *
 * @author Mick Jordan
 *
 */

public abstract class TimeLimitedProc {
    private boolean _return;

    /**
     * This is the method that does the work.
     * It must set _return = true to terminate the run method.
     * @param remaining the amount of time left (or 0 if infinite)
     * @return result of the method
     * @throws InterruptedException
     */
    protected abstract int proc(long remaining) throws InterruptedException;

    /**
     * Run proc until timeout expires.
     * @param timeout if < 0 implies infinite, otherwise given value
     * @return
     */
    public int run(long timeout) {
        final long start = System.currentTimeMillis();
        long remaining = timeout < 0 ? 0 : timeout;
        _return = false;
        while (true) {
            try {
                final int result = proc(remaining);
                if (_return) {
                    log("run " + this + " returning " + result);
                    return result;
                }
                // timeout expired?
                if (timeout > 0) {
                    final long now = System.currentTimeMillis();
                    if (now - start >= timeout) {
                        log("run " + this + " timed out");
                        return 0;
                    }
                    remaining -= now - start;
                }
            } catch (InterruptedException ex) {
                log("run " + this + " interrupted");
                return -ErrorDecoder.Code.EINTR.getCode();
            }
        }
    }

    /**
     * The way to terminate the run method.
     * @param result
     * @return
     */
    public int terminate(int result) {
        _return = true;
        return result;
    }

    static boolean _init;
    static boolean _log;
    static void log(String s) {
        if (!_init) {
            _log = System.getProperty("max.ve.util.tlp.debug") != null;
            _init = true;
        }
        if (_log) {
            Log.print(Thread.currentThread()); Log.print(' '); Log.println(s);
        }
    }
}

