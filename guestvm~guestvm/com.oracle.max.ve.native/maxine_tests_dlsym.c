/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
  * Native symbols for Maxine tests.
  * These are only needed for test programs.
  */

#include <lib.h>
#include <jni.h>

extern jlong JNICALL
Java_test_bench_threads_JNI_1invocations_nativework(JNIEnv *env, jclass cls, jlong workload);
extern jint JNICALL
Java_jtt_jni_JNI_1OverflowArguments_read1(JNIEnv *env, jclass cls, jlong zfile,
														jlong zentry, jlong pos, jbyteArray bytes, jint off, jint len);
extern jint JNICALL
Java_jtt_jni_JNI_1OverflowArguments_read2(JNIEnv *env, jclass cls, jlong zfile,
														jlong zentry, jlong pos, jbyteArray bytes, jint off, jint len);

void *maxine_tests_dlsym(const char *symbol) {
    if (strcmp(symbol, "Java_test_bench_threads_JNI_1invocations_nativework") == 0) return Java_test_bench_threads_JNI_1invocations_nativework;
    else if (strcmp(symbol, "Java_jtt_jni_JNI_1OverflowArguments_read1") == 0) return Java_jtt_jni_JNI_1OverflowArguments_read1;
    else if (strcmp(symbol, "Java_jtt_jni_JNI_1OverflowArguments_read2") == 0) return Java_jtt_jni_JNI_1OverflowArguments_read2;
   else return 0;
}
