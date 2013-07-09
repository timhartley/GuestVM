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
package test.java.lang.management;

import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;

public class MXTest {
    private static Map<String, Info> _commands = new HashMap<String, Info>();
    private static boolean _verbose;
    /**
     * @param args
     */
    public static void main(String[] args) {
        final String[] ops = new String[10];
        final String[] opArgs1 = new String[10];
        final String[] opArgs2 = new String[10];
        final String[] type1 = new String[10];
        final String[] type2 = new String[10];
        int opCount = 0;
        for (int i = 0; i < ops.length; i++) {
            type1[i] = "s";
            type2[i] = "s";
        }
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("a1")) {
                opArgs1[opCount] = args[++i];
            } else if (arg.equals("a2")) {
                opArgs2[opCount] = args[++i];
            } else if (arg.equals("op")) {
                ops[opCount++] = args[++i];
            } else if (arg.equals("t1")) {
                type1[opCount] = args[++i];
            } else if (arg.equals("t2")) {
                type2[opCount] = args[++i];
            } else if (arg.equals("v")) {
                _verbose = true;
            }
        }
        // Checkstyle: resume modified control variable check

        if (opCount == 0 && !_verbose) {
            System.out.println("no operations given");
            return;
        }

        new MyThread(10).start();

        try {
            // the singletons
            enterMXBeanCommands("R", RuntimeMXBean.class, ManagementFactory.getRuntimeMXBean());
            enterMXBeanCommands("T", ThreadMXBean.class, ManagementFactory.getThreadMXBean());
            enterMXBeanCommands("OS", OperatingSystemMXBean.class, ManagementFactory.getOperatingSystemMXBean());
            enterMXBeanCommands("M", MemoryMXBean.class, ManagementFactory.getMemoryMXBean());
            enterMXBeanCommands("CC", CompilationMXBean.class, ManagementFactory.getCompilationMXBean());
            enterMXBeanCommands("CL", ClassLoadingMXBean.class, ManagementFactory.getClassLoadingMXBean());
            // these calls can all return multiple instances
            final List<MemoryManagerMXBean> theMemoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();
            for (int i = 0; i < theMemoryManagerMXBeans.size(); i++) {
                enterMXBeanCommands("MM", MemoryManagerMXBean.class, i, theMemoryManagerMXBeans.get(i));
            }
            final List<GarbageCollectorMXBean> theGarbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (int i = 0; i < theGarbageCollectorMXBeans.size(); i++) {
                enterMXBeanCommands("GC", GarbageCollectorMXBean.class, i, theGarbageCollectorMXBeans.get(i));
            }
            final List<MemoryPoolMXBean> theMemoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
            for (int i = 0; i < theMemoryPoolMXBeans.size(); i++) {
                enterMXBeanCommands("MP", MemoryPoolMXBean.class, i, theMemoryPoolMXBeans.get(i));;
            }

            for (int j = 0; j < opCount; j++) {
                final String opArg1 = opArgs1[j];
                final String opArg2 = opArgs2[j];
                final String op = ops[j];

                final Info info = _commands.get(op);
                if (info != null) {
                    System.out.println("invoking " + op);
                    try {
                        final Object[] opArgs = opArg1 == null ? null : (opArg2 == null ? new Object[1] : new Object[2]);
                        if (opArg1 != null) {
                            opArgs[0] = processType(opArg1, type1[j]);
                        }
                        if (opArg2 != null) {
                            opArgs[1] = processType(opArg2, type2[j]);
                        }
                        final Object result = info._m.invoke(info._bean, opArgs);
                        if (result instanceof String[]) {
                            final String[] resultArray = (String[]) result;
                            for (String r : resultArray) {
                                System.out.print(r); System.out.print(" ");
                            }
                            System.out.println();
                        } else if (result instanceof long[]) {
                            final long[] resultArray = (long[]) result;
                            for (long l : resultArray) {
                                System.out.print(l); System.out.print(" ");
                            }
                            System.out.println();
                        } else {
                            System.out.println(result);
                        }
                    } catch (InvocationTargetException ex) {
                        System.out.println(ex);
                        final Throwable cause = ex.getCause();
                        System.out.println(cause);
                        cause.printStackTrace();
                    } catch (Throwable t) {
                        System.out.println(t);
                        t.printStackTrace();
                    }
                } else {
                    if (op.equals("GC")) {
                        System.gc();
                    } else {
                        throw new Exception("method " + op + "  not found");
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    static class Info {
        Object _bean;
        Method _m;
        Info(Object bean, Method m) {
            _bean = bean;
            _m = m;
        }

    }

    private static Object processType(String arg, String type) throws Exception {
        if (type.equals("s")) {
            return arg;
        } else if (type.equals("l")) {
            return Long.parseLong(arg);
        } else if (type.equals("i")) {
            return Integer.parseInt(arg);
        } else if (type.equals("b")) {
            return Boolean.parseBoolean(arg);
        } else {
            throw new Exception("uninterpreted type  " + type);
        }
    }

    private static void enterMXBeanCommands(String prefix, Class<?> klass, Object mxbean) {
        enterMXBeanCommands(prefix, klass, -1, mxbean);
    }

    private static void enterMXBeanCommands(String prefix, Class<?> klass, int x, Object mxbean) {
        final Method[] methods = klass.getDeclaredMethods();
        String prefixNum = x < 0 ? prefix : prefix + "." + x ;
        for (int i = 0; i < methods.length; i++) {
            final String defaultCommandName = prefixNum + "." + methods[i].getName();
            String commandName = defaultCommandName;
            int suffix = 1;
            while (_commands.get(commandName) != null) {
                commandName = defaultCommandName + "_" + suffix++;
            }
            if (_verbose) {
                System.out.println("entering " + commandName + " for " + methods[i].toGenericString());
            }
            _commands.put(commandName, new Info(mxbean, methods[i]));
        }
    }

    static class MyThread extends Thread {
        private int depth;
        MyThread(int depth) {
            this.depth = depth;
            setDaemon(true);
        }

        @Override
        public void run() {
            System.out.println("myThread id: " + getId());
            recurse(depth);
        }

        private void recurse(int depth) {
            if (depth > 0) {
                recurse(depth - 1);
            } else {
                int count = 0;
                while (true) {
                    count++;
                }
            }
        }
    }

}
