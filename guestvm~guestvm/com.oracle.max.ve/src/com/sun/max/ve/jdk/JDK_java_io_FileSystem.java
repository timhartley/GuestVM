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

import com.sun.max.annotate.*;
import com.sun.max.ve.error.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.heap.Heap;
import com.sun.cri.bytecode.INTRINSIC;
import static com.sun.cri.bytecode.Bytecodes.*;

/**
 * MaxVE also uses a UnixFileSystem object as the FileSystem.
 * getFileSystem is native in the Hotspot JDK so we substitute it here.
 *
 * @author Mick Jordan
 */

@METHOD_SUBSTITUTIONS(className = "java.io.FileSystem")
public final class JDK_java_io_FileSystem {

    private static Object _singleton;
    
    @INTRINSIC(UNSAFE_CAST)
    static native JDK_java_io_FileSystem asThis(Object t);
   
    @ALIAS(declaringClassName = "java.io.UnixFileSystem", name="<init>")
    private native void init();

    @SUBSTITUTE
    private static/* FileSystem */Object getFileSystem() {
        // return new UnixFileSystem();
        if (_singleton == null) {
            try {
                final Object fileSystem = Heap.createTuple(ClassActor.fromJava(Class.forName("java.io.UnixFileSystem")).dynamicHub());
                JDK_java_io_FileSystem thisFileSystem = asThis(fileSystem);
                thisFileSystem.init();
                _singleton = fileSystem;
            } catch (Exception ex) {
                VEError.unexpected("failed to construct java.io.UnixFileSystem: " + ex);
                return null;
            }
        }
        return _singleton;
    }

}
