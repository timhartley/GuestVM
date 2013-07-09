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
package com.sun.max.config.max.ve.jdk;

import com.sun.max.config.BootImagePackage;
import com.sun.max.vm.hosted.Extensions;

/**
 * Additional JDK packages/classes added to the Virtual Edition boot image and their customisations.
 *
 * @author Mick Jordan
 *
 */

public class Package extends BootImagePackage {
    private static final String[] packages = {
        "java.net.*",
        "java.security.cert.Certificate",
        "java.util.concurrent.*",
        "java.util.concurrent.locks.*",
        "java.util.logging.Level",
        "java.util.logging.LogRecord",
        "java.text.*",
        "java.text.spi.*",
        "sun.net.*",
        "sun.net.util.*",
        "sun.net.spi.*",
        "sun.net.www.*",
        "sun.net.www.protocol.jar.*",
        "sun.nio.*",
//        "sun.reflect.*",
        "sun.jkernel.DownloadManager",
         "sun.management.ManagementFactory",
         "sun.management.VMManagementImpl",
         "sun.util.*",
         "sun.util.calendar.*",
         "com.sun.security.auth.*",
         "sun.security.acl.*",
         "sun.security.action.GetBooleanAction",
         "sun.security.action.LoadLibraryAction",
         "sun.security.provider.PolicyFile",
         "sun.security.util.*"
    };

    public Package() {
        super(packages);
        String className = "java.net.InetAddress";
        Extensions.registerClassForReInit(className);
        String[] fields = new String[] {"nameService", "addressCache", "negativeCache",
                        "addressCacheInit", "unknown_array", "impl", "lookupTable"};
        for (String fieldName : fields) {
            Extensions.resetField(className, fieldName);
        }
        className = "java.lang.reflect.Proxy";
        Extensions.resetField(className, "loaderToCache");
        Extensions.resetField(className, "proxyClasses");
        Extensions.registerClassForReInit(className);
        Extensions.registerClassForReInit("sun.security.util.Debug");
        Extensions.registerClassForReInit("sun.util.LocaleServiceProviderPool");
        className =  "sun.util.calendar.ZoneInfo";
        Extensions.resetField(className, "aliasTable");
        className += "File";
        fields = new String[] {"zoneInfoMappings", "rawOffsets", "rawOffsetIndices",
                        "zoneIDs", "excludedIDs", "hasNoExcludeList", "zoneInfoObjects", "ziDir"};
        for (String fieldName : fields) {
            Extensions.resetField(className, fieldName);
        }
        Extensions.registerClassForReInit("sun.util.calendar.ZoneInfoFile");
        Extensions.registerClassForReInit(className);
        Extensions.registerClassForReInit("java.net.NetworkInterface");
        Extensions.registerClassForReInit("java.net.PlainDatagramSocketImpl");
        Extensions.registerClassForReInit("java.net.PlainSocketImpl");
        Extensions.registerClassForReInit("java.text.SimpleDateFormat");
        Extensions.registerClassForReInit("java.text.DateFormatSymbols");
    }

}
