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
 * $Id: Ext2DirectoryRecord.java 4975 2009-02-02 08:30:52Z lsantha $
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

import java.util.Arrays;

import java.util.logging.Level;

import com.sun.max.ve.logging.*;

import org.jnode.fs.FileSystemException;

/**
 * A single directory record, i.e. the inode number, type, and name of an entry in a
 * directory.
 *
 * A record is variable length, rounded up to a multiple of 4 bytes.
 * There is a fixed part of length 8, followed by the variable part (the name)
 * which is limited to 255, since a 1 byte field is used to record the name length.
 *
 * Records are stored in (plain) directory files, which consist of a sequence of directory
 * records, with the last record artificially extended to the end of the block. I.e., there
 * is no distinguished free space, just records. Elsewhere, there is a suggestion that
 * a record with a zero inode number is also free but, currently, this situation doesn't occur.
 *
 * Instances of this class are created in two ways using two different. constructors.
 * One is used for records corresponding to new files/directories yet to be added
 * to the directory file; the other is used when reading an existing directory file.
 *
 * @author Andras Nagy
 * @author Mick Jordan
 */
public class Ext2DirectoryRecord {
    private static final Logger logger = Logger.getLogger(Ext2DirectoryRecord.class.getName());
    /*
     * private int iNodeNr; private int recLen; private short nameLen; private
     * short type; private StringBuffer name;
     */

    private static final int FIXED_LENGTH = 8;
    private static final int INODENR_OFFSET = 0; // 4 byte field
    private static final int RECLEN_OFFSET = 4;   // 2 byte field
    private static final int NAMLEN_OFFSET = 6;  // 1 byte field
    private static final int TYPE_OFFSET = 7;       // 1 byte field
    private static int ALIGN = 4;

    private int offset;
    private byte[] data;
    private long fileOffset;
    private Ext2FileSystem fs;

    /** Create a new instance from the data at the given offset in the data array.
     * This form is used when creating a record from an existing directory file.
     * The relevant portion of data is copied into a new, properly sized array.
     * @param data: the data that makes up the directory block
     * @param offset: the offset where the current DirectoryRecord begins within
     *            the data array.
     * @param fileOffset: the offset from the beginning of the directory file
     */
    public Ext2DirectoryRecord(Ext2FileSystem fs, byte[] data, int offset, int fileOffset) {
        this.fs = fs;
        this.data = data;
        this.offset = offset;
        this.fileOffset = fileOffset;

        // make a copy of the data
        synchronized (data) {
            byte[] newData = new byte[getRecLen()];
            System.arraycopy(data, offset, newData, 0, getRecLen());
            this.data = newData;
            setOffset(0);
        }
    }

    /**
     * Create a new Ext2DirectoryRecord from scratch (it can be retrieved with
     * getData()).
     *
     * @param iNodeNr
     * @param type
     * @param name
     */
    public Ext2DirectoryRecord(Ext2FileSystem fs, long iNodeNr, int type, String name) {
        this.offset = 0;
        this.fs = fs;
        data = new byte[FIXED_LENGTH + name.length()];
        setName(name);
        setINodeNr(iNodeNr);
        setType(type);
        int newLength = align(name.length() + FIXED_LENGTH);
        setRecLen(newLength);
        // Note that fileOffset is not known at this points as this instance is not yet associated with
        // a directiry file.
    }

    public byte[] getData() {
        return data;
    }

    public int getOffset() {
        return offset;
    }

    private void setOffset(int offset) {
        this.offset = offset;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    private void setFileOffset(long fileOffset) {
        this.fileOffset = fileOffset;
    }

    /**
     * Returns the fileType.
     *
     * @return short
     */
    public synchronized int getType() {
        return Ext2Utils.get8(data, offset + TYPE_OFFSET);
    }

    private synchronized void setType(int type) {
        if (!fs.hasIncompatFeature(Ext2Constants.EXT2_FEATURE_INCOMPAT_FILETYPE))
            return;
        Ext2Utils.set8(data, offset + TYPE_OFFSET, type);
    }

    /**
     * Returns the iNodeNr.
     *
     * @return long
     */
    public synchronized int getINodeNr() {
        return (int) Ext2Utils.get32(data, offset + INODENR_OFFSET);
    }

    synchronized void setINodeNr(long nr) {
        Ext2Utils.set32(data, offset + INODENR_OFFSET, nr);
    }

    /**
     * Returns the name.
     *
     * @return StringBuffer
     */
    public synchronized String getName() {
        StringBuffer name = new StringBuffer();
        if (getINodeNr() != 0) {
            // TODO: character conversion??
            for (int i = 0; i < getNameLen(); i++)
                name.append((char) Ext2Utils.get8(data, offset + FIXED_LENGTH + i));
             if (logger.isLoggable(Level.FINER)) {
                 logger.log(Level.FINER, "Ext2DirectoryRecord(): iNode=" + getINodeNr() + ", name=" + name);
             }
        }
        return name.toString();
    }

    synchronized void setName(String name) {
        int len = name.length();
        for (int i = 0; i < len; i++)
            Ext2Utils.set8(data, offset + FIXED_LENGTH + i, name.charAt(i));
        setNameLen(len);
    }

    synchronized boolean equalsName(Ext2DirectoryRecord other) {
        int myLen = getNameLen();
        int otherLen = other.getNameLen();
        if (myLen == otherLen) {
            for (int i = 0; i < myLen; i++) {
                int myC = Ext2Utils.get8(data, offset + FIXED_LENGTH + i);
                int otherC = Ext2Utils.get8(other.data, other.offset + FIXED_LENGTH + i);
                if (myC != otherC) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the recLen.
     *
     * @return int
     */
    public synchronized int getRecLen() {
        return Ext2Utils.get16(data, offset + RECLEN_OFFSET);
    }

    private synchronized void setRecLen(int len) {
        Ext2Utils.set16(data, offset + RECLEN_OFFSET, len);
    }

    private synchronized int getNameLen() {
        return Ext2Utils.get8(data, offset + NAMLEN_OFFSET);
    }

    private synchronized void setNameLen(int len) {
        Ext2Utils.set8(data, offset + NAMLEN_OFFSET, len);
    }

    synchronized int getMaxNameLen() {
        return align(getNameLen() + FIXED_LENGTH) - FIXED_LENGTH;
    }

    /**
     * The last directory record's length is set such that it extends until the
     * end of the block. This method truncates it when a new record is added to
     * the directory. I.e., the record has effectively been split into two, with the
     * second part being used for a new record.
     */
    protected synchronized void truncateRecord() {
        int newLength = align(getNameLen() + FIXED_LENGTH);
        setRecLen(newLength);
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "truncateRecord(): newLength: " + newLength);
        }
    }

    /**
     * Round up n so that it is a multiple of ALIGN.
     * @param n
     * @return
     */
    private static int align(int n) {
        if (n % ALIGN != 0)
            n += ALIGN - n % ALIGN;
        return n;

    }

    /**
     * The last directory record's length is set such that it extends until the
     * end of the block. This method extends the directory record to this
     * length. The directoryRecord's <code>fileOffset</code> will be set to
     * <code>beginning</code>.
     *
     * @param beginning: the offset where the record begins
     * @param end: the offset where the record should end
     *                           (usually the size of a filesystem block)
     */
    protected synchronized void expandRecord(long beginning, long end) throws FileSystemException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "expandRecord(" + beginning + ", " + end + ")");
        }
        if (beginning + getNameLen() + FIXED_LENGTH <= end) {
            // the record fits in the block
            setRecLen((int) (end - beginning));
            // pad the end of the record with zeroes
            byte[] newData = new byte[getRecLen()];
            Arrays.fill(newData, 0, getRecLen(), (byte) 0);
            System.arraycopy(data, getOffset(), newData, 0, getNameLen() + FIXED_LENGTH);
            setOffset(0);
            setFileOffset(beginning);
            data = newData;
        } else {
            throw new FileSystemException("The directory record does not fit into the block!");
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "expandRecord(): newLength: " + getRecLen());
        }
    }
}
