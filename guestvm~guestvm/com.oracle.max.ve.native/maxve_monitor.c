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
 * Native implementation of Java monitors.
 *
 * Author: Mick Jordan
 */

#include <types.h>
#include <lib.h>
#include <xmalloc.h>
#include <smp.h>
#include <events.h>
#include <spinlock.h>
#include <maxve_monitor.h>
#include <arch_sched.h>

maxve_monitor_t *maxve_monitor_create(void) {
  maxve_monitor_t *result = xmalloc(struct maxve_monitor);
  result->holder = NULL;
  result->rcount = 0;
  result->uncontend_count = 0;
  result->contend_count = 0;
  INIT_LIST_HEAD(&result->waiters);
  spin_lock_init(&result->lock);
  return result;
}

/*
static void print_waitqueue(maxve_monitor_t *monitor) {
  // assert hold lock
    struct list_head *it;
    struct thread *th;
    guk_printk("wait queue for %lx\n", monitor);
    list_for_each(it, &monitor->waiters) {
      th = list_entry(it, struct thread, aux_thread_list);
      guk_printk("  Thread %lx \"%s\", id=%d, flags %x\n", th, th->name, th->id, th->flags);
    }
}
*/

static int tracing = 0;

int maxve_monitor_trace(int flag) {
  int result = tracing;
  tracing = flag;
  return result;
}


/* TRACE
static char* tid(char *buf, struct thread *thread) {
  if (thread == NULL) return "none";
  else {
    sprintf(buf, "%d", thread->id);
    return buf;
  }
}
*/

int maxve_monitor_enter(maxve_monitor_t *monitor) {
    struct thread *thread = current;
    int sched = 0;
    //TRACE char tb1[8]; char tb2[8];
    spin_lock(&monitor->lock);
    //TRACE guk_tprintk("menter: m %lx, ct %s, h %s, c %d\n", monitor, tid(tb1, thread), tid(tb2, monitor->holder), monitor->rcount);
    if (monitor->holder == NULL) {
      monitor->holder = thread;
      monitor->uncontend_count++;
    } else if (monitor->holder == thread) {
      //recursive, ok
      monitor->rcount++;
      //monitor->uncontend_count++;
    } else {
      //monitor->contend_count++;
      block(thread);
      set_aux1(thread);
      list_add_tail(&thread->aux_thread_list, &monitor->waiters);
      //TRACE guk_tprintk("menter waiting: m %lx, ct %s, h %s, c %d\n", monitor, tid(tb1, thread), tid(tb2, monitor->holder), monitor->rcount);
      //print_waitqueue(monitor);
      sched = 1;
    }
    spin_unlock(&monitor->lock);
    if (sched) schedule();
    return 0;
}

static int release_any_waiter(maxve_monitor_t *monitor, struct thread *thread) {
  if (list_empty(&monitor->waiters)) {
    monitor->holder = NULL;
    return 0;
  } else {
    struct list_head *it;
    struct thread *wthread = NULL;
    // TRACE char tb1[8]; char tb2[8];
    list_for_each(it, &monitor->waiters) {
	/* FIXME: no loop necessary; just release first thread? */
      wthread = list_entry(it, struct thread, aux_thread_list);
      //TRACE guk_tprintk("mexit releasing: m %lx, ct %s, r %s, c %d\n", monitor, tid(tb1, thread), tid(tb2, wthread), monitor->rcount);
      list_del_init(&wthread->aux_thread_list);
      break;
    }
    //print_waitqueue(monitor);
    monitor->holder = wthread;
    wake(wthread);
    clear_aux1(wthread);
    return 1;
  }
}

int maxve_monitor_exit(maxve_monitor_t *monitor) {
    int result = 0;
    int sched = 0;
    struct thread *thread = current;
    //TRACE  char tb1[8]; char tb2[8];
    spin_lock(&monitor->lock);
    //TRACE guk_tprintk("mexit: m %lx, ct %s, h %s, c %d\n", monitor, tid(tb1, thread), tid(tb2, monitor->holder), monitor->rcount);
    if (monitor->holder != thread) {
      // thread does not own it - this should not happen
      printk("monitor exit %lx not held by %d, %s\n", monitor, monitor->holder->id, monitor->holder->name);
      result = 2;
    } else {
      if (monitor->rcount > 0) {
	monitor->rcount--;
      } else {
        sched = release_any_waiter(monitor, thread);
      }
    }
    spin_unlock(&monitor->lock);
    if (sched)  {
	schedule();
    }
    return result;
}

int maxve_holds_monitor(maxve_monitor_t *monitor) {
    struct thread *thread = current;
    int result;
    //TRACE char tb1[8]; char tb2[8];
    spin_lock(&monitor->lock);
    //TRACE  guk_tprintk("mholds: m %lx, ct %s, h %s\n", monitor, tid(tb1, thread), tid(tb2, monitor->holder));
    result = monitor->holder == thread;
    spin_unlock(&monitor->lock);
    return result;
}

maxve_condition_t *maxve_condition_create(void) {
    maxve_condition_t *result = xmalloc(struct maxve_condition);
    INIT_LIST_HEAD(&result->waiters);
    spin_lock_init(&result->lock);
    return result;
}

/* Returns 1 if thread was interrupted, 0 otherwise.
 */
int maxve_condition_wait(maxve_condition_t *condition, maxve_monitor_t *monitor,
			 struct timespec *timespec) {
      int result = 0;
      struct thread *thread = current;
      int rcount;
      //TRACE char tb1[8]; char tb2[8];
      spin_lock(&monitor->lock);
      if (monitor->holder != thread) {
          printk("monitor wait %lx not held by %d, %s, held by %d, %s\n", monitor, thread->id, thread->name, monitor->holder->id, monitor->holder->name);
          result = 2;
      } else {
	  //TRACE guk_tprintk("cwait releasing: c %lx, m %lx, ct %s\n", condition, monitor, tid(tb1, thread));
	  DEFINE_SLEEP_QUEUE(sq);
	  block(thread);

	  list_add_tail(&thread->aux_thread_list, &condition->waiters);
	  set_aux2(thread);

	  rcount = monitor->rcount;
	  monitor->rcount = 0;
	  release_any_waiter(monitor, thread);
	  spin_unlock(&monitor->lock);

	  if (timespec != NULL) {
	      long nanos = timespec->ts_sec * 1000000000 + timespec ->ts_nsec;
	      sq.wakeup_time = NOW() + nanos;
	      guk_sleep_queue_add(&sq);
	  }

	  schedule();

	  /* we added something to the queue so we have to remove it again */
	  if (timespec != NULL)
	      guk_sleep_queue_del(&sq);

	  // we can wake up for three reasons, either we were notified or
	  // the timeout expired or we were interrupted. In the latter two cases
	  // we need to remove ourselves from the condition_waiters list
	  if (is_interrupted(thread)) {
	      result = 1;
	      clear_interrupted(thread);
	  }

	  if (is_expired(&sq) || result) {
	      struct list_head *iterator, *tmp;
	      struct thread *t;

	      spin_lock(&condition->lock);
	      list_for_each_safe(iterator, tmp, &condition->waiters) {
		  t = list_entry(iterator, struct thread, aux_thread_list);
		  if (t == thread) {
		      list_del_init(&thread->aux_thread_list);
		      break;
		  }
	      }
	      spin_unlock(&condition->lock);
	  }
	  clear_aux2(thread);

	  // need to acquire the monitor again:
	  // TODO: Why not calling monitor_enter() here?
	  spin_lock(&monitor->lock);
	  //TRACE  guk_tprintk("cwait tryacquire: c %lx, m %lx, ct %s\n", condition, monitor, tid(tb1, thread));
	  while (monitor->holder != NULL && monitor->holder != thread) {
	      // someone else got it
	      block(thread);
	      set_aux1(thread);
	      list_add_tail(&thread->aux_thread_list, &monitor->waiters);
	      //TRACE guk_tprintk("cwait waiting: c %lx, m %lx, h %s, ct %s\n", condition, monitor, tid(tb1, monitor->holder), tid(tb2, thread));
	      spin_unlock(&monitor->lock);
	      schedule();
	      spin_lock(&monitor->lock);
	      clear_aux1(thread);
	  }
	  //TRACE guk_tprintk("cwait reacquired: c %lx, m %lx, ct %s\n", condition, monitor, tid(tb1, thread));
	  monitor->holder = thread;
	  monitor->rcount = rcount;
      }
    spin_unlock(&monitor->lock);
    return result;
}

/*
 * No mutex locking is needed here because Java semantics requires the (associated) monitor
 * is held when a notify occurs. however, we have to synchronize with interrupts and
 * timeouts.
 */
int maxve_condition_notify(maxve_condition_t *condition, int all) {
    struct list_head *iterator, *tmp;
    struct thread *thread = current;
    int result = 0;
    int sched = 0;
    //TRACE char tb[8];
    //TRACE guk_tprintk("cnotify: c %lx, ct %s\n", condition, tid(tb, thread));
    spin_lock(&condition->lock);
    if (!list_empty(&condition->waiters)) {
      list_for_each_safe(iterator, tmp, &condition->waiters) {
	thread = list_entry(iterator, struct thread, aux_thread_list);
	//TRACE guk_tprintk("cnotify wakeup: c %lx, nt %s\n", condition, tid(tb, thread));
        list_del_init(&thread->aux_thread_list);
	wake(thread);
	sched = 1;
	if (!all) break;
      }
    }
    spin_unlock(&condition->lock);
    if (sched) schedule();
    return result;
}



