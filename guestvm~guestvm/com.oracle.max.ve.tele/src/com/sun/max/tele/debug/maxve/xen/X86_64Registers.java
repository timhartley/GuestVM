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
package com.sun.max.tele.debug.maxve.xen;

/**
 * @author Puneeet Lakhina
 *
 */
public class X86_64Registers {

    public static final int TOTAL_SIZE = 200;
// byte[] teleCanonicalizedRegisterData;
    byte[] originalRegisterData;
    boolean bigEndian;
    public static enum IntegerRegister {
        //The canonical index is based on amd64.c . The original Index is based on cpu_user_regs in xen-x86_64.c in the xen distribution
        R15(0, 8, 120), R14(8, 8, 112), R13(16, 8, 104), R12(24, 8, 96), RBP(8, 8, 40), RBX(32, 8, 24), R11(40, 8, 88), R10(48, 8, 80), R9(56, 8, 72), R8(64, 8, 64), RAX(72, 8, 0), RCX(80, 8, 8), RDX(
                        88, 8, 16), RSI(96, 8, 48), RDI(104, 8, 56),  RSP(136, 8, 32);

        private int originalIndex;
        private int length;
        private int canonicalIndex;

        IntegerRegister(int index, int length, int canonicalIndex) {
            this.originalIndex = index;
            this.length = length;
            this.canonicalIndex = canonicalIndex;
        }

        public int getOriginalIndex() {
            return this.originalIndex;
        }

        public int getLength() {
            return this.length;
        }

        public int getCanonicalIndex() {
            return this.canonicalIndex;
        }
    }

    public static enum StateRegister {
        RIP(120,8,0),RFLAGS(128, 8, 8);
        private int originalIndex;
        private int length;
        private int canonicalIndex;
        StateRegister(int index, int length, int canonicalIndex) {
            this.originalIndex = index;
            this.length = length;
            this.canonicalIndex = canonicalIndex;
        }

        public int getOriginalIndex() {
            return this.originalIndex;
        }

        public int getLength() {
            return this.length;
        }

        public int getCanonicalIndex() {
            return this.canonicalIndex;
        }
    }

    public X86_64Registers(byte[] originalData,boolean bigEndian) {
        this.originalRegisterData = originalData;
        this.bigEndian = bigEndian;
    }

    public byte[] canonicalizeTeleIntegerRegisters(byte[] canonicalArr) {
        for(IntegerRegister register:IntegerRegister.values()) {
            if(register.getCanonicalIndex() == -1) {
                continue;
            }
            if(bigEndian) {
                for(int i=register.getOriginalIndex(),j=register.getCanonicalIndex(),ctr=0;ctr < register.getLength();i++,j++,ctr++) {
                    canonicalArr[j] = originalRegisterData[i];
                }
            }else {
                for(int i=register.getOriginalIndex()+register.getLength()-1,j=register.getCanonicalIndex(),ctr=0;ctr < register.getLength();i--,j++,ctr++) {
                    canonicalArr[j] = originalRegisterData[i];
                }
            }
        }
        return canonicalArr;
    }

    public byte[] canonicalizeTeleStateRegisters(byte[] canonicalArr) {
        for(StateRegister register:StateRegister.values()) {
            if(register.getCanonicalIndex() == -1) {
                continue;
            }
            if(bigEndian) {
                for(int i=register.getOriginalIndex(),j=register.getCanonicalIndex(),ctr=0;ctr < register.getLength();i++,j++,ctr++) {
                    canonicalArr[j] = originalRegisterData[i];
                }
            }else {
                for(int i=register.getOriginalIndex()+register.getLength()-1,j=register.getCanonicalIndex(),ctr=0;ctr < register.getLength();i--,j++,ctr++) {
                    canonicalArr[j] = originalRegisterData[i];
                }
            }
        }
        return canonicalArr;
    }




//    private long r15;
//    private long r14;
//    private long r13;
//    private long r12;
//    private long rbp;
//    private long rbx;
//    private long r11;
//    private long r10;
//    private long r19;
//    private long r8;
//    private long rax;
//    private long rcx;
//    private long rdx;
//    private long rsi;
//    private long rdi;
//    private long errorCode;// this is unit32
//    private int entryVector;// this is unit32
//    private long rip;
//    private int cs;
//    private int pad0;
//    private short savedUpCallMask;
//    private short[] pad1 = new short[3];
//    private long rflags;
//    private long rsp;
//    // all following are uint16
//    private int ss;
//    private int[] pad2 = new int[3];
//    private int es;
//    private int[] pad3 = new int[3];
//    private int ds;
//    private int[] pad4 = new int[3];
//    private int fs;
//    private int[] pad5 = new int[3];
//    private int gs;
//    private int[] pad6 = new int[3];


}
