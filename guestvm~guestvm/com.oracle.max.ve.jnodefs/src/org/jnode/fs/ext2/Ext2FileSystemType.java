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
 * $Id: Ext2FileSystemType.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.nio.ByteBuffer;

import org.jnode.driver.Device;
import org.jnode.driver.block.FSBlockDeviceAPI;
import org.jnode.fs.BlockDeviceFileSystemType;
import org.jnode.fs.FileSystemException;
import org.jnode.partitions.PartitionTableEntry;
//import org.jnode.partitions.ibm.IBMPartitionTableEntry;
//import org.jnode.partitions.ibm.IBMPartitionTypes;

/**
 * @author Andras Nagy
 */
public class Ext2FileSystemType implements BlockDeviceFileSystemType<Ext2FileSystem> {
    public static final Class<Ext2FileSystemType> ID = Ext2FileSystemType.class;

    /**
     * @see org.jnode.fs.FileSystemType#create(Device, boolean)
     */
    public Ext2FileSystem create(Device device, String[] options) throws FileSystemException {
        Ext2FileSystem fs = new Ext2FileSystem(device, options, this);
        try {
            fs.read();
        } catch (FileSystemException ex) {
            if (!(ex.getMessage().startsWith("Not ext2 superblock"))) {
                throw ex;
            }
        }
        return fs;
    }
    
    /**
     * @see org.jnode.fs.FileSystemType#getName()
     */
    public String getName() {
        return "EXT2";
    }

    /**
     * @see org.jnode.fs.FileSystemType#supports(PartitionTableEntry, byte[], FSBlockDeviceAPI)
     */
    public boolean supports(PartitionTableEntry pte, byte[] firstSector, FSBlockDeviceAPI devApi) {
        /*
        if (pte != null) {
            if (pte instanceof IBMPartitionTableEntry) {
                if (((IBMPartitionTableEntry) pte).getSystemIndicator() != IBMPartitionTypes.PARTTYPE_LINUXNATIVE) {
                    return false;
                }
            }
        }
        */

        //need to check the magic
        ByteBuffer magic = ByteBuffer.allocate(2);
        try {
            devApi.read(1024 + 56, magic);
        } catch (IOException e) {
            return false;
        }
        return (Ext2Utils.get16(magic.array(), 0) == 0xEF53);
    }
}
