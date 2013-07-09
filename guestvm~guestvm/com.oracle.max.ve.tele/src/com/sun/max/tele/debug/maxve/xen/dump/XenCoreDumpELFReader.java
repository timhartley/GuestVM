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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.oracle.max.elf.ELFHeader;
import com.oracle.max.elf.ELFLoader;
import com.oracle.max.elf.ELFSectionHeaderTable;
import com.oracle.max.elf.ELFHeader.FormatError;

/**
 * @author Puneeet Lakhina
 *
 */
public class XenCoreDumpELFReader {

    public static final String NOTES_SECTION_NAME = ".note.Xen";
    public static final String CONTEXT_SECTION_NAME = ".xen_prstatus";
    public static final String SHARED_INFO_SECTION_NAME = ".xen_shared_info";
    public static final String P2M_SECTION_NAME = ".xen_p2m";
    public static final String PFN_SECTION_NAME = ".xen.pfn";
    public static final String XEN_PAGES_SECTION_NAME = ".xen_pages";

    private RandomAccessFile fis;
    private ELFHeader header;

    private ELFSectionHeaderTable sectionHeaderTable;
    private ELFSectionHeaderTable.Entry notesSectionHeader;
    private ELFSectionHeaderTable.Entry contextSectionHeader;
    private ELFSectionHeaderTable.Entry pagesSectionHeader;
    private ELFSectionHeaderTable.Entry p2mSectionHeader;

    private NotesSection notesSection;
    private PagesSection pagesSection;
    private int vcpus;
    GuestContext[] guestContexts;

    public XenCoreDumpELFReader(File dumpFile) throws IOException, FormatError {
        this(new RandomAccessFile(dumpFile, "r"));
    }

    public XenCoreDumpELFReader(RandomAccessFile raf) throws IOException, FormatError {
        this.fis = raf;
        this.header = ELFLoader.readELFHeader(fis);
        this.sectionHeaderTable = ELFLoader.readSHT(raf, header);
        for (ELFSectionHeaderTable.Entry entry : sectionHeaderTable.entries) {
            String sectionName = entry.getName();
            if (NOTES_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                notesSectionHeader = entry;
            }
            if (CONTEXT_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                contextSectionHeader = entry;
            }
            if (XEN_PAGES_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                pagesSectionHeader = entry;
            }
            if (P2M_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                p2mSectionHeader = entry;
            }
        }

    }

    public NotesSection getNotesSection() throws IOException, ImproperDumpFileException {
        if (notesSection == null) {
            notesSection = new NotesSection(fis, header, notesSectionHeader);
            notesSection.read();
        }
        return notesSection;
    }

    public GuestContext getGuestContext(int cpuid) throws IOException, ImproperDumpFileException {
        int vcpus = getVcpus();
        if(vcpus == -1) {
            throw new IOException("Couldnt not get no of vcpus from noets section");
        }
        if(cpuid < 0 || cpuid >= vcpus) {
            throw new IllegalArgumentException("Improper CPU Id value");
        }
        if (guestContexts == null) {
            guestContexts = new GuestContext[vcpus];
        }
        if (guestContexts[cpuid] == null) {
            guestContexts = new GuestContext[vcpus];
            GuestContext context = new GuestContext(fis, header, contextSectionHeader, cpuid);
            context.read();
            guestContexts[cpuid] = context;
            return context;
        }else {
            return guestContexts[cpuid];
        }
    }

    public GuestContext[] getAllGuestContexts() throws IOException, ImproperDumpFileException {
        if (guestContexts == null) {
            for (int i = 0; i < getVcpus(); i++) {
                getGuestContext(i);
            }
        }
        return guestContexts;
    }

    public PagesSection getPagesSection() throws IOException,ImproperDumpFileException {
        if (pagesSection == null) {
            pagesSection = new PagesSection(fis, pagesSectionHeader, p2mSectionHeader, header, getNotesSection().getHeaderNoteDescriptor().getNoOfPages(), getNotesSection()
                            .getHeaderNoteDescriptor().getPageSize());
        }
        return pagesSection;

    }



    public int getVcpus() {
        try {
            if (this.vcpus == 0) {
                vcpus = (int) getNotesSection().getHeaderNoteDescriptor().getVcpus();
            }
            return vcpus;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public long getNoOfPages()throws IOException {
        return getNotesSection().getHeaderNoteDescriptor().getNoOfPages();
    }

}
