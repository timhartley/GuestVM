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

/**
 * This version is an almost pure delegation to the GNU Classpath version. All the public methods in
 * {@link java.util.zip.Inflater} are substituted and call the GNU version, except those that directly forward to
 * another public method without accessing the implementation state.
 * 
 * We use an injected field to store the {@link gnu.java.util.zip.Inflater} instance in the
 * {@link java.util.zip.Inflater} instance. We are somewhat dependent on the implementation
 * of {@link java.util.zip.Inflater} but independent of its state, which is good because this
 * changed in JDK 1.6.0_19. The trick is to substitute the constructor, which allows
 * us to store the {@link gnu.java.util.zip.Inflater} in the injected field. We can't do this
 * in the {@code init} method because it is, sigh, {@code static}.
 *
 * @author Mick Jordan
 */

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.sun.max.annotate.CONSTANT_WHEN_NOT_ZERO;
import com.sun.max.annotate.INLINE;
import com.sun.max.annotate.METHOD_SUBSTITUTIONS;
import com.sun.max.annotate.SUBSTITUTE;
import com.sun.max.ve.error.VEError;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.FieldActor;
import com.sun.max.vm.actor.member.InjectedReferenceFieldActor;
import com.sun.max.vm.classfile.constant.SymbolTable;

@SuppressWarnings("unused")
@METHOD_SUBSTITUTIONS(Inflater.class)
public class JDK_java_util_zip_Inflater {

    /**
     * A field of type {@link gnu.java.util.zip.Inflater} injected into {@link java.util.zip.Inflater}.
     */
    private static final InjectedReferenceFieldActor<gnu.java.util.zip.Inflater> Inflater_gnuInflater = new InjectedReferenceFieldActor<gnu.java.util.zip.Inflater>(java.util.zip.Inflater.class, gnu.java.util.zip.Inflater.class) {
    };
    
    @INLINE
    static gnu.java.util.zip.Inflater getGNUInflater(Object inflater) {
       return (gnu.java.util.zip.Inflater) Inflater_gnuInflater.getObject(inflater);
    }

    @SUBSTITUTE
    private static void initIDs() {
    }
    
    @SUBSTITUTE(constructor=true)
    private void constructor(boolean nowrap) {
        final gnu.java.util.zip.Inflater gnuInflater = new gnu.java.util.zip.Inflater(nowrap);
        Inflater_gnuInflater.setObject(this, gnuInflater);
    }

    @SUBSTITUTE
    private static long init(boolean nowrap) {
        // having substituted the constructor this should never be called, but just to be safe! 
        VEError.unexpected("java.util.zip.Inflater.init should never be called!");
        return 0;
    }

    @SUBSTITUTE
    private void setInput(byte[] b, int off, int len) {
        final gnu.java.util.zip.Inflater gnuInflater = getGNUInflater(this);
        gnuInflater.setInput(b, off, len);
    }

    @SUBSTITUTE
    private void setDictionary(byte[] b, int off, int len) {
        final gnu.java.util.zip.Inflater gnuInflater = getGNUInflater(this);
        gnuInflater.setDictionary(b, off, len);
    }

    @SUBSTITUTE
    private int getRemaining() {
        return getGNUInflater(this).getRemaining();
    }

    @SUBSTITUTE
    private boolean needsInput() {
        return getGNUInflater(this).needsInput();
    }

    @SUBSTITUTE
    private boolean needsDictionary() {
        return getGNUInflater(this).needsDictionary();
    }

    @SUBSTITUTE
    private boolean finished() {
        return getGNUInflater(this).finished();
    }

    @SUBSTITUTE
    private int inflate(byte[] b, int off, int len) throws DataFormatException {
        return getGNUInflater(this).inflate(b, off, len);
    }

    @SUBSTITUTE
    private int getAdler() {
        return getGNUInflater(this).getAdler();
    }

    @SUBSTITUTE
    private long getBytesRead() {
        return getGNUInflater(this).getBytesRead();
    }

    @SUBSTITUTE
    private long getBytesWritten() {
        return getGNUInflater(this).getBytesWritten();
    }

    @SUBSTITUTE
    private void reset() {
        getGNUInflater(this).reset();
    }

    @SUBSTITUTE
    private void end() {
        getGNUInflater(this).end();
        Inflater_gnuInflater.setObject(this, null);
    }
    
    
}
