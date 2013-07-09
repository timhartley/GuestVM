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
package com.sun.max.ve.memory;

import static com.sun.max.vm.VMOptions.register;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.ve.guk.GUKBitMap;
import com.sun.max.ve.guk.GUKPagePool;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMSizeOption;
import com.sun.max.vm.code.CodeManager;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.tele.*;
/**
 * An interface to heap virtual memory pool and control of heap size.
 *
 * All heaps sizes and adjustments are multiples of 4MB.
 *
 * @author Mick Jordan
 *
 */

public final class HeapPool {
    public static Address getBase() {
        return maxve_heapPoolBase();
    }

    public static int getSize() {
        return maxve_heapPoolSize();
    }

    public static long getRegionSize() {
        return maxve_heapPoolRegionSize();
    }

    public static boolean isAllocated(int slot) {
        if (_bitMap.isZero()) {
            _bitMap = maxve_heapPoolBitmap();
        }
        return GUKBitMap.isAllocated(_bitMap, slot);
    }

    private static class MyHeapSizeInfo extends Heap.HeapSizeInfo {
        private Size initialSize;
        private Size maxSize;
        private boolean set;
        
        private static final VMSizeOption maxDirectBufferSize = register(new VMSizeOption("-XX:MaxDirectBufferSize", Size.M.times(16), "Virtual memory to reserve for direct buffers."), MaxineVM.Phase.PRISTINE);
        
        @Override
        protected Size getInitialSize() {
            if (!set) {
                setHeapSizeInfo();
            }
            return initialSize;
        }
        
        @Override
        protected Size getMaxSize() {
            if (!set) {
                setHeapSizeInfo();
            }
            return maxSize;
        }
        
        private void setHeapSizeInfo() {
            // Unless overridden on the command line, we set the heap sizes
            // based on the current and maximum memory allocated by the hypervisor,
            // what we have used to date and the code region size (which is managed by the heap)
            final long extra = GUKPagePool.getMaximumReservation() - GUKPagePool.getCurrentReservation();
            long initialHeapSize = toUnit(GUKPagePool.getFreeBulkPages() * 4096);
            initialHeapSize -= toUnit(CodeManager.runtimeCodeRegionSize.getValue().toLong()) +
                                           toUnit(maxDirectBufferSize.getValue().toLong());
            
            if (Inspectable.isVmInspected()) {
                /* some slop for inspectable heap info, should be provided by Inspector not guessed at */
                initialHeapSize -= toUnit(initialHeapSize / 100);
            }
            final long maxHeapSize = toUnit(initialHeapSize + extra * 4096);
            
            initialSize = Heap.initialHeapSizeOption.isPresent() ? super.getInitialSize() : Size.fromLong(initialHeapSize);
            maxSize = Heap.maxHeapSizeOption.isPresent() ? super.getMaxSize() : Size.fromLong(maxHeapSize);
            set = true;
        }
    }

    private static Heap.HeapSizeInfo heapSizeInfo = new MyHeapSizeInfo();
    
    public static Heap.HeapSizeInfo getHeapSizeInfo() {
        return heapSizeInfo;
    }

    private static final long FOUR_MB_MASK = ~((4 * 1024 * 1024) - 1);

    /**
     * Round a size in bytes to multiple of a heap unit.
     * @param n
     * @return
     */
    @INLINE
    public static long toUnit(long n) {
        return n & FOUR_MB_MASK;
    }


    @CONSTANT_WHEN_NOT_ZERO
    private static Pointer _bitMap;

    @C_FUNCTION
    private static native Address maxve_heapPoolBase();

    @C_FUNCTION
    private static native int maxve_heapPoolSize();

    @C_FUNCTION
    private static native Pointer maxve_heapPoolBitmap();

    @C_FUNCTION
    private static native long maxve_heapPoolRegionSize();

}
