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
package com.sun.max.vm.hosted;

import java.io.*;

import com.oracle.max.elf.*;
import com.sun.max.program.Trace;

/**
 * A program to convert a Maxine VM image into a (sequence of) assembler file(s) or ELF file(s).
 *
 * @author Mick Jordan
 * @author Pradeep Natajaran
 *
 */
public class MemoryBootImage {

    private static final int MEGABYTE = 1024 * 1024;
    private static final int AS_CHUNK_SIZE = MEGABYTE * 100;
    private static final int MAXLL = 16;
    private static final String IMAGE_START = "maxvm_image_start";
    private static final String IMAGE_END = "maxvm_image_end";

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            int chunkSize = AS_CHUNK_SIZE;
            String imageFileName = null;
            boolean elf = true;
            // Checkstyle: stop modified control variable check
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg.equals("-chunksize")) {
                    final String css = args[++i];
                    final char m = css.charAt(css.length() - 1);
                    final int cs = Integer.parseInt(css.substring(0, css.length() - 1));
                    if (m == 'm') {
                        chunkSize = cs * MEGABYTE;
                    } else {
                        throw new Exception("invalid scale character: " + m);
                    }
                } else if (arg.equals("-image")) {
                    imageFileName = args[++i];
                } else if (arg.equals("-asm")) {
                    elf = false;
                }
            }
            // Checkstyle: resume modified control variable check
            File bootImage = null;
            if (imageFileName == null) {
                bootImage = BootImageGenerator.getBootImageFile(BootImageGenerator.getDefaultVMDirectory());
            } else {
                bootImage = new File(imageFileName);
            }
            if (elf) {
                writeELFFile(bootImage, chunkSize);
            } else {
                writeAsmFile(bootImage, chunkSize);
            }

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private static File asFile(File bootImage, int index) {
        final String parent = bootImage.getParent();
        final String name = bootImage.getName();
        return new File(parent, name + "." + index + ".s");
    }

    private static void flushLine(PrintWriter wr, byte[] line, int ll) {
        wr.print(".byte ");
        for (int i = 0; i < ll; i++) {
            final int v = line[i];
            wr.print("0x" + Integer.toHexString(v & 0xFF));
            if (i != ll - 1) {
                wr.print(",");
            }
        }
        wr.println();
    }


    private static void writeELFFile(File bootImage, int chunkSize) {

        int index = 0;
        long offsetCount = 0;
        try {
            final long startTime = System.currentTimeMillis();
            Trace.line(1, "Writing Elf File....");
            final long size = bootImage.length(); // The size of the image file that is stored in a section.
            final ELFHeader imageElfHeader = new ELFHeader();
            imageElfHeader.writeHeader64(size);
            final ELFSectionHeaderTable imageElfSectionHdr = new ELFSectionHeaderTable(imageElfHeader);
            ELFSymbolTable imageSymTab;
            final BufferedInputStream is = new BufferedInputStream(
                    new FileInputStream(bootImage));

            // Set the string table .
            final ELFStringTable strtab = new ELFStringTable(imageElfHeader);
            strtab.addStringInTable("maxvm_image");
            strtab.addStringInTable(".shstrtab");
            strtab.addStringInTable(".symtab");
            strtab.addStringInTable(".strtab");
            imageElfSectionHdr.setStringTable(strtab);


            // to create a new file to write the ELF object file.
            final String parent = bootImage.getParent();
            final String name = bootImage.getName();
            final File f = new File(parent, name + ".0.o");
            // Open the file for writing.
            final RandomAccessFile fis = new RandomAccessFile(f, "rw");
            final ELFDataOutputStream os = new ELFDataOutputStream(imageElfHeader, fis);
            imageElfHeader.writeELFHeader64ToFile(os, fis);
            writeImageToFile(os, fis, size, is);
            imageElfSectionHdr.write(size);
            final ELFSectionHeaderTable.Entry e = imageElfSectionHdr.entries[imageElfHeader.e_shstrndx];
            imageElfSectionHdr.getStringTable().setSection(e);
            imageElfSectionHdr.getStringTable().write64ToFile(os, fis);

            // Creating Symbol Table and the string table.
            for (int cntr = 0; cntr < imageElfSectionHdr.entries.length; cntr++) {
                final ELFSectionHeaderTable.Entry e1 = imageElfSectionHdr.entries[cntr];
                if (e1.isSymbolTable()) {
                    imageSymTab = new ELFSymbolTable(imageElfHeader, e1);
                    final ELFSectionHeaderTable.Entry strent = imageElfSectionHdr.entries[e1.getLink()];
                    if (strent.isStringTable()) {
                        final ELFStringTable symStrTab = new ELFStringTable(imageElfHeader, strent);
                        // To add the values of the string names that will be used in the symbol table
                        symStrTab.addStringInTable("maxvm_image_start");
                        symStrTab.addStringInTable("maxvm_image_end");
                        imageSymTab.setStringTable(symStrTab);
                        index = imageElfSectionHdr.getStringTable().getIndex("maxvm_image");
                        imageSymTab.setSymbolTableEntries(index, size);
                        //set the size of the string table section.
                        strent.setEntrySize(symStrTab.getStringLength());
                        // Now we need to write the symbol table onto the file.
                        // It should be aligned to 8 bits, so we increment the offsetcount to be aligned to 8 bits and then
                        // seek to that position to write the symbol.
                        offsetCount = fis.getFilePointer();
                        if (offsetCount % 8 != 0) {
                            offsetCount += 8 - (offsetCount % 8);
                        }
                        fis.seek(offsetCount);
                        imageSymTab.write64ToFile(os, fis);
                        // Write the symbol table's string table on to the file
                        imageSymTab.getStringTable().write64ToFile(os, fis);
                    }
                    break;
                }
            }
            // We have written all the sections. We need to write the section headers now.
            //Lets update the size of the file till this point in the section header.
            offsetCount = fis.getFilePointer();
            imageElfSectionHdr.setOffsetCount(offsetCount);


            //Before that we need to retrieve the ELF header and update its e_shoff to give the correct value for the offset of the section Header
            fis.seek(0); // seek to the beginning of the file.

            final ELFHeader header = ELFLoader.readELFHeader(fis);

            // Set the offset Count for the section header in the ELF header.
            header.setShOff(offsetCount);

            fis.seek(0); // Seek to beginning of the file again to write this header.
            header.writeELFHeader64ToFile(os, fis);
            // Now seek to end of the sections to write the section header.

            fis.seek(offsetCount);

            imageElfSectionHdr.writeSectionHeadersToFile64(os, fis);
            fis.close();
            Trace.line(1, "Wrote ELF file in: " + ((System.currentTimeMillis() - startTime) / 1000.0f) + " seconds");

        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    private static void writeImageToFile(ELFDataOutputStream os, RandomAccessFile fis, long size, BufferedInputStream is) throws IOException {
        final byte[] buffer = new byte[8192];
        while (size > 0) {
            final int n = is.read(buffer, 0, buffer.length);
            fis.write(buffer, 0, n);
            // CheckStyle: stop parameter assignment check
            size -= n;
            // CheckStyle: resume parameter assignment check
        }
    }

    private static void writeAsmFile(File bootImage, int chunkSize) {
        try {
            long size = bootImage.length();
            int fx = 0;
            int chunkCount = 0;
            final BufferedInputStream is = new BufferedInputStream(
                    new FileInputStream(bootImage));

            final long startTime = System.currentTimeMillis();
            while (size > 0) {
                final boolean lastFile = size <= chunkSize;
                final File f = asFile(bootImage, fx);
                final PrintWriter wr = new PrintWriter(new BufferedWriter(new FileWriter(f)));
                System.out.println(" writing  " + f);
                wr.println(".section maxvm_image, \"a\"");
                if (fx == 0) {
                    wr.println(".global " + IMAGE_START);
                    wr.println(IMAGE_START + ":");
                }
                if (lastFile) {
                    wr.println(".global " + IMAGE_END);
                }
                int count = 0;
                final byte[] line = new byte[MAXLL];
                while (chunkCount < chunkSize) {
                    final int b = is.read();
                    if (b < 0) {
                        break;
                    }
                    line[count++] = (byte) b; chunkCount++;
                    if (count == MAXLL) {
                        flushLine(wr, line, MAXLL);
                        count = 0;
                    }
                }
                if (count > 0) {
                    flushLine(wr, line, count);
                }
                if (lastFile) {
                    wr.println(IMAGE_END + ":");
                }
                wr.close();
                size -= chunkSize;
                chunkCount = 0;
                fx++;
            }
            is.close();
            Trace.line(1, "Wrote ASM file in: " + ((System.currentTimeMillis() - startTime) / 1000.0f) + " seconds");
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
