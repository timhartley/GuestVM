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
package com.sun.max.ve.net.guk;

import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.device.*;

public final class NetInit  extends Init implements Runnable {
    private static final String DROP_PROPERTY = "max.ve.net.dropreport";
    private static NetDevice _netDevice;
    private static int _dropReportInterval;
    private NetInit() {
        super(new NetDevice[] {create()});
        final String dropReport = System.getProperty(DROP_PROPERTY);
        if (dropReport != null) {
            _dropReportInterval = Integer.parseInt(dropReport);
            final Thread t = new Thread(this);
            t.setDaemon(true);
            t.start();
        }

    }

    public static NetInit init() {
        return new NetInit();
    }

    public void run() {
        while (true) {
            Debug.println("NetDevice: drop count " + _netDevice.dropCount());
            try {
                Thread.sleep(_dropReportInterval);
            } catch (InterruptedException ex) {
            }
        }
    }

    private static NetDevice create() {
        _netDevice = GUKNetDevice.create();
        return _netDevice;
    }
}
