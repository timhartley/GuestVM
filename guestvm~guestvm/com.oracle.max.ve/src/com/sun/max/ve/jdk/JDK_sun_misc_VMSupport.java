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

import java.util.Properties;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;

/**
 * Substitutions for the @see sun.misc.VMSupport class.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(sun.misc.VMSupport.class)


final class JDK_sun_misc_VMSupport {
    @SUBSTITUTE
    private static Properties initAgentProperties(Properties props) {
        // sun.jvm.args, sun.jvm.flags, sun.java.command
        props.put("sun.jvm.args", VMOptions.getVmArguments());
        props.put("sun.jvm.flags", "");
        props.put("sun.java.command", VMOptions.mainClassAndArguments());
        return props;
    }
}
