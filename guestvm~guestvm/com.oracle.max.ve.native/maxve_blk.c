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

/*
 * Shim to the blk device.
 * Author: Mick Jordan
 */
#include <os.h>
#include <hypervisor.h>
#include <types.h>
#include <lib.h>
#include <mm.h>
#include <jni.h>
#include <blk_front.h>

JNIEXPORT int JNICALL
Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeGetDevices(JNIEnv *env, jclass c) {
    return blk_get_devices();
}

JNIEXPORT int JNICALL
Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeGetSectors(JNIEnv *env, jclass c, jint device) {
    return blk_get_sectors(device);
}

JNIEXPORT long JNICALL
Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeWrite(JNIEnv *env, jclass c, jint device, jlong address, void *buf, jint length) {
    return blk_write(device, address, buf, length);
}

JNIEXPORT long JNICALL
Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeRead(JNIEnv *env, jclass c, jint device, jlong address, void *buf, jint length) {
    return blk_read(device, address, buf, length);
}

void *blk_dlsym(const char *symbol) {
    if (strcmp(symbol, "Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeGetDevices") == 0)
      return Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeGetDevices;
    else if (strcmp(symbol, "Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeGetSectors") == 0)
      return Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeGetSectors;
    else if (strcmp(symbol, "Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeWrite") == 0)
      return Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeWrite;
    else if (strcmp(symbol, "Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeRead") == 0)
      return Java_com_sun_max_ve_blk_guk_GUKBlkDevice_nativeRead;
    else return 0;
}
