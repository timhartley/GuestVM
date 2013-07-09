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
package com.sun.max.ve.spinlock;

import com.sun.max.program.ProgramError;

/**
 * A factory that permits subclasses of SpinLock to b created. To create instances of a {@code SpinLock} subclass,
 * the {@link #SPINLOCK_FACTORY_CLASS_PROPERTY_NAME} property needs to be defined at image build time.
 *
 * @author Mick Jordan
 */
public abstract class SpinLockFactory {
    /**
     * The name of the system property specifying a subclass of {@link SpinLockFactory} that is
     * to be instantiated and used at runtime to create SpinLock instances. If not specified,
     * then the instance must be set at runtime via setInstance.
     */
    public static final String SPINLOCK_FACTORY_CLASS_PROPERTY_NAME = "max.ve.spinlock.factory.class";

    private static SpinLockFactory _instance = null;

    static {
        final String factoryClassName = System.getProperty(SPINLOCK_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName != null) {
            try {
                _instance = (SpinLockFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
            }
        }
    }

    /**
     * Subclasses override this method to instantiate objects of a SpinLock subclass.
     *
     */
    protected abstract SpinLock newSpinLock();

    public static SpinLockFactory getInstance() {
        return _instance;
    }

    public static void setInstance(SpinLockFactory instance) {
        _instance = instance;
    }

    /**
     * Creates a SpinLock object.
     *
     * @return a particular subclass of a SpinLock
     */
    public static SpinLock create() {
        return _instance.newSpinLock();
    }

    /**
     * Creates a SpinLock object and initializes it.
     *
     * @return a particular subclass of a SpinLock
     */
    public static SpinLock createAndInit() {
        return (SpinLock) _instance.newSpinLock().initialize();
    }

}
