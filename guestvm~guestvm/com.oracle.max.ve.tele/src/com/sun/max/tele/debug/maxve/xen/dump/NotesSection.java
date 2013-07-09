/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug.maxve.xen.dump;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.oracle.max.elf.ELFDataInputStream;
import com.oracle.max.elf.ELFHeader;
import com.oracle.max.elf.ELFSectionHeaderTable;

/**
 * Represents the notes section in the xen core dump elf file
 *
 * @author Puneeet Lakhina
 *
 */
public class NotesSection {

    private ELFDataInputStream elfdis;
    private NoneNoteDescriptor noneNoteDescriptor;
    private HeaderNoteDescriptor headerNoteDescriptor;
    private XenVersionDescriptor xenVersionDescriptor;
    private FormatVersionDescriptor formatVersionDescriptor;
    private ELFHeader elfHeader;
    private ELFSectionHeaderTable.Entry sectionHeader;
    private RandomAccessFile dumpraf;

    static enum DescriptorType {
        NONE, HEADER, XEN_VERSION, FORMAT_VERSION;

        public static DescriptorType fromIntType(int type) {
            switch (type) {
                case 0x2000000:
                    return NONE;
                case 0x2000001:
                    return HEADER;
                case 0x2000002:
                    return XEN_VERSION;
                case 0x2000003:
                    return FORMAT_VERSION;
                default:
                    throw new IllegalArgumentException("Improper type value");
            }
        }
    };

    public NotesSection(RandomAccessFile raf, ELFHeader elfheader, ELFSectionHeaderTable.Entry sectionHeader) {
        this.dumpraf = raf;
        this.elfHeader = elfheader;
        this.sectionHeader = sectionHeader;
    }

    public void read() throws IOException, ImproperDumpFileException {
        dumpraf.seek(sectionHeader.getOffset());
        this.elfdis = new ELFDataInputStream(elfHeader, dumpraf);
        // readNone
        // readHeader
        // readVersion
        /*
         * the layout of the notes sections is Name Size (4 bytes) Descriptor Size (4 bytes) Type (4 bytes) - usually
         * interpreted as Int. Name Descriptor
         */
        /* the Name is always Xen and in case of notes section in thus coredump thus is length = 4 (including nullbyte) */
        int readLength = 0;
        while (readLength < sectionHeader.getSize()) {
            int nameLength = elfdis.read_Elf64_Word();
            if (nameLength != 4) {
                throw new ImproperDumpFileException("Length of name in notes section must be 4");
            }
            int descriptorlength = elfdis.read_Elf64_Word();
            DescriptorType type = DescriptorType.fromIntType(elfdis.read_Elf64_Word());
            String name = readString(nameLength);
            if (!name.equals("Xen")) {
                throw new ImproperDumpFileException("Name of each descriptor in the notes section should be xen");
            }
            readLength += (12 + nameLength);
            switch (type) {
                case NONE:
                    if (descriptorlength != 0) {
                        throw new ImproperDumpFileException("None descriptor should be 0 length");
                    }
                    this.noneNoteDescriptor = new NoneNoteDescriptor();
                    readLength += descriptorlength;
                    break;
                case HEADER:
                    readHeaderDescriptor(descriptorlength);
                    readLength += descriptorlength;
                    break;
                case XEN_VERSION:
                    readXenVersionDescriptor(descriptorlength);
                    readLength += descriptorlength;
                    break;

                case FORMAT_VERSION:
                    readFormatVersionDescriptor(descriptorlength);
                    readLength += descriptorlength;
                    break;
            }
        }
    }

    private void readHeaderDescriptor(int length) throws IOException, ImproperDumpFileException {
        if (length != 32) {
            throw new ImproperDumpFileException("Length of the header section should be 32 bytes");
        }
        this.headerNoteDescriptor = new HeaderNoteDescriptor();
        this.headerNoteDescriptor.setMagicnumber(elfdis.read_Elf64_XWord());
        this.headerNoteDescriptor.setVcpus((int)elfdis.read_Elf64_XWord());
        this.headerNoteDescriptor.setNoOfPages(elfdis.read_Elf64_XWord());
        this.headerNoteDescriptor.setPageSize(elfdis.read_Elf64_XWord());
    }

    private void readXenVersionDescriptor(int length) throws IOException, ImproperDumpFileException {
        this.xenVersionDescriptor = new XenVersionDescriptor();
        // 1272 =
        // sizeof(majorversion)+sizeof(minorversion)+sizeof(extraversion)+sizeof(compileinfo)+sizeof(capabilitiesinfo)+sizeof(changesetinfo)+sizeof(pagesize)
        // the platform param length is platform dependent thus we deduce it based on the total size
        int platformParamLength = length - 1272;
        if (platformParamLength != 4 && platformParamLength != 8) {
            throw new ImproperDumpFileException("Improper xen version descriptor");
        }
        this.xenVersionDescriptor.setMajorVersion(elfdis.read_Elf64_XWord());
        this.xenVersionDescriptor.setMinorVersion(elfdis.read_Elf64_XWord());
        this.xenVersionDescriptor.setExtraVersion(readString(XenVersionDescriptor.EXTRA_VERSION_LENGTH));
        this.xenVersionDescriptor.setCompileInfo(readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILER_LENGTH),
                        readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILE_BY_LENGTH), readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILER_DOMAIN_LENGTH),
                        readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILE_DATE_LENGTH));
        this.xenVersionDescriptor.setCapabilities(readString(XenVersionDescriptor.CAPABILITIES_LENGTH));
        this.xenVersionDescriptor.setChangeSet(readString(XenVersionDescriptor.CHANGESET_LENGTH));
        if(platformParamLength == 4) {
            this.xenVersionDescriptor.setPlatformParamters(elfdis.read_Elf64_Word());
        }else {
            this.xenVersionDescriptor.setPlatformParamters(elfdis.read_Elf64_XWord());
        }
        this.xenVersionDescriptor.setPageSize(elfdis.read_Elf64_XWord());
    }

    private void readFormatVersionDescriptor(int length) throws IOException, ImproperDumpFileException {
        if (length != 8) {
            throw new ImproperDumpFileException("the format version notes descriptor should be 8 bytes");
        }

        this.formatVersionDescriptor = new FormatVersionDescriptor();
        this.formatVersionDescriptor.setFormatVersion(this.elfdis.read_Elf64_XWord());
    }

    /**
     * Read a string from the file with length length. The returned string is of size length - 1 as java strings arent
     * null terminated
     *
     * @param length
     * @return
     */
    private String readString(int length) throws IOException {
        byte[] arr = new byte[length - 1];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = elfdis.read_Elf64_byte();
        }
        elfdis.read_Elf64_byte();
        return new String(arr);

    }

    @Override
    public String toString() {
        return "Header:"+headerNoteDescriptor != null ? headerNoteDescriptor.toString():null;
    }


    /**
     * @return the headerNoteDescriptor
     */
    public HeaderNoteDescriptor getHeaderNoteDescriptor() {
        return headerNoteDescriptor;
    }


    /**
     * @param headerNoteDescriptor the headerNoteDescriptor to set
     */
    public void setHeaderNoteDescriptor(HeaderNoteDescriptor headerNoteDescriptor) {
        this.headerNoteDescriptor = headerNoteDescriptor;
    }



}
