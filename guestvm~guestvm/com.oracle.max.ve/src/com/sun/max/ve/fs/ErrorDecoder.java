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
package com.sun.max.ve.fs;

/**
 * Standard "errno" error decoding.
 * TODO: flesh it out
 *
 * @author Mick Jordan
 *
 */
public class ErrorDecoder {

    public enum Code {
        ENOENT(2, "No such file or directory"),
        EINTR(4, "Interrupted system call"),
        EIO(5, "I/O error"),
        EBADF(9, "Bad file number"),
        EAGAIN(11, "Resource temporarily unavailable"),
        EACCES(13, "Permission denied"),
        EISDIR(21, "Is a directory"),
        EROFS(30, "Read only file system"),
        EPIPE(32, "Broken pipe");

        private int _code;
        private String _message;

        Code(int code, String message) {
            _code = code;
            _message = message;
        }

        public int getCode() {
            return _code;
        }

        public String getMessage() {
            return _message;
        }

    }

    public static String getMessage(int errno) {
        for (Code c : Code.values()) {
            if (errno == c.getCode()) {
                return c.getMessage();
            }
        }
        return "unknown error code: " + errno;
    }

    public static String getFileMessage(int errno, String name) {
        return name + " (" + getMessage(errno) + ")";
    }

}
