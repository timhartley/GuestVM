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
package sun.nio.ch;

import com.sun.max.config.BootImagePackage;
import com.sun.max.vm.hosted.*;

/**
 * NOTE: The handling of {@code sun.nio.ch} is convoluted but necessary owing to its curious structure and our desired
 * to change its native API.
 * 
 * There are two issues with {@code sun.nio.ch}. The first is that we need to change the {@link NativeDispatcher}
 * interface to use {@link ByteBuffer}. This requires the use of SUBSTITUTE but it also requires that the substitution
 * class be "in" sun.nio.ch because {@link NativeDispatcher} is not public.  The second is that package contains
 * implementations for multiple operating systems, that use native code, that obviously cannot all load on any
 * given system. To handle 1 we make {@code sun.nio.ch} an {@link ExtPackage}, even though it is part of the JDK
 * and to handle 2, we explicitly load just the classes we need.
 * 
 * @author Mick Jordan
 */

public class Package extends BootImagePackage {
    private static final String[] classes = {
        "sun.nio.ch.IOUtil", "sun.nio.ch.Util", "sun.nio.ch.FileKey", "sun.nio.ch.IOStatus", "sun.nio.ch.BBNativeDispatcher", 
        "sun.nio.ch.DatagramChannelImpl", "sun.nio.ch.ServerSocketChannelImpl", "sun.nio.ch.SocketChannelImpl",
        "sun.nio.ch.SinkChannelImpl", "sun.nio.ch.SourceChannelImpl", "sun.nio.ch.FileChannelImpl", 
        "sun.nio.ch.MaxVENativePollArrayWrapper", 
        "sun.nio.ch.MaxVENativePollArrayWrapper$PollOut", "sun.nio.ch.MaxVENativePollArrayWrapper$PollThread",
        "sun.nio.ch.JDK_sun_nio_ch_IOUtil"
    };
    
    public Package() {
        super(classes);
        final String[] args = {"Datagram", "ServerSocket", "Socket", "Sink", "Source", "File"};
        for (String arg : args) {
            Extensions.resetField("sun.nio.ch." + arg + "ChannelImpl", "nd");
        }
    }

    @Override
    public boolean containsMethodSubstitutions() {
        return true;
    }
}
