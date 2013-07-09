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
package test.com.sun.max.ve.guk;

import java.util.*;
import com.sun.max.memory.VirtualMemory;
import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.*;
import com.sun.max.ve.guk.GUKPageTables.PteNotPresentException;
import com.sun.max.ve.guk.x64.*;
import com.sun.max.ve.memory.*;
import com.sun.max.ve.sched.*;
import com.sun.max.ve.spinlock.guk.GUKSpinLock;
import com.sun.max.ve.test.VMTestHelper;
import com.sun.max.ve.test.VmThreadTestHelper;
import com.sun.max.vm.VMConfiguration;
import test.util.*;

public class KernelTest {

    static long _allocatedObject;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        final ArgsHandler h = ArgsHandler.process(args);
        if (h._opCount == 0) {
            System.out.println("no operations given");
            return;
        }

        Thread.sleep(1000);
        for (int j = 0; j < h._opCount; j++) {
            final String op = h._ops[j];
            final String opArg1 = h._opArgs1[j];
            final String opArg2 = h._opArgs2[j];
            final String opArg3 = h._opArgs3[j];
            final String opArg4 = h._opArgs4[j];
            if (h._verbose) {
                System.out.println("op=" + op + ", a1=" + opArg1 + ", a2=" + opArg2 + ", a3=" + opArg3 + ", a4=" + opArg4);
            }
            if (op.equals("getNumPages")) {
                System.out.println("NumPages: " + GUKPagePool.getCurrentReservation());
            } else if (op.equals("getPTBase")) {
                System.out.println("PT Base: " + Long.toHexString(VMTestHelper.toLong(GUKPageTables.getPageTableBase())));
            } else if (op.equals("getMFN")) {
                final int pfn = Integer.parseInt(opArg1);
                final long mfn = GUKPageTables.getMfnForPfn(pfn);
                System.out.println("MFN[" + pfn + "] " + Long.toHexString(mfn));
                final long xpfn = GUKPageTables.getPfnForMfn(mfn);
                if (xpfn != pfn) {
                    System.out.println("getPFN(" + Long.toHexString(mfn) + ") mismatch " + xpfn);
                }
            } else if (op.equals("printPageTable")) {
                final long table = opArg1 == null ? VMTestHelper.toLong(GUKPageTables.getPageTableBase()) : parseLong(opArg1, 16);
                final int start = opArg2 == null ? 0 : parseInt(opArg2);
                final int end = opArg3 == null ? GUKPageTables.getNumPTEntries(4) : parseInt(opArg3);
                printPageTable(VMTestHelper.fromLong(table), start, end);
            } else if (op.equals("getMFNs")) {
                final int start = parseInt(opArg1);
                final int end = parseInt(opArg2);
                int count = 0;
                for (int p = start; p <= (end < 0 ? start : end); p++) {
                    if (count % 10 == 0) {
                        if (count > 0) {
                            System.out.println();
                        }
                        System.out.print(p +  ":");
                    }
                    System.out.print(" " + GUKPageTables.getMfnForPfn(p));
                    count++;
                }
                System.out.println();
            } else if (op.equals("getPFNs")) {
                final int start = parseInt(opArg1);
                final int end = parseInt(opArg2);
                int count = 0;
                for (int p = start; p <= (end <= 0 ? start : end); p++) {
                    if (count % 10 == 0) {
                        if (count > 0) {
                            System.out.println();
                        }
                        System.out.print(p +  ":");
                    }
                    System.out.print(" " + Long.toHexString(GUKPageTables.getPfnForMfn(p)));
                    count++;
                }
                System.out.println();
            } else if (op.equals("getPTInfo")) {
                final Address a = VMTestHelper.fromLong(parseAddr(opArg1));
                getPTInfo(a);
            } else if (op.equals("getPTIndices")) {
                final Address a = VMTestHelper.fromLong(parseAddr(opArg1));
                getPTInfo(a);
            } else if (op.equals("validate")) {
                validate(VMTestHelper.fromLong(parseAddr(opArg1)));
            } else if (op.equals("pageFrames")) {
                final int minLevel = parseInt(opArg1);
                pageFramesInfo(minLevel);
            } else if (op.equals("pageFrameAtIndex")) {
                final int l4 = parseInt(opArg1);
                final int l3 = parseInt(opArg2);
                final int l2 = parseInt(opArg3);
                final int l1 = parseInt(opArg4);
                pageFrameAtIndex(l4, l3, l2, l1);
            } else if (op.equals("pageFramesFromIndex")) {
                final int l4 = parseInt(opArg1);
                final int l3 = parseInt(opArg2);
                final int l2 = parseInt(opArg3);
                final int l1 = parseInt(opArg4);
                pageFramesForIndex(l4, l3, l2, l1);
            } else if (op.equals("unmappedFrames")) {
                frameState(parseInt(opArg1), parseInt(opArg2));
            } else if (op.equals("getCurrent")) {
                System.out.println("current memory reservation: " + GUKPagePool.getCurrentReservation());
            } else if (op.equals("getMaximum")) {
                System.out.println("maximum memory reservation: " + GUKPagePool.getMaximumReservation());
            } else if (op.equals("getMaxRam")) {
                System.out.println("maximum ram page: " + GUKPagePool.getMaximumRamPage());
            } else if (op.equals("increasePool")) {
                final int inc = parseInt(opArg1);
                if (inc > 0) {
                    final long[] currentMfns = new long[(int) GUKPagePool.getCurrentReservation()];
                    for (int m = 0; m < currentMfns.length; m++) {
                        currentMfns[m] = GUKPageTables.getMfnForPfn(m);
                    }
                    final long rc = GUKPagePool.increasePagePool(inc);
                    System.out.println("increasePool returned: " + rc);
                    if (rc > 0) {
                        for (int m = 0; m < currentMfns.length; m++) {
                            final long newMfn = GUKPageTables.getMfnForPfn(m);
                            if (currentMfns[m] != newMfn) {
                                System.out.println("mfn mismatch m[" + m + "] was " + currentMfns[m] + " now " + newMfn);
                            }
                        }

                    }
                }
            } else if (op.equals("decreasePool")) {
                final int inc = parseInt(opArg1);
                if (inc > 0) {
                    final long rc = GUKPagePool.decreasePagePool(inc);
                    System.out.println("decreasePool returned: " + rc);
                }
            } else if (op.equals("allocatePages")) {
                allocatePages(parseInt(opArg1));
            } else if (op.equals("pagePool")) {
                pagePool();
            } else if (op.equals("stackPool")) {
                stackPool();
            } else if (op.equals("heapPool")) {
                heapPool();
            } else if (op.equals("codePool")) {
                codePool();
            } else if (op.equals("createThread")) {
                createThread();
            } else if (op.equals("shrinkHeap")) {
                shrinkHeap(parseInt(opArg1));
            } else if (op.equals("mfnMap")) {
                mfnMap(parseInt(opArg1));
            } else if (op.equals("mfnMap2")) {
                mfnMap2(parseInt(opArg1));
            } else if (op.equals("allocate2MBPages")) {
                final long page = GUKPageTables.allocate_2mb_machine_pages(parseInt(opArg1), VirtualMemory.Type.HEAP.ordinal());
                System.out.println("page: " + page);
            } else if (op.equals("cpuRunningTime")) {
                System.out.println("CPU " + opArg1 + " running time: " + GUKScheduler.getCPURunningTime(Integer.parseInt(opArg1)));
            } else if (op.equals("threadRunningTime")) {
                final GUKVmThread vmThread = (GUKVmThread) VmThreadTestHelper.current();
                System.out.println("Current Thread running time: " + vmThread.getRunningTime());
            } else if (op.equals("allocateObject")) {
                final Object obj = new Object();
                _allocatedObject = VMTestHelper.toLong(obj);
                System.out.println("address " + _allocatedObject);
            } else if (op.equals("sleep")) {
                try {
                    Thread.sleep(Long.parseLong(opArg1));
                } catch (InterruptedException ex) {
                }
            } else if (op.equals("validateStackPool")) {
                stackPoolValidate();
            } else {
                System.out.println("command " + op + " not recognized");
            }
        }
    }

    private static Address allocatePages(int n) {
        if (n > 0) {
            final Pointer p = VirtualMemory.allocate(VMTestHelper.fromInt(n * X64VM.PAGE_SIZE), VirtualMemory.Type.DATA);
            final long va = VMTestHelper.toLong(p);
            System.out.println("allocatePages: " + Long.toHexString(va) + ", pn: " + (va / X64VM.PAGE_SIZE));
            return p;
        } else {
            System.out.println("usage: a1 n op allocatePages ");
            return Address.zero();
        }
    }

    private static void getPTInfo(Address a) {
        System.out.print("PT info for " + Long.toHexString(VMTestHelper.toLong(a)) + ": L[" + GUKPageTables.getPTIndex(a, 4) + "," + GUKPageTables.getPTIndex(a, 3) +
                        "," + GUKPageTables.getPTIndex(a, 2) + "," + GUKPageTables.getPTIndex(a, 1) + "] ");
        try {
            final long pte = GUKPageTables.getPteForAddress(a);
            System.out.print(Long.toHexString(pte) + " mfn "  + Long.toHexString(GUKPageTables.getMfnForPte(pte)) + ", pfn " + Long.toHexString(GUKPageTables.getPfnForPte(pte)) + ", state " + getState(pte));
        } catch (GUKPageTables.PteNotPresentException ex) {
            System.out.print("page not present");
        }
        System.out.println();
    }

    private static void printPageTable(Address table, int start, int end) {
        for (int i = start; i < end; i++) {
            final long pte = GUKPageTables.getPTEntryAtIndex(table, i);
            final long mfn = GUKPageTables.getMfnForPte(pte);
            System.out.println(i + ": mfn " + Long.toHexString(mfn) + " state " + Long.toHexString(GUKPageTables.getState(pte)) + " " + getState(pte));
        }
    }

    private static String getState(long pte) {
        final StringBuilder sb = new StringBuilder();
        if (GUKPageTables.isPresent(pte)) {
            sb.append('P');
        }
        if (GUKPageTables.isWritable(pte)) {
            sb.append('W');
        }
        if (GUKPageTables.isUser(pte)) {
            sb.append('U');
        }
        if (GUKPageTables.isAccessed(pte)) {
            sb.append('A');
        }
        if (GUKPageTables.isDirty(pte)) {
            sb.append('D');
        }
        return sb.toString();
    }

    private static long parseLong(String s, int radix) {
        if (s != null) {
            return Long.parseLong(s, radix);
        } else {
            return -1;
        }
    }

    private static int parseInt(String s) {
        if (s != null) {
            return Integer.parseInt(s);
        } else {
            return -1;
        }
    }

    private static final long K = 1024;
    private static final long M = 1024 * K;
    private static final long G = 1024 * M;
    private static final long T = 1024 * G;
    private static final long P = 1024 * T;

    /**
     * Parse an address given as a hex number, with optional scaling suffix.
     * If the argument is "null", then the value of the variables {@link allocatedObject} is used instead.
     * @param string to parse
     * @return the address
     */
    private static long parseAddr(String s) {
        if (s == null || s.equals("null")) {
            return _allocatedObject;
        } else {
            long r = 0;
            final int l = s.length();
            int i = 0;
            char ch = ' ';
            while (i < l) {
                ch = Character.toUpperCase(s.charAt(i));
                if ('0' <= ch && ch <= '9') {
                    r = r * 16 + (ch - '0');
                } else if ('A' <= ch && ch <= 'F') {
                    r = r * 16 + (ch - 'A' + 10);
                } else {
                    break;
                }
                i++;
            }
            switch (Character.toUpperCase(ch)) {
                case 'K':
                    return r * K;
                case 'M':
                    return r * M;
                case 'G':
                    return r * G;
                case 'T':
                    return r * T;
                case 'P':
                    return r * P;
                default:
                    return r;
            }
        }
    }

     /**
     * Validates the page table setup for virtual address.
     * @param a
     */
    private static void validate(Address a) {
        Address table = GUKPageTables.getPageTableBase(); // level 4 table
        int level = 4;
        long pte = 0;
        while (level > 0) {
            final int index = GUKPageTables.getPTIndex(a, level);
            System.out.println("L" + level + " table: " + Long.toHexString(VMTestHelper.toLong(table)) + " index " + index);
            pte = GUKPageTables.getPTEntryAtIndex(table, index);
            if (!GUKPageTables.isPresent(pte)) {
                System.out.println("page table entry at index " + index + " in level " + level + " is not present");
                return;
            }
            table = GUKPageTables.getAddressForPte(pte);
            level--;
        }
        final long pfn = GUKPageTables.getPfnForPte(pte);
        final long mfn = GUKPageTables.getMfnForPte(pte);
        System.out.println("pfn " + Long.toHexString(pfn) + ", mfn " + Long.toHexString(mfn) + ", state " + getState(pte));

    }

    private static final int MEMORY = 1;
    private static final int STACK = 2;
    private static final int MAPPED = 1;
    private static final int UNMAPPED = 2;


    private static void frameState(int afs, int akind) {
        int kind = akind;
        int fs = afs;
        if (kind < 0) {
            kind = MEMORY | STACK;
        }
        if (fs < 0) {
            fs = MAPPED | UNMAPPED;
        }
        if ((kind & MEMORY) != 0) {
            final long maxPfn = GUKPagePool.getCurrentReservation() - 1;
            final Address maxAddress = GUKPageTables.getAddressForPfn(maxPfn);
            System.out.println("Unmapped frames in 1-1 mapped memory");
            frameState(VMTestHelper.fromLong(0), maxAddress, fs);
        }
        if ((kind & STACK) != 0) {
            System.out.println("Unmapped frames in allocated thread stack memory");
            final Address stackPoolBase = StackPool.getBase();
            final int stackSize = (int) StackPool.getRegionSize();
            final int size = StackPool.getSize();
            int p = 0;
            while (p < size) {
                if (StackPool.isAllocated(p)) {
                    final Address stackBase = VMTestHelper.plus(stackPoolBase, p * stackSize);
                    System.out.println("stackbase: " + Long.toHexString(VMTestHelper.toLong(stackBase)));
                    frameState(stackBase, VMTestHelper.plus(stackBase, stackSize - 1), fs);
                }
                p++;
            }
        }
    }

    private static void getPageTableIndices(Address address, int[] indices) {
        Address table = GUKPageTables.getPageTableBase(); // level 4 table
        int level = 4;
        while (level > 0) {
            final int index = GUKPageTables.getPTIndex(address, level);
            indices[level] = index;
            final long pte = GUKPageTables.getPTEntryAtIndex(table, index);
            if (level > 1 && !GUKPageTables.isPresent(pte)) {
                throw new Error("decoding address " + Long.toHexString(VMTestHelper.toLong(address)) + ": page table entry at index " + index + " in level " + level + " is not present");
            }
            table = GUKPageTables.getAddressForPte(pte);
            level--;
        }
    }

    /**
     * Print the unmapped L1 frame entries in the range startAddress .. endAddress
     */
    private static void frameState(Address startAddress, Address endAddress, int fs) {
        final int[] startIndices = new int[5];
        final int[] endIndices = new int[5];

        getPageTableIndices(startAddress, startIndices);
        getPageTableIndices(endAddress, endIndices);
        final Address table4 = GUKPageTables.getPageTableBase();
        for (int l4 = startIndices[4]; l4 <= endIndices[4]; l4++) {
            final long pte4 = GUKPageTables.getPTEntryAtIndex(table4, l4);
            final Address table3 = GUKPageTables.getAddressForPte(pte4);
            for (int l3 = startIndices[3]; l3 <= endIndices[3]; l3++) {
                final long pte3 = GUKPageTables.getPTEntryAtIndex(table3, l3);
                final Address table2 = GUKPageTables.getAddressForPte(pte3);
                for (int l2 = startIndices[2]; l2 <= endIndices[2]; l2++) {
                    final long pte2 = GUKPageTables.getPTEntryAtIndex(table2, l2);
                    final Address table1 = GUKPageTables.getAddressForPte(pte2);
                    int l1 = startIndices[1];
                    while (l1 <= endIndices[1]) {
                        final int ql1 = l1;
                        if (isPresent(table1, l1)) {
                            while (l1 <= endIndices[1] && isPresent(table1, l1)) {
                                l1++;
                            }
                            if ((fs & MAPPED)  != 0) {
                                printFrame(l4, l3, l2, ql1, l1, true);
                            }
                        } else {
                            while (l1 <= endIndices[1] && !isPresent(table1, l1)) {
                                l1++;
                            }
                            if ((fs & UNMAPPED) != 0) {
                                printFrame(l4, l3, l2, ql1, l1, false);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isPresent(Address table, int index) {
        return GUKPageTables.isPresent(GUKPageTables.getPTEntryAtIndex(table, index));
    }

    private static void printFrame(int l4, int l3, int l2, int ql1, int l1, boolean present) {
        System.out.println("L[" + l4 + "," + l3 + "," + l2 + "," + ql1 + ".." + (l1 - 1) + "] va " +
                        Long.toHexString(vaFor(l4, l3, l2, ql1)) + ".." + Long.toHexString(vaFor(l4, l3, l2, l1)) +
                        (l1 - ql1 == 1 ? " is" : " are") + (present ? "" : " not") + " present");
    }

    /**
     * Prints all the page frames starting from the (singleton) level 4, optionally truncating at minLevel.
     * @param minLevel
     */
    private static void pageFramesInfo(int minLevel) {
        pageFrames(GUKPageTables.getPageTableBase(), 4, minLevel < 0 ? 1 : minLevel, 0);
    }

    /**
     * Prints all the page frames starting from the given table at given level, optionally truncating at minLevel.
     * @param table
     * @param level
     * @param minLevel
     * @param va
     */
    private static void pageFrames(Address table, int level, int minLevel, long ava) {
        long va = ava;
        final int numPTE = GUKPageTables.getNumPTEntries(level);
        int i = 0;
        while (i < numPTE) {
            long pte = GUKPageTables.getPTEntryAtIndex(table, i);
            spaces(level);
            if (GUKPageTables.isPresent(pte)) {
                final long pfn = GUKPageTables.getPfnForPte(pte);
                System.out.print("L" + level + "[" + i + "] va: " + Long.toHexString(va) + "->" + Long.toHexString(va + vaInc(level) - 1));
                System.out.println(" mfn: " + Long.toHexString(GUKPageTables.getMfnForPte(pte)) + " pfn:"  + (checkPfn(pfn) ? Long.toHexString(pfn) : "???") + " " + getState(pte));
                if (level > minLevel && checkPfn(pfn)) {
                    pageFrames(GUKPageTables.getAddressForPte(pte), level - 1, minLevel, va);
                }
                va += vaInc(level);
                i++;
            } else {
                // skip over a batch of missing frames
                final int j = i;
                while (i++ < numPTE && !GUKPageTables.isPresent(pte)) {
                    pte = GUKPageTables.getPTEntryAtIndex(table, i);
                }
                // i is now one beyond the last missing frame
                i--;
                System.out.print("L" + level + "[" + j);
                if (i > j + 1) {
                    System.out.print(".." + i);
                }
                System.out.println("] va: " + Long.toHexString(va) + "->" + Long.toHexString(va + (vaInc(level) * (i - j)) - 1) + " not present");
                va += vaInc(level) * (i - j);
            }
        }
    }

    private static boolean checkPfn(long pfn) {
        return pfn > 0 && pfn < GUKPagePool.getCurrentReservation();
    }

    private static void spaces(int level) {
        String sp = "";
        switch (level) {
            case 4:
                break;
            case 3:
                sp = "  ";
                break;
            case 2:
                sp = "    ";
                break;
            case 1:
                sp = "      ";
                break;
            default:
        }
        System.out.print(sp);
    }

    /**
     * Validates and prints the page table entry at the given indices.
     * @param l4
     * @param l3
     * @param l2
     * @param l1
     */
    private static void pageFrameAtIndex(int l4, int l3, int l2, int l1) {
        Address table = GUKPageTables.getPageTableBase();
        long pte = GUKPageTables.getPTEntryAtIndex(table, l4);
        checkPresent(pte, 4);
        table = GUKPageTables.getAddressForPte(pte);
        pte = GUKPageTables.getPTEntryAtIndex(table, l3);
        checkPresent(pte, 3);
        table = GUKPageTables.getAddressForPte(pte);
        pte = GUKPageTables.getPTEntryAtIndex(table, l2);
        checkPresent(pte, 2);
        table = GUKPageTables.getAddressForPte(pte);
        pte = GUKPageTables.getPTEntryAtIndex(table, l1);
        checkPresent(pte, 1);
        final long pfn = GUKPageTables.getPfnForPte(pte);
        System.out.print("L[" + l4 + "," + l3 + "," + l2 + "," + l1 + "] va " + Long.toHexString(vaFor(l4, l3, l2, l1)));
        System.out.println(" mfn: " + Long.toHexString(GUKPageTables.getMfnForPte(pte)) + " pfn:" + (checkPfn(pfn) ? Long.toHexString(pfn) : "???") + " " + getState(pte));
    }

    /**
     * Prints the page table tree below a given index. If all four indices are >= 0, this is equivalent to
     * pageFrameIndex
     *
     * @param l4
     * @param l3
     * @param l2
     * @param l1
     */
    private static void pageFramesForIndex(int l4, int l3, int l2, int l1) {
        Address table = GUKPageTables.getPageTableBase();
        long pte = GUKPageTables.getPTEntryAtIndex(table, l4);
        checkPresent(pte, 4);
        table = GUKPageTables.getAddressForPte(pte);
        if (l3 >= 0) {
            pte = GUKPageTables.getPTEntryAtIndex(table, l3);
            checkPresent(pte, 3);
            table = GUKPageTables.getAddressForPte(pte);
            if (l2 >= 0) {
                pte = GUKPageTables.getPTEntryAtIndex(table, l2);
                checkPresent(pte, 2);
                table = GUKPageTables.getAddressForPte(pte);
                if (l1 >= 0) {
                    pageFrameAtIndex(l4, l3, l2, l1);
                } else {
                    System.out.println("L4[" + l4 + "] va: " + Long.toHexString(vaFor(l4, 0, 0, 0)) + "->" + Long.toHexString(vaFor(l4, 0, 0, 0) + vaInc(4) - 1));
                    spaces(3);
                    System.out.println("L3[" + l3 + "] va: " + Long.toHexString(vaFor(l4, l3, 0, 0)) + "->" + Long.toHexString(vaFor(l4, l3, 0, 0) + vaInc(3) - 1));
                    spaces(2);
                    System.out.println("L2[" + l2 + "] va: " + Long.toHexString(vaFor(l4, l3, l2, 0)) + "->" + Long.toHexString(vaFor(l4, l3, 2, 0) + vaInc(2) - 1));
                    pageFrames(table, 1, 1, vaFor(l4, l3, l2, 0));
                }
            } else {
                System.out.println("L4[" + l4 + "] va: " + Long.toHexString(vaFor(l4, 0, 0, 0)) + "->" + Long.toHexString(vaFor(l4, 0, 0, 0) + vaInc(4) - 1));
                spaces(3);
                System.out.println("L3[" + l3 + "] va: " + Long.toHexString(vaFor(l4, l3, 0, 0)) + "->" + Long.toHexString(vaFor(l4, l3, 0, 0) + vaInc(3) - 1));
                pageFrames(table, 2, 1, vaFor(l4, l3, 0, 0));
            }
        } else {
            pageFrames(table, 3, 1, vaFor(l4, 0, 0, 0));
        }
    }

    private static void checkPresent(long pte, int level) {
        if (!GUKPageTables.isPresent(pte)) {
            throw new RuntimeException("page not present at level " + level);
        }
    }

    private static long vaFor(int l4, int l3, int l2, int l1) {
        return (1L << X64VM.L4_SHIFT) * l4 + (1L << X64VM.L3_SHIFT) * l3 + (1 << X64VM.L2_SHIFT) * l2 + (1 << X64VM.L1_SHIFT) * l1;
    }

    private static long vaInc(int level) {
        switch (level) {
            case 4:
                return 1L << X64VM.L4_SHIFT;
            case 3:
                return 1L << X64VM.L3_SHIFT;
            case 2:
                return 1 << X64VM.L2_SHIFT;
            case 1:
                return 1 << X64VM.L1_SHIFT;
            default:
                throw new Error("illegal level " + level);
        }
    }

    private static void pagePool() {
        GUKPagePool.logState();
    }

    @SuppressWarnings("unused")
    private static void javaPagePool() {
        final long start = GUKPagePool.getStart();
        final long end = GUKPagePool.getEnd();
        System.out.println("page pool size: " + sizeInK(end - start + 1) + ", range [" + start + "," + end + "]");
        long allocated = 0;
        long p = start;
        while (p <= end) {
            final long q = p;
            if (GUKPagePool.isAllocated(p)) {
                while (p <= end && GUKPagePool.isAllocated(p)) {
                    p++;
                }
                System.out.println(q + ".." + (p - 1) + " allocated");
                allocated += p - q;
            } else {
                while (p <= end && !GUKPagePool.isAllocated(p)) {
                    p++;
                }
                System.out.println(q + ".." + (p - 1) + " free");
            }
        }
        System.out.println("allocated: " + sizeInK(allocated)  + ", free: " + sizeInK(end - start + 1 - allocated));
    }

    private static String sizeInK(long s) {
        return Long.toString(s * 4096) + "K";
    }

    interface Pool {
        String getName();
        Address getBase();
        int getSize();
        long getRegionSize();
        boolean isAllocated(int p);
    }

    static class StackPoolImpl implements Pool {
        public String getName() {
            return "stack";
        }
        public Address getBase() {
            return StackPool.getBase();
        }
        public int getSize() {
            return StackPool.getSize();
        }
        public long getRegionSize() {
            return StackPool.getRegionSize();
        }
        public boolean isAllocated(int p) {
            return StackPool.isAllocated(p);
        }
    }

    static class HeapPoolImpl implements Pool {
        public String getName() {
            return "heap";
        }
        public Address getBase() {
            return HeapPool.getBase();
        }
        public int getSize() {
            return HeapPool.getSize();
        }
        public long getRegionSize() {
            return HeapPool.getRegionSize();
        }
        public boolean isAllocated(int p) {
            return HeapPool.isAllocated(p);
        }
    }

    static class CodePoolImpl implements Pool {
        public String getName() {
            return "code";
        }
        public Address getBase() {
            return CodePool.getBase();
        }
        public int getSize() {
            return CodePool.getSize();
        }
        public long getRegionSize() {
            return CodePool.getRegionSize();
        }
        public boolean isAllocated(int p) {
            return CodePool.isAllocated(p);
        }
    }

    private static void stackPool() {
        printPool(new StackPoolImpl());
    }

    private static void heapPool() {
        printPool(new HeapPoolImpl());
    }

    private static void codePool() {
        printPool(new CodePoolImpl());
    }

    /**
     * Prints the allocated/free entries in a {@link Pool}.
     * @param pool the pool to  print
     */
    private static void printPool(Pool pool) {
        final long base = VMTestHelper.toLong(pool.getBase());
        final int size = pool.getSize();
        System.out.println(pool.getName() + " pool, base: " + Long.toHexString(base) + ", size: " + size + ", region size: " + pool.getRegionSize());
        int p = 0;
        while (p < size) {
            final long q = p;
            if (pool.isAllocated(p)) {
                while (p < size && pool.isAllocated(p)) {
                    p++;
                }
                System.out.println(q + ".." + (p - 1) + " allocated");
            } else {
                while (p < size && !pool.isAllocated(p)) {
                    p++;
                }
                System.out.println(q + ".." + (p - 1) + " free");
            }
        }
    }

    /**
     * Generates a map of the machine page frames allocated to the domain, and computes maximum contiguous run.
     *
     * @param mask if bit 0 is set prints the map, if bit 1 is set prints maximum contiguous run
     */
    private static void mfnMap(int mask) {
        final long start = GUKPagePool.getStart();
        final long end = GUKPagePool.getEnd();
        final SortedMap<Long, Long> mfnToPfnMap = new TreeMap<Long, Long>();
        for (long i = start; i <= end; i++) {
            final long mfn = GUKPageTables.getMfnForPfn(i);
            mfnToPfnMap.put(mfn, i);
        }
        if ((mask & 1) != 0) {
            int count = 0;
            for (Map.Entry<Long, Long> entry : mfnToPfnMap.entrySet()) {
                System.out.print("mfn: " + Long.toHexString(entry.getKey()) + " pfn: " + Long.toHexString(entry.getValue()));
                count++;
                if (count % 4 == 0) {
                    System.out.println();
                } else {
                    System.out.print("  ");
                }
            }
        }
        if ((mask & 2) != 0) {
            int maxc = 0;
            final Long[] mfns = mfnToPfnMap.keySet().toArray(new Long[mfnToPfnMap.size()]);
            int p = 0;
            while (p < mfns.length) {
                final int q = p;
                p++;
                while (p < mfns.length && mfns[p] == mfns[p - 1] + 1) {
                    p++;
                }
                if (p - q  > maxc) {
                    maxc = p - q;
                }
                System.out.println(Long.toHexString(mfns[q]) + ".." + Long.toHexString(mfns[p - 1]) + " (" + (p - q) + ")");
            }
            System.out.println("max contiguous run " + maxc);
        }
    }

    /**
     * Calculates maximum contiguous run of machine pages allocated to domain using machine page pool map directly.
     * @param mask
     */
    private static void mfnMap2(int mask) {
        final long maxRam = GUKPagePool.getMaximumRamPage();
        if ((mask & 2) != 0) {
            int maxc = 0;
            int p = 0;
            while (p < maxRam) {
                final long q = p;
                if (GUKMachinePagePool.isAllocated(p)) {
                    while (p < maxRam && GUKMachinePagePool.isAllocated(p)) {
                        p++;
                    }
                    if (p - q > maxc) {
                        maxc = (int) (p - q);
                    }
                    System.out.println(Long.toHexString(q) + ".." + Long.toHexString(p - 1) +  " (" + (p - q) + ")");
                } else {
                    p++;
                }
            }
            System.out.println("max contiguous run " + maxc);
        }
    }

    private static void shrinkHeap(int amount) {
        VMConfiguration.vmConfig().heapScheme().decreaseMemory(VMTestHelper.fromInt(amount));
    }

    /**
     * Validates the page frame mappings of all the thread stacks in the pool.
     */
    public static void stackPoolValidate() {
        final Pointer lock = StackPool.getSpinLock();
        GUKSpinLock.spinLock(lock);
        for (int i = 0; i < StackPool.getSize(); i++) {
            if (StackPool.isAllocated(i)) {
                GUKSpinLock.spinUnlock(lock);
                validateStack(i);
                GUKSpinLock.spinLock(lock);
            }
        }
        GUKSpinLock.spinUnlock(lock);
    }

    /**
     * A Java thread stack should have the following format:
     *
     * Red zone page: unmapped
     * Yellow zone page: mapped but not present
     * Zero or more unmapped pages
     * Blue zone page: mapped but not present
     * 7 or more active pages: mapped and present
     *
     * The VM thinks the stack base is at the start of the Yellow zone page.
     * The information on where the blue zone page is currently located is stored
     * in the NativeThreadLocalsStruct, {@link NativeThreadLocal}, but that
     * is not accessible from the stack pool.
     */
    private static void validateStack(int i) {
        final long regionSize = StackPool.getRegionSize();
        final long stackBase = StackPool.getBase().toLong() + i * regionSize;
        final long stackEnd = stackBase + regionSize;
        boolean invalid = false;
        long p = stackBase;
        System.out.println("Validating stack page frames for slot " + i);
        check(p, pageStatus(p), PageStatus.UNMAPPED);
        p += X64VM.PAGE_SIZE;
        check(p, pageStatus(p), PageStatus.MAPPED_NOT_PRESENT);
        p += X64VM.PAGE_SIZE;
        while (p < stackEnd) {
            PageStatus ps = pageStatus(p);
            while (p < stackEnd && ps == PageStatus.UNMAPPED) {
                p += X64VM.PAGE_SIZE;
                ps = pageStatus(p);
            }
            if (p >= stackEnd) {
                // never found any mapped pages
                System.out.println("no active pages found");
                invalid = true;
                break;
            }
            check(p, ps, PageStatus.MAPPED_NOT_PRESENT);
            p += X64VM.PAGE_SIZE;
            ps = pageStatus(p);
            while (p < stackEnd && ps == PageStatus.MAPPED_PRESENT) {
                p += X64VM.PAGE_SIZE;
                ps = pageStatus(p);
            }
            if (p < stackEnd) {
                System.out.println("unexpected page state " + ps + ", at " + Long.toHexString(p));
                invalid = true;
                break;
            }
        }
        System.out.println("stack page frames are " + (invalid ? "invalid" : "valid"));
    }


    private enum PageStatus {
        MAPPED_PRESENT,
        MAPPED_NOT_PRESENT,
        UNMAPPED
    }

    private static PageStatus pageStatus(long p) {
        try {
            GUKPageTables.getPteForAddress(Address.fromLong(p));
            return PageStatus.MAPPED_PRESENT;
        } catch (PteNotPresentException ex) {
            return ex._pte == 0 ? PageStatus.UNMAPPED : PageStatus.MAPPED_NOT_PRESENT;
        }
    }

    private static void check(long p, PageStatus real, PageStatus desired) {
        if (real != desired) {
            System.out.println("page at address " + Long.toHexString(p) + " is " + real + ", should be " + desired);
        }
    }



    private static void createThread() {
        final Thread sleeper = new Sleeper("sleeper");
        sleeper.setDaemon(true);
        sleeper.start();
    }

    static class Sleeper extends Thread {
        Sleeper(String s) {
            super(s);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {

                }
            }
        }
    }
}
