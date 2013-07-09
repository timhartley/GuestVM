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
package com.sun.max.ve.blk.device;

import java.nio.ByteBuffer;

/**
 * Generic interface to block devices.
 *
 * @author Mick Jordan
 *
 */
public interface BlkDevice {

    /**
     * Return the number of sectors on this device.
     * @return number of sectors
     */
    int getSectors();

    /**
     * Return the sector size.
     * @return the sector size
     */
    int getSectorSize();

    /**
     * Write bytes to given address on this device.
     * @param devAddress
     * @param data byte buffer containing data to write
     * @return number of bytes written
     */
    long write(long devAddress, ByteBuffer data);

    /**
     * Read bytes from given address on this device.
     * @param devAddress
     * @param data byte buffer to place data read
     * @return number of bytes read
     */
    long read(long devAddress, ByteBuffer data);
}
