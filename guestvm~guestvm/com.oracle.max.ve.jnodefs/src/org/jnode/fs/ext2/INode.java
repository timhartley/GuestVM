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
 * $Id: INode.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.util.Arrays;

import java.util.logging.Level;

import com.sun.max.ve.logging.Logger;

import org.jnode.fs.FileSystemException;
import org.jnode.fs.ext2.cache.Block;
import org.jnode.fs.ext2.exception.UnallocatedBlockException;

/**
 * This class represents an inode. Once they are allocated, inodes are read and
 * written by the INodeTable (which is accessible through desc.getINodeTable().
 *
 * @author Andras Nagy
 * @author Mick Jordan
 */
public class INode {
    public static final int INODE_LENGTH = 128;

    private static final Logger logger = Logger.getLogger(INode.class.getName());

    /**
     * the data constituting the inode itself
     */
    private byte[] data;

    private volatile boolean dirty;

    /**
     * If an inode is locked, it may not be flushed from the cache (locked
     * counts the number of threads that have locked the inode)
     */
    private volatile int locked;

    /**
     * nonpersistent data stored in memory only
     */
    INodeDescriptor desc = null;

    public final Ext2FileSystem fs;
    
    /**
     * Create an INode object from an existing inode on the disk.
     *
     * @param fs
     * @param desc
     */
    public INode(Ext2FileSystem fs, INodeDescriptor desc, boolean read) throws IOException, FileSystemException {
        this.fs = fs;
        this.desc = desc;
        this.data = new byte[INODE_LENGTH];
        locked = 0;
        if (read) {
            desc.getINodeTable().getInodeData(desc.getIndex(), data);
        }
    }

    /**
     * Create a new INode object from scratch
     */
    public void create(int fileFormat, int accessRights, int uid, int gid) {
        long time = System.currentTimeMillis() / 1000;
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,deviceString() + Long.toString(time));
        }

        setUid(uid);
        setGid(gid);
        setMode(fileFormat | accessRights);
        setSize(0);
        setAtime(time);
        setCtime(time);
        setMtime(time);
        setDtime(0);
        setLinksCount(0);
        //TODO: set other pesistent parameters?

        setDirty(true);
    }

    public int getINodeNr() {
        return desc.getINodeNr();
    }

    @Override
    protected void finalize() throws Exception {
        flush();
    }

    /**
     * Called when an inode is flushed from the inode buffer and its state must
     * be saved to the disk
     */
    public void flush() throws IOException, FileSystemException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,deviceString() + Integer.toString(getINodeNr()));
        }

        freePreallocatedBlocks();
        update();
    }

    /**
     * write an inode back to disk
     *
     * @throws IOException
     *
     * synchronize to avoid that half-set fields get written to the inode
     */
    protected synchronized void update() throws IOException {
        try {
            if (dirty) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER,deviceString() + Integer.toString(getINodeNr()));
                }
                desc.getINodeTable().writeInodeData(desc.getIndex(), data);
                dirty = false;
            }
        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    /**
     * Return the number of the group that contains the inode.
     *
     * @return
     */
    protected long getGroup() {
        return desc.getGroup();
    }

    /**
     * return the number of direct blocks that an indirect block can point to
     *
     * @return
     */
    private final int getIndirectCount() {
        return fs.getSuperblock().getBlockSize() >> 2; //a block index is 4
        // bytes long
    }

    /**
     * Parse the indirect blocks of level <code>indirectionLevel</code> and
     * return the address of the <code>offset</code> th block. For example,
     * indirectRead( doubleIndirectBlockNumber, 45, 3) will return the 45th
     * block that is reachable via triple indirection, which is the (12 +
     * getIndirectCount() +getIndirectCount()^2 + 45)th block of the inode (12
     * direct blocks, getIndirectCount() simple indirect blocks,
     * getIndirectCount()^2 double indirect blocks, 45th triple indirect block).
     *
     * @param indirectionLevel:
     *            0: direct block, 1: indirect block, ...
     */
    private final long indirectRead(long dataBlockNr, long offset, int indirectionLevel)
        throws IOException {
        Block block = fs.readBlock(dataBlockNr);
        final ByteBuffer data = block.getBuffer();
        if (indirectionLevel == 1) {
            //data is a (simple) indirect block
            long result = Ext2Utils.get32(data, (int) offset * 4);
            block.unlock();
            return result;
        }

        long blockIndex = offset
                / /*(long) Math.*/pow(getIndirectCount(), indirectionLevel - 1);
        long blockOffset = offset
                % /*(long) Math.*/pow(getIndirectCount(), indirectionLevel - 1);
        long blockNr = Ext2Utils.get32(data, (int) blockIndex * 4);
        block.unlock();

        return indirectRead(blockNr, blockOffset, indirectionLevel - 1);
    }

    private static long pow(long a, long b) {
        if (b == 0) {
            return 1;
        } else if (b == 1) {
            return a;
        } else {
            long result = a;
            for (long i = 1; i < b; i++) {
                result *= a;
            }
            return result;
        }
    }

    /**
     * Parse the indirect blocks of level <code>indirectionLevel</code> and
     * register the address of the <code>offset</code> th block. Also see
     * indirectRead().
     * 
     * @param allocatedBlocks: (the number of blocks allocated so far)-1
     */
    private final void indirectWrite(long dataBlockNr, long offset, long allocatedBlocks,
            long value, int indirectionLevel) throws IOException, FileSystemException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,deviceString() + "bn: " + dataBlockNr + ", ic: " + indirectionLevel + ", offset: " + offset);
        }
        final Block block = fs.readBlock(dataBlockNr);
        ByteBuffer data = block.getBuffer();
        if (indirectionLevel == 1) {
            // data is a (simple) indirect block
            Ext2Utils.set32(data, (int) offset * 4, value);
            // write back the updated block
            fs.writeBlock(dataBlockNr, data, false);
            block.unlock();
        } else {
            long blockNr;
            long blockIndex =
                    offset / /* (long) Math. */pow(getIndirectCount(), indirectionLevel - 1);
            long blockOffset =
                    offset % /* (long) Math. */pow(getIndirectCount(), indirectionLevel - 1);
            if (blockOffset == 0) {
                // need to reserve the indirect block itself
                blockNr = findFreeBlock(allocatedBlocks++);
                Ext2Utils.set32(data, (int) blockIndex * 4, blockNr);
                fs.writeBlock(dataBlockNr, data, false);

                // need to blank the block so that e2fsck does not complain
                byte[] zeroes = new byte[fs.getBlockSize()]; // blank the block
                Arrays.fill(zeroes, 0, fs.getBlockSize(), (byte) 0);
                fs.writeBlock(blockNr, zeroes, false);
            } else {
                blockNr = Ext2Utils.get32(data, (int) blockIndex * 4);
            }
            block.unlock();
            indirectWrite(blockNr, blockOffset, allocatedBlocks, value, indirectionLevel - 1);
        }
    }

    /**
     * Free up block dataBlockNr, and free up any indirect blocks, if needed
     *
     * @param dataBlockNr
     * @param offset
     * @param indirectionLevel
     * @throws IOException
     */
    private final void indirectFree(long dataBlockNr, long offset, int indirectionLevel)
        throws IOException, FileSystemException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,deviceString() + "bn: " + dataBlockNr + ", ic: " + indirectionLevel + ", offset: " + offset);
        }
        if (indirectionLevel == 0) {
            fs.freeBlock(dataBlockNr);
            return;
        }

        final Block block = fs.readBlock(dataBlockNr);
        ByteBuffer data = block.getBuffer();

        long blockIndex = offset
                / /*(long) Math.*/pow(getIndirectCount(), indirectionLevel - 1);
        long blockOffset = offset
                % /*(long) Math.*/pow(getIndirectCount(), indirectionLevel - 1);
        long blockNr = Ext2Utils.get32(data, (int) blockIndex * 4);
        block.unlock();

        indirectFree(blockNr, blockOffset, indirectionLevel - 1);

        if (offset == 0) {
            //block blockNr has been the last block pointer on the indirect
            // block,
            //so the indirect block can be freed up as well
            fs.freeBlock(dataBlockNr);
            long block512 = fs.getBlockSize() / 512;
            setBlocks(getBlocks() - block512);
        }
    }

    /**
     * Return the number of the block in the filesystem that stores the ith
     * block of the inode (i is a sequential index from the beginning of the
     * file)
     *
     * [Naming convention used: in the code, a <code>...BlockNr</code> always
     * means an absolute block nr (of the filesystem), while a
     * <code>...BlockIndex</code> means an index relative to the beginning of
     * a block]
     *
     * @param i
     * @return @throws
     *         IOException
     */
    private long getDataBlockNr(long i) throws IOException {
        final long blockCount = getAllocatedBlockCount();
        final int indirectCount = getIndirectCount();
        if (i > blockCount - 1) {
            throw new IOException("Trying to read block " + i + " (counts from 0), while" +
                    " INode contains only " + blockCount + " blocks");
        }
        
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,deviceString() + "bn: " + i + ", bc: " + blockCount + ", ic: " + indirectCount);
        }

        //get the direct blocks (0; 11)
        if (i < 12) {
            return Ext2Utils.get32(data, 40 + (int) i * 4);
        }

        //see the indirect blocks (12; indirectCount-1)
        i -= 12;
        if (i < indirectCount) {
            //the 12th index points to the indirect block
            return indirectRead(Ext2Utils.get32(data, 40 + 12 * 4), i, 1);
        }

        //see the double indirect blocks (indirectCount; doubleIndirectCount-1)
        i -= indirectCount;
        if (i < indirectCount * indirectCount) {
            //the 13th index points to the double indirect block
            return indirectRead(Ext2Utils.get32(data, 40 + 13 * 4), i, 2);
        }

        //see the triple indirect blocks (doubleIndirectCount;
        // tripleIndirectCount-1)
        i -= indirectCount * indirectCount;
        if (i < indirectCount * indirectCount * indirectCount) {
            //the 14th index points to the triple indirect block
            return indirectRead(Ext2Utils.get32(data, 40 + 14 * 4), i, 3);
        }

        //shouldn't get here
        throw new IOException("Internal FS exception: getDataBlockIndex(i=" + i + ")");
    }
    
    /**
     * Read n'th data block and return it.
     * @param n index of block to read, in the range 0 - nl
     * @param nr index of the last block the caller could usefully access
     * @param nl the last block in the file
     * @param sequential true iff the caller is reading sequentially
     * @return
     * @throws IOException
     */    
    public Block getDataBlock(long n, long nr, long nl, boolean sequential) throws IOException {
        /*
         * The nr parameter effectively makes this a getDataBlocks call; however,
         * everything is done in terms of a single block partly for historical reasons 
         * and partly to simplify cache management.
         * However, the ability to prefetch and to read ahead means we only pay a small
         * price for doing things a block at a time.
         * 
         * If we are not reading sequentially, then we prefetch nr - n, as that is all the caller can use,
         * and we don't know where the next read will be.
         * 
         * If we are reading sequentially then we prefetch nl - n, This likely will be reduced to the
         * maximum block transfer count but, in that case, a read ahead request should be issued.
         */
        int preFetch = (int) (sequential ? nl - n : nr - n);
        final Block result =  fs.readBlock(getDataBlockNr(n), preFetch);
        return result;
    }
    
    /**
     * @param n 
     * @return {@link ByteBuffer} containing data in block
     * @throws IOException
     */
    public Block getDataBlock(long n) throws IOException {
        return getDataBlock(n, n, n, false);
    }
    
    /**
     * A new block has been allocated for the inode, so register it (the
     * <code>i</code> th block of the inode is the block at
     * <code>blockNr</code>
     *
     * [Naming convention used: in the code, a <code>...BlockNr</code> always
     * means an absolute block nr (of the filesystem), while a
     * <code>...BlockIndex</code> means an index relative to the beginning of
     * a block]
     *
     * @param i the ith block of the inode has been reserved
     * @param blockNr the block (in the filesystem) that has been reserved
     */
    private final void registerBlockIndex(long i, long blockNr)
        throws FileSystemException, IOException {
        final long blockCount = getAllocatedBlockCount();
        final int indirectCount = getIndirectCount();
        long allocatedBlocks = i;
        if (i >= blockCount) {
            throw new FileSystemException("Trying to register block " + i +
                    " (counts from 0), when INode contains only " + blockCount + " blocks");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,deviceString() + Long.toString(blockNr));
        }

        setDirty(true);

        //the direct blocks (0; 11)
        if (i < 12) {
            Ext2Utils.set32(data, 40 + (int) i * 4, blockNr);
            return;
        }

        //see the indirect blocks (12; indirectCount-1)
        i -= 12;
        if (i < indirectCount) {
            long indirectBlockNr;
            //the 12th index points to the indirect block
            if (i == 0) {
                //need to reserve the indirect block itself, as this is the
                //first time it is used
                indirectBlockNr = findFreeBlock(allocatedBlocks++);
                Ext2Utils.set32(data, 40 + 12 * 4, indirectBlockNr);

                //need to blank the block so that e2fsck does not complain
                byte[] zeroes = new byte[fs.getBlockSize()]; //blank the block
                Arrays.fill(zeroes, 0, fs.getBlockSize(), (byte) 0);
                fs.writeBlock(indirectBlockNr, zeroes, false);
            } else {
                //the indirect block has already been used
                indirectBlockNr = Ext2Utils.get32(data, 40 + 12 * 4);
            }

            indirectWrite(indirectBlockNr, i, allocatedBlocks, blockNr, 1);

            return;
        }

        //see the double indirect blocks (indirectCount; doubleIndirectCount-1)
        i -= indirectCount;
        final int doubleIndirectCount = indirectCount * indirectCount;
        if (i < doubleIndirectCount) {
            long doubleIndirectBlockNr;
            //the 13th index points to the double indirect block
            if (i == 0) {
                //need to reserve the double indirect block itself
                doubleIndirectBlockNr = findFreeBlock(allocatedBlocks++);
                Ext2Utils.set32(data, 40 + 13 * 4, doubleIndirectBlockNr);

                //need to blank the block so that e2fsck does not complain
                byte[] zeroes = new byte[fs.getBlockSize()]; //blank the block
                Arrays.fill(zeroes, 0, fs.getBlockSize(), (byte) 0);
                fs.writeBlock(doubleIndirectBlockNr, zeroes, false);
            } else {
                doubleIndirectBlockNr = Ext2Utils.get32(data, 40 + 13 * 4);
            }

            indirectWrite(doubleIndirectBlockNr, i, allocatedBlocks, blockNr, 2);

            return;
        }

        //see the triple indirect blocks (doubleIndirectCount;
        // tripleIndirectCount-1)
        final int tripleIndirectCount = indirectCount * indirectCount * indirectCount;
        i -= doubleIndirectCount;
        if (i < tripleIndirectCount) {
            long tripleIndirectBlockNr;
            //the 14th index points to the triple indirect block
            if (i == 0) {
                //need to reserve the triple indirect block itself
                tripleIndirectBlockNr = findFreeBlock(allocatedBlocks++);
                Ext2Utils.set32(data, 40 + 13 * 4, tripleIndirectBlockNr);

                //need to blank the block so that e2fsck does not complain
                byte[] zeroes = new byte[fs.getBlockSize()]; //blank the block
                Arrays.fill(zeroes, 0, fs.getBlockSize(), (byte) 0);
                fs.writeBlock(tripleIndirectBlockNr, zeroes, false);
            } else {
                tripleIndirectBlockNr = Ext2Utils.get32(data, 40 + 14 * 4);
            }

            indirectWrite(tripleIndirectBlockNr, i, allocatedBlocks, blockNr, 3);
            return;
        }

        //shouldn't get here
        throw new FileSystemException("Internal FS exception: getDataBlockIndex(i=" + i + ")");
    }

    /**
     * Free the preallocated blocks
     *
     * @throws FileSystemException
     * @throws IOException
     */
    private void freePreallocatedBlocks() throws FileSystemException, IOException {
        int preallocCount = desc.getPreallocCount();
        if (preallocCount > 0) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,deviceString() + Integer.toString(preallocCount));
            }
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,deviceString() + "no allocated blocks");
            }
            return;
        }

        long prealloc512 = preallocCount * (fs.getBlockSize() / 512);
        setBlocks(getBlocks() - prealloc512);

        while (desc.getPreallocCount() > 0) {
            fs.freeBlock(desc.usePreallocBlock());
        }
    }

    /**
     * Free up the ith data block of the inode. It is neccessary to free up
     * indirect blocks as well, if the last pointer on an indirect block has
     * been freed.
     *
     * @param i
     * @throws IOException
     */
    protected synchronized void freeDataBlock(long i) throws IOException, FileSystemException {
        final long blockCount = getAllocatedBlockCount();
        final int indirectCount = getIndirectCount();

        if (i != blockCount - 1) {
            throw new IOException("Only the last block of the inode can be freed." +
                    "You were trying to free block nr. " + i + ", while inode contains " +
                    blockCount + " blocks.");
        }

        desc.setLastAllocatedBlockIndex(i - 1);

        //preallocated blocks follow the last allocated block: when the last
        // block is freed,
        //free the preallocated blocks as well
        freePreallocatedBlocks();

        long block512 = fs.getBlockSize() / 512;
        setBlocks(getBlocks() - block512);

        setDirty(true);

        //see the direct blocks (0; 11)
        if (i < 12) {
            indirectFree(Ext2Utils.get32(data, 40 + (int) i * 4), 0, 0);
            Ext2Utils.set32(data, 40 + (int) i * 4, 0);
            return;
        }

        //see the indirect blocks (12; indirectCount-1)
        i -= 12;
        if (i < indirectCount) {
            //the 12th index points to the indirect block
            indirectFree(Ext2Utils.get32(data, 40 + 12 * 4), i, 1);
            //if this was the last block on the indirect block, then delete the
            // record of
            //the indirect block from the inode
            if (i == 0) {
                Ext2Utils.set32(data, 40 + 12 * 4, 0);
            }
            return;
        }

        //see the double indirect blocks (indirectCount; doubleIndirectCount-1)
        i -= indirectCount;
        if (i < indirectCount * indirectCount) {
            //the 13th index points to the double indirect block
            indirectFree(Ext2Utils.get32(data, 40 + 13 * 4), i, 2);
            //if this was the last block on the double indirect block, then
            // delete the record of
            //the double indirect block from the inode
            if (i == 0) {
                Ext2Utils.set32(data, 40 + 13 * 4, 0);
            }
            return;
        }

        //see the triple indirect blocks (doubleIndirectCount;
        // tripleIndirectCount-1)
        i -= indirectCount * indirectCount;
        if (i < indirectCount * indirectCount * indirectCount) {
            //the 14th index points to the triple indirect block
            indirectFree(Ext2Utils.get32(data, 40 + 14 * 4), i, 3);
            //if this was the last block on the triple indirect block, then
            // delete the record of
            //the triple indirect block from the inode
            if (i == 0) {
                Ext2Utils.set32(data, 40 + 14 * 4, 0);
            }
            return;
        }

        //shouldn't get here
        throw new IOException("Internal FS exception: getDataBlockIndex(i=" + i + ")");
    }

    /**
     * Write the i. data block of the inode (i is a sequential index from the
     * beginning of the file, and not an absolute block number)
     *
     * This method assumes that the block has already been reserved.
     *
     * @param i
     * @param data
     */
    public void writeDataBlock(long i, ByteBuffer data) throws IOException {
        //see if the block is already reserved for the inode
        long blockCount = getAllocatedBlockCount();

        if (i < blockCount) {
            long blockIndex = getDataBlockNr(i);
            //overwrite the block
            fs.writeBlock(blockIndex, data, false);
        } else {
            throw new UnallocatedBlockException("Block " + i + " not yet reserved " +
                    "for the inode");
        }
    }

    /**
     * Get the number of blocks allocated so far for the inode. It is possible
     * that a new block has been allocated, but not yet been written to. In this
     * case, it is not counted by getSizeInBlocks(), because it returns the size
     * of the file in blocks, counting only written bytes
     *
     * @return
     */
    protected long getAllocatedBlockCount() {
        if (desc.getLastAllocatedBlockIndex() != -1) {
            return desc.getLastAllocatedBlockIndex() + 1;
        } else {
            return getSizeInBlocks();
        }
    }

    /**
     * Allocate the ith data block of the inode (i is a sequential index from
     * the beginning of the file, and not an absolute block number)
     */
    public synchronized void allocateDataBlock(long i) throws FileSystemException, IOException {
        if (i < getAllocatedBlockCount()) {
            throw new IOException(i + " blocks are already allocated for this inode");
        }
        if (i > getAllocatedBlockCount()) {
            throw new IOException("Allocate block " + getAllocatedBlockCount() + " first!");
        }

        long newBlock = findFreeBlock(i);

        if (logger.isLoggable(Level.FINER)) {
           logger.log(Level.FINER,deviceString() + Long.toString(newBlock));
        }

        desc.setLastAllocatedBlockIndex(i);

        registerBlockIndex(i, newBlock);
    }

    /**
     * FINDS a free block which will be the indexth block of the inode: -first
     * check the preallocated blocks -then check around the last allocated block
     * and ALLOCATES it in the block bitmap at the same time.
     *
     * Block allocation should be contiguous if possible, i.e. the new block
     * should be the one that follows the last allocated block (that's why the
     * <code>index</code> parameter is needed).
     *
     * @param index:
     *            the block to be found should be around the (index-1)th block
     *            of the inode (which is already allocated, unless index==0)
     */
    private long findFreeBlock(long index) throws IOException, FileSystemException {
        //long newBlock;
        long lastBlock = -1;
        BlockReservation reservation;

        //first, see if preallocated blocks exist
        if (desc.getPreallocCount() > 0) {
            return desc.usePreallocBlock();
        }

        //no preallocated blocks:
        //check around the last allocated block
        if (index > 0)
            lastBlock = getDataBlockNr(index - 1);
        if (lastBlock != -1) {
            for (int i = 1; i < 16; i++) {
                reservation = fs.testAndSetBlock(lastBlock + i);
                if (reservation.isSuccessful()) {
                    desc.setPreallocBlock(reservation.getBlock() + 1);
                    desc.setPreallocCount(reservation.getPreallocCount());

                    long prealloc512 =
                            (1 + reservation.getPreallocCount()) * (fs.getBlockSize() / 512);
                    setBlocks(getBlocks() + prealloc512);

                    return lastBlock + i;
                }
            }

            for (int i = -15; i < 0; i++) {
                reservation = fs.testAndSetBlock(lastBlock + i);
                if (reservation.isSuccessful()) {
                    desc.setPreallocBlock(reservation.getBlock() + 1);
                    desc.setPreallocCount(reservation.getPreallocCount());

                    long prealloc512 =
                            (1 + reservation.getPreallocCount()) * (fs.getBlockSize() / 512);
                    setBlocks(getBlocks() + prealloc512);

                    return lastBlock + i;
                }
            }
        }

        //then check the current block group from the beginning
        //(threshold=1 means: find is successul if at least one free block is
        // found)
        reservation = fs.findFreeBlocks(desc.getGroup(), 1);
        if (reservation.isSuccessful()) {
            desc.setPreallocBlock(reservation.getBlock() + 1);
            desc.setPreallocCount(reservation.getPreallocCount());

            long prealloc512 = (1 + reservation.getPreallocCount()) * (fs.getBlockSize() / 512);
            setBlocks(getBlocks() + prealloc512);

            return reservation.getBlock();
        }

        //then check the other block groups, first those that have "more" free
        // space,
        //but take a note if a non-full group is found
        long nonfullBlockGroup = -1;
        for (int i = 0; i < fs.getGroupCount(); i++) {
            if (i == desc.getGroup()) {
                continue;
            }
            long threshold =
                    (fs.getSuperblock().getBlocksPerGroup() *
                            Ext2Constants.EXT2_BLOCK_THRESHOLD_PERCENT) / 100;
            reservation = fs.findFreeBlocks(i, threshold);
            if (reservation.isSuccessful()) {
                desc.setPreallocBlock(reservation.getBlock() + 1);
                desc.setPreallocCount(reservation.getPreallocCount());

                long prealloc512 = (1 + reservation.getPreallocCount()) * (fs.getBlockSize() / 512);
                setBlocks(getBlocks() + prealloc512);

                return reservation.getBlock();
            }

            if (reservation.getFreeBlocksCount() > 0) {
                nonfullBlockGroup = i;
            }
        }

        //if no block group with at least the threshold number of free blocks
        // is found,
        //then check if there was any nonfull group
        if (nonfullBlockGroup != -1) {
            reservation = fs.findFreeBlocks(desc.getGroup(), 1);
            if (reservation.isSuccessful()) {
                desc.setPreallocBlock(reservation.getBlock() + 1);
                desc.setPreallocCount(reservation.getPreallocCount());

                long prealloc512 = (1 + reservation.getPreallocCount()) * (fs.getBlockSize() / 512);
                setBlocks(getBlocks() + prealloc512);

                return reservation.getBlock();
            }
        }

        throw new IOException("No free blocks: disk full!");
    }

    // **************** other persistent inode data *******************
    public synchronized int getMode() {
        int iMode = Ext2Utils.get16(data, 0);
        return iMode;
    }

    public synchronized void setMode(int imode) {
        Ext2Utils.set16(data, 0, imode);
        setDirty(true);
    }

    public synchronized int getUid() {
        return Ext2Utils.get16(data, 2);
    }

    public synchronized void setUid(int uid) {
        Ext2Utils.set16(data, 2, uid);
        setDirty(true);
    }

    /**
     * Return the size of the file in bytes.
     *
     * @return the size of the file in bytes
     */
    public synchronized long getSize() {
        return Ext2Utils.get32(data, 4);
    }

    public synchronized void setSize(long size) {
        Ext2Utils.set32(data, 4, size);
        setDirty(true);
    }

    /**
     * Return the size in ext2-blocks (getBlocks() returns the size in 512-byte
     * blocks, but an ext2 block can be of different size).
     *
     * @return
     */
    public long getSizeInBlocks() {
        return Ext2Utils.ceilDiv(getSize(), fs.getBlockSize());
    }

    public synchronized long getAtime() {
        return Ext2Utils.get32(data, 8);
    }

    public synchronized void setAtime(long atime) {
        Ext2Utils.set32(data, 8, atime);
        setDirty(true);
    }

    public synchronized long getCtime() {
        return Ext2Utils.get32(data, 12);
    }

    public synchronized void setCtime(long ctime) {
        Ext2Utils.set32(data, 12, ctime);
        setDirty(true);
    }

    public synchronized long getMtime() {
        return Ext2Utils.get32(data, 16);
    }

    public synchronized void setMtime(long mtime) {
        Ext2Utils.set32(data, 16, mtime);
        setDirty(true);
    }

    public synchronized long getDtime() {
        return Ext2Utils.get32(data, 20);
    }

    public synchronized void setDtime(long dtime) {
        Ext2Utils.set32(data, 20, dtime);
        setDirty(true);
    }

    public synchronized int getGid() {
        return Ext2Utils.get16(data, 24);
    }

    public synchronized void setGid(int gid) {
        Ext2Utils.set16(data, 24, gid);
        setDirty(true);
    }

    public synchronized int getLinksCount() {
        return Ext2Utils.get16(data, 26);
    }

    public synchronized void setLinksCount(int lc) {
        Ext2Utils.set16(data, 26, lc);
        setDirty(true);
    }

    /**
     * Return the size in 512-byte blocks.
     */
    public synchronized long getBlocks() {
        return Ext2Utils.get32(data, 28);
    }

    public synchronized void setBlocks(long count) {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,deviceString() + ", count: " + Long.toString(count));
        }
        Ext2Utils.set32(data, 28, count);
        setDirty(true);
    }

    //this value is set by setSize

    public synchronized long getFlags() {
        return Ext2Utils.get32(data, 32);
    }

    public synchronized void setFlags(long flags) {
        Ext2Utils.set32(data, 32, flags);
        setDirty(true);
    }

    public synchronized long getOSD1() {
        return Ext2Utils.get32(data, 36);
    }

    public synchronized void setOSD1(long osd1) {
        Ext2Utils.set32(data, 36, osd1);
        setDirty(true);
    }

    public synchronized long getGeneration() {
        return Ext2Utils.get32(data, 100);
    }

    public synchronized void setGeneration(long gen) {
        Ext2Utils.set32(data, 100, gen);
        setDirty(true);
    }

    public synchronized long getFileACL() {
        return Ext2Utils.get32(data, 104);
    }

    public synchronized void setFileACL(long acl) {
        Ext2Utils.set32(data, 104, acl);
        setDirty(true);
    }

    public synchronized long getDirACL() {
        return Ext2Utils.get32(data, 108);
    }

    public synchronized void setDirACL(long acl) {
        Ext2Utils.set32(data, 108, acl);
        setDirty(true);
    }

    public synchronized long getFAddr() {
        return Ext2Utils.get32(data, 112);
    }

    public synchronized void setFAddr(long faddr) {
        Ext2Utils.set32(data, 112, faddr);
        setDirty(true);
    }

    //TODO: return OSD2 fields (12 bytes from offset 116)

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean b) {
        dirty = b;
    }

    public synchronized boolean isLocked() {
        return locked > 0;
    }

    public synchronized void incLocked() {
        ++locked;
    }

    public synchronized void decLocked() {
        --locked;
        if (locked == 0) {
            this.notifyAll();
        }
        if (locked < 0) {
            //What!??
            locked = 0;
            throw new RuntimeException("INode has been unlocked more than locked");
        }
    }

    private INode rereadInode() throws IOException {
        int iNodeNr = getINodeNr();
        try {
            return fs.getINode(iNodeNr);
        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    /**
     * Synchronize to the inode cache to make sure that the inode does not
     * get flushed between reading it and locking it.
     * @return the locked INode instance (N.B. may differ from method target but will denote the same inode)
     */
    public INode syncAndLock()  throws IOException{
        synchronized (fs.getInodeCache()) {
            //reread the inode before synchronizing to it to make sure
            //all threads use the same instance

            INode iNode = rereadInode();

            //lock the inode into the cache so it is not flushed before synchronizing to it
            //(otherwise a new instance of INode referring to the same inode could be put
            //in the cache resulting in the possibility of two threads manipulating the same
            //inode at the same time because they would synchronize to different INode instances)
            iNode.incLocked();
            return iNode;
        }
    }
    
    private String deviceString() {
        return fs.getDevice().getId() + ", ";
    }

}
