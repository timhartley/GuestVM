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
package test.sun.attach;

import java.util.*;

import com.sun.tools.attach.*;
import sun.tools.attach.*;
import test.util.*;


public class AttachTest {

    private static boolean _verbose;
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        final ArgsHandler h = ArgsHandler.process(args);
        if (h._opCount == 0) {
            System.out.println("no operations given");
            return;
        }
        _verbose = h._verbose;
        final VirtualMachineDescriptor vmd = findMaxVEVMD();
        assert vmd != null;
        VirtualMachine vm = null;

        for (int j = 0; j < h._opCount; j++) {
            final String opArg1 = h._opArgs1[j];
            final String opArg2 = h._opArgs2[j];
            final String op = h._ops[j];

            try {
                if (op.equals("attach")) {
                    vm = vmd.provider().attachVirtualMachine(vmd);
                } else if (op.equals("getSystemProperties")) {
                    final Properties sysProps = vm.getSystemProperties();
                    for (Map.Entry<Object, Object> entry : sysProps.entrySet()) {
                        System.out.print(entry.getKey());
                        System.out.print('=');
                        System.out.println(entry.getValue());
                    }
                } else if (op.equals("loadAgent")) {
                    vm.loadAgent(opArg1, opArg2);
                    System.out.println("agent: " + opArg1 + " loaded with arg: " + opArg2);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    static VirtualMachineDescriptor findMaxVEVMD() {
        final List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for (VirtualMachineDescriptor vmd : list) {
            if (_verbose) {
                System.out.println(vmd.toString());
            }
            if (vmd.provider() instanceof MaxVEAttachProvider) {
                return vmd;
            }
        }
        return null;
    }
}
