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
  * Native symbols for Maxine Log class
  */

#include <lib.h>
#include <log.h>

void *maxine_log_dlsym(const char *symbol) {
    if (strcmp(symbol, "log_print_symbol") == 0) return log_print_symbol;
    else if (strcmp(symbol, "log_lock") == 0) return log_lock;
    else if (strcmp(symbol, "log_unlock") == 0) return log_unlock;
    else if (strcmp(symbol, "log_print_int") == 0) return log_print_int;
    else if (strcmp(symbol, "log_print_long") == 0) return log_print_long;
    else if (strcmp(symbol, "log_print_word") == 0) return log_print_word;
    else if (strcmp(symbol, "log_print_boolean") == 0) return log_print_boolean;
    else if (strcmp(symbol, "log_print_char") == 0) return log_print_char;
    else if (strcmp(symbol, "log_print_buffer") == 0) return log_print_buffer;
    else if (strcmp(symbol, "log_print_newline") == 0) return log_print_newline;
    else if (strcmp(symbol, "log_print_float") == 0) return log_print_float;
    else if (strcmp(symbol, "log_print_double") == 0) return log_print_double;
    else if (strcmp(symbol, "log_flush") == 0) return log_flush;
    else return 0;
}
