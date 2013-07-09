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
package com.sun.max.ve.blk.guk;

import java.nio.ByteBuffer;

import sun.nio.ch.DirectBuffer;

import com.sun.max.memory.Memory;
import com.sun.max.memory.VirtualMemory;
import com.sun.max.unsafe.*;
import com.sun.max.ve.blk.device.BlkDevice;
import com.sun.max.ve.guk.*;

/**
 * Guest VM microkernel implementation of BlkDevice.
 *
 * @author Mick Jordan
 *
 */

public final class GUKBlkDevice implements BlkDevice {

    private static boolean _init;
    private static boolean _available;
    private static int _devices;
    private Pointer _writeBuffer;
    private Pointer _readBuffer;
    private int _id;
    private static int _pageSize = 4096;

    private GUKBlkDevice(int id) {
        _id = id;
        _writeBuffer = GUKPagePool.allocatePages(1, VirtualMemory.Type.DATA);
        _readBuffer = GUKPagePool.allocatePages(1, VirtualMemory.Type.DATA);
    }

    public static GUKBlkDevice create(int id) {
        if (!_init) {
            _devices = nativeGetDevices();
            _available = _devices > 0;
            _init = true;
        }
        if (_available) {
            if (id < _devices) {
                return new GUKBlkDevice(id);
            }
        }
        return null;
    }

    /**
     * Return the number of devices available.
     * Devices number from zero.
     * @return the number of devices
     */
    public static int getDevices() {
        return nativeGetDevices();
    }

    public int getSectors() {
        return nativeGetSectors(_id);
    }

    public int getSectorSize() {
        return 512;
    }

// CheckStyle: stop parameter assignment check

    public synchronized long write(long devAddress, ByteBuffer data) {
        if (_available) {
            int left = data.remaining();
            assert (left & 511) == 0;
            if (data.isDirect()) {
                DirectBuffer directData = (DirectBuffer) data;
                nativeWrite(_id, devAddress, Address.fromLong(directData.address()).asPointer(), left);
                return left;
            } else {
                assert data.hasArray();
                int offset = data.arrayOffset();
                long bytesWritten = 0;
                while (left > 0) {
                    final int toDo = left > _pageSize ? _pageSize : left;
                    Memory.writeBytes(data.array(), offset, toDo, _writeBuffer);
                    nativeWrite(_id, devAddress, _writeBuffer, toDo);
                    left -= toDo;
                    offset += toDo;
                    bytesWritten += toDo;
                }
                return bytesWritten;
            }
        }
        return -1;
    }

    public synchronized long read(long devAddress, ByteBuffer data) {
        if (_available) {
            int left = data.remaining();
            assert (left & 511) == 0;
            if (data.isDirect()) {
                DirectBuffer directData = (DirectBuffer) data;
                nativeRead(_id, devAddress, Address.fromLong(directData.address()).asPointer(), left);
                return left;
            } else {
                assert data.hasArray();
                int offset = data.arrayOffset();
                long bytesRead = 0;
                while (left > 0) {
                    final int toDo = left > _pageSize ? _pageSize : left;
                    nativeRead(_id, devAddress, _readBuffer, toDo);
                    Memory.readBytes(_readBuffer, toDo, data.array(), offset);
                    left -= toDo;
                    offset += toDo;
                    bytesRead += toDo;
                }
                return bytesRead;
            }
        }
        return -1;
    }

    private static native int nativeGetDevices();

    private static native int nativeGetSectors(int device);

    private static native long nativeWrite(int device, long address, Pointer data, int length);

    private static native long nativeRead(int device, long address, Pointer data, int length);
}
