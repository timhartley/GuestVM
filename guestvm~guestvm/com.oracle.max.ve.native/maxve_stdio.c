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
 * Standard I/O functions needed by Maxine, mostly by log.c and image.c
 */

#include <os.h>
#include <hypervisor.h>
#include <types.h>
#include <spinlock.h>
#include <lib.h>
#include <mm.h>
#include <time.h>
#include <fs.h>
#include <jni.h>
#include <xmalloc.h>

extern void print(int direct, const char *fmt, va_list args);  // console.c

/*
 * Implicitly accessed in log_* on Solaris via stdout/stderr
 */

#ifdef solaris
FILE __iob[_NFILE];
#endif

#ifdef linux
FILE *stdout;
FILE *stderr;
#endif

/*
 * Used in image.c
 */
int fprintf(FILE *file, const char *format, ...) {
    va_list       args;
    va_start(args, format);
    guk_cprintk(0, format, args);
    va_end(args);
    return 0;
}

/*
 * Used in log_*
 */
int printf(const char *format, ...) {
    va_list       args;
    va_start(args, format);
    guk_cprintk(0, format, args);
    va_end(args);
    return 0;
}


/*
 * Used in log_println
 */
int vprintf(const char *format, va_list args) {
    guk_cprintk(0, format, args);
    return 0;
}

/*
 * Used in log_exit
 */
int vfprintf(FILE *file, const char *format, va_list args) {
	guk_cprintk(0, format, args);
	return 0;
}

/*
 * Used in log_*
 */
int fflush(FILE *file) {
  return 0;
}


/*
 * Used by Maxine log_* when compiled under Solaris
 */
#ifdef solaris
int putc(int a1, FILE *a2) {
  char buf[2];
  buf[0] = a1; buf[1] = 0;
  guk_printk("%s", buf);
  return 0;
}
#endif

/*
 * Used by Maxine log_* when compiled under Linux
 */
#ifdef linux
int _IO_putc(int a1, FILE *a2) {
	  char buf[2];
	  buf[0] = a1; buf[1] = 0;
	  guk_printk("%s", buf);
	  return 0;
}
#endif

/*
 * A mystery why we need this. Some of the zlib object files
 * have this as an undefined symbol. However, neither gcc -E nor objdump
 * provide any indication as to where the reference actually is.
 */
#ifdef linux
size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream) {
    guk_crash_exit_msg("fwrite called!");
    return 0;
}
#endif
/*
 * Used in maxine, aka maxine main method
 */
void close(int fd) {
}

/*
 * Used in log_lock/unlock
 */
char * strerror(int errnum) {
    return "strerror not implemented";
}

/*
 * Used in memory.c
 */
void perror(const char *msg) {
    guk_printk("%s\n", msg);
}

/*
  Used in fdlibm
  */
int fputs(const char *msg, FILE *stream) {
    guk_printk("%s\n", msg);
    return 0;
}
