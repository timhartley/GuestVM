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
#include <trace.h>
#include <lib.h>
#include <maxve.h>
#include <mm.h>
#include <trace.h>
#include <spinlock.h>
#include <bitmap.h>
// from Maxine
#include <image.h>

/*
 * Code to handle the allocation of Maxine code regions.
 * Code regions are high in the virtual address space, above the
 * thread stack area, starting at 3TB. We use a global bit map to indicate
 * that a given virtual address area is in use or not.
 *
 * The boot image code region is remapped to 3TB and dynamic code
 * regions are mapped beyond that as needed.
 *
 * Author: Mick Jordan
 */

static unsigned long *alloc_bitmap;
static int max_code_regions;

#define MAX_CODE_REGIONS 64

static DEFINE_SPINLOCK(bitmap_lock);

#define BOOT_CODE_REGION_BASE  (3L * 1024L *1024L * 1024L * 1024L)  // 3TB

static unsigned long code_region_base;
static unsigned long _code_region_size = 0;

unsigned long maxve_codePoolBase(void) {
  return code_region_base;
}

unsigned long maxve_codePoolSize(void) {
  return max_code_regions;
}

void *maxve_codePoolBitmap(void) {
  return alloc_bitmap;
}

unsigned long maxve_codePoolRegionSize(void) {
	return _code_region_size;
}

unsigned long maxve_remap_boot_code_region(unsigned long base, size_t size) {
	  struct pfn_alloc_env pfn_frame_alloc_env = {
	    .pfn_alloc = pfn_alloc_alloc
	  };
	  struct pfn_alloc_env pfn_boot_code_region_env = {
	    .pfn_alloc = pfn_linear_alloc
	  };
	  // The boot code is mapped 1-1 with physical memory where it was placed by the linker
	  // So we map the same physical page frames at our preferred address.
	  pfn_boot_code_region_env.pfn = virt_to_pfn(base);
	  build_pagetable(BOOT_CODE_REGION_BASE,  BOOT_CODE_REGION_BASE + size, &pfn_boot_code_region_env, &pfn_frame_alloc_env);
	  return BOOT_CODE_REGION_BASE;
}

static void init_code_regions(int region_size) {
	_code_region_size = region_size;
	max_code_regions = MAX_CODE_REGIONS;
    int bitmap_size = round_pgup(map_size_in_bytes(max_code_regions)); // in bytes
    code_region_base = image_code_end();
    alloc_bitmap = (unsigned long *) allocate_pages(bitmap_size / PAGE_SIZE, DATA_VM);
    memset(alloc_bitmap, 0, bitmap_size);
}

/* allocate a code region of size n pages at vaddr.
 * All code regions are of the same size. */
unsigned long allocate_code_region(int n, unsigned long vaddr) {
	  int slot;
	  int vsize = n * PAGE_SIZE;
	  if (code_region_base == 0) {
		  init_code_regions(vsize);
	  }
///	  guk_printk("allocate_code_region %lx %lx\n", vaddr, n);
	  slot = (vaddr - code_region_base) / _code_region_size;
	  spin_lock(&bitmap_lock);
	  if (allocated_in_map(alloc_bitmap, slot)) {
		  guk_printk("code region slot at %lx is already allocated\n", vaddr);
		  crash_exit();
	  } else {
	      set_map(alloc_bitmap, slot);
	  }
	  spin_unlock(&bitmap_lock);
	  struct pfn_alloc_env pfn_frame_alloc_env = {
	    .pfn_alloc = pfn_alloc_alloc
	  };
	  struct pfn_alloc_env pfn_code_region_env = {
	    .pfn_alloc = pfn_linear_alloc
	  };
	  // allocate n consecutive physical pages
	  unsigned long pfn = virt_to_pfn(allocate_pages(n, CODE_VM));
	  pfn_code_region_env.pfn = pfn;
	  build_pagetable(vaddr, vaddr + vsize, &pfn_code_region_env, &pfn_frame_alloc_env);
	  //guk_printk("allocate_code_region: %lx, %lx\n", vaddr, pfn);
	  return vaddr;
}

void *code_pool_dlsym(const char * symbol) {
  if (strcmp(symbol, "maxve_codePoolBase") == 0) return maxve_codePoolBase;
  else if (strcmp(symbol, "maxve_codePoolSize") == 0) return maxve_codePoolSize;
  else if (strcmp(symbol, "maxve_codePoolBitmap") == 0) return maxve_codePoolBitmap;
  else if (strcmp(symbol, "maxve_codePoolRegionSize") == 0) return maxve_codePoolRegionSize;
  else return 0;
}
