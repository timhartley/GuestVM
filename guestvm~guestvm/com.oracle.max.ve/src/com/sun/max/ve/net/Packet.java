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

import com.sun.max.annotate.*;

/**
 * A class that denotes a network packet.
 *
 * The packet is backed by an underlying byte buffer that is logically separated into
 * a "header" portion and a "data" portion as per the IP protocols. Of course,
 * the IP protocols are layered so that one layers header is a lower layers data.
 * When a packet is allocated the size of the header and the size of the data
 * are specified separately and the header offset field is set based on the given
 * header size.
 *
 * To allow packet construction at the different layers without copying and without
 * being aware of the upper layers, it is possible to adjust the header offset.
 * All "put" operations add in the current header offset before storing in the
 * underlying buffer. Obviously there are risks of data corruption with this approach
 * if the header offset is set incorrectly.
 *
 */
public class Packet {

    // useful for building linked lists of packets
    public Packet _next;
    public Packet _prev;

    private int _srcPort;
    private int _srcIP;
    private int _dstIP;

    byte[] _buf;                          // where the packet data is stored, so max size is _buf.length
    private int _length;              // may be set to < _buf.length
    private int _hdrOffset;          // the offset of the first byte after the "header", gets changed by protocol handlers
    private long _timeStamp;     // incoming packet: arrival time at network device, outgoing packet: undefined

    private Packet(int hlen, int dlen) {
        this(hlen, dlen, new byte[hlen + dlen]);
    }

    // CheckStyle: stop parameter assignment check

    /**
     * The following four methods exists for the benefit of the network device interrupt handler, 
     * {@link com.sun.max.ve.net.guk.GUKNetDevice}.
     * They must be inlined. We name them specially to indicate that fact.
     */

    @INLINE
    public final void inlineReset() {
        _length = _buf.length;
        _hdrOffset = 0;
    }

    @INLINE
    public final int inlineLength() {
        return _length;
    }

    @INLINE
    public final void inlineSetLength(int len) {
        _length = len;
    }

    @INLINE
    public final void inlinePutByteIgnoringHdrOffset(byte b, int off) {
        _buf[off] = b;
    }

    @INLINE
    public final byte inlineGetByteIgnoringHeaderOffset(int off) {
        return _buf[off];
    }

    @INLINE
    public final void inlineSetTimeStamp(long ts) {
        _timeStamp = ts;
    }

    protected Packet(int hlen, int dlen, byte[] buf) {
        _length = hlen + dlen;
        _hdrOffset = hlen;
        _buf = buf;
    }

    public static Packet getTx(int dstIp, int hlen, int dlen) {
        return new Packet(hlen, dlen);
    }

    public static Packet get(int hlen, int dlen) {
        return new Packet(hlen, dlen);
    }

    public static Packet get(int length) {
        return new Packet(0, length);
    }

    public long getTimeStamp() {
        return _timeStamp;
    }

    public int getSrcPort() {
        return _srcPort;
    }

    public int getDstIP() {
        return _dstIP;
    }

    public int getSrcIP() {
        return _srcIP;
    }

    public void setPortAndIPs(int port, int srcIP, int dstIP) {
        _srcPort = port;
        _srcIP = srcIP;
        _dstIP = dstIP;
    }

    protected byte[] getBuf() {
        return _buf;
    }

    /**
     * Create a copy of this packet.
     * Note that the actual buffer length is ignored, only the data up to @see length is copied.
     * @return
     */
    public Packet copy() {
        final Packet sp = new Packet(_hdrOffset, _length - _hdrOffset);
        System.arraycopy(_buf, 0, sp._buf, 0, sp._buf.length);
        sp._srcIP = _srcIP;
        sp._srcPort = _srcPort;
        return sp;
    }

    public final void reset() {
        _length = _buf.length;
        _hdrOffset = 0;
    }

    public boolean isFragment() {
        return false;
    }

    /**Perform an Internet checksum on the packet data.
     * *
     * @param offset
     * @param len
     * @return
     */
    public int cksum(int offset, int len) {
        return ~ocsum(_buf, _hdrOffset + offset, len)  & 0xFFFF;
    }

    public static int ocsum(byte[] buf, int off, int len) {
        int sum = 0;
        int i = 0;
        while (len > 1) {
            final int incr = ((buf[i + off] << 8) & 0xFF00) + (buf[i + off + 1] & 0xFF);
            sum += incr;
            i += 2;
            len -= 2;
        }
        if (len > 0) {
            sum += (buf[i + off] << 8) & 0xFF00;
        }
        while ((sum >> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        return sum;

    }

    public void shiftHeader(int bytes) {
        _hdrOffset += bytes;
    }

    public int getHeaderOffset() {
        return _hdrOffset;
    }

    public void setHeaderOffset(int off) {
        _hdrOffset = off;
    }

    public final int length() {
        return _length;
    }

    public int dataLength() {
        return _length - _hdrOffset;
    }

    public void setDataLength(int len) {
        _length = _hdrOffset + len;
    }

    public final void setLength(int len) {
        _length = len;
    }

    /**
     * Return the byte at offset ignoring any header offset.
     * @param off
     * @return
     */
    public byte getByteIgnoringHeaderOffset(int off) {
        return _buf[off];
    }

     /**
     * Get the ethernet address as a byte array at given offset from _hdr_offset.
     * @param off
     * @return
     */
    public byte[] getEthAddr(int offset) {
        final byte[] result = new byte[6];
        getBytes(offset, result, 0, 6);
        return result;
    }

    /**
     * Get the ethernet address as a long at given offset from _hdr_offset.
     * @param off
     * @return
     */
    public long getEthAddrAsLong(int off) {
        off += _hdrOffset;
        return ((long) (_buf[off]   & 0xff) << 40) |
               ((long) (_buf[off + 1] & 0xff) << 32) |
               ((long) (_buf[off + 2] & 0xff) << 24) |
               ((long) (_buf[off + 3] & 0xff) << 16) |
               ((long) (_buf[off + 4] & 0xff) << 8) |
                (long) (_buf[off + 5] & 0xff);
    }

    public int getInt(int off) {
        off += _hdrOffset;
        return ((_buf[off]) << 24) |
               ((_buf[off + 1] & 0xff) << 16) |
               ((_buf[off + 2] & 0xff) << 8) |
                (_buf[off + 3] & 0xff);
    }

    public int getShort(int off) {
        off += _hdrOffset;
        return ((_buf[off] & 0xff) << 8) |
                (_buf[off + 1] & 0xff);
    }

    public int getByte(int off) {
        return _buf[_hdrOffset + off] & 0xff;
    }

    // Copy data from packet to supplied buffer.
    public void getBytes(int srcOffset, byte[] dst, int dstOffset, int len) {
        System.arraycopy(_buf, _hdrOffset + srcOffset, dst, dstOffset, len);
    }

    /**
     * Put ethernet address at given offset from _hdr_offset.
     * @param addr
     * @param offset
     */
    public void putEthAddr(byte[] addr, int offset) {
        System.arraycopy(addr, 0, _buf, offset + _hdrOffset, 6);
    }

    /**
     * Put the ethernet address (as a long) at given offset from _hdr_offset.
     * @param d
     * @param off
     */
    public void putEthAddr(long d, int off) {
        off += _hdrOffset;
        _buf[off] =   (byte)  (d >>> 40);
        _buf[off + 1] = (byte) ((d >> 32) & 0xff);
        _buf[off + 2] = (byte) ((d >> 24) & 0xff);
        _buf[off + 3] = (byte) ((d >> 16) & 0xff);
        _buf[off + 4] = (byte) ((d >> 8) & 0xff);
        _buf[off + 5] = (byte) (d & 0xff);
    }

    public void putInt(int d, int off) {
        off += _hdrOffset;
        _buf[off] =   (byte)  (d >>> 24);
        _buf[off + 1] = (byte) ((d >> 16) & 0xff);
        _buf[off + 2] = (byte) ((d >> 8) & 0xff);
        _buf[off + 3] = (byte) (d & 0xff);
    }

    public void putShort(int d, int off) {

        off += _hdrOffset;
        _buf[off] =   (byte) ((d >> 8) & 0xff);
        _buf[off + 1] = (byte) (d & 0xff);
    }

    public void putByte(int d, int off) {
        _buf[_hdrOffset + off] = (byte) (d & 0xff);
    }

    // Copy data from supplied buffer to packet.
    public void putBytes(byte[] src, int srcOffset, int dstOffset, int len) {
        System.arraycopy(src, srcOffset, _buf, dstOffset + _hdrOffset, len);
    }


    // Copy the specified range from the given packet into this packet.
    public void putBytes(Packet pkt, int srcOffset, int dstOffset, int len) {
        pkt.getBytes(srcOffset, _buf, dstOffset + _hdrOffset, len);
    }
}
