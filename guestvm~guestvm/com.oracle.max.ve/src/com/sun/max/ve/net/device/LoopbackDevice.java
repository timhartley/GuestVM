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
package com.sun.max.ve.net.device;

import com.sun.max.ve.net.Packet;

public class LoopbackDevice implements NetDevice {

    public boolean active() {
        return true;
    }


    public long dropCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long truncateCount() {
        return 0;
    }

    public byte[] getMACAddress() {
        // TODO Auto-generated method stub
        return null;
    }


    public int getMTU() {
        // TODO Auto-generated method stub
        return 0;
    }


    public String getNICName() {
        // TODO Auto-generated method stub
        return "lo0";
    }


    public void registerHandler(Handler handler) {
        // TODO Auto-generated method stub

    }


    public void setReceiveMode(int mode) {
        // TODO Auto-generated method stub

    }


    public void transmit(Packet buf) {
        // TODO Auto-generated method stub
    }


    public void transmit1(Packet buf, int offset, int size) {
        // TODO Auto-generated method stub
    }

}
