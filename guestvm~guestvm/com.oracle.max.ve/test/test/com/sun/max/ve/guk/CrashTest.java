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
package test.com.sun.max.ve.guk;

import com.sun.max.ve.guk.*;
import com.sun.max.ve.spinlock.guk.*;

/**
 * This is a simple test that forces a GUK crash by calling the scheduler in a thread that
 * holds a spin-lock, and therefore, has pre-emption disabled. It can be used to test
 * whether the Inspector gains control (if the domain is running under the Inspector).
 *
 * @author Mick Jordan
 *
 */
public class CrashTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final GUKSpinLock spinLock = (GUKSpinLock) GUKSpinLockFactory.createAndInit();
        System.out.println("Testing lock");
        test(spinLock);
        System.out.println("Tested lock ok, going for crash");
        spinLock.lock();
        GUKScheduler.schedule();
    }

    private static void test(GUKSpinLock spinLock) {
        for (int i = 0; i < 100; i++) {
            spinLock.lock();
            spinLock.unlock();
        }
    }

}
