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
package sun.tools.attach;

import java.io.IOException;
import java.util.*;

/**
 * An implementation of @see Attachprovider for Guest VM.
 *
 * @author Mick Jordan
 */

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.AttachProvider;

public class MaxVEAttachProvider extends HotSpotAttachProvider {

    private static final String ATTACH_ID_PROPERTY = "max.ve.attach.id";
    private static Map<String, MaxVEVirtualMachineDescriptor> _vmMap = new HashMap<String, MaxVEVirtualMachineDescriptor>();

    public MaxVEAttachProvider() {

    }

    @Override
    public String name() {
        return "sun";
    }

    @Override
    public String type() {
        return "socket";
    }

    @Override
    public VirtualMachine attachVirtualMachine(String vmid) throws AttachNotSupportedException, IOException {
        return new MaxVEVirtualMachine(this, vmid, _vmMap.get(vmid));
    }

    @Override
    public List<VirtualMachineDescriptor> listVirtualMachines() {
        final List<VirtualMachineDescriptor> result = new ArrayList<VirtualMachineDescriptor>();
        /*
         * For the local environment the complete way to do this would be to access the Xenstore to find all the Guest VM domains.
         * That requires a native library so, for now, we require the host:domainid to be passed as a property, max.ve.attach.id.
         */
        final String attachProperty = System.getProperty(ATTACH_ID_PROPERTY);
        if (attachProperty != null) {
            final int cx = attachProperty.indexOf(':');
            if (cx > 0) {
                final String domainIdString = attachProperty.substring(cx + 1);
                final String host = attachProperty.substring(0, cx);
                final int domainId = Integer.parseInt(domainIdString);
                final MaxVEVirtualMachineDescriptor vmd = new MaxVEVirtualMachineDescriptor(this, attachProperty, "max.ve domain " + attachProperty, host, domainId);
                _vmMap.put(attachProperty, vmd);
                result.add(vmd);
            }
        }
        return result;
    }

}
