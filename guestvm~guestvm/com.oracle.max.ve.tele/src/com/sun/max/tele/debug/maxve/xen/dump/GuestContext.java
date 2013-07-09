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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.max.elf.ELFHeader;
import com.oracle.max.elf.ELFSectionHeaderTable;
import com.sun.max.tele.debug.maxve.xen.X86_64Registers;

/**
 * The CPU context dumped by Xen.
 *
 * @author Puneeet Lakhina
 *
 */
public class GuestContext {

    /**
     * This only includes XMM0 - XMM15.
     */
    private byte[] fpuRegisters = new byte[128];
    private long flags;
   private X86_64Registers cpuUserRegs = null;
    private TrapInfo[] trapInfo = new TrapInfo[256];
    private long linearAddressBase, linearAddressEntries;
    private long[] gdtFrames = new long[16];
    private long gdtEntries;
    private long kernelSS;
    private long kernelSP;
    private long[] ctrlreg = new long[8];
    private long[] debugreg = new long[8];
    private long eventCallBackEip;
    private long failsafeCallbackEip;
    private long syscallCallbackEip;
    private long vmAssist;
    private long fsBase;
    private long gsBaseKernel;
    private long gsBaseUser;
    private RandomAccessFile dumpraf;
    private ELFHeader header;
    private ELFSectionHeaderTable.Entry sectionHeader;
    private ByteBuffer sectionDataBuffer;
    private int cpuid;

    private static final int CPUCONTEXT_STRUCT_SIZE=5168;
    private static final int CPUCONTEXT_SKIP_BYTES=4264;

    public GuestContext(RandomAccessFile dumpraf, ELFHeader header, ELFSectionHeaderTable.Entry sectionHeader,int cpuid) {
        this.dumpraf = dumpraf;
        this.header = header;
        this.sectionHeader = sectionHeader;
        this.cpuid = cpuid;
    }

    public void read() throws IOException, ImproperDumpFileException {
        dumpraf.seek(sectionHeader.getOffset()+cpuid * CPUCONTEXT_STRUCT_SIZE);
        byte[] sectionData = new byte[CPUCONTEXT_STRUCT_SIZE];
        dumpraf.read(sectionData);
        sectionDataBuffer = ByteBuffer.wrap(sectionData);
        sectionDataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        //read fpu registers
        readfpu();
        flags = sectionDataBuffer.getLong();
        //Read registers
        byte[] registerData = new byte[X86_64Registers.TOTAL_SIZE];
        sectionDataBuffer.get(registerData);
        cpuUserRegs = new X86_64Registers(registerData , header.isBigEndian());
        //Skip trap info ldt gdt kernel ss
        sectionDataBuffer.position(sectionDataBuffer.position() + CPUCONTEXT_SKIP_BYTES);
        readctrlregs();
    }

    private void readctrlregs()throws IOException {
        for (int i = 0; i < ctrlreg.length; i++) {
            // for each register read 8 bytes
            ctrlreg[i] = sectionDataBuffer.getLong();
        }
    }
    private void readfpu() {
        sectionDataBuffer.position(20 * 8);
        // Skip registers we dont want
        for (int i = 0; i < 15; i++) {
            // for each register read 8 bytes
            byte[] data = new byte[8];
            sectionDataBuffer.get(data);
            for (int j = 0; j < 8; j++) {
                fpuRegisters[i*8+j] = data[header.isBigEndian()? j:7 - j];
            }
            sectionDataBuffer.position(sectionDataBuffer.position()+8);
        }
        sectionDataBuffer.position(512);
    }


    /**
     * @return the fpuRegisters
     */
    public byte[] getfpuRegisters() {
        return fpuRegisters;
    }

    /**
     * @return the cpuUserRegs
     */
    public X86_64Registers getCpuUserRegs() {
        return cpuUserRegs;
    }


    /**
     * @return the ctrlreg
     */
    public long[] getCtrlreg() {
        return ctrlreg;
    }


}
