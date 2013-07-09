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
package com.sun.max.tele.channel.agent.maxve;

import com.sun.max.tele.channel.agent.AgentProtocolAdaptor;
import com.sun.max.tele.channel.agent.TeleProcess;
import com.sun.max.tele.debug.maxve.MaxVETeleChannelProtocol;

/**
 * Variant of the adaptor that implements the {@link MaxVETeleChannelProtocol} protocol.
 *
 * @author Mick Jordan
 *
 */

public class MaxVEAgentProtocolAdaptor extends AgentProtocolAdaptor implements MaxVETeleChannelDataIOProtocol {
    private MaxVETeleChannelProtocol impl;

    public MaxVEAgentProtocolAdaptor(TeleProcess teleProcess, MaxVETeleChannelProtocol impl) {
        super(teleProcess, impl);
        this.impl = impl;
    }

    @Override
    public void setNativeAddresses(long threadListAddress, long bootHeapStartAddress, long resumeAddress) {
        impl.setNativeAddresses(threadListAddress, bootHeapStartAddress, resumeAddress);
    }
}
