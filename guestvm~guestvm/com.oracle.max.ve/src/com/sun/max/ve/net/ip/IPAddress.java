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
package com.sun.max.ve.net.ip;

public class IPAddress {

    private int _address;

    public IPAddress(int a, int b, int c, int d) {
        _address = to32(a, b, c, d);
    }

    public IPAddress(int[] a) {
        this(a[0], a[1], a[2], a[3]);
    }

    public IPAddress(byte[] a) {
        this(a[0], a[1], a[2], a[3]);
    }

    public IPAddress(int a) {
        _address = a;
    }

    public static int byteToInt(byte[] a) {
        return to32(a[0], a[1], a[2], a[3]);
    }

    private static int to32(int a, int b, int c, int d) {
        return ((a & 0xff) << 24) | ((b & 0xff) << 16) | ((c & 0xff) << 8) | (d & 0xff);
    }

    public int addressAsInt() {
        return _address;
    }

    public static String toString(int a) {
        StringBuilder sb = new StringBuilder();
        sb.append((a >> 24) & 0xff).append('.').append((a >> 16) & 0xff).append('.').append((a >> 8) & 0xff).append('.').append(a & 0xff);
        return sb.toString();
    }

    public static String toReverseString(int a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a & 0xff).append('.').append((a >> 8) & 0xff).append('.').append((a >> 16) & 0xff).append('.').append((a >> 24)  & 0xff);
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(_address);
    }

    private static IPAddress _loopback;
    public static IPAddress loopback() {
        if (_loopback == null) {
            _loopback = new IPAddress(127, 0, 0, 1);
        }
        return _loopback;
    }

    /**
     * Utility to convert a dotted-decimal string to an IP address.
     *
     */
    public static IPAddress parse(String s)
                                throws java.lang.NumberFormatException {
        int val = 0;
        int idx = 0;


        fail: while (true) {
            for (int pos = 0 ; pos < 4 ; pos++) {        // loop through 4 bytes
                int n = 0;
                boolean firstDigit = true;
                while (idx < s.length()) {
                    char c = s.charAt(idx);

                    // ensure at least one digit
                    if (firstDigit && !Character.isDigit(c)) {
                        break fail;
                    }

                    // terminator must be . for 1st three bytes
                    if (c == '.' && pos < 3) {
                        idx++;
                        break;        // done with this position
                    }

                    // any non digit is bad
                    if (!Character.isDigit(c)) {
                        break fail;
                    }

                    // use digit
                    n = n*10 + Character.digit(c, 10);
                    idx++;

                    // range check result
                    if (n > 255) {
                        break fail;
                    }
                    firstDigit = false;
                } // while

                // if we used the entire string but didn't do
                // all four bytes, we've got a problem
                if (idx == s.length() && pos != 3) {
                    break fail;
                }
                val += (n << ((3-pos) * 8));
            } // for

            return new IPAddress(val);
        }

        throw new java.lang.NumberFormatException("Illegal IP address");
    }

}
