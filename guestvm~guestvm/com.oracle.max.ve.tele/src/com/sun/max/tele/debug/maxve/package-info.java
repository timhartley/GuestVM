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
/**
 * @author Grzegorz Milos
 * @author Mick Jordan
 *
 * The Maxine Virtual Edition specific implementation the "tele" layer for the Maxine Inspector.
 * Several implementations are provided and selected at runtime by {@link com.sun.max.tele.debug.max.ve.MaxVEXenDBChannel}.
 * <ul>
 * <li>Direct connection via {@link com.sun.max.tele.debug.max.ve.MaxVEDBNativeTeleChannelProtocol}.</li>
 * <li>Indirection connection via TCP using {@link com.sun.max.tele.debug.max.ve.MaxVETCPTeleChannelProtocol}, to an agent running in dom0
 * using {@link com.sun.max.tele.debug.max.ve.MaxVEDBNativeTeleChannelProtocol}.</li>
 * <li>Indirection connection via TCP using {@link com.sun.max.tele.debug.max.ve.MaxVETCPTeleChannelProtocol}, to an agent running in dom0
 * using the "gdbsx" agwnt (TBD).
 * <li>Connection to a Xen dump file using {@link com.sun.max.tele.debug.max.ve.MaxVEDumpTeleChannelProtocol}.</li>
 * </ul>
 */
package com.sun.max.tele.debug.maxve;
