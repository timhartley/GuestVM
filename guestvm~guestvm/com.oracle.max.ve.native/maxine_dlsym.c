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
 * Master interface to Maxine dlsym handling.
 */

extern void * maxine_misc_dlsym(const char *symbol);
extern void * maxine_monitor_dlsym(const char *symbol);
extern void * maxine_mm_dlsym(const char *symbol);
extern void * maxine_numerics_dlsym(const char *symbol);
extern void * maxine_log_dlsym(const char *symbol);
extern void * maxine_traps_dlsym(const char *symbol);
extern void * maxine_threads_dlsym(const char *symbol);
extern void * maxine_time_dlsym(const char *symbol);
extern void * maxine_tests_dlsym(const char *symbol);


void *maxine_dlsym(const char *symbol) {
  void *result;
  if ((result = maxine_misc_dlsym(symbol)) ||
      (result = maxine_threads_dlsym(symbol)) ||
      (result = maxine_log_dlsym(symbol)) ||
      (result = maxine_monitor_dlsym(symbol)) ||
      (result = maxine_mm_dlsym(symbol)) ||
      (result = maxine_numerics_dlsym(symbol)) ||
      (result = maxine_time_dlsym(symbol)) ||
      (result = maxine_traps_dlsym(symbol)) ||
      (result = maxine_tests_dlsym(symbol))) {
    return result;
    } else {
      return 0;
    }
}

