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
 * These symbols are defined in MaxineNative, and it is those functions that actually
 * invoke the functions in maxve_monitor.c.
 *
 * Author: Mick Jordan
 *
 * TODO: access the headers from MaxineNative instead of using externs
 *
 */

#include <lib.h>

extern int nativeMutexSize(void);
extern int nativeMutexInitialize(void *p);
extern int nativeConditionSize(void);
extern int nativeConditionInitialize(void *p);
extern int nativeMutexUnlock(void *p);
extern int nativeConditionNotify(void *p, int a);
extern int Java_com_sun_max_vm_monitor_modal_sync_nat_NativeMutex_nativeMutexLock(void *env, void *c, void* mutex);
extern int Java_com_sun_max_vm_monitor_modal_sync_nat_NativeConditionVariable_nativeConditionWait(void *env, void *c, void *mutex, void *condition, long timeoutMilliSeconds);
extern void Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalInit(void *env, void *c);
extern void Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalWait(void *env, void *c);
extern void Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalFinalize(void *env, void *c);
extern void Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalNotify(void *env, void *c);

void *maxine_monitor_dlsym(char *symbol) {
    if (strcmp(symbol, "nativeMutexSize") == 0) return nativeMutexSize;
    else if (strcmp(symbol, "nativeMutexInitialize") == 0) return nativeMutexInitialize;
    else if (strcmp(symbol, "nativeConditionSize") == 0) return nativeConditionSize;
    else if (strcmp(symbol, "nativeConditionInitialize") == 0) return nativeConditionInitialize;
    else if (strcmp(symbol, "nativeMutexUnlock") == 0) return nativeMutexUnlock;
    else if (strcmp(symbol, "nativeConditionNotify") == 0) return nativeConditionNotify;
     else if (strcmp(symbol, "Java_com_sun_max_vm_monitor_modal_sync_nat_NativeMutex_nativeMutexLock") == 0)
      return Java_com_sun_max_vm_monitor_modal_sync_nat_NativeMutex_nativeMutexLock;
    else if (strcmp(symbol, "Java_com_sun_max_vm_monitor_modal_sync_nat_NativeConditionVariable_nativeConditionWait") == 0)
      return  Java_com_sun_max_vm_monitor_modal_sync_nat_NativeConditionVariable_nativeConditionWait;
    else if (strcmp(symbol, "Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalInit") == 0)
    	return Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalInit;
    else if (strcmp(symbol, "Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalWait") == 0)
    	return Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalWait;
    else if (strcmp(symbol, "Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalFinalize") == 0)
    	return Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalFinalize;
    else if (strcmp(symbol, "Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalNotify") == 0)
    	return Java_com_sun_max_vm_runtime_SignalDispatcher_nativeSignalNotify;
    else return 0;
}
