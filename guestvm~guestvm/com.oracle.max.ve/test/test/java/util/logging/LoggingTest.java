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
package test.java.util.logging;

import java.util.logging.*;

public class LoggingTest {

    private static Level[] _levels = new Level[] {Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST};
    /**
     * @param args
     */
    public static void main(String[] args) {
        final Logger globalLogger = globalLogger();
        showLevel(globalLogger);
        final Logger myLoggerA = Logger.getLogger("LoggingTest.A");
        showLevel(myLoggerA);
        final Logger myLoggerB = Logger.getLogger("LoggingTest.B");
        showLevel(myLoggerB);
        iterate(globalLogger);
        iterate(myLoggerA);
        iterate(myLoggerB);
    }

    private static Logger globalLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }
    
    private static void iterate(Logger logger) {
        for (Level level : _levels) {
            logger.log(level, "Logger \"" + logger.getName() + "\" logging at level " + level);
        }
    }
    
    private static void showLevel(Logger logger) {
        System.out.println("Logger \"" + logger.getName() +  "\" level is " + logger.getLevel());
    }

}
