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
package com.sun.max.ve.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.INTRINSIC;


/**
 * Collect all the unsafe casts used by clients of {@link ALIAS} methods in the JDK substitution classes.
 * 
 * @author Mick Jordan
 *
 */
final class AliasCast {
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_io_FileDescriptor asJDK_java_io_FileDescriptor(Object obj);    
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_io_FileInputStream asJDK_java_io_FileInputStream(Object obj);
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_io_FileOutputStream asJDK_java_io_FileOutputStream(Object obj);    
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_io_RandomAccessFile asJDK_java_io_RandomAccessFile(Object obj);    
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_net_PlainDatagramSocketImpl asJDK_java_net_PlainDatagramSocketImpl(Object obj);    
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_net_PlainSocketImpl asJDK_java_net_PlainSocketImpl(Object obj);    
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_net_Inet4AddressImpl asJDK_java_net_Inet4AddressImpl(Object obj);
    @INTRINSIC(UNSAFE_CAST) static native JDK_java_net_NetworkInterface asJDK_java_net_NetworkInterface(Object obj);
    @INTRINSIC(UNSAFE_CAST) static native JDK_sun_nio_ch_FileChannelImpl asJDK_sun_nio_ch_FileChannelImpl(Object obj);
    @INTRINSIC(UNSAFE_CAST) static native JDK_sun_nio_ch_FileKey asJDK_sun_nio_ch_FileKey(Object obj);

}
