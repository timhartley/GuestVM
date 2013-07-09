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
 * Varieties of test and set spin locks.
 * We elide the NP prefix in the concrete classes, i.e., the lack of a P prefix implies
 * that thread pre-emption is disabled while the lock is held.
 *
 * TAS: Simple test and set.
 * TTAS: Test and test and set.
 * TAST: Test and set and test.
 *
 * C prefix: Count number of spins.
 * P prefix: Allow thread pre-emption while holding lock.
 *
 * The P variants are in the tas.p subpackage, the C variants in the tas.c subpackage and
 * the PC variants in the tas.cp subpackage.
 *
 * The difference between TTAS and TAST is that the latter tries to get the lock
 * first and, if unsuccessful, spins waiting for it to appear free, whereas the former checks first
 * and then tries to get the lock. If TAST fails to get the lock on the first attempt,
 * it therefore becomes a TTAS lock.
 *
 * E.g. TASSpinLock: Test and set spin lock that disables pre-emption while holding the lock.
 *        PCTTASSpinLock: Test and test and set spin lock, with pre-emption enabled and counts spins.
 *
 * Each spin lock class has an associated factory class to create instances.
 *
  */

package com.sun.max.ve.spinlock.tas;
