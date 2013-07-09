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
#include <os.h>
#include <hypervisor.h>
#include <types.h>
#include <list.h>
#include <sched.h>
#include <arch_sched.h>
#include <trace.h>
#include <lib.h>
#include <maxve.h>
#include <mm.h>
#include <trace.h>
#include <spinlock.h>
#include <bitmap.h>
#include <threadLocals.h>

/*
 * Code to handle the allocation and mapping of Java thread stacks.
 * Stacks are high in the 64 bit virtual address space (2TB onwards)
 * and sparsely mapped to real (physical) memory as needed.
 * N.B. All stacks are of the same virtual size, although that size can vary
 * with each run of the JVM. We use a global bit map to indicate that a given
 * virtual address area is in use or not.
 *
 * Author: Mick Jordan
 */

/**
 * A Java thread stack should have the following format, low to high:
 *
 * Red zone page: unmapped
 * Yellow zone page: mapped but not present
 * Zero or more unmapped pages
 * Blue zone page: mapped but not present
 * 7 or more active pages: mapped and present
 *
 * The VM thinks the stack base is at the start of the Yellow zone page.
*/

static unsigned long *alloc_bitmap;
static int max_threads;
static void (*specifics_destructor)(void *);

static DEFINE_SPINLOCK(bitmap_lock);

#define STACK_INCREMENT_PAGES 8
#define STACK_INCREMENT_SIZE  (STACK_INCREMENT_PAGES * PAGE_SIZE)

#define THREAD_STACK_BASE (2L * 1024L *1024L * 1024L * 1024L)  // 2TB

extern void *malloc(size_t n);
extern void *calloc(int nelems, size_t n);
extern void free(void *p);

unsigned long thread_stack_base;
unsigned long thread_stack_size = 0;

unsigned long maxve_stackPoolBase(void) {
  return thread_stack_base;
}

unsigned long maxve_stackPoolSize(void) {
  return max_threads;
}

void *maxve_stackPoolSpinLock(void) {
  return &bitmap_lock;
}

void *maxve_stackPoolBitmap(void) {
  return alloc_bitmap;
}

unsigned long maxve_stackRegionSize(void) {
  return thread_stack_size;
}

void init_thread_stacks(void) {
	// conservative estimate
  max_threads = (guk_maximum_reservation() * PAGE_SIZE) / STACK_INCREMENT_SIZE;
  int bitmap_size = round_pgup(map_size_in_bytes(max_threads));
  thread_stack_base = THREAD_STACK_BASE;
  alloc_bitmap = (unsigned long *) allocate_pages(bitmap_size / PAGE_SIZE, DATA_VM);
  memset(alloc_bitmap, 0, bitmap_size);
}

void set_specifics_destructor(void (*destructor)(void *)) {
  specifics_destructor = destructor;
}

static unsigned long allocate_page(unsigned long addr) {
	unsigned long pfn = virt_to_pfn(allocate_pages(1, STACK_VM));
	//ttprintk("stack_allocate_page %lx %lx\n", addr, pfn);
	return pfn;
}


static long pfn_alloc_thread_stack(pfn_alloc_env_t *env, unsigned long addr) {
	NativeThreadLocals nativeThreadLocals = (NativeThreadLocals) env->data;
	int map = 0;
	/*
	 * This is called back from build_pagetable in all phases of the stack setup.
	 * In the first phase, the initial stack allocation, nativeThreadLocals->stackBase == NULL
	 * and we are just mapping the Yellow and Blue zones to get started.
	 * We could map the Yellow after the thread is running in initStack, but mapping it
	 * eagerly makes sure that all memory allocation is done before the thread is run,
	 * allowing failures to cause an exception in the creating thread.
	 * In the second and subsequent calls we are extending the stack down from the blue zone.
	 * Identifying the phases:
	 *   Phase 1: nativeThreadLocals->stackBase == 0 && nativeThreadLocals->yellowZone != 0 && nativeThreadLocals->blueZone != 0;
	 *   Phase 2: nativeThreadLocals->stackBase != 0;
	 */
	if (nativeThreadLocals->stackBase == 0) {
		map = addr == nativeThreadLocals->yellowZone || addr >= nativeThreadLocals->blueZone;
	} else {
   	    // lowering blue zone
   	    map = 1;
	}
	//if (map) ttprintk("PATS ntl %lx, addr %lx\n", nativeThreadLocals, addr);
	if (map) {
		long pfn = allocate_page(addr);
		if (pfn < 0) {
			return pfn;
		} else {
			return pfn_to_mfn(pfn);
		}
	} else {
		return 0;
	}
}


int extend_stack(NativeThreadLocals nativeThreadLocals, unsigned long start_address, unsigned long end_address);

/* Allocate the virtual memory (only) for a thread stack of size n pages.
 * Return 0 on failure.
  */
unsigned long allocate_thread_stack(unsigned long vsize) {
    /*
     * Maxine allocates large (virtual) stacks that limits scaling the number of threads
     * if we actually allocated physical memory for the entire stack.
     * Instead we just allocate virtual memory at this point and do the physical
     * allocation incrementally.
     */
  unsigned long stackbase = 0;
  unsigned long stackend = 0;
  unsigned long result = 0;
  int slot;
  spin_lock(&bitmap_lock);
  thread_stack_size = vsize;
  for (slot = 0; slot < max_threads; slot++) {
    if (!allocated_in_map(alloc_bitmap, slot)) {
      set_map(alloc_bitmap, slot);
      stackbase = thread_stack_base + slot * (vsize);
      stackend =  stackbase + (vsize);
      break;
    }
  }
  spin_unlock(&bitmap_lock);
  /*
   * We have to map the top of the stack because initStack does not get called
   * until the thread has actually started running.
   */
  //ttprintk("ATS %lx\n", stackbase);
  NativeThreadLocals ntl = malloc(sizeof(NativeThreadLocalsStruct));
  if (ntl == NULL) {
	  return 0;
  }
  /* Clear the NativeThreadLocals: */
  memset((void *) ntl, 0, sizeof(NativeThreadLocalsStruct));
  ntl->yellowZone = stackbase + PAGE_SIZE;
  ntl->blueZone = stackend  - STACK_INCREMENT_SIZE;

  if (extend_stack(ntl, stackbase, stackend)) {
	  result = stackbase;
  } else {
	  spin_lock(&bitmap_lock);
	  clear_map(alloc_bitmap, slot);
	  spin_unlock(&bitmap_lock);
  }
  // We only needed the NativeThreadLocals for extend_stack, Maxine will allocate a new one.
  free(ntl);
  return result;
}

void guk_invoke_destroy(void *specifics) {
	struct thread *thread = current;
	// Maxine assumes thread->specific has been reset (other thread libraries do this)
	thread->specific = NULL;
    specifics_destructor(specifics);
}

void guk_free_thread_stack(void *stack, unsigned long stack_size) {
    unsigned long stackBase = (unsigned long) stack;
    unsigned long stackEnd = stackBase + stack_size;
    while (stackBase < stackEnd) {
		unsigned long pte;
		long pfn = guk_not11_virt_to_pfn(stackBase, &pte);
		if (pfn > 0) {
			guk_clear_pte(stackBase);
			free_page(pfn_to_virt(pfn));
		}
    	stackBase += PAGE_SIZE;
    }
    int slot = ((unsigned long ) stack - thread_stack_base) / stack_size;
    spin_lock(&bitmap_lock);
    clear_map(alloc_bitmap, slot);
    spin_unlock(&bitmap_lock);
}

int extend_stack(NativeThreadLocals nativeThreadLocals, unsigned long start_address, unsigned long end_address) {
	  struct pfn_alloc_env pfn_frame_alloc_env = {
	    .pfn_alloc = pfn_alloc_alloc
	  };
	  struct pfn_alloc_env pfn_thread_stack_env = {
	    .pfn_alloc = pfn_alloc_thread_stack
	  };
	  pfn_thread_stack_env.data = nativeThreadLocals;
	  return build_pagetable(start_address, end_address, &pfn_thread_stack_env, &pfn_frame_alloc_env);
}

void check_stack_protectPages(unsigned long addr, int count);

void maxve_initStack(NativeThreadLocals nativeThreadLocals) {
	nativeThreadLocals->blueZone = nativeThreadLocals->stackBase + nativeThreadLocals->stackSize - STACK_INCREMENT_SIZE;
    check_stack_protectPages(nativeThreadLocals->blueZone, 1);
    check_stack_protectPages(nativeThreadLocals->yellowZone, 1);
}

unsigned long get_pfn_for_address(unsigned long address) {
	unsigned long pte;
	long pfn = guk_not11_virt_to_pfn(address, &pte);
	if (pfn < 0) {
		///*guk_x*/ttprintk("get_pfn_for_addr %lx, thread %d failed, pfn %lx, pte %lx\n", address, current->id, pfn, pte);
		///*guk_x*/ttprintk("crashing\n");
		guk_xprintk("get_pfn_for_addr %lx, thread %d failed, pfn %lx, pte %lx\n", address, current->id, pfn, pte);
		crash_exit_backtrace();
	}
	return pfn;
}

/*
 * Lowers the blue zone page by STACK_INCREMENT_SIZE but only if greater than
 * yellow zone page.
 */
void lower_blue_zone(NativeThreadLocals nativeThreadLocals) {
  Address nbz = nativeThreadLocals->blueZone - STACK_INCREMENT_SIZE;
  unsigned long start_address;
  unsigned long end_address = nativeThreadLocals->blueZone;
  if (nbz > nativeThreadLocals->yellowZone) {
    nativeThreadLocals->blueZone = nbz;
	start_address = nbz;
  } else {
    nativeThreadLocals->blueZone = nativeThreadLocals->yellowZone;
    start_address = nativeThreadLocals->yellowZone + PAGE_SIZE;
  }
  /* Need to allocate and map new pages */
  //guk_printk(" nbz %lx\n", start_address);
  if (end_address > start_address) {
    extend_stack(nativeThreadLocals, start_address, end_address);
    /* There must be at least two mapped pages above the yellow zone for the stack check code to work.
     * If we are in the last increment no point in unmapping the blue zone page. */
    if (start_address  > nativeThreadLocals->yellowZone + STACK_INCREMENT_SIZE) {
    	guk_unmap_page_pfn(start_address, get_pfn_for_address(start_address));
    }
  }
}

void maxve_blue_zone_trap(NativeThreadLocals nativeThreadLocals) {
  //guk_printk("blue zone trap bz %lx, yz %lx\n", nativeThreadLocals->blueZone, nativeThreadLocals->yellowZone);
  guk_remap_page_pfn(nativeThreadLocals->blueZone, get_pfn_for_address(nativeThreadLocals->blueZone));
  lower_blue_zone(nativeThreadLocals);
}

void check_stack_protectPages(unsigned long address, int count) {
	while (count > 0) {
	    if (address > thread_stack_base) {
		    guk_unmap_page_pfn(address, get_pfn_for_address(address));;
	    } else {
		    guk_unmap_page(address);
	    }
	    address += PAGE_SIZE;
	    count--;
	}
}

void check_stack_unProtectPages(unsigned long address, int count) {
	while (count > 0) {
	    if (address > thread_stack_base) {
		    guk_remap_page_pfn(address, get_pfn_for_address(address));
	    } else {
		    guk_remap_page(address);
	    }
        address += PAGE_SIZE;
        count--;
	}
}

void *thread_stack_pool_dlsym(const char * symbol) {
  if (strcmp(symbol, "maxve_stackPoolBase") == 0) return maxve_stackPoolBase;
  else if (strcmp(symbol, "maxve_stackPoolSize") == 0) return maxve_stackPoolSize;
  else if (strcmp(symbol, "maxve_stackPoolBitmap") == 0) return maxve_stackPoolBitmap;
  else if (strcmp(symbol, "maxve_stackRegionSize") == 0) return maxve_stackRegionSize;
  else if (strcmp(symbol, "maxve_stackPoolSpinLock") == 0) return maxve_stackPoolSpinLock;
  else return 0;
}
