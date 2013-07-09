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
package com.sun.max.ve.jdk;

import java.io.IOException;
import sun.nio.ch.*;
import com.sun.max.annotate.*;
import com.sun.max.ve.error.VEError;

/**
 * Substitute methods for sun.nio.ch.PollArrayWrapper.
 * In an ideal world we would provide a MaxVE specific subclass of
 * PollArrayWrapper, similar to EPollArrayWrapper (for Linux epoll)
 * that avoided all the native ugliness. For now however, we stick
 * to the strategy of not changing the JDK at all and substituting the
 * native methods.
 *
 * However, in order to leverage the methods of PollArrayWrapper,
 * which is a package private class, we do delegate to a MaxVE specific class
 * declared in sun.nio.ch so that we can access the fields of the poll structure,
 * which is a struct pollfd from poll.h, using the PollArrayWrapper methods.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "sun.nio.ch.PollArrayWrapper")
public class JDK_sun_nio_ch_PollArrayWrapper {

    @SUBSTITUTE
    private int poll0(long pollAddress, int numfds, long timeout) throws IOException {
        return MaxVENativePollArrayWrapper.poll0(this, pollAddress, numfds, timeout);
    }

    @SUBSTITUTE
    private static void interrupt(int fd) throws IOException {
        MaxVENativePollArrayWrapper.interrupt(fd);
    }

}
