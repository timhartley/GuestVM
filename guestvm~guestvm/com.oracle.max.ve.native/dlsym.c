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
 * Maxine assumes that native symbols are resolved by the dlsym function, which is normally
 * part of the dynamic library mechanism on traditional operating systems. dlsym is typically
 * called once per native symbol resolution and subsequently cached in the VM, so performance
 * of the lookup is not an issue.
 *
 * Maxine VE has a very simple dlsym implementation that simply assumes that the native
 * symbol is defined somewhere in the image. So for a "char *symbol" argument it
 * simply returns symbol. This requires a definition for the symbol at compile time and
 * an actual implementation at run time.
 *
 * Rather than have one large lookup table, the implementation of dlsym is distributed
 * among the several files. In particular Maxine VE native code that defines such functions
 * always includes a subsystem dlsym function named "subsystem_dlsym".
 *
 * Author: Mick Jordan
 *
 */

#include <os.h>
#include <hypervisor.h>
#include <types.h>
#include <spinlock.h>
#include <sched.h>
#include <lib.h>
#include <mm.h>
#include <time.h>
#include <jni.h>
#include <xmalloc.h>
#include <console.h>

extern void * maxine_dlsym(const char *symbol);
extern void * fs_dlsym(const char *symbol);
extern void * net_dlsym(const char *symbol);
extern void * guk_dlsym(const char *symbol);
extern void * blk_dlsym(const char *symbol);
extern void * thread_stack_pool_dlsym(const char *symbol);
extern void * heap_pool_dlsym(const char *symbol);
extern void * code_pool_dlsym(const char *symbol);
extern void * inflater_dlsym(const char *symbol);
extern void * StrictMath_dlsym(const char *symbol);


void *dlsym(void *a1, const char *symbol) {
  void *result;
  if ((result = maxine_dlsym(symbol)) ||
      (result = fs_dlsym(symbol)) ||
      (result = net_dlsym(symbol)) ||
      (result = guk_dlsym(symbol)) ||
      (result = blk_dlsym(symbol)) ||
      (result = thread_stack_pool_dlsym(symbol)) ||
      (result = heap_pool_dlsym(symbol)) ||
      (result = code_pool_dlsym(symbol)) ||
      (result = StrictMath_dlsym(symbol))) {
    return result;
  } else if (strcmp(symbol, "JNI_OnLoad") == 0) {
	  // special case, allows library loading to appear to work but avoids invoking JNI_OnLoad
	  return 0;
  } else {
    guk_printk("maxve: symbol %s not found, exiting\n", symbol);
    crash_exit();
    return 0;
  }
}

struct dlhandle {
  char *path;
};

void *dlopen(char *path, int flags) {
  struct dlhandle * handle = xmalloc(struct dlhandle);
  //printk("dlopen: %s\n", path);
  handle->path = path;
  return handle;
}

char *dlerror(void) {
  guk_printk("maxve: dlerror not implemented\n");
  return NULL;
}

int dlclose(void) {
	guk_printk("maxve: dlclose not implemented\n");
  return 0;
}

