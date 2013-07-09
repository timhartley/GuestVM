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
package com.sun.max.ve.net;

import com.sun.max.program.*;

/**
 * A FragPacket is used to represent fragmented IP packets that share data with the original
 * unfragmented packet, which avoids copying but, of course, has its perils. When a FragPacket is first
 * created it is essentially a clone of the original packet, save that the underlying data buffer is not copied
 * but shared. The other state, e.g., header offset, data length, is copied and therefore capable of separate
 * evolution. All methods that access this state, e.g., @see getHeaderOffset simply invoke the superclass
 * definition to access this separate state, forwarding the arguments. All methods that access the underlying
 * buffer, e.g., @see putInt, also invoke the superclass method, but adjust the offset with the value that
 * was set when the FragPacket was created. N.B. This depends on there being no nested calls in Packet that might
 * callback into here and add the offset twice.
 *
 * N.B. FragPackets are only used for output.
 *
 * @author Mick Jordan
 *
 */
public class FragPacket extends Packet {
    // private Packet _unFrag;
    private int _offset;

    /**
     * Create a fragmented packet from the given underlying packet.
     * The offset is where the FragPacket is to start in the underlying packet.
     * @param unfrag
     * @param offset
     */
    public FragPacket(Packet unfrag, int offset) {
        super(unfrag.getHeaderOffset(), unfrag.dataLength(), unfrag.getBuf());
        // _unFrag = unfrag;
        _offset = offset;
    }

    @Override
    public boolean isFragment() {
        return true;
    }

    @Override
    public Packet copy() {
        error("copy");
        return null;
    }

    @Override
    public int cksum(int offset, int len) {
        return super.cksum(offset + _offset, len);
    }

    @Override
    public byte getByteIgnoringHeaderOffset(int off) {
        return super.getByteIgnoringHeaderOffset(off + _offset);
    }

    @Override
    public byte[] getEthAddr(int offset) {
        error("getEthAddr");
        return null;
    }

    @Override
    public long getEthAddrAsLong(int off) {
        error("getEthAddrAsLong");
        return 0;
    }

    @Override
    public int getInt(int off) {
        return super.getInt(off + _offset);
    }

    @Override
    public int getShort(int off) {
        return super.getShort(off + _offset);
    }

    @Override
    public int getByte(int off) {
        return super.getByte(off + _offset);
    }

    @Override
    public void getBytes(int srcOffset, byte[] dst, int dstOffset, int len) {
        error("getBytes");
    }

    @Override
    public void putEthAddr(byte[] addr, int offset) {
        super.putEthAddr(addr, offset + _offset);
    }

    @Override
    public void putEthAddr(long d, int off) {
        super.putEthAddr(d, off + _offset);
    }

    @Override
    public void putInt(int d, int off) {
        super.putInt(d, _offset + off);
    }

    @Override
    public void putShort(int d, int off) {
        super.putShort(d, off + _offset);
    }

    @Override
    public void putByte(int d, int off) {
        super.putByte(d, off + _offset);
    }

    @Override
    public void putBytes(byte[] src, int srcOffset, int dstOffset, int len) {
        super.putBytes(src, srcOffset, dstOffset + _offset, len);
    }

    @Override
    public void putBytes(Packet pkt, int srcOffset, int dstOffset, int len) {
        error("putBytes");
    }

    private static void error(String m) {
        ProgramError.unexpected("FragPacket." + m + " not implemented");
    }

}
