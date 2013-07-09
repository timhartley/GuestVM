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

/*
 * Code to handle the allocation of Maxine heap regions.
 * Heap regions are allocated at 1TB and up.
 * Heap is allocated in minimum units of 2MB.
 * TODO: Use large (2MB) pages
 *
 * Author: Mick Jordan
 */

#define HEAP_REGIONS_BASE  (1L * 1024L *1024L * 1024L * 1024L)  // 1TB

static unsigned long *alloc_bitmap;
static int max_heap_regions;

#define TWOMB (2 * 1024 * 1024)
#define MIN_HEAP_REGION_SIZE TWOMB
#define HEAP_REGIONS_PER_MAPWORD ENTRIES_PER_MAPWORD

static DEFINE_SPINLOCK(bitmap_lock);

static unsigned long heap_regions_base;

unsigned long maxve_heapPoolBase(void) {
  return heap_regions_base;
}

unsigned long maxve_heapPoolSize(void) {
  return max_heap_regions;
}

void *maxve_heapPoolBitmap(void) {
  return alloc_bitmap;
}

unsigned long maxve_heapPoolRegionSize(void) {
	return MIN_HEAP_REGION_SIZE;
}

void init_bitmap(void) {
	  max_heap_regions = (guk_maximum_reservation() * PAGE_SIZE) / MIN_HEAP_REGION_SIZE;
	  int bitmap_size = round_pgup(map_size_in_bytes(max_heap_regions));
	  heap_regions_base = HEAP_REGIONS_BASE;
	  alloc_bitmap = (unsigned long *) allocate_pages(bitmap_size / PAGE_SIZE, DATA_VM);
	  memset(alloc_bitmap, 0, bitmap_size);
}

unsigned long pfn_linear_2mballoc(pfn_alloc_env_t *env, unsigned long addr) {
	// we get called every 2MB of address space
	unsigned long paddr = (unsigned long) env->data;
	if (paddr != addr) {
		unsigned long mfn = pfn_to_mfn(env->pfn) + TWOMB;
		env->pfn = mfn_to_pfn(mfn);
	}
	env->data = (void*) addr;
	return pfn_to_mfn(env->pfn);
}

/* allocate a heap region of size n pages. */
unsigned long allocate_heap_region_vs(int n, int twomb) {
	  unsigned long heapregionbase = 0;
	  unsigned long physaddr = 0;
	  int slot = 0;
	  int vsize = n * PAGE_SIZE;
	  if (vsize < MIN_HEAP_REGION_SIZE) {
		  vsize = MIN_HEAP_REGION_SIZE;
		  n = vsize / PAGE_SIZE;
	  }
	  if (heap_regions_base == 0) {
		  init_bitmap();
	  }
	  int num_regions = vsize / MIN_HEAP_REGION_SIZE;
	  spin_lock(&bitmap_lock);
	  while  (slot < max_heap_regions) {
	    if (!allocated_in_map(alloc_bitmap, slot)) {
	  	  int nn = num_regions;
	  	  int nslot = slot + 1;
	  	  while (nn > 1 && (nslot < max_heap_regions)) {
	  	    if (!allocated_in_map(alloc_bitmap, nslot)) {
	  	      nn--; nslot++;
	  	    } else {
	  	      slot = nslot;
	  	      break;
	  	    }
	  	  }
	  	  /* if nn==1, found n consecutive */
	  	  if (nn == 1) {
	  		  int i;
	  		  for (i = slot; i < slot + num_regions; i++) {
	  			set_map(alloc_bitmap, i);
	  		  }
		      heapregionbase = heap_regions_base + slot * MIN_HEAP_REGION_SIZE;
		      break;
	  	  }
	    }
	    slot++;
	  }
	  spin_unlock(&bitmap_lock);

	  if (heapregionbase != 0) {
	      struct pfn_alloc_env pfn_frame_alloc_env = {
	        .pfn_alloc = pfn_alloc_alloc
	      };
	      struct pfn_alloc_env pfn_heap_region_env = {
	         .pfn_alloc = pfn_linear_alloc
	      };
	      /*
	      if (twomb) {
	          struct pfn_alloc_env pfn_heap_2mbregion_env = {
	             .pfn_alloc = pfn_linear_2mballoc
	          };
	    	  unsigned long mfn = guk_allocate_2mb_machine_pages(num_regions, HEAP_VM);
	    	  if (mfn != 0) {
	    		  pfn_heap_2mbregion_env.pfn = mfn_to_pfn(mfn);
	    		  pfn_heap_2mbregion_env.data = (void*) heapregionbase;
		          build_pagetable_2mb(heapregionbase, heapregionbase + vsize, &pfn_heap_2mbregion_env, &pfn_frame_alloc_env);
		          goto done;
	    	  }
	    	  twomb = 0;
	      }
	      */
	      // allocate n consecutive physical pages
	      physaddr = allocate_pages(n, HEAP_VM);
	      if (physaddr != 0) {
	          unsigned long pfn = virt_to_pfn(physaddr);
	          pfn_heap_region_env.pfn = pfn;
	          build_pagetable(heapregionbase, heapregionbase + vsize, &pfn_heap_region_env, &pfn_frame_alloc_env);
	      } else {
		      heapregionbase = 0;
	      }
	  }
//	  done:
	  //guk_printk("allocate_heap_region: n=%x (%d) v=%lx s=%d p=%lx, 2mb=%d\n", n, n, heapregionbase, slot, physaddr, twomb);
	  return heapregionbase;
}

unsigned long allocate_heap_region(int n) {
	return allocate_heap_region_vs(n, 0);
}

unsigned long allocate_heap_region_2mb(int n) {
	return allocate_heap_region_vs(n, 1);
}

void deallocate_heap_region(unsigned long vaddr, int n) {
	  int vsize = n * PAGE_SIZE;
	  int num_regions = vsize / MIN_HEAP_REGION_SIZE;
	  int slot = (vaddr - heap_regions_base) / MIN_HEAP_REGION_SIZE;
	  //guk_printk("deallocate_heap_region: n= %x (%d) v=%lx, s=%d\n", n, n, vaddr, slot);
	  // undo mappings
	  unsigned long pte;
	  long pfn = guk_not11_virt_to_pfn(vaddr, &pte);
	  demolish_pagetable(vaddr, vaddr + vsize);
	  // deallocate the pages - assumes contiguous physical
	  deallocate_pages(pfn_to_virt(pfn), n, HEAP_VM);
	  // clear bitmap
	  spin_lock(&bitmap_lock);
	  int i;
	  for (i = 0; i < num_regions; i++) {
		  clear_map(alloc_bitmap, i + slot);
	  }
	  spin_unlock(&bitmap_lock);
}

void *heap_pool_dlsym(const char * symbol) {
  if (strcmp(symbol, "maxve_heapPoolBase") == 0) return maxve_heapPoolBase;
  else if (strcmp(symbol, "maxve_heapPoolSize") == 0) return maxve_heapPoolSize;
  else if (strcmp(symbol, "maxve_heapPoolBitmap") == 0) return maxve_heapPoolBitmap;
  else if (strcmp(symbol, "maxve_heapPoolRegionSize") == 0) return maxve_heapPoolRegionSize;
  else return 0;
}
