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

import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.jni.*;

/**
 * Implementation of native methods for java.lang.SecirityManager.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(SecurityManager.class)
public class JDK_java_lang_SecurityManager {

    @CONSTANT_WHEN_NOT_ZERO
    private static FieldActor _initFieldActor;

    private static boolean check(Object self) {
        if (_initFieldActor == null) {
            _initFieldActor = (FieldActor) ClassActor.fromJava(SecurityManager.class).findFieldActor(SymbolTable.makeSymbol("initialized"), null);
        }
        final boolean initialized = _initFieldActor.getBoolean(self);
        if (!initialized) {
            throw new SecurityException("security manager not initialized.");
        }
        return true;
    }

    @SUBSTITUTE
    private Class[] getClassContext() {
        check(this);
        return JVMFunctions.GetClassContext();
    }

    @SUBSTITUTE
    private ClassLoader currentClassLoader0() {
        unimplemented("currentClassLoader0");
        return null;
    }

    @SUBSTITUTE
    private int classDepth(String name) {
        unimplemented("classDepth");
        return 0;
    }

    @SUBSTITUTE
    private int classLoaderDepth0() {
        unimplemented("classLoaderDepth0");
        return 0;
    }

    @SUBSTITUTE
    private Class currentLoadedClass0() {
        unimplemented("currentLoadedClass0");
        return null;
    }

    private static void unimplemented(String w) {
        VEError.unimplemented("java.lang.SecurityManager."+ w);
    }

}
