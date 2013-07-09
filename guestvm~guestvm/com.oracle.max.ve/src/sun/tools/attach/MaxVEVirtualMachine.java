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

import java.io.*;
import java.net.Socket;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.AttachProvider;

import com.sun.max.ve.attach.AttachPort;

public class MaxVEVirtualMachine extends HotSpotVirtualMachine {

    private MaxVEVirtualMachineDescriptor _vmd;

    MaxVEVirtualMachine(AttachProvider provider, String vmid, MaxVEVirtualMachineDescriptor vmd) throws AttachNotSupportedException, IOException {
        super(provider, vmid);
        _vmd = vmd;
    }

    @Override
    InputStream execute(String cmd, Object... args) throws AgentLoadException, IOException {
        System.out.print("execute:" + cmd);
        final Socket sock = new Socket(_vmd.host(), AttachPort.getPort());
        InputStream in = null;
        OutputStream out = null;
        System.out.println("connected");
        for (Object arg : args) {
            if (arg != null) {
                System.out.print(" ");
                System.out.print(arg);
            }
        }
        System.out.println();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        out.write(1 + args.length);
        writeString(out, cmd);
        for (Object arg : args) {
            writeString(out, (String) arg);
        }
        return in;
    }

    /*
     * Write/sends the given to the target VM. String is transmitted in UTF-8 encoding.
     */
    private void writeString(OutputStream out, String s) throws IOException {
        byte[] b;
        if (s != null && s.length() > 0) {
            try {
                b = s.getBytes("UTF-8");
                out.write(b.length);
                out.write(b);
            } catch (java.io.UnsupportedEncodingException x) {
                throw new InternalError();
            }
        } else {
            out.write(0);
        }
    }

    @Override
    public void detach() throws IOException {

    }

    @Override
    public void loadAgent(String agent, String options) throws AgentLoadException, AgentInitializationException, IOException {
        final InputStream in = execute("load", agent, "true", options);
        try {
            final int result = readInt(in);
            if (result != 0) {
                throw new AgentInitializationException("Agent_OnAttach failed", result);
            }
        } finally {
            in.close();
        }
    }

    @Override
    public void loadAgentLibrary(String agentLibrary, String options) throws AgentLoadException, AgentInitializationException, IOException {
        throwAgentLibraryNotSupported();
    }

    @Override
    public void loadAgentPath(String agentLibrary, String options) throws AgentLoadException, AgentInitializationException, IOException {
        throwAgentLibraryNotSupported();
    }

    private static void throwAgentLibraryNotSupported() throws AgentLoadException {
        throw new AgentLoadException("native agent libraries not supported");
    }

}
