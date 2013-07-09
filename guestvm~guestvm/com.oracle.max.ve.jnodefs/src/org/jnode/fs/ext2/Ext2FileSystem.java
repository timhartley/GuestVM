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
 * $Id: Ext2FileSystem.java 4975 2009-02-02 08:30:52Z lsantha $
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
//import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import java.util.logging.Level;

import com.sun.max.ve.logging.Logger;

import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.ReadOnlyFileSystemException;
import org.jnode.fs.ext2.cache.*;
import org.jnode.fs.spi.*;

/**
 * @author Andras Nagy
 * @author Mick Jordan
 *
 */
public class Ext2FileSystem extends AbstractFileSystem<Ext2Entry> {
    //@CONSTANT_WHEN_NOT_ZERO
    private Superblock superblock;
    //@CONSTANT_WHEN_NOT_ZERO
    private int blockSize;

    private GroupDescriptor groupDescriptors[];

    private INodeTable iNodeTables[];

    private int groupCount;

    private static final int DEFAULT_BLOCK_CACHE_SIZE = 4 * 1024 * 1024;
    private BlockCache blockCache;
    private int blockCacheSize;

    private INodeCache inodeCache;

    private static final Logger logger = Logger.getLogger(Ext2FileSystem.class.getName());

    // TODO: SYNC_WRITE should be made a parameter
    /** if true, writeBlock() does not return until the block is written to disk */
    private boolean SYNC_WRITE = true;

    private static int DEFAULT_MAX_TRANSFER = 8;
    
    private DeviceReaderThread deviceReaderThread;
    
    /**
     * The maximum number of blocks that will be read in one device transfer
     */
    // @CONSTANT_WHEN_NOT_ZERO
    private int maxTransferCount;
    
    /**
     * Constructor for Ext2FileSystem in specified readOnly mode
     *
     * @throws FileSystemException
     */
    public Ext2FileSystem(Device device, String[] options, Ext2FileSystemType type) throws FileSystemException {
        super(device, readOnly(options), type, logger);

        inodeCache = new INodeCache(50, (float) 0.75);
        
        final int transferCount = getOptionValue("mts", options);
        maxTransferCount = transferCount < 0 ? DEFAULT_MAX_TRANSFER : transferCount;
        blockCacheSize = getOptionValue("cs", options);
        if (blockCacheSize < 0) {
            blockCacheSize = DEFAULT_BLOCK_CACHE_SIZE;
        }
        deviceReaderThread = new DeviceReaderThread(shortId(device.getId()));
        deviceReaderThread.start();
        
        if (logger.isLoggable(Level.CONFIG)) {
            logger.log(Level.CONFIG, "device: " + device.getId() + ", cache size " + blockCacheSize + 
                    ", max transfer count " + maxTransferCount);
        }
    }
    
    private static String shortId(String deviceId) {
        final String[] parts = deviceId.split(":");
        return parts[1].substring(parts[1].lastIndexOf('/') + 1);
    }
    
    private static boolean readOnly(String[] options) {
        for (String option : options) {
            if (option.equals("ro")) {
                return true;
            }
        }
        return false;        
    }
    
    private static int getOptionValue(String optionKey, String[] options) {
        for (String option : options) {
            if (option.startsWith(optionKey)) {
                final int vx = option.indexOf('=');
                if (vx < 0 || vx == option.length() - 1) {
                    logger.warning("syntax error in " + optionKey + " option: " + option);
                } else {
                    String value = option.substring(vx + 1);
                    int scale =1;
                    final int len = value.length();
                    switch (value.charAt(len - 1)) {
                        case 'm': case 'M': scale = 1024 * 1024; break;
                        case 'k': case 'K': scale = 1024; break;
                        default:
                    }
                    if (scale > 1) {
                        value = value.substring(0, len - 1);
                    }
                    return Integer.parseInt(value) * scale;
                }
            }
        }
        return -1;        
    }

    public void read() throws FileSystemException {
        ByteBuffer data;

        try {
            data = ByteBuffer.allocate(Superblock.SUPERBLOCK_LENGTH);

            // skip the first 1024 bytes (bootsector) and read the superblock
            // TODO: the superblock should read itself
            api.read(1024, data);
            // superblock = new Superblock(data, this);
            superblock = new Superblock();
            superblock.read(data.array(), this);
            blockSize = superblock.getBlockSize();
            blockCache = new BlockCache(blockCacheSize, blockSize, maxTransferCount);

            // read the group descriptors
            groupCount = (int) Ext2Utils.ceilDiv(superblock.getBlocksCount(), superblock.getBlocksPerGroup());
            groupDescriptors = new GroupDescriptor[groupCount];
            iNodeTables = new INodeTable[groupCount];

            for (int i = 0; i < groupCount; i++) {
                // groupDescriptors[i]=new GroupDescriptor(i, this);
                groupDescriptors[i] = new GroupDescriptor();
                groupDescriptors[i].read(i, this);

                iNodeTables[i] = new INodeTable(this, (int) groupDescriptors[i].getInodeTable());
            }

        } catch (FileSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new FileSystemException(e);
        }

        // check for unsupported filesystem options
        // (an unsupported INCOMPAT feature means that the fs may not be mounted
        // at all)
        if (hasIncompatFeature(Ext2Constants.EXT2_FEATURE_INCOMPAT_COMPRESSION))
            throw new FileSystemException(device.getId() +
                    " Unsupported filesystem feature (COMPRESSION) disallows mounting");
        if (hasIncompatFeature(Ext2Constants.EXT2_FEATURE_INCOMPAT_META_BG))
            throw new FileSystemException(device.getId() +
                    " Unsupported filesystem feature (META_BG) disallows mounting");
        if (hasIncompatFeature(Ext2Constants.EXT3_FEATURE_INCOMPAT_JOURNAL_DEV))
            throw new FileSystemException(device.getId() +
                    " Unsupported filesystem feature (JOURNAL_DEV) disallows mounting");
        if (hasIncompatFeature(Ext2Constants.EXT3_FEATURE_INCOMPAT_RECOVER))
            throw new FileSystemException(device.getId() +
                    " Unsupported filesystem feature (RECOVER) disallows mounting");

        // an unsupported RO_COMPAT feature means that the filesystem can only
        // be mounted readonly
        if (hasROFeature(Ext2Constants.EXT2_FEATURE_RO_COMPAT_LARGE_FILE)) {
            logger.info(device.getId() + " Unsupported filesystem feature (LARGE_FILE) forces readonly mode");
            setReadOnly(true);
        }
        if (hasROFeature(Ext2Constants.EXT2_FEATURE_RO_COMPAT_BTREE_DIR)) {
            logger.info(device.getId() + " Unsupported filesystem feature (BTREE_DIR) forces readonly mode");
            setReadOnly(true);
        }

        // if the filesystem has not been cleanly unmounted, mount it readonly
        if (superblock.getState() == Ext2Constants.EXT2_ERROR_FS) {
            logger.info(device.getId() + " Filesystem has not been cleanly unmounted, mounting it readonly");
            System.err.println(device.getId() + " Filesystem has not been cleanly unmounted, mounting it readonly");
            setReadOnly(true);
        }

        logger.info(device.getId() + " mounting fs r/w");
        
        // if the filesystem has been mounted R/W, set it to "unclean"
        if (!isReadOnly()) {
            superblock.setState(Ext2Constants.EXT2_ERROR_FS);
        }
        // Mount successfull, update some superblock informations.
        superblock.setMntCount(superblock.getMntCount() + 1);
        superblock.setMTime(Ext2Utils.encodeDate(new Date()));
        superblock.setWTime(Ext2Utils.encodeDate(new Date()));
        if (logger.isLoggable(Level.FINER)) {
            // This causes bootstrap problems as it causes recursive initialization
            //SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
            logger.log(Level.FINER, device.getId() + " superblock: " + "\n" +
                "  #Mount: " + superblock.getMntCount() + "\n" +
                "  #MaxMount: " + superblock.getMaxMntCount() + "\n" + "  Last mount time: " +
                //sdf.format(Ext2Utils.decodeDate(superblock.getMTime()).getTime()) + "\n" +
                superblock.getMTime() +
                "  Last write time: " +
                //sdf.format(Ext2Utils.decodeDate(superblock.getWTime()).getTime()) + "\n" +
                superblock.getWTime() +
                "  #blocks: " + superblock.getBlocksCount() + "\n" +
                "  #blocks/group: " + superblock.getBlocksPerGroup() + "\n" +
                "  #block groups: " + groupCount + "\n" +
                "  block size: " + superblock.getBlockSize() + "\n" +
                "  #inodes: " + superblock.getINodesCount() + "\n" +
                "  #inodes/group: " + superblock.getINodesPerGroup());
        }
    }

    public void create(BlockSize blockSize) throws FileSystemException {
        try {
            // create the superblock
            superblock = new Superblock();
            superblock.create(blockSize, this);
            blockCache = new BlockCache(blockCacheSize, superblock.getBlockSize(), maxTransferCount);

            // create the group descriptors
            groupCount = (int) Ext2Utils.ceilDiv(superblock.getBlocksCount(), superblock.getBlocksPerGroup());
            groupDescriptors = new GroupDescriptor[groupCount];

            iNodeTables = new INodeTable[groupCount];

            for (int i = 0; i < groupCount; i++) {
                groupDescriptors[i] = new GroupDescriptor();
                groupDescriptors[i].create(i, this);
            }

            // create each block group:
            // create the block bitmap
            // create the inode bitmap
            // fill the inode table with zeroes
            for (int i = 0; i < groupCount; i++) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, device.getId() + " creating group " + i);
                }

                byte[] blockBitmap = new byte[blockSize.getSize()];
                byte[] inodeBitmap = new byte[blockSize.getSize()];

                // update the block bitmap: mark the metadata blocks allocated
                long iNodeTableBlock = groupDescriptors[i].getInodeTable();
                long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);
                int metadataLength =
                        (int) (firstNonMetadataBlock - (superblock.getFirstDataBlock() + i *
                                superblock.getBlocksPerGroup()));
                for (int j = 0; j < metadataLength; j++)
                    BlockBitmap.setBit(blockBitmap, j);

                // set the padding at the end of the last block group
                if (i == groupCount - 1) {
                    for (long k = superblock.getBlocksCount(); k < groupCount * superblock.getBlocksPerGroup(); k++)
                        BlockBitmap.setBit(blockBitmap, (int) (k % superblock.getBlocksPerGroup()));
                }

                // update the inode bitmap: mark the special inodes allocated in
                // the first block group
                if (i == 0)
                    for (int j = 0; j < superblock.getFirstInode() - 1; j++)
                        INodeBitmap.setBit(inodeBitmap, j);

                // create an empty inode table
                byte[] emptyBlock = new byte[blockSize.getSize()];
                for (long j = iNodeTableBlock; j < firstNonMetadataBlock; j++)
                    writeBlock(j, emptyBlock, false);

                iNodeTables[i] = new INodeTable(this, (int) iNodeTableBlock);

                writeBlock(groupDescriptors[i].getBlockBitmap(), blockBitmap, false);
                writeBlock(groupDescriptors[i].getInodeBitmap(), inodeBitmap, false);
            }

            logger.info(device.getId() + " superblock.getBlockSize(): " + superblock.getBlockSize());

            buildRootEntry();

            // write everything to disk
            flush();

        } catch (IOException ioe) {
            throw new FileSystemException("Unable to create filesystem", ioe);
        }

    }

    /**
     * Flush all changed structures to the device.
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        logger.info(device.getId() + " flushing the contents of the filesystem");
        // update the inodes
        synchronized (inodeCache) {
            try {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, device.getId() + " inodecache size: " + inodeCache.size());
                }
                for (INode iNode : inodeCache.values()) {
                    iNode.flush();
                }
            } catch (FileSystemException ex) {
                final IOException ioe = new IOException();
                ioe.initCause(ex);
                throw ioe;
            }
        }

        // update the group descriptors and the superblock copies
        updateFS();

        // flush the blocks
        synchronized (blockCache) {
            for (Block block : blockCache.values()) {
                block.flush();
            }
        }

        logger.info(device.getId() + " filesystem flushed");
    }

    protected void updateFS() throws IOException {
        // updating one group descriptor updates all its copies
        for (int i = 0; i < groupCount; i++)
            groupDescriptors[i].updateGroupDescriptor();
        superblock.update();
    }

    @Override
    public void close() throws IOException {
        // mark the filesystem clean
        superblock.setState(Ext2Constants.EXT2_VALID_FS);
        super.close();
    }

    /**
     * @see org.jnode.fs.spi.AbstractFileSystem#createRootEntry()
     */
    @Override
    public Ext2Entry createRootEntry() throws IOException {
        try {
            return new Ext2Entry(getINode(Ext2Constants.EXT2_ROOT_INO), "/", Ext2Constants.EXT2_FT_DIR, this, null);
        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    /**
     * Return the block size of the file system
     */
    public int getBlockSize() {
        if (blockSize == 0) {
            blockSize = superblock.getBlockSize();
        }
        return blockSize;
    }

    /**
     * Read a data block and put it in the cache if it is not yet cached,
     * otherwise get it from the cache.
     *
     * Synchronized access to the blockCache is important as the bitmap
     * operations are synchronized to the blocks so at any point in time it has to 
     * be sure that no two copies of the same block are stored in the cache.
     *
     * @param nr number of block to read
     * @param maxPreFetch maximum number of additional blocks to read
     * @return <code>ByteBuffer</code> for the block
     */
    protected Block readBlock(long nr, int maxPreFetch) throws IOException {
        if (isClosed())
            throw new IOException("FS closed (fs instance: " + this + ")");

        Block block;
        Block readAheadBlock = null;

        synchronized (blockCache) {
            // check if the block has already been retrieved
            if ((block = blockCache.get(nr)) != null) {
                return block.lock();
            } else {
                int requestedTransferCount = maxPreFetch + 1;
                int transferCount = checkTransferCount(requestedTransferCount);
                block = blockCache.getBlock(this, nr, transferCount);
                if (transferCount < requestedTransferCount) {
                    readAheadBlock = blockCache.getBlock(this, nr + transferCount, checkTransferCount(requestedTransferCount - transferCount));
                }
            }
        }

        // sync read
        block = readBlocksFromDevice(block, false);
        
        if (readAheadBlock != null) {
            // async read for more
            readBlocksFromDevice(readAheadBlock, true);
        }

        return block.lock();
    }

    /**
     * Similar to {@link #readBlock(long, int) but zero prefetch.
     * @param nr
     * @return
     * @throws IOException
     */
    protected Block readBlock(long nr) throws IOException {
        return readBlock(nr, 0);
    }
    
    private int checkTransferCount(int request) {
        return Math.min(request, maxTransferCount);
    }
    
    private Block readBlocksFromDevice(Block block, boolean async) throws IOException {
        ReadRequest protoRequest = new ReadRequest(block);
        final ReadRequest request = deviceReaderThread.queueRequest(protoRequest);
        if (async) {
            return null;
        }
        if (protoRequest != request) {
            // the same request was already in flight so release the incoming block
            synchronized (blockCache) {
                blockCache.releaseBlock(protoRequest.block);
            }
        }
        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        if (request.ex != null) {
            throw request.ex;
        } else {
            return request.block;
        }
    }
    
    private class ReadRequest {
        Block block;
        IOException ex;
        boolean done;
        
        ReadRequest(Block block) {
            this.block = block;
        }
    }
    
    private class DeviceReaderThread extends Thread {
        private LinkedList<ReadRequest> requests = new LinkedList<ReadRequest>();
        
        DeviceReaderThread(String id) {
            setName("Ext2R-" + id);
            setDaemon(true);
        }
        
        synchronized ReadRequest queueRequest(ReadRequest request) {
            ReadRequest ifr;
            if ((ifr = isInflight(request)) != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, device.getId() + " request for block " + request.block.getBlockNr() + " in flight");
                }                
                return ifr;
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, device.getId() + " queuing request for block " + request.block.getBlockNr());
                }
                requests.add(request);
                notify();
                return request;
            }
        }
        
        private ReadRequest isInflight(ReadRequest request) {
            for (ReadRequest rrq : requests) {
                if (rrq.block.getBlockNr() == request.block.getBlockNr()) {
                    return rrq;
                }
            }
            return null;
        }
        
        @Override
        public void run() {
            while (true) {
                ReadRequest request = null;
                synchronized (this) {
                    while (requests.isEmpty()) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                    request = requests.getFirst();
                }
                try {
                    long nr = request.block.getBlockNr();
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, device.getId() + " reading block " + nr + " from disk");
                    }        
                    api.read(nr * blockSize, request.block.getBuffer());        
                    synchronized (blockCache) {
                        request.block = blockCache.put(request.block);
                    }
                } catch (IOException ex) {
                    logger.warning("read of block " + request.block.getBlockNr() + " failed");
                    request.ex = ex;
                } finally {
                    synchronized (this) {
                        requests.remove(request);                        
                    }
                    synchronized (request) {
                        request.done = true;
                        request.notifyAll();
                    }                                        
                }
            }
        }
    }

    /**
     * Update the block in cache, or write the block to disk
     *
     * @param nr: block number
     * @param data: block data
     * @param forceWrite: if forceWrite is false, the block is only updated in
     *            the cache (if it was in the cache). If forceWrite is true, or
     *            the block is not in the cache, write it to disk.
     * @throws IOException
     */
    public void writeBlock(long nr, byte[] data, boolean forceWrite) throws IOException {
        writeBlock(nr, ByteBuffer.wrap(data), forceWrite);
    }
    
    public void writeBlock(long nr, ByteBuffer dataBuf, boolean forceWrite) throws IOException {
        if (isClosed())
            throw new IOException("FS closed");

        if (isReadOnly())
            throw new ReadOnlyFileSystemException("Filesystem is mounted read-only!");

        Block block;

        int blockSize = superblock.getBlockSize();
        // check if the block is in the cache
        synchronized (blockCache) {
            if ((block = blockCache.get(nr)) != null) {
                // update the data in the cache
                block.setBuffer(dataBuf);
                if (forceWrite || SYNC_WRITE) {
                    // write the block to disk, using the direct buffer
                    api.write(nr * blockSize, block.getBuffer());
                    block.setDirty(false);

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, device.getId() + " writing block " + nr + " to disk");
                    }
                } else
                    block.setDirty(true);
            } else {
                // If the block was not in the cache, I see no reason to put it
                // in the cache when it is written.
                // It is simply written to disk.
                api.write(nr * blockSize, dataBuf);
                // timedWrite(nr, data);
            }
        }
    }

    public Superblock getSuperblock() {
        return superblock;
    }

    /**
     * Return the inode numbered inodeNr (the first inode is #1)
     *
     * Synchronized access to the inodeCache is important as the file/directory
     * operations are synchronized to the inodes, so at any point in time it has
     * to be sure that only one instance of any inode is present in the
     * filesystem.
     */
    public INode getINode(int iNodeNr) throws IOException, FileSystemException {
        if ((iNodeNr < 1) || (iNodeNr > superblock.getINodesCount()))
            throw new FileSystemException("INode number (" + iNodeNr + ") out of range (0-" +
                    superblock.getINodesCount() + ")");

        Integer key = new Integer(iNodeNr);

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, device.getId() + " iNodeCache size: " + inodeCache.size());
        }

        synchronized (inodeCache) {
            // check if the inode is already in the cache
            if (inodeCache.containsKey(key))
                return inodeCache.get(key);
        }

        // move the time consuming disk read out of the synchronized block
        // (see comments at getBlock())

        int group = (int) ((iNodeNr - 1) / superblock.getINodesPerGroup());
        int index = (int) ((iNodeNr - 1) % superblock.getINodesPerGroup());

        // get the part of the inode table that contains the inode
        INodeTable iNodeTable = iNodeTables[group];
        INode result = new INode(this, new INodeDescriptor(iNodeTable, iNodeNr, group, index), true);

        synchronized (inodeCache) {
            // check if the inode is still not in the cache
            if (!inodeCache.containsKey(key)) {
                inodeCache.put(key, result);
                return result;
            } else
                return inodeCache.get(key);
        }
    }

    /**
     * Checks whether block <code>blockNr</code> is free, and if it is, then
     * allocates it with preallocation.
     * 
     * @param blockNr
     * @return
     * @throws IOException
     */
    public BlockReservation testAndSetBlock(long blockNr) throws IOException {

        if (blockNr < superblock.getFirstDataBlock() || blockNr >= superblock.getBlocksCount())
            return new BlockReservation(false, -1, -1);
        int group = translateToGroup(blockNr);
        int index = translateToIndex(blockNr);

        /*
         * Return false if the block is not a data block but a filesystem
         * metadata block, as the beginning of each block group is filesystem
         * metadata: superblock copy (if present) block bitmap inode bitmap
         * inode table Free blocks begin after the inode table.
         */
        long iNodeTableBlock = groupDescriptors[group].getInodeTable();
        long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);

        if (blockNr < firstNonMetadataBlock)
            return new BlockReservation(false, -1, -1);

        Block bitmapBlock = readBlock(groupDescriptors[group].getBlockBitmap());
        try {
            synchronized (bitmapBlock) {
                BlockReservation result =
                        BlockBitmap.testAndSetBlock(bitmapBlock.getBuffer(), index);
                // update the block bitmap
                if (result.isSuccessful()) {
                    writeBlock(groupDescriptors[group].getBlockBitmap(), bitmapBlock.getBuffer(),
                            false);
                    modifyFreeBlocksCount(group, -1 - result.getPreallocCount());
                    // result.setBlock(
                    // result.getBlock()+superblock.getFirstDataBlock() );
                    result.setBlock(blockNr);
                }
                return result;
            }
        } finally {
            bitmapBlock.unlock();
        }

    }

    /**
     * Create a new INode
     *
     * @param preferredBlockBroup: first try to allocate the inode in this block
     *            group
     * @return
     */
    protected INode createINode(int preferredBlockBroup, int fileFormat, int accessRights, int uid, int gid)
        throws FileSystemException, IOException {
        if (preferredBlockBroup >= superblock.getBlocksCount())
            throw new FileSystemException("Block group " + preferredBlockBroup + " does not exist");

        int groupNr = preferredBlockBroup;
        // first check the preferred block group, if it has any free inodes
        INodeReservation res = findFreeINode(groupNr);

        // if no free inode has been found in the preferred block group, then
        // try the others
        if (!res.isSuccessful()) {
            for (groupNr = 0; groupNr < superblock.getBlockGroupNr(); groupNr++) {
                res = findFreeINode(groupNr);
                if (res.isSuccessful()) {
                    break;
                }
            }
        }

        if (!res.isSuccessful())
            throw new FileSystemException("No free inodes found!");

        // a free inode has been found: create the inode and write it into the
        // inode table
        INodeTable iNodeTable = iNodeTables[preferredBlockBroup];
        // byte[] iNodeData = new byte[INode.INODE_LENGTH];
        int iNodeNr = res.getINodeNr((int) superblock.getINodesPerGroup());
        INode iNode = new INode(this, new INodeDescriptor(iNodeTable, iNodeNr, groupNr, res.getIndex()), false);
        iNode.create(fileFormat, accessRights, uid, gid);
        // trigger a write to disk
        iNode.update();

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,
                    device.getId() + " ** NEW INODE ALLOCATED: inode number: " +
                            iNode.getINodeNr());
        }

        // put the inode into the cache
        synchronized (inodeCache) {
            Integer key = new Integer(iNodeNr);
            if (inodeCache.containsKey(key))
                throw new FileSystemException("Newly allocated inode is already in the inode cache!?");
            else
                inodeCache.put(key, iNode);
        }

        return iNode;
    }

    /**
     * Find a free INode in the inode bitmap and allocate it
     * 
     * @param blockGroup
     * @return
     * @throws IOException
     */
    protected INodeReservation findFreeINode(int blockGroup) throws IOException {
        GroupDescriptor gdesc = groupDescriptors[blockGroup];
        if (gdesc.getFreeInodesCount() > 0) {
            Block bitmapBlock = readBlock(gdesc.getInodeBitmap());

            try {
                synchronized (bitmapBlock) {
                    INodeReservation result = INodeBitmap.findFreeINode(bitmapBlock.getBuffer());

                    if (result.isSuccessful()) {
                        // update the inode bitmap
                        writeBlock(gdesc.getInodeBitmap(), bitmapBlock.getBuffer(), true);
                        modifyFreeInodesCount(blockGroup, -1);

                        result.setGroup(blockGroup);

                        return result;
                    }
                }
            } finally {
                bitmapBlock.unlock();
            }
        }
        return new INodeReservation(false, -1);
    }

    protected int translateToGroup(long i) {
        return (int) ((i - superblock.getFirstDataBlock()) / superblock.getBlocksPerGroup());
    }

    protected int translateToIndex(long i) {
        return (int) ((i - superblock.getFirstDataBlock()) % superblock.getBlocksPerGroup());
    }

    /**
     * Modify the number of free blocks in the block group
     *
     * @param group
     * @param diff can be positive or negative
     */
    protected void modifyFreeBlocksCount(int group, int diff) {
        GroupDescriptor gdesc = groupDescriptors[group];
        gdesc.setFreeBlocksCount(gdesc.getFreeBlocksCount() + diff);

        superblock.setFreeBlocksCount(superblock.getFreeBlocksCount() + diff);
    }

    /**
     * Modify the number of free inodes in the block group
     *
     * @param group
     * @param diff can be positive or negative
     */
    protected void modifyFreeInodesCount(int group, int diff) {
        GroupDescriptor gdesc = groupDescriptors[group];
        gdesc.setFreeInodesCount(gdesc.getFreeInodesCount() + diff);

        superblock.setFreeInodesCount(superblock.getFreeInodesCount() + diff);
    }

    /**
     * Modify the number of used directories in a block group
     *
     * @param group
     * @param diff
     */
    protected void modifyUsedDirsCount(int group, int diff) {
        GroupDescriptor gdesc = groupDescriptors[group];
        gdesc.setUsedDirsCount(gdesc.getUsedDirsCount() + diff);
    }

    /**
     * Free up a block in the block bitmap.
     *
     * @param blockNr
     * @throws FileSystemException
     * @throws IOException
     */
    public void freeBlock(long blockNr) throws FileSystemException, IOException {
        if (blockNr < 0 || blockNr >= superblock.getBlocksCount())
            throw new FileSystemException("Attempt to free nonexisting block (" + blockNr + ")");

        int group = translateToGroup(blockNr);
        int index = translateToIndex(blockNr);
        GroupDescriptor gdesc = groupDescriptors[group];

        /*
         * Throw an exception if an attempt is made to free up a filesystem
         * metadata block (the beginning of each block group is filesystem
         * metadata): superblock copy (if present) block bitmap inode bitmap
         * inode table Free blocks begin after the inode table.
         */
        long iNodeTableBlock = groupDescriptors[group].getInodeTable();
        long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);

        if (blockNr < firstNonMetadataBlock)
            throw new FileSystemException("Attempt to free a filesystem metadata block!");

            Block bitmapBlock = readBlock(gdesc.getBlockBitmap());
            synchronized (bitmapBlock) {
                BlockBitmap.freeBit(bitmapBlock.getBuffer(), index);
                // update the bitmap block
                writeBlock(groupDescriptors[group].getBlockBitmap(), bitmapBlock.getBuffer(), false);
                bitmapBlock.unlock();
                modifyFreeBlocksCount(group, 1);
            }
    }

    /**
     * Find free blocks in the block group <code>group</code>'s block bitmap.
     * First check for a whole byte of free blocks (0x00) in the bitmap, then
     * check for any free bit. If blocks are found, mark them as allocated.
     * 
     * @return the index of the block (from the beginning of the partition)
     * @param group the block group to check
     * @param threshold find the free blocks only if there are at least
     *            <code>threshold</code> number of free blocks
     */
    public BlockReservation findFreeBlocks(int group, long threshold) throws IOException {
        GroupDescriptor gdesc = groupDescriptors[group];
        // see if it's worth to check the block group at all
        if (gdesc.getFreeBlocksCount() < threshold)
            return new BlockReservation(false, -1, -1, gdesc.getFreeBlocksCount());

        /*
         * Return false if the block is not a data block but a filesystem
         * metadata block, as the beginning of each block group is filesystem
         * metadata: superblock copy (if present) block bitmap inode bitmap
         * inode table Free blocks begin after the inode table.
         */
        long iNodeTableBlock = groupDescriptors[group].getInodeTable();
        long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);
        int metadataLength =
                (int) (firstNonMetadataBlock - (superblock.getFirstDataBlock() + group *
                        superblock.getBlocksPerGroup()));
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,
                    device.getId() + " group[" + group + "].getInodeTable()=" +
                            iNodeTableBlock + ", iNodeTable.getSizeInBlocks()=" +
                            INodeTable.getSizeInBlocks(this));
            logger.log(Level.FINER, device.getId() + " metadata length for block group(" +
                    group + "): " + metadataLength);
        }

        BlockReservation result;

        Block bitmapBlock = readBlock(gdesc.getBlockBitmap());

        synchronized (bitmapBlock) {
            result = BlockBitmap.findFreeBlocks(bitmapBlock.getBuffer(), metadataLength);

            // if the reservation was successful, write the bitmap data to
            // disk within the same synchronized block
            if (result.isSuccessful()) {
                writeBlock(groupDescriptors[group].getBlockBitmap(), bitmapBlock.getBuffer(), true);
                bitmapBlock.unlock();
                modifyFreeBlocksCount(group, -1 - result.getPreallocCount());
            }
        }

        if (result.isSuccessful()) {
            result.setBlock(group * getSuperblock().getBlocksPerGroup() +
                    superblock.getFirstDataBlock() + result.getBlock());
            result.setFreeBlocksCount(gdesc.getFreeBlocksCount());
        }

        return result;
    }

    /**
     * Returns the number of groups.
     *
     * @return int
     */
    protected int getGroupCount() {
        return groupCount;
    }

    /**
     * Check whether the filesystem uses the given RO feature
     * (S_FEATURE_RO_COMPAT)
     *
     * @param mask
     * @return
     */
    protected boolean hasROFeature(long mask) {
        return (mask & superblock.getFeatureROCompat()) != 0;
    }

    /**
     * Check whether the filesystem uses the given COMPAT feature
     * (S_FEATURE_INCOMPAT)
     *
     * @param mask
     * @return
     */
    protected boolean hasIncompatFeature(long mask) {
        return (mask & superblock.getFeatureIncompat()) != 0;
    }

    /**
     * utility function for determining if a given block group has superblock
     * and group descriptor copies
     *
     * @param a positive integer
     * @param b positive integer > 1
     * @return true if an n integer exists such that a=b^n; false otherwise
     */
    private boolean checkPow(int a, int b) {
        if (a <= 1)
            return true;
        while (true) {
            if (a == b)
                return true;
            if (a % b == 0) {
                a = a / b;
                continue;
            }
            return false;
        }
    }

    /**
     * With the sparse_super option set, a filesystem does not have a superblock
     * and group descriptor copy in every block group.
     *
     * @param groupNr
     * @return true if the block group <code>groupNr</code> has a superblock
     *         and a group descriptor copy, otherwise false
     */
    protected boolean groupHasDescriptors(int groupNr) {
        if (hasROFeature(Ext2Constants.EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER))
            return (checkPow(groupNr, 3) || checkPow(groupNr, 5) || checkPow(groupNr, 7));
        else
            return true;
    }

    /**
     *
     */
    @Override
    protected FSFile createFile(FSEntry entry) throws IOException {
        Ext2Entry e = (Ext2Entry) entry;
        return new Ext2File(e.getINode());
    }

    /**
     *
     */
    @Override
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        Ext2Entry e = (Ext2Entry) entry;
        return new Ext2Directory(e);
    }

    protected FSEntry buildRootEntry() throws IOException {
        // a free inode has been found: create the inode and write it into the
        // inode table
        INodeTable iNodeTable = iNodeTables[0];
        // byte[] iNodeData = new byte[INode.INODE_LENGTH];
        int iNodeNr = Ext2Constants.EXT2_ROOT_INO;
        INode iNode = null;
        try {
            iNode = new INode(this, new INodeDescriptor(iNodeTable, iNodeNr, 0, iNodeNr - 1), false);
        } catch (FileSystemException ex) {
            // can't happen
        }
        int rights = 0xFFFF & (Ext2Constants.EXT2_S_IRWXU | Ext2Constants.EXT2_S_IRWXG | Ext2Constants.EXT2_S_IRWXO);
        iNode.create(Ext2Constants.EXT2_S_IFDIR, rights, 0, 0);
        // trigger a write to disk
        iNode.update();

        // add the inode to the inode cache
        synchronized (inodeCache) {
            inodeCache.put(new Integer(Ext2Constants.EXT2_ROOT_INO), iNode);
        }

        modifyUsedDirsCount(0, 1);

        Ext2Entry rootEntry = new Ext2Entry(iNode, "/", Ext2Constants.EXT2_FT_DIR, this, null);
        ((Ext2Directory) rootEntry.getDirectory())
                .addINode(Ext2Constants.EXT2_ROOT_INO, ".", Ext2Constants.EXT2_FT_DIR);
        ((Ext2Directory) rootEntry.getDirectory()).addINode(Ext2Constants.EXT2_ROOT_INO, "..",
                Ext2Constants.EXT2_FT_DIR);
        rootEntry.getDirectory().addDirectory("lost+found");
        return rootEntry;
    }

    protected void handleFSError(Exception e) {
        // mark the fs as having errors
        superblock.setState(Ext2Constants.EXT2_ERROR_FS);
        if (superblock.getErrors() == Ext2Constants.EXT2_ERRORS_RO)
            setReadOnly(true); // remount readonly

        if (superblock.getErrors() == Ext2Constants.EXT2_ERRORS_PANIC)
            throw new RuntimeException("EXT2 FileSystem exception", e);
    }

    /**
     * @return Returns the blockCache (outside of this class only used to
     *         synchronize to)
     */
    protected synchronized BlockCache getBlockCache() {
        return blockCache;
    }

    /**
     * @return Returns the inodeCache (outside of this class only used to
     *         syncronized to)
     */
    protected synchronized INodeCache getInodeCache() {
        return inodeCache;
    }

    public long getFreeSpace() {
        return superblock.getFreeBlocksCount() * superblock.getBlockSize();
    }

    public long getTotalSpace() {
        return superblock.getBlocksCount() * superblock.getBlockSize();
    }

    public long getUsableSpace() {
        // TODO implement me
        return -1;
    }

    @Override
    public FSEntry renameEntry(Ext2Entry from, Ext2Entry to, String newName) throws IOException {
        int fromInodeNr = from.getINode().getINodeNr();
        int toInodeNr = to.getINode().getINodeNr();
        if (fromInodeNr == toInodeNr) {
            // a simple rename
            from.setName(newName);
            return from;
        } else {
            // from can be a directory or file
            // to must be a directory
            synchronized (getInodeCache()) {
                Ext2Directory fromParentDir = (Ext2Directory) from.getParent();
                Ext2Directory toDir = (Ext2Directory) to.getDirectory();
                fromParentDir.deleteEntry(from.getName(), false);
                return toDir.addEntry(newName, from);
            }
        }
    }

}
