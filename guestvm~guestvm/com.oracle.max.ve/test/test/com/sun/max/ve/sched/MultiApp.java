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
package test.com.sun.max.ve.sched;

/**
 * Runs multiple "apps", i.e., tests, each as a separate thread. Useful for testing the scheduler.
 *
 * @author Mick Jordan
 *
 */

import java.lang.reflect.*;
import java.util.*;

public class MultiApp {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final List<App> apps = new ArrayList<App>();
        App app = null;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("app")) {
                app = new App(args[++i]);
                apps.add(app);
            } else {
                app.addArg(arg);
            }
        }
        // Checkstyle: resume modified control variable check

        for (App a : apps) {
            a.start();
        }
        for (App a : apps) {
            a.join();
        }
    }

    static class App implements Runnable {
        String _className;
        List<String> _args = new ArrayList<String>();
        Thread _thread;

        App(String className) {
            _className = className;
        }

        void addArg(String arg) {
            _args.add(arg);
        }

        void start() {
            _thread = new Thread(this);
            _thread.setName("App:" + _className);
            _thread.start();
        }

        void join() {
            try {
                _thread.join();
            } catch (InterruptedException ex) {

            }
        }

        public void run() {
            final String[] args = _args.toArray(new String[_args.size()]);
            try {
                final Class<?> klass = Class.forName(_className);
                final Method main = klass.getDeclaredMethod("main", String[].class);
                main.invoke(null, new Object[]{args});
            } catch (Exception ex) {
                System.err.println("failed to run app " + _className);
                ex.printStackTrace();
            }
        }
    }

}
