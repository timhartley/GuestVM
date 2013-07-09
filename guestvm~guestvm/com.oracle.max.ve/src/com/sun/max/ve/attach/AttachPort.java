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
package com.sun.max.ve.attach;


public class AttachPort {
    public static final String ATTACH_PORT_PROPERTY = "max.ve.attach.port";
    private static final int DEFAULT_VALUE = 2010;
    private static int _port = DEFAULT_VALUE;
    private static boolean _init;

    private static void init() {
        final String portProperty = System.getProperty(ATTACH_PORT_PROPERTY);
        if (portProperty != null) {
            _port = Integer.parseInt(portProperty);
        }
    }

    public static int getPort() {
        if (!_init) {
            init();
        }
        return _port;
    }

}
