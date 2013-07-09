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
 * Network shim.
 * Author: Mick Jordan
 */

#include <os.h>
#include <sched.h>
#include <hypervisor.h>
#include <types.h>
#include <spinlock.h>
#include <lib.h>
#include <mm.h>
#include <time.h>
#include <jni.h>
#include <xmalloc.h>

static int mac_address_set = 0;
static unsigned char mac_address[6];
static unsigned char nic_name[32];
static int net_available = 0;
static int net_started = 0;

typedef void (*GUKNetDeviceCopyPacketMethod)(void *p, int len, long ts);
static GUKNetDeviceCopyPacketMethod copy_packet_method;

/*
 * This is the Guest VM override (strong) definition of the GUK netfront function
 * that is called after network initialization. A NULL value for mac implies
 * that there is no network available.
 */
void guk_net_app_main(unsigned char *mac, char *nic) {
    if (mac != NULL) {
        memcpy(mac_address, mac, 6);
        net_available = 1;
    }
    if (nic != NULL) {
        int i = 0;
        while (nic[i] != 0 && i < 31) {
	      nic_name[i] = nic[i];
          i++;
        }
        if (i == 31) nic_name[i] = 0;
    }
    mac_address_set = 1;
}
/*
 * This is the Guest VM override (strong) definition of the GUK netfront function
 * that is called whenever a packet is taken off the ring.
 * Note that packets may arrive before the Guest VM driver is set up - they are ignored.
 * We mark the interrupted thread as needing rescheduling to get the packet
 * handling thread running with minimal latency (the scheduler may override this).
 */
void guk_netif_rx(unsigned char* data, int len) {
  if (net_started) {
	  struct thread *current;
    (*copy_packet_method)(data, len, NOW());
    current = guk_not_idle_or_stepped();
    if (current != NULL) {
    	set_need_resched(current);
    }
  }
}

unsigned char *maxve_getMacAddress(void) {
  while (mac_address_set == 0) {
    sleep(1000);
  }
  return mac_address;
}

unsigned char *maxve_getNicName(void) {
  while (mac_address_set == 0) {
    sleep(1000);
  }
  return nic_name;
}

int maxve_netStart(GUKNetDeviceCopyPacketMethod m) {
  while (mac_address_set == 0) {
    sleep(1000);
  }
  if (!net_available) return 0;
  net_started = 1;
  copy_packet_method = m;
  return 1;
}

extern void guk_netfront_xmit(unsigned char *, int length);  // netfront

void *net_dlsym(const char *symbol) {
    if (strcmp(symbol, "maxve_getMacAddress") == 0) return maxve_getMacAddress;
    else if (strcmp(symbol, "maxve_getNicName") == 0) return maxve_getNicName;
    else if (strcmp(symbol, "maxve_netStart") == 0) return maxve_netStart;
    else return 0;
}
