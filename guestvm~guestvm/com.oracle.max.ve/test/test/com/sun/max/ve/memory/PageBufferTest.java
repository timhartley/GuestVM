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
package test.com.sun.max.ve.memory;

import java.nio.*;
import com.sun.max.ve.memory.PageDirectByteBuffer;


public class PageBufferTest {
    private static final int BLOCKSIZE = 4096;
    private static final int NBLOCKS= 8;
    

    public static void main(String[] args) {
        ByteBuffer parent = PageDirectByteBuffer.allocateDirect(BLOCKSIZE * NBLOCKS);
        System.out.println("parent: " + parent.toString());
        ByteBuffer[] subBlocks = new ByteBuffer[NBLOCKS];
        for (int i = 0; i < NBLOCKS; i++) {
                parent.position(i * BLOCKSIZE);
                subBlocks[i] = parent.slice();
                subBlocks[i].limit(BLOCKSIZE);
                System.out.println("subBlock[" + i + "]: " + subBlocks[i].toString());
        }
        parent.position(0);

    }

}
