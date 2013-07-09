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
 * Native symbols for Maxine startup/shutdown and other miscellaneous functions.
 */

#include <os.h>
#include <hypervisor.h>
#include <types.h>
#include <lib.h>
#include <mm.h>
#include <lib.h>

extern void *native_executablePath(void);
extern void native_exit(void);
extern void native_trap_exit(int code, void *address);
extern void *native_environment(void);
//extern void nativeInitializeJniInterface(void *jnienv);
extern void *native_properties(void);
/*
 * These functions are referenced from MaxineNative/image.c
 */
void exit(int n) {
  ok_exit();
}

int getpagesize(void) {
   return PAGE_SIZE;
}

void *maxine_misc_dlsym(const char *symbol) {
/*    if (strcmp(symbol, "nativeInitializeJniInterface")  == 0) return nativeInitializeJniInterface;
    else */if (strcmp(symbol, "native_exit") == 0) return native_exit;
    else if (strcmp(symbol, "native_trap_exit") == 0) return native_trap_exit;
    else if (strcmp(symbol, "native_executablePath") == 0) return native_executablePath;
    else if (strcmp(symbol, "native_environment") == 0) return native_environment;
    else if (strcmp(symbol, "native_properties") == 0) return native_properties;
    else if (strcmp(symbol, "exit") == 0) return exit;
    else return 0;
}
