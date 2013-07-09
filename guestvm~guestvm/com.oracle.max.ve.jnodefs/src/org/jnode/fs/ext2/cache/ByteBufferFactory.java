/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package org.jnode.fs.ext2.cache;

import java.nio.ByteBuffer;

/**
 * Controls the subclass of {@link DirectByteBuffer} that is created for {@link SingleBlock} instances.
 * Allows a hosted implementation using {@link DirectByteBuffer}, for {@link Ext2FileTool}, and a custom implementation for VE.
 * 
 * @author Mick Jordan
 *
 */
public abstract class ByteBufferFactory {
    public static final String BYTEBUFFER_FACTORY_CLASS_PROPERTY_NAME = "max.ve.fs.ext2.bbfactory.class";
    
    protected ByteBufferFactory() {
    }
    
    public abstract ByteBuffer allocate(int cap);
    
    public static ByteBufferFactory create() {
        final String factoryClassName = System.getProperty(BYTEBUFFER_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            return new DefaultByteBufferFactory();
        } else {
            try {
                return (ByteBufferFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                return null;
            }
        }
    }

}
