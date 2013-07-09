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
/**
 * Basic support for spin locks.
 *
 * All the classes are abstract, either specifying interfaces or proving support methods for concrete subclasses.
 * The most basic concrete subclasses are in the guk sub-package, and delegate to the C implementation
 * in the microkernel, which is a test and set and test lock that disables pre-emption.
 *
 * The abstract class SpinLock provides the basic lock/unlock abstract methods.
 *
 * For pedagogical reasons the class hierarchy is split into two branches, one that disables
 * thread pre-emption (NP) while holding the lock and one that doesn't (P). In practice it is a bad
 * idea to allow pre-emption while holding a spin lock and this can be demonstrated by
 * testing with the pre-empting variants. Of course, in a context where pre-emption is already disabled
 * the P variants are useful.
 *
 * OPSpinLock adds the abstracts methods to disable/enable pre-emption. PSpinLock
 * implements these with empty methods, whereas NPSpinLock invokes the (inlined) code in GVmThread
 * to actually disable/enable pre-emption in the microkernel.
 *
 * In order to be able to inline as much code as possible, the class hierarchy is duplicated into P and NP
 * variants that have identical code except that the superclass follows the P or NP track.
 * In principle it would be possible to avoid this duplication if the compiler was smart enough to duplicate
 * the compiled code of the intermediate classes in the hierarchy, e.g., NPSpinLock, and inline it appropriately.
 *
 * The P/NPFieldSpinLock subclasses provide the basic lock and unlock implementation in terms of
 * a volatile int lock field and a compare and exchange instruction, courtesy of Maxine. They also handle
 * the enabling and disabling of pre-emption.
 *
 * For measurement purposes, the subclasses P/NPCountingSpinLock add a field that counts the number
 * of times a thread spins trying to acquire the lock. The max count observed can be accessed by the
 * getMaxCount method of the CountingSpinLock interface.
 *
 * Concrete classes are created elsewhere in Guest VM using the SpinLockFactory class or its subclasses.
 * The default subclass to use can be specified by a system property, "max.ve.spinlock.factory.class".
 *
 */

package com.sun.max.ve.spinlock;
