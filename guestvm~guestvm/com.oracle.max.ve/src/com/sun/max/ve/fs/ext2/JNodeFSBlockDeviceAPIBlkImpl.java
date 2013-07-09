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
package com.sun.max.ve.fs.ext2;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jnode.driver.block.FSBlockDeviceAPI;
import org.jnode.partitions.PartitionTableEntry;

import com.sun.max.ve.blk.guk.*;


public final  class JNodeFSBlockDeviceAPIBlkImpl implements FSBlockDeviceAPI {

    private GUKBlkDevice _blkDevice;
    private long _length;

    private JNodeFSBlockDeviceAPIBlkImpl(GUKBlkDevice blkDevice) {
        _blkDevice = blkDevice;
        _length = _blkDevice.getSectors() * _blkDevice.getSectorSize();
    }

    public static JNodeFSBlockDeviceAPIBlkImpl create(int id) {
        final GUKBlkDevice blkDevice = GUKBlkDevice.create(id);
        if (blkDevice != null) {
            return new JNodeFSBlockDeviceAPIBlkImpl(blkDevice);
        }
        return null;
    }

    @Override
    public PartitionTableEntry getPartitionTableEntry() {
        return null;
    }

    @Override
    public int getSectorSize() throws IOException {
        check();
        return _blkDevice.getSectorSize();
    }

    @Override
    public void flush() throws IOException {
        // nothing to do
    }

    @Override
    public long getLength() throws IOException {
        check();
        return _length;
    }

    @Override
    public void read(long devOffset, ByteBuffer dest) throws IOException {
        check();
        _blkDevice.read(devOffset, dest);
    }

    @Override
    public void write(long devOffset, ByteBuffer src) throws IOException {
        check();
        _blkDevice.write(devOffset, src);
    }

    private void check() throws IOException {
        if (_blkDevice == null) {
            throw new IOException("device not available");
        }
    }

}
