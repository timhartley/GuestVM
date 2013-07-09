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
 * $Id: Ext2Entry.java 4975 2009-02-02 08:30:52Z lsantha $
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

package org.jnode.fs.ext2;

import java.io.IOException;

import java.util.logging.Level;

import com.sun.max.ve.logging.Logger;

import org.jnode.fs.spi.*;

/**
 * @author Andras Nagy
 *
 * In case of a directory, the data will be parsed to get the file-list by
 * Ext2Directory. In case of a regular file, no more processing is needed.
 *
 * TODO: besides getFile() and getDirectory(), we will need getBlockDevice()
 * getCharacterDevice(), etc.
 */
public class Ext2Entry extends AbstractFSEntry {

    private static final Logger logger = Logger.getLogger(Ext2Entry.class.getName());
    private INode iNode = null;
    private int type;

    public Ext2Entry(INode iNode, String name, int type, Ext2FileSystem fs, AbstractFSDirectory parent) {
        super(fs, parent, name, getFSEntryType(name, iNode));
        this.iNode = iNode;
        this.type = type;

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "name: " + name + ", type: " + type);
        }
    }
    public long getLastChanged() throws IOException {
        return iNode.getCtime();
    }

    @Override
    public long getLastModified() throws IOException {
        return iNode.getMtime();
    }

    public long getLastAccessed() throws IOException {
        return iNode.getAtime();
    }

    public void setLastChanged(long lastChanged) throws IOException {
        iNode.setCtime(lastChanged);
    }

    @Override
    public void setLastModified(long lastModified) throws IOException {
        iNode.setMtime(lastModified);
    }

    public void setLastAccessed(long lastAccessed) throws IOException {
        iNode.setAtime(lastAccessed);
    }

    /**
     * Returns the type.
     *
     * @return int type. Valid types are Ext2Constants.EXT2_FT_*
     */
    public int getType() {
        return type;
    }

    INode getINode() {
        return iNode;
    }

    private static int getFSEntryType(String name, INode iNode) {
        int mode = iNode.getMode() & Ext2Constants.EXT2_S_IFMT;

        if ("/".equals(name))
            return AbstractFSEntry.ROOT_ENTRY;
        else if (mode == Ext2Constants.EXT2_S_IFDIR)
            return AbstractFSEntry.DIR_ENTRY;
        else if (mode == Ext2Constants.EXT2_S_IFREG || mode == Ext2Constants.EXT2_FT_SYMLINK)
            return AbstractFSEntry.FILE_ENTRY;
        else
            return AbstractFSEntry.OTHER_ENTRY;
    }
}
