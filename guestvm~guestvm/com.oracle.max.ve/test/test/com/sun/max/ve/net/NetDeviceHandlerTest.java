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
 * A simple test for handler registration with the network device driver.
 * Usage: [s n]
 * Reports packet delivery and number of bytes received.
 * The main thread sleeps for n seconds (default 5), reporting
 * the number of dropped packets.
 *
 * @author Mick Jordan
 */
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.device.*;
import com.sun.max.ve.net.guk.*;

public class NetDeviceHandlerTest  {
    private static int _sleepTime = 5000;
    public static void main(String[] args) throws InterruptedException {
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("s")) {
                _sleepTime = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check
        new NetDeviceHandlerTest().run();
    }

    public void run() throws InterruptedException {
        final NetDevice nd = GUKNetDevice.create();
        nd.registerHandler(new Handler());
        while (true) {
            Thread.sleep(_sleepTime);
            System.out.println("drop count " + nd.dropCount());
        }
    }

    static class Handler implements NetDevice.Handler {
        public void handle(Packet packet) {
            System.out.println("" + packet.length() + " bytes received");
        }
    }
}
