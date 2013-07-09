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
#ifndef _MAXWELL_SIMPLEMON_H_
#define _MAXWELL_SIMPLEMON_H_

#include <sched.h>

/* A monitor can be in one of three states:
   1. unowned, indicated by holder == NULL
   2. owned by holder, no waiters
   3. owned by holder, list of waiters.
   In state 2 or 3, the recursion count may be > 0
*/
typedef struct maxve_monitor {
    struct thread *holder;
    long rcount;
    struct list_head waiters;
    spinlock_t lock;
    long contend_count;
    long uncontend_count;
} maxve_monitor_t;

maxve_monitor_t *maxve_monitor_create(void);
int maxve_monitor_enter(maxve_monitor_t *monitor);
int maxve_monitor_exit(maxve_monitor_t *monitor);

typedef struct maxve_condition {
    spinlock_t lock;
    struct list_head waiters;
} maxve_condition_t;

maxve_condition_t *maxve_condition_create(void);
int maxve_condition_wait(maxve_condition_t *condition, maxve_monitor_t *monitor, struct timespec *timespec);
int maxve_condition_notify(maxve_condition_t *condition, int all);

#endif
