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
 * $Id: Ext2Directory.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.logging.Level;

import com.sun.max.ve.logging.Logger;

import org.jnode.fs.FSEntry;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.spi.AbstractFSDirectory;
import org.jnode.fs.spi.AbstractFileSystem;
import org.jnode.fs.spi.FSEntryTable;
import org.jnode.fs.util.FSUtils;

/**
 * @author Andras Nagy
 * @author Mick Jordan
 */
public class Ext2Directory extends AbstractFSDirectory {

    INode iNode;

    private Ext2Entry entry;

    private static final Logger logger = Logger.getLogger(Ext2Directory.class.getName());

    /**
     * @param entry
     *            the Ext2Entry representing this directory
     */
    public Ext2Directory(Ext2Entry entry) throws IOException {
        super((Ext2FileSystem) entry.fileSystem);
        this.iNode = entry.getINode();
        Ext2FileSystem fs = (Ext2FileSystem) entry.fileSystem;
        this.entry = entry;
        boolean readOnly;
        if ((iNode.getFlags() & Ext2Constants.EXT2_INDEX_FL) == 1)
            readOnly = true; //force readonly
        else
            readOnly = fs.isReadOnly();
        setRights(true, !readOnly);

        if (logger.isLoggable(Level.FINE)) {
            doLog(Level.FINE, "directory size: " + iNode.getSize());
        }
    }

    /**
     * Method to create a new ext2 directory entry from the given name
     *
     * @param name
     * @Param fsEntry non-null for a rename (move)
     * @return @throws
     *         IOException
     */
    @Override
    public FSEntry createDirectoryEntry(String name, FSEntry fsEntry) throws IOException {
        if (!canWrite())
            throw new IOException("Filesystem or directory is mounted read-only!");

        //create a new iNode for the file
        //TODO: access rights, file type, UID and GID should be passed through
        // the FSDirectory interface
        INode newINode;
        Ext2DirectoryRecord dr;
        Ext2Entry newEntry;
        Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
        try {
            int rights =
                    0xFFFF & (Ext2Constants.EXT2_S_IRWXU | Ext2Constants.EXT2_S_IRWXG | Ext2Constants.EXT2_S_IRWXO);
            newINode = fs.createINode((int) iNode.getGroup(), Ext2Constants.EXT2_S_IFDIR, rights, 0, 0);

            dr = new Ext2DirectoryRecord(fs, newINode.getINodeNr(), Ext2Constants.EXT2_FT_DIR, name);

            addDirectoryRecord(dr);

            newINode.setLinksCount(newINode.getLinksCount() + 1);

            newEntry = new Ext2Entry(newINode, name, Ext2Constants.EXT2_FT_DIR, fs, this);

            //add "."
            Ext2Directory newDir = new Ext2Directory(newEntry);
            Ext2DirectoryRecord drThis =
                    new Ext2DirectoryRecord(fs, newINode.getINodeNr(), Ext2Constants.EXT2_FT_DIR, ".");
            newINode.setLinksCount(2);
            newDir.addDirectoryRecord(drThis);

            //add ".."
            long parentINodeNr = ((Ext2Directory) entry.getDirectory()).getINode().getINodeNr();
            Ext2DirectoryRecord drParent = new Ext2DirectoryRecord(fs, parentINodeNr, Ext2Constants.EXT2_FT_DIR, "..");
            newDir.addDirectoryRecord(drParent);

            //increase the reference count for the parent directory
            INode parentINode = fs.getINode((int) parentINodeNr);
            parentINode.setLinksCount(parentINode.getLinksCount() + 1);

            //update the number of used directories in the block group
            int group = (int) ((newINode.getINodeNr() - 1) / fs.getSuperblock().getINodesPerGroup());
            fs.modifyUsedDirsCount(group, 1);

            //update the new inode
            newINode.update();
        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }

        return newEntry;
    }

    /**
     * Abstract method to create a new ext2 file entry from the given name
     *
     * @param name
     * @return @throws
     *         IOException
     */
    @Override
    public FSEntry createFileEntry(String name) throws IOException {
        if (!canWrite())
            throw new IOException("Filesystem or directory is mounted read-only!");

        //create a new iNode for the file
        //TODO: access rights, file type, UID and GID should be passed through
        // the FSDirectory interface
        INode newINode;
        Ext2DirectoryRecord dr;
        Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
        try {
            int rights =
                    0xFFFF & (Ext2Constants.EXT2_S_IRWXU | Ext2Constants.EXT2_S_IRWXG | Ext2Constants.EXT2_S_IRWXO);
            newINode = fs.createINode((int) iNode.getGroup(), Ext2Constants.EXT2_S_IFREG, rights, 0, 0);

            dr = new Ext2DirectoryRecord(fs, newINode.getINodeNr(), Ext2Constants.EXT2_FT_REG_FILE, name);

            addDirectoryRecord(dr);

            newINode.setLinksCount(newINode.getLinksCount() + 1);

        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
        return new Ext2Entry(newINode, name, Ext2Constants.EXT2_FT_REG_FILE, fs, this);
    }

    @Override
    public void deleteEntry(String name, boolean deleteContents)  throws IOException {
        Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
        Ext2DirectoryRecord dr = new Ext2DirectoryRecord(fs, 0, Ext2Constants.EXT2_FT_REG_FILE, name);
        // Setting iNode to 0 effectively removes the record
        //TODO reclamation of file space iff deleteContents == true
        try {
            replaceDirectoryRecord(dr, dr);
        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    @Override
    public void renameEntry(String oldName, String newName)  throws IOException {
        Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
        Ext2DirectoryRecord oldDr = new Ext2DirectoryRecord(fs, -1, Ext2Constants.EXT2_FT_REG_FILE, oldName);
        Ext2DirectoryRecord newDr = new Ext2DirectoryRecord(fs, -1, Ext2Constants.EXT2_FT_REG_FILE, newName);
        try {
            replaceDirectoryRecord(oldDr, newDr);
        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    @Override
    public FSEntry addEntry(String name, FSEntry fsEntry)  throws IOException {
        Ext2Entry ext2Entry = (Ext2Entry) fsEntry;
        return addINode(ext2Entry.getINode().getINodeNr(), name, ext2Entry.getType());
    }

    /**
     * Attach an inode to a directory (not used normally, only during fs
     * creation)
     *
     * @param iNodeNr
     * @return @throws
     *         IOException
     */
    protected FSEntry addINode(int iNodeNr, String linkName, int fileType) throws IOException {
        if (!canWrite())
            throw new IOException("Filesystem or directory is mounted read-only!");

        //TODO: access rights, file type, UID and GID should be passed through
        // the FSDirectory interface
        Ext2DirectoryRecord dr;
        Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
        try {
            dr = new Ext2DirectoryRecord(fs, iNodeNr, fileType, linkName);
            addDirectoryRecord(dr);

            // update the directory inode
            iNode.update();

            INode linkedINode = fs.getINode(iNodeNr);

            linkedINode.setLinksCount(linkedINode.getLinksCount() + 1);

            return new Ext2Entry(linkedINode, linkName, fileType, fs, this);

        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    /**
     * Adds a new directory record to the directory file.
     * @param dr
     * @throws IOException
     * @throws FileSystemException
     */
    private void addDirectoryRecord(Ext2DirectoryRecord dr) throws IOException, FileSystemException {
        iNode = iNode.syncAndLock();
        //a single inode may be represented by more than one Ext2Directory instances,
        //but each will use the same instance of the underlying inode (see Ext2FileSystem.getINode()),
        //so synchronize to the inode.
        synchronized (iNode) {
            try {
                Ext2File dir = new Ext2File(iNode); //read itself as a file

                /* Find the last directory record (if any)
                 * This is quite inefficient as it reads the directory file sequentially.
                 */
                Ext2FSEntryIterator iterator = new Ext2FSEntryIterator(iNode);
                Ext2DirectoryRecord rec = null;
                while (iterator.hasNext()) {
                    rec = iterator.nextDirectoryRecord();
                }

                Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
                if (rec != null) {
                    long lastPos = rec.getFileOffset();
                    long lastLen = rec.getRecLen();

                    //truncate the last record to its minimal size (cut the padding from the end)
                    rec.truncateRecord();

                    //directoryRecords may not extend over block boundaries:
                    //see if the new record fits in the same block after truncating the last record
                    long remainingLength = fs.getBlockSize() - (lastPos % fs.getBlockSize()) - rec.getRecLen();
                    if (logger.isLoggable(Level.FINER)) {
                        doLog(Level.FINER, "LAST-1 record: begins at: " + lastPos
                                + ", length: " + lastLen);
                        doLog(Level.FINER, "LAST-1 truncated length: " + rec.getRecLen());
                        doLog(Level.FINER, "Remaining length: " + remainingLength);
                    }
                    if (remainingLength >= dr.getRecLen()) {
                        //write back the last record truncated
                        //TODO optimize it also to use ByteBuffer at lower level
                        ByteBuffer buf = ByteBuffer.wrap(rec.getData(), rec.getOffset(), rec.getRecLen());
                        dir.write(lastPos, buf);
                        //                      dir.write(lastPos, rec.getData(), rec.getOffset(), rec
                        //                      .getRecLen());

                        //pad the end of the new record with zeroes
                        dr.expandRecord(lastPos + rec.getRecLen(), lastPos + rec.getRecLen() + remainingLength);

                        //append the new record at the end of the list
                        //TODO optimize it also to use ByteBuffer at lower level
                        buf = ByteBuffer.wrap(dr.getData(), dr.getOffset(), dr.getRecLen());
                        dir.write(lastPos + rec.getRecLen(), buf);
                        //                      dir.write(lastPos + rec.getRecLen(), dr.getData(), dr
                        //                      .getOffset(), dr.getRecLen());

                        if (logger.isLoggable(Level.FINER)) {
                            doLog(Level.FINER, "addDirectoryRecord(): LAST   record: begins at: "
                                            + (rec.getFileOffset() + rec.getRecLen())
                                            + ", length: "
                                            + dr.getRecLen());
                        }
                    } else {
                        //the new record must go to the next block
                        //(the previously last record (rec) stays padded to the
                        // end of the block, so we can append after that)
                        dr.expandRecord(lastPos + lastLen, lastPos + lastLen + fs.getBlockSize());

                        //TODO optimize it also to use ByteBuffer at lower level
                        ByteBuffer buf = ByteBuffer.wrap(dr.getData(), dr.getOffset(), dr.getRecLen());
                        dir.write(lastPos + lastLen, buf);
                        //                      dir.write(lastPos + lastLen, dr.getData(), dr
                        //                      .getOffset(), dr.getRecLen());
                        if (logger.isLoggable(Level.FINER)) {
                            doLog(Level.FINER,
                                            "addDirectoryRecord(): LAST   record: begins at: "
                                            + (lastPos + lastLen)
                                            + ", length: "
                                            + dr.getRecLen());
                        }
                    }
                } else { //rec==null, ie. this is the first record in the directory
                    dr.expandRecord(0, fs.getBlockSize());
                    //TODO optimize it also to use ByteBuffer at lower level
                    ByteBuffer buf = ByteBuffer.wrap(dr.getData(), dr.getOffset(), dr.getRecLen());
                    dir.write(0, buf);
                    //dir.write(0, dr.getData(), dr.getOffset(), dr.getRecLen());
                    if (logger.isLoggable(Level.FINER)) {
                        doLog(Level.FINER,
                                        "addDirectoryRecord(): LAST   record: begins at: 0, length: "
                                        + dr.getRecLen());
                    }
                }

                //dir.flush();
                iNode.setMtime(System.currentTimeMillis() / 1000);

                // update the directory inode
                iNode.update();

                return;
            } catch (Throwable ex) {
                final IOException ioe = new IOException();
                ioe.initCause(ex);
                throw ioe;
            } finally {
                //unlock the inode from the cache
                iNode.decLocked();
            }
        }
    }

    /**
     * Replaces an existing directory record with a new one, in place if possible.
     * @param oldDr
     * @param newDr a freshly created record containing replacement information
     * @throws IOException
     * @throws FileSystemException
     */
    private void replaceDirectoryRecord(Ext2DirectoryRecord oldDr, Ext2DirectoryRecord newDr) throws IOException, FileSystemException {
        iNode = iNode.syncAndLock();
        //a single inode may be represented by more than one Ext2Directory instances,
        //but each will use the same instance of the underlying inode (see Ext2FileSystem.getINode()),
        //so synchronize to the inode.
        synchronized (iNode) {
            try {
                Ext2File dir = new Ext2File(iNode); //read itself as a file

                Ext2FSEntryIterator iterator = new Ext2FSEntryIterator(iNode);
                Ext2DirectoryRecord rec = null;
                while (iterator.hasNext()) {
                    rec = iterator.nextDirectoryRecord();
                    if (rec.equalsName(oldDr)) {
                        break;
                    }
                }
                assert rec != null;
                /* The actual data we are replacing is the name and/or iNode.
                 * The iNode is fixed length, but the name isn't. So we may have to
                 * "delete" the record and add a new one.
                 * The newDr iNode is interpreted as follows:
                 * < 0: leave existing, i.e. rename
                 * = 0: replace, i.e. delete record
                 * > 0: update existing with new value, i.e. a move
                 */
                if (newDr.getINodeNr() >= 0) {
                    rec.setINodeNr(newDr.getINodeNr());
                }
                boolean needAdd =false;
                if (!oldDr.equalsName(newDr)) {
                    int maxNameLen = rec.getMaxNameLen();
                    if (newDr.getName().length() <= maxNameLen) {
                        rec.setName(newDr.getName());
                    } else {
                        // name is too big for record, so delete the entry and add it back
                        if (newDr.getINodeNr() < 0) {
                            newDr.setINodeNr(rec.getINodeNr());
                        }
                        rec.setINodeNr(0);
                        needAdd = true;
                    }
                }
                ByteBuffer buf = ByteBuffer.wrap(rec.getData(), rec.getOffset(), rec.getRecLen());
                dir.write(rec.getFileOffset(), buf);
                if (needAdd) {
                    addDirectoryRecord(newDr);
                    return;
                }
                iNode.setMtime(System.currentTimeMillis() / 1000);
                // update the directory inode
                iNode.update();
                return;
            } catch (Throwable ex) {
                final IOException ioe = new IOException();
                ioe.initCause(ex);
                throw ioe;
            } finally {
                //unlock the inode from the cache
                iNode.decLocked();
            }
        }
    }

   /**
     * Return the number of the block that contains the given byte
     */
    int translateToBlock(long index) {
        return (int) (index / iNode.fs.getBlockSize());
    }

    /**
     * Return the offset inside the block that contains the given byte
     */
    int translateToOffset(long index) {
        return (int) (index % iNode.fs.getBlockSize());
    }

    private INode getINode() {
        return iNode;
    }

    class Ext2FSEntryIterator implements Iterator<FSEntry> {
        ByteBuffer data;

        int index;

        Ext2DirectoryRecord current;

        public Ext2FSEntryIterator(INode iNode) throws IOException {
            //read itself as a file
            Ext2File directoryFile = new Ext2File(iNode);
            //read the whole directory and make a snapshot of contents for the iteration

            data = ByteBuffer.allocate((int) directoryFile.getLength());
            directoryFile.read(0, data);

            index = 0;
        }

        public boolean hasNext() {
            Ext2DirectoryRecord dr = null;
            Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
            try {
                do {
                    if (index >= iNode.getSize())
                        return false;

                    //TODO this is somewhat wasteful, as we make a copy of the
                    // directory entry, but it is only used to create the Ext2Entry in next.
                    dr = new Ext2DirectoryRecord(fs, data.array(), index, index);
                    index += dr.getRecLen();
                    //inode nr=0 means the entry is unused, so skip those
                } while (dr.getINodeNr() == 0);
            } catch (Exception e) {
                fs.handleFSError(e);
                return false;
            }
            current = dr;
            return true;
        }

        public FSEntry next() {

            if (current == null) {
                //hasNext actually reads the next element
                if (!hasNext())
                    throw new NoSuchElementException();
            }

            Ext2DirectoryRecord dr = current;
            Ext2FileSystem fs = (Ext2FileSystem) fileSystem;
            current = null;
            try {
                return new Ext2Entry(fs.getINode(dr.getINodeNr()),
                        dr.getName(), dr.getType(), fs, Ext2Directory.this);
            } catch (IOException e) {
                throw new NoSuchElementException("Root cause: " + e.getMessage());
            } catch (FileSystemException e) {
                throw new NoSuchElementException("Root cause: " + e.getMessage());
            }
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the next record as an Ext2DirectoryRecord instance
         *
         * @return
         */
        protected Ext2DirectoryRecord nextDirectoryRecord() {
            if (current == null) {
                //hasNext actually reads the next element
                if (!hasNext())
                    throw new NoSuchElementException();
            }

            Ext2DirectoryRecord dr = current;
            current = null;
            return dr;
        }
    }

    /**
     * Read the entries from the device and return the result in a new
     * FSEntryTable
     *
     * @return
     */
    @Override
    protected FSEntryTable readEntries() throws IOException {
        Ext2FSEntryIterator it = new Ext2FSEntryIterator(iNode);
        ArrayList<FSEntry> entries = new ArrayList<FSEntry>();

        while (it.hasNext()) {
            final FSEntry entry = it.next();
            if (logger.isLoggable(Level.FINE)) {
                doLog(Level.FINE, "readEntries: entry=" + FSUtils.toString(entry, false));
            }
            entries.add(entry);
        }

        FSEntryTable table = new FSEntryTable((AbstractFileSystem) fileSystem, entries);

        return table;
    }

    /**
     * Write the entries in the table to the device.
     *
     * @param table
     */
    @Override
    protected void writeEntries(FSEntryTable table) throws IOException {
        // Nothing to do because createFileEntry, createDirectoryEntry, deleteDirectoryEntry do the job.
        // This would have to be a wholesale write anyway, as any information about what changes
        // actually were made has been lost by this point.
    }

    private void doLog(Level level, String msg) {
        logger.log(level, fileSystem.getDevice().getId() + " " + msg);
    }
}
