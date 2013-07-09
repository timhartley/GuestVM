/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ve.logging;

import java.io.*;

/**
 * A log handler that outputs to a file. The sibling guest file system is the default location.
 * 
 * @author Mick Jordan
 *
 */
public class FileLogHandler extends Handler {

    private static final String FILE_PROPERTY = "max.ve.logging.handler.sg.file";
    private static final String DEFAULT_FILE = "/sg/logger.log";
    private static PrintStream ps;
    
    public FileLogHandler() {
        String file = System.getProperty(FILE_PROPERTY);
        if (file == null) {
            file = DEFAULT_FILE;
        }
        try {
            ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));   
            Runtime.getRuntime().addShutdownHook(new CloseHook());
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
    
    @Override
    public void println(String msg) {
        if (ps != null) {
            ps.println(msg);
        }
    }
    
    static class CloseHook extends Thread {
        @Override
        public void run() {
            ps.close();
        }
    }

}
