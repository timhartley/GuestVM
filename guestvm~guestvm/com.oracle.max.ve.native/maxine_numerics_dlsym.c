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
 * Miscellaneous numeric function symbol definitions from Maxine.
 *
 * Author: Mick Jordan
 */

#include <types.h>
#include <lib.h>
#include <jni.h>

extern double __ieee754_fmod(double x, double y);
double fmod(double a, double b) {
	return __ieee754_fmod(a, b);
}

// Defined in substrate/snippet.c
extern jint nativeLongCompare(jlong greater, long less);
extern jlong nativeLongSignedShiftedRight(jlong number, int shift);
extern jlong nativeLongMultiply(jlong factor1, jlong factor2);
extern jlong nativeLongDivided(jlong dividend, jlong divisor);
extern jlong nativeLongRemainder(jlong dividend, jlong divisor);
extern jfloat nativeFloatRemainder(jfloat dividend, jfloat divisor);
extern jdouble nativeDoubleRemainder(JNIEnv *env, jclass c, jdouble dividend, jdouble divisor);

JNIEXPORT jfloat JNICALL
 Java_java_lang_Float_intBitsToFloat(JNIEnv *env, jclass unused, jint v) {
    union {
	int i;
	float f;
    } u;
    u.i = (long)v;
    return (jfloat)u.f;
}

JNIEXPORT jint JNICALL
Java_java_lang_Float_floatToRawIntBits(JNIEnv *env, jclass unused, jfloat v)
{
    union {
	int i;
	float f;
    } u;
    u.f = (float)v;
    return (jint)u.i;
}

/* Useful on machines where jlong and jdouble have different endianness. */
#define jlong_to_jdouble_bits(a)
#define jdouble_to_jlong_bits(a)

JNIEXPORT jdouble JNICALL
Java_java_lang_Double_longBitsToDouble(JNIEnv *env, jclass unused, jlong v)
{
    union {
	jlong l;
	double d;
    } u;
    jlong_to_jdouble_bits(&v);
    u.l = v;
    return (jdouble)u.d;
}

/*
 * Find the bit pattern corresponding to a given double float, NOT collapsing NaNs
 */
JNIEXPORT jlong JNICALL
Java_java_lang_Double_doubleToRawLongBits(JNIEnv *env, jclass unused, jdouble v)
{
    union {
	jlong l;
	double d;
    } u;
    jdouble_to_jlong_bits(&v);
    u.d = (double)v;
    return u.l;
}


void *maxine_numerics_dlsym(const char *symbol) {
  if (strcmp(symbol, "nativeLongCompare") == 0) return nativeLongCompare;
  else if (strcmp(symbol, "nativeLongSignedShiftedRight") == 0) return nativeLongSignedShiftedRight;
  else if (strcmp(symbol, "nativeLongMultiply") == 0) return nativeLongMultiply;
  else if (strcmp(symbol, "nativeLongDivided") == 0) return nativeLongDivided;
  else if (strcmp(symbol, "nativeLongRemainder") == 0) return nativeLongRemainder;
  else if (strcmp(symbol, "nativeFloatRemainder") == 0) return nativeFloatRemainder;
  else if (strcmp(symbol, "nativeDoubleRemainder") == 0) return nativeDoubleRemainder;
  else if (strcmp(symbol, "Java_java_lang_Float_intBitsToFloat") == 0) return Java_java_lang_Float_intBitsToFloat;
  else if (strcmp(symbol, "Java_java_lang_Float_floatToRawIntBits") == 0) return Java_java_lang_Float_floatToRawIntBits;
  else if (strcmp(symbol, "Java_java_lang_Double_longBitsToDouble") == 0) return Java_java_lang_Double_longBitsToDouble;
  else if (strcmp(symbol, "Java_java_lang_Double_doubleToRawLongBits") == 0) return Java_java_lang_Double_doubleToRawLongBits;
  else return 0;
}

