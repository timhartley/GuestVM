/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.VirtualMemory;
import com.sun.max.program.ProgramError;
import com.sun.max.unsafe.Size;
import com.sun.max.ve.error.VEError;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.heap.Heap;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.INTRINSIC;

/**
 * A {@link PageDirectByteBuffer} is a custom variant of {@link DirectByteBuffer} for use
 * in VE for buffers that are multiples of the page size and have a lifetime that matches the VM.
 * They are allocated from virtual memory, whereas {@link DirectByteBuffer} uses malloc
 * memory and over-allocates in order to guarantee page alignment, resulting
 * in 3 pages allocated when 1 is requested. 
 * 
 * We invoke the private constructor provided for JNI_NewDirectByteBuffer using
 * Maxine's {@link ALIAS} mechanism. No "cleaner" is set up for such buffers
 * so they cannot be reclaimed.
 * 
 * 
 * @author Mick Jordan
 *
 */

public class PageDirectByteBuffer {

    @ALIAS(declaringClassName="java.nio.DirectByteBuffer", name="<init>")
    private native void init(long addr, int cap);
    
    @INTRINSIC(UNSAFE_CAST) static native PageDirectByteBuffer asPageDirectByteBuffer(Object obj);
    @INTRINSIC(UNSAFE_CAST) public static native ByteBuffer asByteBuffer(Object obj);
    
    private static ClassActor directByteBufferActor;
    
    static {
        try {
            directByteBufferActor = ClassActor.fromJava(Class.forName("java.nio.DirectByteBuffer"));
        } catch (ClassNotFoundException ex) {
            // happens at image build time
            ProgramError.unexpected("can't load DirectByteBuffer", ex);
        }
    }
    
    public static ByteBuffer allocateDirect(int cap) {
        final ByteBuffer byteBuffer = asByteBuffer(Heap.createTuple(directByteBufferActor.dynamicHub()));
        PageDirectByteBuffer thisByteBuffer = asPageDirectByteBuffer(byteBuffer);
        long va = VirtualMemory.allocate(Size.fromInt(cap), VirtualMemory.Type.DATA).toLong();
        if (va == 0) {
            VEError.unexpected("can't allocate direct byte buffer of size: " + cap);
        }
        thisByteBuffer.init(va, cap);
        return byteBuffer;
    }
}
