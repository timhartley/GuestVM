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

import javax.management.remote.*;
import javax.management.*;

import com.sun.tools.attach.*;
import sun.tools.attach.*;
import test.util.*;

public class ManageAttachTest {

    private static String _jarFile = "/max.ve/java/jdk1.6.0_20/jre/lib/management-agent.jar";
    private static String _agentProps;
    private static String _host = "javaguest7";

    /**
     * @param args
     */
    public static void main(String[] args) {
        final ArgsHandler h = ArgsHandler.process(args);
        if (h._opCount == 0) {
            System.out.println("no operations given");
            return;
        }
        final VirtualMachineDescriptor vmd = AttachTest.findMaxVEVMD();
        assert vmd != null;
        try {
            final VirtualMachine vm = vmd.provider().attachVirtualMachine(vmd);
            MBeanServerConnection mbc = null;
            String agentProps = null;

            for (int j = 0; j < h._opCount; j++) {
                final String opArg1 = h._opArgs1[j];
                final String opArg2 = h._opArgs2[j];
                final String op = h._ops[j];

                if (op.equals("port")) {
                    addArg("com.sun.management.jmxremote.port=" + opArg1);
                } else if (op.equals("ssl")) {
                    addArg("com.sun.management.jmxremote.ssl=" + opArg1);
                } else if (op.equals("auth")) {
                    addArg("com.sun.management.jmxremote.authenticate=" + opArg1);
                } else if (op.equals("localonly")) {
                    addArg("com.sun.management.jmxremote.local.only=" + opArg1);
                } else if (op.equals("connect")) {
                    vm.loadAgent(_jarFile, _agentProps);
                    final JMXServiceURL u = new JMXServiceURL(
                                    "service:jmx:rmi:///jndi/rmi://" + _host + ":" + opArg1 + "/jmxrmi");
                    System.out.println("connecting to: " + u.toString());
                    final JMXConnector c = JMXConnectorFactory.connect(u);
                    System.out.println("connector id: " + c.getConnectionId());
                    mbc = c.getMBeanServerConnection();

                } else if (op.equals("names")) {
                    final Set<ObjectName> objectNames = mbc.queryNames(null, null);
                    System.out.println("ObjectNames");
                    for (ObjectName objectName : objectNames) {
                        System.out.println(objectName.toString());
                    }
                } else if (op.equals("instances")) {
                    final Set<ObjectInstance> objectInstances = mbc.queryMBeans(null, null);
                    for (ObjectInstance objectInstance : objectInstances) {
                        System.out.println(objectInstance.toString());
                    }
                } else if (op.equals("namepattern")) {
                    final Set<ObjectName> objectNames = mbc.queryNames(new ObjectName(opArg1), null);
                    System.out.println("ObjectNames");
                    for (ObjectName objectName : objectNames) {
                        System.out.println(objectName.toString());
                    }
                } else if (op.equals("invoke")) {
                    final ObjectName objectName = new ObjectName(opArg1);
                    try {
                        final Object result = mbc.invoke(objectName, opArg2, new Object[0], new String[0]);
                        System.out.println(result);
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private static void addArg(String arg) {
        if (_agentProps == null) {
            _agentProps = arg;
        } else {
            _agentProps += "," + arg;
        }
    }
}
