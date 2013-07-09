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
 * or visit www.oracle.com if you need additional FINErmation or have any
 * questions.
 */
/*
 * $Id: BlockCache.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.util.*;
import java.util.logging.Level;

import org.jnode.fs.ext2.Ext2FileSystem;

import com.sun.max.ve.logging.*;

/**
 * The file system block cache.
 * 
 * A {@link SingleBlock} stores its data using a {@link DirectByteBuffer}, a pool of which is managed
 * by some subclass of {@link ByteBufferFactory}.
 * 
 * Public methods are not explicitly synchronized, but all callers *must* synchronize on the instance 
 * before calling.
 * 
 * The basic cache actions can be logged by setting <code>-Dorg.jnode.fs.ext2.cache.Blockcache.level=FINE</code>.
 * Hits and misses can be logged with <code>level=FINER</code>
 * 
 * @author Andras Nagy
 * @author Mick Jordan
 */
public final class BlockCache {
    static final Logger logger = Logger.getLogger(BlockCache.class.getName());
    
    private int maxCacheSize;
    private int maxCacheSizeInBlocks;
    private static ByteBufferFactory byteBufferFactory;
    private LinkedHashMap<Long, Block> cache;
    private int blockSize;
    private int cushion;
    private BufferManager bufferManager;

    public BlockCache(int cacheSize, int blockSize, int maxTransferCount) {
        this.blockSize = blockSize;
        // a heuristic to avoid running out of buffers before blocks are put in the cache but
        // allocated for the data transfer
        this.cushion = maxTransferCount * (4 * Runtime.getRuntime().availableProcessors());
        
        maxCacheSize = cacheSize;
        if ((maxCacheSize % blockSize) != 0) {
            maxCacheSize += blockSize - (maxCacheSize % blockSize);
        }
        maxCacheSizeInBlocks = maxCacheSize / blockSize;
        cache = new CacheMap();
        byteBufferFactory = ByteBufferFactory.create();
        if (byteBufferFactory ==  null) {
            logger.log(Level.SEVERE, "failed to create ByteBufferFactory");
        }
        bufferManager = new CVMBufferManager();        
    }
    
    public Block put(Block block) {
        Block result = block;
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, block.toString());
        }
        
        if (block instanceof MultiBlock) {
            SingleBlock[] subBlocks = ((MultiBlock) block).getSubBlocks();
            for (SingleBlock subBlock : subBlocks) {
                cache.put(subBlock.blockNr, subBlock);
            }
            result = subBlocks[0];
        } else {
            cache.put(block.blockNr, block);
        }
        return result;
    }
    
    public Collection<Block> values() {
        return cache.values();
    }
    
    public Block get(long blockNr) {
        final Block result = cache.get(blockNr);
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, (result ==  null ? "miss " : "hit ") + blockNr);
        }
        if (result != null) {
            result.accessCount++;
        }
        return result;
    }
    
    public boolean contains(long blockNr) {
        return cache.containsKey(blockNr);
    }
    
    /**
     * Get a {@link Block}  for I/O and subsequent placement in the cache.
     * Pre-fetching is supported by passing a value for <code>maxBlocks</code>
     * that is greater than one. Note there is no guarantee that a buffer 
     * of size <code>maxBlocks</code> is available but the buffer will
     * accommodate at least one block.
     * @param fs
     * @param blockNr block number that will be read into this block
     * @param maxBlocks the maximum number of blocks to allocate the buffer for
     * @return
     */
    public Block getBlock(Ext2FileSystem fs, long blockNr, int maxBlocks) {
        ByteBuffer buffer = bufferManager.allocateBuffer(maxBlocks);
        final int nBlocks = buffer.remaining() / blockSize;
        return nBlocks == 1 ? new SingleBlock(fs, blockNr, buffer) : new MultiBlock(fs, blockNr, buffer, blockSize);
    }
    
    /**
     * Release a {@link Block} that was allocated by {@#getBlock} but not placed in the cache,
     * because some other thread got there first.
     * @param block
     */
    public void releaseBlock(Block block) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, block.toString());
        }        
        bufferManager.recycleBuffer((block.getBuffer()));
    }
    
    /**
     * Map from fs block numbers to {@link Block} instances, utilising the LRU
     * feature of {@link LinkedHashMap} to recycle blocks when the cache fills up.
     * The map maintains the LRU ordering (at modest cost) and we maintain
     * the access count. Note that in the steady state the cache is always full so
     * {@link LinkedHashMap#removeEldestEntry} will be called on *every* put.
     */
    class CacheMap extends LinkedHashMap<Long, Block> {
        
        CacheMap() {
            super(maxCacheSizeInBlocks, 0.75f, true);
        }
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Block> eldest) {
            boolean remove = false;  // result of the method - true to remove the candidate (eldest)
            if (bufferManager.freeBuffers() < cushion) {
                Block block = eldest.getValue();
                
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, block.toString());
                }

                // if this block or any sub-block of the parent MultiBlock is locked, can't remove
                Iterator<Block> iter = null;
                while (block.locked || (block.parent != null && block.parent.locked)) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "block " + block.blockNr + " locked");
                    }
                    if (iter == null) {
                        iter = values().iterator();
                    }
                    block = iter.next();
                }
                
                try {
                    block.flush();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Exception when flushing a block from the cache", e);
                    return false;
                }

                // recycle the byte buffer, and 
                if (block.parent != null) {
                    // explicitly remove the children so we will return false
                    for (SingleBlock singleBlock : ((MultiBlock) block.parent).getSubBlocks()) {
                        this.remove(singleBlock.blockNr);
                    }
                    bufferManager.recycleBuffer(block.parent.getBuffer());
                } else {
                    // if we had to find an alternative to a locked block, then don't remove eldest
                    bufferManager.recycleBuffer(block.getBuffer());
                    if (block != eldest.getValue()) {
                        this.remove(block.blockNr);
                    } else {
                        // eldest was chosen so return true
                        remove = true;
                    }
                }
            }
            return remove;
        }
    }
    
    private abstract class BufferManager {
        abstract ByteBuffer allocateBuffer(int nBlocks);
        abstract void recycleBuffer(ByteBuffer buffer);
        abstract int freeBuffers();
    }
    
    /**
     * Contiguous Virtual Memory implementation.
     * Allocate a contiguous master {@link ByteBuffer} up front and
     * slices it up as necessary. Uses a {@link BitSet} to record
     * free/used single block buffers. Since can't get from a slice
     * back to the bitmap slot, have to also keep byte buffer instances
     * in an array for recycling.
     */
    private class CVMBufferManager extends BufferManager {
        private BitSet bufferBitMap; // 1 bit per block
        private ByteBuffer masterBuffer;
        private ByteBuffer[] buffers;
        private int bitMapSize;
        int freeBuffers;
        int waiterCount;

        public CVMBufferManager() {
            masterBuffer = byteBufferFactory.allocate(maxCacheSize);
            bitMapSize = maxCacheSize / blockSize;
            buffers = new ByteBuffer[bitMapSize];
            bufferBitMap = new BitSet(bitMapSize);
            bufferBitMap.set(0, bitMapSize);
            freeBuffers = bitMapSize;
        }
        
        @Override
        ByteBuffer allocateBuffer(int nBlocks) {
            int aBlocks;   // number actually allocated
            int b = 0;       // when >= 0 slot in buffer array/slice list
            do {                
                b = findBlocks(nBlocks);
                if (b < 0) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "failed to find " + nBlocks + ", max " + (-b - 1) + ", free " +
                                logFreeBlocks());
                    }
                    aBlocks = -b - 1;
                    if (aBlocks == 0) {
                        // We ran out despite the cushion
                        waiterCount++;
                        try {
                            // release blockCache lock and wait
                            BlockCache.this.wait();
                        } catch (InterruptedException ex) {
                        }
                        waiterCount--;
                    } else {
                        // this will succeed; it's less than requested but we'll take it.
                        // an alternative would be to retry for the requested amount 
                        // by changing previous conditional.
                        b = findBlocks(aBlocks);
                    }
                } else {
                    // got what we requested
                    aBlocks = nBlocks;
                }
            } while (b < 0);

            masterBuffer.position(b * blockSize);
            ByteBuffer result = masterBuffer.slice();
            result.limit(aBlocks * blockSize);
            assert buffers[b] == null;
            buffers[b] = result;
            masterBuffer.position(0);
            freeBuffers -= aBlocks;
            return result;
        }
        
        @Override
        void recycleBuffer(ByteBuffer buffer) {
            for (int i = 0; i < bitMapSize; i++) {
                if (buffers[i] == buffer) {
                    int nBlocks = buffer.remaining() / blockSize;
                    bufferBitMap.set(i, i + nBlocks);
                    buffers[i] = null;
                    freeBuffers += nBlocks;
                    if (waiterCount > 0) {
                        BlockCache.this.notifyAll();
                    }
                    return;
                }
            }
            assert false;
        }
        
        @Override
        int freeBuffers() {
            return freeBuffers;
        }
        
        /**
         * Tries to find n contiguous blocks using best fit, then first fit.
         * 
         * @param n
         * @return index of first free block or -(n + 1) if not found, where n is largest
         */
        private int findBlocks(int n) {
            boolean best = true;
            int max = 0;
            while (true) {
                 int i = 0;
                while (i < bitMapSize) {
                    int j = bufferBitMap.nextSetBit(i);
                    if (j < 0) {
                        break;
                    }
                    i = bufferBitMap.nextClearBit(j);
                    final int m = i - j;
                    if (m > max) {
                        max = m;
                    }
                    final boolean hit = best ? m == n : m >= n;
                    if (hit) {
                        bufferBitMap.clear(j, j + n);
                        return j;
                    }
                }
                if (!best) {
                    break;
                } else {
                    best = false;
                }
            }
            return -(max + 1);
        }
        
        private String logFreeBlocks() {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            sb.append('{');
            int i = 0;
            while (i < bitMapSize) {
                int j = bufferBitMap.nextSetBit(i);
                if (j < 0) {
                    break;
                }
                i = bufferBitMap.nextClearBit(j);
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(j);
                if ((i - j) > 1) {
                  sb.append('-');
                  sb.append(i);
                }
            }
            sb.append('}');
            return sb.toString();
        }
        
        
    }
    
    /**
     * A {@link MultiBlock} supports pre-fetching of a range of blocks efficiently,
     * It maintains a single contiguous direct buffer that is used to read the blocks
     * and a set of {@link Block block} instances that subdivide the multiblock
     * and are put in the {@link BlockCache} to keep the rest of the system happy.
     * 
     * N.B. Only the constituent {@link SingleBlock} instances should ever be put in the cache
     * or handed out to clients. The {@link MultiBlock} instances should be entirely private to the
     * {@link BlockCache} implementation
     * 
     */
    static class MultiBlock extends Block {
        private SingleBlock[] subBlocks;
        
        MultiBlock(Ext2FileSystem fs, long blockNr, ByteBuffer buffer, int blockSize) {
            super(fs, blockNr, buffer);
            
            int numBlocks = buffer.remaining() / blockSize;
            subBlocks = new SingleBlock[numBlocks];
            for (int i = 0; i < numBlocks; i++) {
                buffer.position(i * blockSize);
                ByteBuffer slice = buffer.slice();
                slice.limit(blockSize);
                subBlocks[i] = new SingleBlock(fs, blockNr + i, slice);
                subBlocks[i].parent = this;
            }
            buffer.position(0);
        }
        
        SingleBlock[] getSubBlocks() {
            return subBlocks;
        }
        
        @Override
        protected void childLocked(Block child) {
            // if any child is locked the whole group is locked
            locked = true;
        }
        
        @Override
        protected synchronized void childUnlocked(Block child) {
            for (SingleBlock subBlock : subBlocks) {
                if (subBlock.locked) {
                    return;
                }
            }
            // no blocks locked, so unlock group
            locked = false;
        }
        
        
        @Override
        public void flush() {
            throw new IllegalStateException("MultiBlock.flush should never be called");
        }
        
        @Override
        public void setBuffer(ByteBuffer data) {
            throw new IllegalStateException("MultiBlock.setBuffer should never be called");
        }
        
        @Override
        public String toString() {
            return "MB(" + subBlocks.length + "):" + super.toString();
        }
        
    }
}
