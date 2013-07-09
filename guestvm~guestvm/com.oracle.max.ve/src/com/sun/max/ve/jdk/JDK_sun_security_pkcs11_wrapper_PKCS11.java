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

import java.util.*;
import com.sun.max.annotate.*;

/**
 * A very incomplete substitution for the native methods in PKCS11.
 * C_GenerateRandom is invoked by File.createTempFile (why?) and
 * this is just a hack to support that for now.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")
@METHOD_SUBSTITUTIONS(sun.security.pkcs11.wrapper.PKCS11.class)
public class JDK_sun_security_pkcs11_wrapper_PKCS11 {

    private static Random _random = new Random();;

    // Checkstyle: stop method name check

     @SUBSTITUTE
    private void  C_GenerateRandom(long hSession, byte[] randomData) throws sun.security.pkcs11.wrapper.PKCS11Exception {
        _random.nextBytes(randomData);
    }
}
