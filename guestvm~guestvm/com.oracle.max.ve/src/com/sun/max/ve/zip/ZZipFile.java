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
package com.sun.max.ve.zip;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.sun.max.program.ProgramError;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Represents a zip file. Use ZZipFile to avoid name clash with java.util.ZipFile.
 *
 * @author Mick Jordan
 *
 */
public final class ZZipFile {
    /*
     * For better or worse, java.util.ZipFile communicates with the "native" layer through
     * ids (which in Hotspot are actually C heap addresses). We simulate this
     * with the following list, with the index as the communication vehicle.
     * N.B. Id zero cannot be used as java.util.ZipFile treats this a "closed" value.
     *
     * ZipEntry uses a similar mechanism. In this case we do not preallocate an object
     * and map it to an id; instead we return the zipfile id and the file header offset as
     * a long, which eventually gets passed back to us in initEntryFields.
     */
    private static List<ZZipFile> _idTable = new ArrayList<ZZipFile>();
    private static Map<String, ZZipFile> _cache = new HashMap<String, ZZipFile>();

    private long _id;
    private String _name;
    private int _refs;
    private long _len;
    private long _lastModified;
    private RandomAccessFile _fd;
    private int _total;
    private long _locPos;
    private List<String> _metaNames = new ArrayList<String>();
    private Map<String, Long> _entries;
    private long _cenPos;
    private byte[] _cenBuf;
    // private int _lastEntry = 0;
    private byte[] _locHdr = new byte[LOCHDR];

    private ZZipFile(String name, long lastModified, RandomAccessFile fd) throws IOException {
        _name = name;
        _lastModified = lastModified;
        _fd = fd;
        _len = fd.length();
        _refs = 1;
        createId(this);
    }

    public static synchronized ZZipFile create(String name, int mode, long lastModified) throws ZipException {
        // look in the cache first
        ZZipFile zipFile = _cache.get(name);
        if (zipFile != null && (zipFile._lastModified == lastModified || zipFile._lastModified == 0)) {
            zipFile._refs++;
        } else {
            try {
                final RandomAccessFile fd = new RandomAccessFile(name, "r");
                zipFile = new ZZipFile(name, lastModified, fd);
                zipFile.readCEN(-1);
                _cache.put(name, zipFile);
            } catch (FileNotFoundException ex) {
                throwDefaultZipException(ex);
            } catch (IOException ex) {
                throwDefaultZipException(ex);
            }
        }
        return zipFile;
    }

    private static void throwDefaultZipException(IOException ex) throws ZipException {
        throw new ZipException("error in opening zip file: " + ex.getMessage());
    }

    public long getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public long getEntry(String name, boolean addSlash) {
        long result = 0;
        Long offset = _entries.get(name);
        if (offset != null) {
            result = offset;
        } else {
            if (addSlash) {
                offset = _entries.get(name + "/");
                if (offset != null) {
                    result = offset;
                }
            }
        }
        // Encode zip file in top 16 bits
        if (result != 0) {
            result |= _id << 48;
        }
        return result;
    }

    public long getNextEntry(int entry) {
        if (entry < 0 || entry >= _total) {
            return 0;
        }
        final long entryOffset = _cenPos + getEntryIndex(entry);
        return createEntryId(entryOffset);
    }

    private int getEntryIndex(int entry) {
        int cenIndex = 0;
        for (int i = 0; i < entry; i++) {
            cenIndex += CENHDR + getShort(_cenBuf, cenIndex + CENNAM) + getShort(_cenBuf, cenIndex + CENEXT) + getShort(_cenBuf, cenIndex + CENCOM);
        }
        // _lastEntry = entry;
        return cenIndex;
    }

    public long getLen() {
        return _len;
    }

    public long getLastModified() {
        return _lastModified;
    }

    public int getTotal() {
        return _total;
    }

    public synchronized void close(long id) {
        if (--_refs > 0) {
            return;
        }
        try {
            _fd.close();
        } catch (IOException ex) {

        }
        _cache.remove(_name);
        _idTable.set((int) id - 1, null);
    }

    /*
     * Methods that take "jzentry" arguments which, here, are values that encode
     * the ZZipFile and the offset to the entry.
     */

    @INLINE
    private static int getZid(long entryId) {
        return ((int) (entryId >> 48)) - 1;
    }

    @INLINE
    private static long getCenOffset(long entryId) {
        return entryId & 0xFFFFFFFF;
    }

    @INLINE
    private long createEntryId(long entryOffset) {
        return (_id << 48) | entryOffset;
    }

    @INLINE
    private int getCenIndex(long cenOffset) {
        return (int) (cenOffset - _cenPos);
    }

    public static int getMethod(long entryId) {
        final int zid = getZid(entryId);
        final long cenOffset = getCenOffset(entryId);
        return _idTable.get(zid).getMethodV(cenOffset);
    }

    public int getMethodV(long cenOffset) {
        final int cenIndex = getCenIndex(cenOffset);
        return getShort(_cenBuf, cenIndex  + CENHOW);
    }

    public static int getCSize(long entryId) {
        final int zid = getZid(entryId);
        final long cenOffset = getCenOffset(entryId);
        return _idTable.get(zid).getCSizeV(cenOffset);
    }

    private int getCSizeV(long cenOffset) {
        final int cenIndex = getCenIndex(cenOffset);
        final int size = getInt(_cenBuf, cenIndex + CENLEN);
        final int mode = getShort(_cenBuf, cenIndex  + CENHOW);
        return mode == STORED ? size : getInt(_cenBuf, cenIndex + CENSIZ);
    }

    public static int getSize(long entryId) {
        final int zid = getZid(entryId);
        final long cenOffset = getCenOffset(entryId);
        return _idTable.get(zid).getSizeV(cenOffset);
    }

    private int getSizeV(long cenOffset) {
        final int cenIndex = getCenIndex(cenOffset);
        return getInt(_cenBuf, cenIndex + CENLEN);
    }

    public static int read(long jzfile, long entryId, long pos, byte[] b, int off, int len)  throws ZipException {
        return get(jzfile).readV(entryId, pos, b, off, len);
    }

    public synchronized int readV(long entryId, long pos, byte[] b, int off, int len)  throws ZipException {
        final int cenIndex = getCenIndex(getCenOffset(entryId));
        // It is not clear that these checks are required, since our only client is ZipFile which is making
        // calls based on data we passed back in earlier calls.
        final long entrySize = getShort(_cenBuf, cenIndex  + CENHOW) == STORED ? getInt(_cenBuf, cenIndex + CENLEN) : getInt(_cenBuf, cenIndex + CENSIZ);
        if (pos < 0 || pos > entrySize - 1) {
            throw new ZipException("ZIP_Read: specified offset out of range");
        }
        if (len <= 0) {
            return 0;
        }
        if (len > entrySize - pos) {
            // CheckStyle: stop parameter assignment check"
            len = (int) (entrySize - pos);
            // CheckStyle: resume parameter assignment check"
        }
        // This is the one place where not having a cache of the entry fields is a problem.
        // The fix is to inject a new  field into ZipEntry
        long entryPos = 0;
        try {
            readFullyAt(_locHdr, 0, LOCHDR, _locPos + getInt(_cenBuf, cenIndex + CENOFF));
            if (getInt(_locHdr, 0) != LOCSIG) {
                throw new ZipException("invalid LOC header (bad signature)");
            }
            entryPos = _locPos + getInt(_cenBuf, cenIndex + CENOFF) + LOCHDR + getShort(_cenBuf, cenIndex + CENNAM) + getShort(_cenBuf, cenIndex + CENEXT);
            entryPos += pos;
            if (entryPos + len > _len) {
                throw new ZipException("ZIP_Read: corrupt zip file: invalid entry size");
            }
            readFullyAt(b, off, len, entryPos);
            return len;
        } catch (IOException ex) {
            throwDefaultZipException(ex);
        }
        return -1;
    }

    public static void initEntryFields(long entryId, Object zipEntryObj) {
        final int zid = getZid(entryId);
        _idTable.get(zid).initEntryFieldsV(getCenOffset(entryId), (ZipEntry) zipEntryObj);
    }

    private void initEntryFieldsV(long cenOffset, ZipEntry zipEntry) {
        if (_cenBuf == null) {
            ProgramError.unexpected("cenBuf is null!");
        } else {
            final int cenIndex = (int) (cenOffset - _cenPos);
            final int mode = getShort(_cenBuf, cenIndex  + CENHOW);
            zipEntry.setMethod(mode);
            final long time = getInt(_cenBuf, cenIndex + CENTIM);
            // This is the one field we can't set through the ZipEntry interface because
            // setTime converts java time to DOS time and "time" is already DOS time
            timeFieldActor().setLong(zipEntry, time & 0xFFFFFFFFL);
            final int size = getInt(_cenBuf, cenIndex + CENLEN);
            zipEntry.setCompressedSize(mode == STORED ? size : getInt(_cenBuf, cenIndex + CENSIZ));
            zipEntry.setSize(size);
            final long crc = getInt(_cenBuf, cenIndex + CENCRC);
            zipEntry.setCrc(crc & 0xFFFFFFFFL);
            String name = zipEntry.getName();
            if (name == null) {
                name = new String(_cenBuf, cenIndex + CENHDR, getShort(_cenBuf, cenIndex + CENNAM));
                nameFieldActor().setObject(zipEntry, name);
            }
            final int nameLen = name.length();
            final int extraLen = getShort(_cenBuf, cenIndex + CENEXT);
            if (extraLen > 0) {
                final byte[] extra = new byte[extraLen];
                System.arraycopy(_cenBuf, cenIndex + CENHDR + nameLen, extra, 0, extraLen);
            }
            final int commentLen = getShort(_cenBuf, cenIndex + CENCOM);
            if (commentLen > 0) {
                final byte[] comment = new byte[commentLen];
                System.arraycopy(_cenBuf, cenIndex + CENHDR + nameLen + extraLen, comment, 0, commentLen);
            }
        }
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static FieldActor _timeFieldActor;

    @INLINE
    private static FieldActor timeFieldActor() {
        if (_timeFieldActor == null) {
            _timeFieldActor = (FieldActor) ClassActor.fromJava(ZipEntry.class).findFieldActor(SymbolTable.makeSymbol("time"), null);
        }
        return _timeFieldActor;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static FieldActor _nameFieldActor;

    @INLINE
    private static FieldActor nameFieldActor() {
        if (_nameFieldActor == null) {
            _nameFieldActor = (FieldActor) ClassActor.fromJava(ZipEntry.class).findFieldActor(SymbolTable.makeSymbol("name"), null);
        }
        return _nameFieldActor;
    }

    /*
     * Support for mapping between ids and ZZipFiles
     */

    private synchronized void createId(ZZipFile zipFile) {
        int result = -1;
        for (int i = 0; i < _idTable.size(); i++) {
            if (_idTable.get(i) == null) {
                _idTable.set(i, zipFile);
                result = i;
                break;
            }
        }
        if (result < 0) {
            result = _idTable.size();
            _idTable.add(zipFile);
        }
        zipFile._id = result + 1;
    }

    public static ZZipFile get(long id) {
        return _idTable.get((int) id - 1);
    }

    /*
     * This supports JarFile
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static FieldActor _jzfileFieldActor;

    @INLINE
    private static FieldActor jzfileFieldActor() {
        if (_jzfileFieldActor == null) {
            _jzfileFieldActor = (FieldActor) ClassActor.fromJava(ZipFile.class).findFieldActor(SymbolTable.makeSymbol("jzfile"), null);
        }
        return _jzfileFieldActor;
    }

    public static String[] getMetaInfEntryNames(Object zipFileObj) {
        final ZipFile zipFile = (ZipFile) zipFileObj;
        final ZZipFile zzipFile = get(jzfileFieldActor().getLong(zipFile));
        final int size = zzipFile._metaNames.size();
        return zzipFile._metaNames.toArray(new String[size]);
    }

    /*
     * This is the start of the zip file processing methods
     */

    private static final long LOCSIG = 0x04034b50L;
    //private static final long EXTSIG = 0x08074b50L;
    private static final long CENSIG = 0x02014b50L;
    //private static final long ENDSIG = 0x06054b50L;

    private static final int LOCHDR = 30;
    //private static final int EXTHDR = 16;
    private static final int CENHDR = 46;
    private static final int ENDHDR = 22; // Length of end of central directory header
    private static final long END_MAXLEN = ENDHDR + 0xFFFF; // The END header is followed by a variable length comment of size < 64k.

    private static final int ENDTOT = 10;
    private static final int ENDSIZ = 12;
    private static final int ENDOFF = 16;
    private static final int ENDCOM = 20;

    private static final int CENFLG = 8;
    private static final int CENHOW = 10;
    private static final int CENTIM = 12;
    private static final int CENCRC = 16;
    private static final int CENSIZ = 20;
    private static final int CENLEN = 24;
    private static final int CENNAM = 28;
    private static final int CENEXT = 30;
    private static final int CENCOM = 32;
    private static final int CENOFF = 42;

    /*
     * Supported compression methods
     */
    private static final int STORED = 0;
    private static final int DEFLATED = 8;

    private static final int BUFSIZE = 128;

    private void readCEN(int knownTotal) throws IOException, ZipException {
        final byte[] endBuf = new byte[ENDHDR];
        final long endPos = findEND(endBuf);
        if (endPos == 0) {
            throw new ZipException("findEND returned zero");
        }
        final int cenLen = getInt(endBuf, ENDSIZ);
        if (cenLen > endPos) {
            throw new ZipException("invalid END header (bad central directory size)");
        }
        _cenPos = endPos - cenLen;

        _locPos = _cenPos - getInt(endBuf, ENDOFF);
        if (_locPos < 0) {
            throw new ZipException("invalid END header (bad central directory offset)");
        }
        _cenBuf = new byte[cenLen];
        readFullyAt(_cenBuf, 0, cenLen, _cenPos);

        _total = getShort(endBuf, ENDTOT);
        _entries = new HashMap<String, Long>(_total);
        int cenIndex = 0;
        for (int i = 0; i < _total; i++) {
            final int method = getShort(_cenBuf, cenIndex + CENHOW);
            final int nlen = getShort(_cenBuf, cenIndex + CENNAM);
            if (getInt(_cenBuf, cenIndex) != CENSIG) {
                throw new ZipException("invalid CEN header (bad signature)");
            }
            if ((getShort(_cenBuf, cenIndex + CENFLG) & 1) != 0) {
                throw new ZipException("invalid CEN header (encrypted entry)");
            }
            if (!(method == STORED || method == DEFLATED)) {
                throw new ZipException("invalid CEN header (bad compression method)");
            }

            final String name = new String(_cenBuf, cenIndex + CENHDR, nlen);
            if (name.toUpperCase().startsWith("META-INF/")) {
                addMetaName(name);
            }
            _entries.put(name, _cenPos + cenIndex);
            cenIndex += CENHDR + nlen + getShort(_cenBuf, cenIndex + CENEXT) + getShort(_cenBuf, cenIndex + CENCOM);
        }
        if (cenIndex != cenLen) {
            // TODO: This is likely  the situation where the number of entries is > 65535,
            // in which case the total has to be figured out by brute force.
            ProgramError.unexpected("zip file has too many entries");
        }
    }

    private long findEND(byte[] endBuf) throws IOException {
        final byte[] buf = new byte[BUFSIZE];
        final long minHDR = _len - END_MAXLEN > 0 ? _len  - END_MAXLEN : 0;
        final long minPos = minHDR - (BUFSIZE - ENDHDR);

        for (long pos = _len - BUFSIZE; pos >= minPos; pos -= BUFSIZE - ENDHDR) {
            long off = 0;
            if (pos < 0) {
                // Pretend there are some NUL bytes before start of file
                off = -pos;
                for (int i = 0; i < off; i++) {
                    buf[i] = 0;
                }
            }

            readFullyAt(buf, (int) off, BUFSIZE - off, pos + off);

            for (int i = BUFSIZE - ENDHDR; i > -0; i--) {
                if (buf[i + 0] == 'P'
                        && buf[i + 1] == 'K'
                        && buf[i + 2] == '\005'
                        && buf[i + 3] == '\006'
                        && (pos + i + ENDHDR + getShort(buf, ENDCOM + i) == _len)) {
                    /* Found END header */
                    System.arraycopy(buf, i, endBuf, 0, ENDHDR);
                    return pos + i;
                }

            }
        }
        return 0;
    }

    private void addMetaName(String name) {
        _metaNames.add(name);
    }

    private static int getInt(byte[] buf, int off) {
        return buf[off] & 0xFF | ((buf[off + 1] & 0xFF) << 8) | ((buf[off + 2] & 0xFF) << 16) | ((buf[off + 3] & 0xFF) << 24);
    }

    private static int getShort(byte[] buf, int off) {
        return buf[off] & 0xFF | ((buf[off + 1] & 0xFF) << 8);
    }

    private synchronized void readFullyAt(byte[] buf, int offset,  long len, long fileOffset) throws IOException {
        _fd.seek(fileOffset);
        _fd.readFully(buf, offset, (int) len);
    }
}
