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
import com.sun.max.ve.sched.std.StdSchedulerFactory;

/**
 * A factory that permits subclasses of Scheduler to be created. To create instances of a {@code Scheduler} subclass,
 * the {@link #MUTEX_FACTORY_CLASS_PROPERTY_NAME} property needs to be defined at image build time.
 *
 * @author Mick Jordan
 *
 */

public abstract class SchedulerFactory {

    /**
     * The name of the system property specifying a subclass of {@link SchedulerFactory} that is
     * to be instantiated and used to create Scheduler instances.
     */
    public static final String SCHEDULER_FACTORY_CLASS_PROPERTY_NAME = "max.ve.scheduler.factory.class";

    protected static SchedulerFactory _instance;
    protected static Scheduler _scheduler;

    protected SchedulerFactory() {
    }

    static {
        final String factoryClassName = System.getProperty(SCHEDULER_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            _instance = new StdSchedulerFactory();
        } else {
            try {
                _instance = (SchedulerFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
            }
        }
        // Scheduler must be created at image build time
        _scheduler = _instance.createScheduler();
    }

    public static SchedulerFactory instance() {
        return _instance;
    }

    public abstract Scheduler createScheduler();

    public static Scheduler scheduler() {
        if (_scheduler == null) {
            _scheduler = _instance.createScheduler();
        }
        return _scheduler;
    }
}
