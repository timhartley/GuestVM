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
package com.sun.max.ve.sched;

import com.sun.max.program.*;
import com.sun.max.ve.sched.nopriority.*;
import com.sun.max.vm.thread.*;

/**
 * An abstract Factory to create compatible threads and run queue for the MaxVE scheduler.
 *
 * @author Harald Roeck
 * @author Mick Jordan
 *
 */
public abstract class RunQueueFactory {
    /**
     * The name of the system property specifying a subclass of {@link RunQueueFactory} that is
     * to be instantiated and used at runtime to create Scheduler run queue instances and their
     * associated subclass of GVmThread.
     * The choice of class is made at image build time.
     */
    public static final String RUNQUEUE_FACTORY_CLASS_PROPERTY_NAME = "max.ve.scheduler.runqueue.factory.class";

    protected static RunQueueFactory _instance = null;

    protected RunQueueFactory() {
    }

    private static void instantiateFactory() {
        final String factoryClassName = System.getProperty(RUNQUEUE_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            _instance = new RingRunQueueFactory();
        } else {
            try {
                _instance = (RunQueueFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
            }
        }
    }

    public static RunQueueFactory getInstance() {
        if (_instance == null) {
            instantiateFactory();
        }
        return _instance;
    }

    public abstract VmThread newVmThread();

    public abstract RunQueue<GUKVmThread> createRunQueue();
}
