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
package com.sun.max.ve.jdk;

import com.sun.max.annotate.*;
import com.sun.max.ve.fs.exec.*;
import com.sun.max.ve.guk.GUKExec;
import com.sun.max.ve.logging.*;
import com.sun.max.ve.process.*;

import java.io.*;
import java.util.logging.Level;

/**
  *Substitutions for @see java.lang.UNIXProcess.
  * @author Mick Jordan
  */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(className = "java.lang.UNIXProcess")
public final class JDK_java_lang_UNIXProcess {
    private static Logger _logger;
    private static ExecFileSystem _execFS;
    private static FilterFileSystem _filterFS;

    private static void init() {
        if (_logger == null) {
            _logger = Logger.getLogger("UNIXProcess");
            _execFS = ExecFileSystem.create();
            _filterFS = FilterFileSystem.create();
        }
    }

    @SUBSTITUTE
    private int waitForProcessExit(int key) {
        final VEProcessFilter filter = VEProcessFilter.getFilter(key);
        if (filter != null) {
            return filter.waitForProcessExit(key);
        } else {
            return GUKExec.waitForProcessExit(key);
        }
    }

    @SUBSTITUTE
    private int forkAndExec(byte[] prog, byte[] argBlock, int argc, byte[] envBlock, int envc, byte[] dir, boolean redirectErrorStream, FileDescriptor stdinFd, FileDescriptor stdoutFd,
                    FileDescriptor stderrFd) throws IOException {
        init();
        ExecHelperFileSystem execFS = _execFS;
        int key;

        final VEProcessFilter filter = VEProcessFilter.filter(prog);
        if (filter != null) {
            key = filter.exec(prog, argBlock, argc, envBlock, envc, dir);
            /* The filter may have elected to have the call (possibly modified) handled externally. */
            if (VEProcessFilter.getFilter(key) != null) {
                execFS = _filterFS;
            }
        } else {
            logExec(prog, argBlock, dir);
            key = GUKExec.forkAndExec(prog, argBlock, argc, dir);
        }
        if (key < 0) {
            throw new IOException("Exec failed");
        } else {
            final int[] fds = execFS.getFds(key);
            JDK_java_io_FileDescriptor.setFd(stdinFd, fds[0]);
            JDK_java_io_FileDescriptor.setFd(stdoutFd, fds[1]);
            JDK_java_io_FileDescriptor.setFd(stderrFd, fds[2]);
        }
        return key;
    }

    public static void logExec(byte[] prog, byte[] argBlock, byte[] dir) {
        if (_logger.isLoggable(Level.WARNING)) {
            final StringBuilder sb = new StringBuilder("application is trying to start a subprocess: ");
            if (dir != null) {
                sb.append("in directory: ");
                bytePrint(sb, dir, '\n');
            }
            bytePrint(sb, prog, ' ');
            bytePrintBlock(sb, argBlock, '\n');
            _logger.warning(sb.toString());
        }
    }

    public static Logger getLogger() {
        return _logger;
    }

    /**
     * Output a null-terminated byte array (@See java.lang.ProcessImpl).
     * @param data
     */
    private static void bytePrint(StringBuilder sb, byte[] data, char term) {
        for (int i = 0; i < data.length; i++) {
            final byte b = data[i];
            if (b == 0) {
                break;
            }
            sb.append((char) b);
        }
        sb.append(term);
    }

    /**
     * Output a null-separated, null-terminated byte array (@See java.lang.ProcessImpl).
     * @param data
     */
    private static void bytePrintBlock(StringBuilder sb, byte[] data, char term) {
        for (int i = 0; i < data.length; i++) {
            final byte b = data[i];
            if (b == 0) {
                sb.append(' ');
            } else {
                sb.append((char) b);
            }
        }
        sb.append(term);
    }

    @SUBSTITUTE
    private static void destroyProcess(int key) {
        init();
        final VEProcessFilter filter = VEProcessFilter.getFilter(key);
        if (filter != null) {
            filter.destroyProcess(key);
            return;
        }
        if (_logger.isLoggable(Level.WARNING)) {
            _logger.warning("application is trying to destroy process: " + key);
        }
        GUKExec.destroyProcess(key);
    }

    @SUBSTITUTE
    private static void initIDs() {

    }
}
