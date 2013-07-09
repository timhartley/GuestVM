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
 * $Id: Block.java 4975 2009-02-02 08:30:52Z lsantha $
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

package org.jnode.fs.ext2.cache;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.logging.Level;

import com.sun.max.ve.logging.Logger;

import org.jnode.fs.ext2.Ext2FileSystem;

/**
 * Stores the data associated with a block.
 * Used as the value in the {@link BlockCache block cache}.
 * For performance reasons, we now use {@link DirectByteBuffer direct byte buffers}.
 * The actual direct byte buffer is allocated by {@link BlockCache} from a pool.
 * 
 * @author Andras Nagy
 * @author Mick Jordan
 */
public class SingleBlock extends Block {

    public SingleBlock(Ext2FileSystem fs, long blockNr, ByteBuffer data) {
        super(fs, blockNr, data);
    }

    /**
     * Updates the data. Since we want to preserve the invariant that the byte
     * buffer is direct, we copy the data to the byte buffer.
     * 
     * @param data The data to set
     */
    @Override
    public void setBuffer(ByteBuffer data) {
        if (this.buffer != data) {
            final int srcPos = data.position();
            final int pos = this.buffer.position();
            this.buffer.put(data);
            this.buffer.position(pos);
            data.position(srcPos);
            dirty = true;
        }
    }

    /**
     * flush is called when the block is being removed from the cache
     */
    @Override
    public void flush() throws IOException {
        if (dirty) {
            fs.writeBlock(blockNr, buffer, true);
            if (BlockCache.logger.isLoggable(Level.INFO)) {
                BlockCache.logger.log(Level.INFO, this.toString());
            }
        }
    }
    
    @Override
    public String toString() {
        String result =  "SB:" + super.toString();
        if (parent != null) {
            result += ";" + parent.toString();
        }
        return result;
    }

}
