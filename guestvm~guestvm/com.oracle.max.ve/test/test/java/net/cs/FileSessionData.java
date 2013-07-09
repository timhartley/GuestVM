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
package test.java.net.cs;

import java.io.*;

public class FileSessionData extends SessionData {

    private String _pathname;

    FileSessionData(String pathname) {
        _pathname = pathname;
    }

    @Override
    public byte[] getSessionData() {
        FileInputStream fs = null;
        byte[] data = null;
        try {
            final File f = new File(_pathname);
            data = new byte[(int) f.length()];
            fs = new FileInputStream(f);
            int count = 0;
            int offset = 0;
            // CheckStyle: stop inner assignment check
            while ((offset < data.length) && (count = fs.read(data, offset, data.length - offset)) > 0) {
                offset += count;
            }
         // CheckStyle: resume inner assignment check
            if (offset != data.length) {
                throw new IOException("partial read of serialized file");
            }
        } catch (IOException e) {
            System.out.print(e);
            System.exit(-1);
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e2) {
                }
            }
        }
        return data;
    }
}

