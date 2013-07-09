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
package test.jdk;

import java.net.*;
import java.lang.reflect.*;

public class InetAddress4Constructor {
    public static void main(String[] args) {
        try {
            Class klass = Class.forName("java.net.Inet4Address");
            Constructor c = klass.getDeclaredConstructor(String.class, int.class);
            c.setAccessible(true);
            Inet4Address inet4Address = (Inet4Address) c.newInstance(new Object[] {"maxwell", 444444});
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
