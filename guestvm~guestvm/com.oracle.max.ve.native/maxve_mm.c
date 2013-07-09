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
 * Implementation of native methods from the Maxine Memory/VirtualMemory classes and malloc etc.
 *
 * Author: Mick Jordan
 */
#include <os.h>
#include <sched.h>
#include <hypervisor.h>
#include <types.h>
#include <lib.h>
#include <xmalloc.h>
#include <jni.h>
#include <virtualMemory.h>

#undef malloc
#undef free
#undef realloc

/*
 * This version just forwards to GUK xmalloc
 * TODO: not exit here when out of heap
 */
void init_malloc(void) {
}

void *malloc(size_t n) {
  void *result = guk_xmalloc(n, 4);
  if (result == NULL) {
    guk_xprintk("malloc: out of heap, request is %lx\n", n);
    guk_printk("malloc: out of heap, request is %lx\n", n);
    guk_crash_exit_backtrace();
  }
  return result;
}

void free(void *a1) {
  guk_xfree(a1);
}

void *realloc(void *p, size_t n) {
  void *result = guk_xrealloc(p, n, 4);
  if (result == NULL) {
    guk_xprintk("realloc: out of heap, request is %lx\n", n);
    guk_crash_exit();
  }
  return result;
}

void *calloc(int nelems, size_t n) {
  size_t count = n * nelems;
  void *result = malloc(count);
  memset(result, 0, count);
//  ttprintk("calloc %d %d %lx\n", nelems, n , result);
  return result;
}

void *maxve_virtualMemory_allocate(size_t size, int type);
void *valloc(size_t size) {
	return maxve_virtualMemory_allocate(size, DATA_VM);
}

extern void *allocate_heap_region(int n);
extern void *extend_allocate_heap_region(unsigned long vaddr, int n);
extern void *deallocate_heap_region(unsigned long vaddr, size_t size);
extern void *allocate_code_region(int n, unsigned long vaddr);

void *maxve_virtualMemory_allocate(size_t size, int type) {
   int pages = PAGE_ALIGN(size) / PAGE_SIZE;
   void * result = NULL;
   switch (type) {
   case STACK_VM:
   case CODE_VM:
     guk_printk("maxve_virtualMemory_allocate called with type = %d", type);
     crash_exit();
     break;
   case HEAP_VM:
     result = allocate_heap_region(pages);
     break;
   default:
     result = (void*) allocate_pages(pages, type);
   }
   return result;
}

void *maxve_virtualMemory_allocateIn31BitSpace(size_t size, int type) {
  return (void*) allocate_pages(size, type);
}

void *maxve_virtualMemory_allocateAtFixedAddress(unsigned long vaddr, size_t size, int type) {
  if (type == CODE_VM) {
       int pages = PAGE_ALIGN(size) / PAGE_SIZE;
       return (void*) allocate_code_region(pages, vaddr);
  } else {
       guk_printk("maxve_virtualMemory_allocateAtFixedAddress called with type = %d", type);
       crash_exit();
       return NULL;
  }
}

void maxve_virtualMemory_deallocate(void *start, size_t size, int type) {
  int pages = PAGE_ALIGN(size) / PAGE_SIZE;
     switch (type) {
     case STACK_VM:
     case CODE_VM:
       guk_printk("maxve_virtualMemory_deallocate called with type = %d", type);
       crash_exit();;
     case HEAP_VM:
       deallocate_heap_region((unsigned long) start, pages);
       break;
     case DATA_VM:
       deallocate_pages(start, pages, type);
       break;
     }


}

int maxve_virtualMemory_pageSize(void) {
  return PAGE_SIZE;
}

extern void check_stack_protectPages(unsigned long address, int count);
void maxve_virtualMemory_protectPages(unsigned long address, int count) {
  check_stack_protectPages(address, count);
}

extern void check_stack_unProtectPages(unsigned long address, int count);
void maxve_virtualMemory_unProtectPages(unsigned long address, int count) {
  check_stack_unProtectPages(address, count);
}

extern void* memory_allocate(size_t size);
extern void* memory_deallocate(void *p);
extern void* memory_reallocate(size_t size);

void *maxine_mm_dlsym(const char *symbol) {
    if (strcmp(symbol, "memory_allocate") == 0) return memory_allocate;
    else if (strcmp(symbol, "memory_deallocate") == 0) return memory_deallocate;
    else if (strcmp(symbol, "memory_reallocate") == 0) return memory_reallocate;
    else if (strcmp(symbol, "virtualMemory_deallocate") == 0) return virtualMemory_deallocate;
    else if (strcmp(symbol, "virtualMemory_allocate") == 0) return virtualMemory_allocate;
    else if (strcmp(symbol, "virtualMemory_allocateIn31BitSpace") == 0) return virtualMemory_allocateIn31BitSpace;
    else if (strcmp(symbol, "virtualMemory_allocateAtFixedAddress") == 0) return virtualMemory_allocateAtFixedAddress;
    else if (strcmp(symbol, "virtualMemory_pageAlign") == 0) return virtualMemory_pageAlign;
    else if (strcmp(symbol, "virtualMemory_protectPages") == 0) return virtualMemory_protectPages;
    else if (strcmp(symbol, "virtualMemory_unprotectPages") == 0) return virtualMemory_unprotectPages;
    else return 0;
}
