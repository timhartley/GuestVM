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
/*
 * $Id: FSFile.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package org.jnode.fs;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A FSFile is a representation of a single block of bytes on a filesystem. It
 * is comparable to an inode in Unix.
 * 
 * An FSFile does not have any knowledge of who is using this file. It is also
 * possible that the system uses a single FSFile instance to create two
 * inputstream's for two different principals.
 * 
 * @author epr
 */
public interface FSFile extends FSObject {

    /**
     * Gets the length (in bytes) of this file
     * 
     * @return long
     */
    public long getLength();

    /**
     * Sets the length of this file.
     * 
     * @param length
     * @throws IOException
     */
    public void setLength(long length) throws IOException;

    /**
     * Read a given number of bytes from the file position specified by <code>fileOffset</code>.
     * The number of bytes to read is defined by <code>dest.remaining()</code> and
     * are stored starting at <code>dest.position()</code>.
     * N.B. <code>dest.position()</code> is changed by the method.
     * 
     * @param fileOffset offset in file to start reading
     * @param dest byte buffer in which to store read data
     * @throws IOException
     */
    public void read(long fileOffset, ByteBuffer dest) throws IOException;

    /**
     * Write a given number of bytes to the file position specified by <code>fileOffset</code>.
     * The number of bytes to write is defined by <code>dest.remaining()</code> and
     * are accessed starting at <code>dest.position()</code>.
     * N.B. <code>dest.position()</code> is changed by the method.
     * 
     * @param fileOffset
     * @param src
     * @throws IOException
     */
    public void write(long fileOffset, ByteBuffer src) throws IOException;

    /**
     * Flush any cached data to the disk.
     * 
     * @throws IOException
     */
    public void flush() throws IOException;
}
