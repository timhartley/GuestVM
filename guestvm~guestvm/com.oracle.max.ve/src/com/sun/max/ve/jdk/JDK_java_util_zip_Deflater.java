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
package com.sun.max.ve.jdk;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.classfile.constant.SymbolTable;

/**
 * Substitutions for  @see java.util.zip.Deflater.
 * This version is an almost pure delegation to the GNU Classpath version.
 * All the public methods in java.util.zip.Deflater are substituted and call the
 * GNU version, except those that directly forward to another public method
 * without accessing the implementation state.
 *
 * N.B. This code depends on the implementation of java.util.zip.Deflater.
 * It substitutes the native "init" method and uses the "strm" field to index an
 * array of GNU Deflater instances.
 *
 * @author Mick Jordan
 *
 */

@SuppressWarnings("unused")

@METHOD_SUBSTITUTIONS(Deflater.class)
public class JDK_java_util_zip_Deflater {

    @SUBSTITUTE
    private static void initIDs() {
    }

    private static List<gnu.java.util.zip.Deflater> _gnuDeflaters = new ArrayList<gnu.java.util.zip.Deflater>();

    @CONSTANT_WHEN_NOT_ZERO
    private static FieldActor _strmFieldActor;

    @INLINE
    static FieldActor strmFieldActor() {
        if (_strmFieldActor == null) {
            _strmFieldActor = (FieldActor) ClassActor.fromJava(Deflater.class).findFieldActor(SymbolTable.makeSymbol("strm"), null);
        }
        return _strmFieldActor;
    }

    static int getIndex() {
        final int size = _gnuDeflaters.size();
        for (int i = 0; i < size; i++) {
            if (_gnuDeflaters.get(i) == null) {
                return i;
            }
        }
        _gnuDeflaters.add(null);
        return size;
    }

    static gnu.java.util.zip.Deflater getGNUDeflater(Object deflater) {
        return _gnuDeflaters.get((int) strmFieldActor().getLong(deflater));
    }

    @SUBSTITUTE
    private static long init(int level, int strategy, boolean nowrap) {
        final gnu.java.util.zip.Deflater gnuDeflater = new gnu.java.util.zip.Deflater(level, nowrap);
        synchronized (_gnuDeflaters) {
            final int index = getIndex();
            _gnuDeflaters.add(index, gnuDeflater);
            return index;
        }
    }

    @SUBSTITUTE
    private void setInput(byte[] b, int off, int len) {
        final gnu.java.util.zip.Deflater gnuDeflater = getGNUDeflater(this);
        gnuDeflater.setInput(b, off, len);
    }

    @SUBSTITUTE
    private void setDictionary(byte[] b, int off, int len) {
        final gnu.java.util.zip.Deflater gnuDeflater = getGNUDeflater(this);
        gnuDeflater.setDictionary(b, off, len);
    }

    @SUBSTITUTE
    private void setStrategy(int strategy) {
        getGNUDeflater(this).setStrategy(strategy);
    }

    @SUBSTITUTE
    private boolean needsInput() {
        return getGNUDeflater(this).needsInput();
    }

    @SUBSTITUTE
    private void setLevel(int level) {
        getGNUDeflater(this).setLevel(level);
    }

    @SUBSTITUTE
    private boolean finished() {
        return getGNUDeflater(this).finished();
    }

    @SUBSTITUTE
    private void finish() {
        getGNUDeflater(this).finish();
    }

    @SUBSTITUTE
    private int deflate(byte[] b, int off, int len) throws DataFormatException {
        return getGNUDeflater(this).deflate(b, off, len);
    }

    @SUBSTITUTE
    private int getAdler() {
        return getGNUDeflater(this).getAdler();
    }

    @SUBSTITUTE
    private long getBytesRead() {
        return getGNUDeflater(this).getBytesRead();
    }

    @SUBSTITUTE
    private long getBytesWritten() {
        return getGNUDeflater(this).getBytesWritten();
    }

    @SUBSTITUTE
    private void reset() {
        getGNUDeflater(this).reset();
    }

    @SUBSTITUTE
    private void end() {
        final int strm = (int) strmFieldActor().getLong(this);
        if (strm != 0) {
            _gnuDeflaters.get(strm).end();
            strmFieldActor().setLong(this, 0);
            _gnuDeflaters.set(strm, null);
        }
    }

}
