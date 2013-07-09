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
package com.sun.max.vm.hosted;

import java.io.*;
import java.util.*;

import com.sun.max.io.*;
import com.sun.max.io.Streams.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.ve.fs.image.BootImageFileSystem;
import com.sun.max.vm.hosted.BootImageGenerator;

import test.com.sun.max.vm.compiler.JavaTester;

/**
 * Generates the Maxine VE boot image.
 * Assumes that the current working directory contains the VE project directories and
 * that "maxine" and "guk" are sibling directories.
 */
public class VEBootImageGenerator {
    private static final OptionSet _options = new OptionSet(true);
    private static final String VMNative = "com.oracle.max.vm.native";
    private static final String VENative = "com.oracle.max.ve.native";

    private static final Option<String> BOOTIMAGEFS_SPEC_FILE = _options.newStringOption("bootimagefscontents", VENative + "/bootimagefilespecs/tests",
                "File defining the content of the boot image file system");
    private static final Option<String> MAINCLASS = _options.newStringOption("mainclass", null,
                "main class to compile (bound image)");
    private static final Option<Boolean> NO_MAXINE_IMAGE = _options.newBooleanOption("nomaxineimage", false,
                "Suppress image build step");
    private static final Option<Boolean> NO_ASM_IMAGE = _options.newBooleanOption("noasmimage", false,
                "Suppress the image assembly step");
    private static final Option<Boolean> USE_ASM = _options.newBooleanOption("asm", false,
                "Create assembler form of image and use gcc (i.e. do not create ELF directly)");
    private static final Option<Boolean> NO_MAXINE_NATIVE = _options.newBooleanOption("nomaxinenative", false,
                "Suppress the Maxine native gmake step");
    private static final Option<Boolean> NO_VE_NATIVE = _options.newBooleanOption("novenative", false,
                "Suppress the VE native gmake step");
    private static final Option<List<String>> MAXINE_IMAGE_PROPS = _options.newStringListOption("maxineimageprops", null, ',',
                "list of system properties to pass to Maxine image generator");
    private static final Option<List<String>> MAXINE_IMAGE_ARGS = _options.newStringListOption("maxineimageargs", null, ',',
                "list of additional args to pass to Maxine image generator");
    private static final Option<List<String>> MAXINE_IMAGE_VMARGS = _options.newStringListOption("maxineimagevmargs", "-Xms1200m,-Xmx1200m", ',',
                "list of vm args to pass to Maxine image generator");
    private static final Option<String> TESTGEN = _options.newStringOption("testgen", null,
                "Generate the JavaTesterRunScheme/JavaTests for given directory in test.com.sun.max.vm.testrun.some package");
    private static final Option<String> MONITOR = _options.newStringOption("monitor", null,
                 "Use given monitor scheme");
    private static final Option<Boolean> ECHO = _options.newBooleanOption("echo", false,
                  "Echo the commands but do not execute them");
    private static final Option<String> MUTEX_FACTORY = _options.newStringOption("mutexfactory", "com.sun.max.vm.monitor.modal.sync.nat.NativeMutexFactory",
                  "Name of mutex factory class");
    private static final Option<String> CONDITIONVARIABLE_FACTORY = _options.newStringOption("condvarfactory", "com.sun.max.vm.monitor.modal.sync.nat.NativeConditionVariableFactory",
                  "Name of condition variable factory class");
    private static final Option<String> SPINLOCK_FACTORY = _options.newStringOption("spinlockfactory", "com.sun.max.ve.spinlock.guk.GUKSpinLockFactory",
                  "Name of spinlock factory class");
    private static final Option<String> TESTRUN = _options.newStringOption("testrun", null,
                  "Run the JavaTesterRunScheme in given package");
    private static final Option<String> SCHEDULER_FACTORY = _options.newStringOption("schedfactory", "com.sun.max.ve.sched.std.StdSchedulerFactory",
                   "Name of scheduler factory class");
    private static final Option<String> SCHEDRUNQUEUE_FACTORY = _options.newStringOption("schedrunqueuefactory", "com.sun.max.ve.sched.nopriority.RingRunQueueFactory",
                  "Name of scheduler run queue factory class");
    private static final Option<String> NETCONFIG_FILE = _options.newStringOption("netconfig", VENative + "/netconfig",
                  "Name of file containing network configuration properties");
    private static final Option<String> VE_IMAGE_SUFFIX = _options.newStringOption("imagesuffix", null,
                  "Suffix to append to max.ve image name, i.e., giving max.ve-imagesuffix");
    private static final Option<String> BUILD_TYPE = _options.newStringOption("buildtype", "PRODUCT",
                   "Build type: DEBUG or PRODUCT (default)");
    private static final Option<String> PREFIX_CLASSPATH = _options.newStringOption("prefixclasspath", null,
                    "classpath to prefix");

    public static void main(String[] args)  throws IOException, InterruptedException {
        Trace.addTo(_options);
        // parse the arguments
        final String[] arguments = _options.parseArguments(args).getArguments();
        if (TESTGEN.getValue() != null) {
            generateTests(arguments);
        }
        if (!NO_MAXINE_IMAGE.getValue()) {
            buildMaxineImage(arguments);
        }
        if (!NO_ASM_IMAGE.getValue()) {
            asmImage(arguments);
        }
        if (!NO_MAXINE_NATIVE.getValue()) {
            maxineNative(arguments);
        }
        if (!NO_VE_NATIVE.getValue()) {
            veNative(arguments);
        }
    }

    private static void generateTests(String[] arguments)  throws IOException, InterruptedException {
        final String[] testerArgs = {"-scenario=target", TESTGEN.getValue()};
        final String [] javaArgs = buildJavaArgs(JavaTester.class, null, new String[0], new String[0], testerArgs);
        if (!checkEcho(javaArgs)) {
            exec(new File(maxineDir() + "/com.oracle.max.vm/test"), javaArgs, System.out, System.err, System.in);
        }
    }

    private static void buildMaxineImage(String[] arguments)  throws IOException, InterruptedException {
        final String[] generatorArgs = {"-trace=1", "-run=com.sun.max.vm.run.max.ve", "-build=" + BUILD_TYPE.getValue()};
        final String[] defaultSystemProperties = {
            "max.os=MaxVE",
            BootImageFileSystem.BOOTIMAGE_FILESYSTEM_PROPERTY + "=" + BOOTIMAGEFS_SPEC_FILE.getValue(),
            "max.allow.all.core.packages",
            "max.vmthread.factory.class=com.sun.max.ve.sched.VEVmThreadFactory",
            "max.mutex.factory.class=" + MUTEX_FACTORY.getValue(),
            "max.conditionvariable.factory.class=" + CONDITIONVARIABLE_FACTORY.getValue(),
            "max.ve.scheduler.factory.class=" + SCHEDULER_FACTORY.getValue(),
            "max.ve.scheduler.runqueue.factory.class=" +  SCHEDRUNQUEUE_FACTORY.getValue(),
            "max.ve.spinlock.factory.class=" + SPINLOCK_FACTORY.getValue(),
            "max.ve.net.config.file=" + NETCONFIG_FILE.getValue()
        };
        String[] systemProperties = defaultSystemProperties;
        if (TESTRUN.getValue() != null) {
            systemProperties = addArg(systemProperties, "max.vm.run.extendimage.testrun=" + TESTRUN.getValue());
        }
        if (MAINCLASS.getValue() != null) {
            systemProperties = addArg(systemProperties, "max.vm.run.extendimage.mainclass=" + MAINCLASS.getValue());
        }
        if (MAXINE_IMAGE_PROPS.getValue() != null) {
            final List<String> props = MAXINE_IMAGE_PROPS.getValue();
            for (String prop : props) {
                systemProperties = addArg(systemProperties, prop);
            }
        }

        final List<String> vmArgs = MAXINE_IMAGE_VMARGS.getValue();
        final String[] vmArgsArray = addArg(vmArgs.toArray(new String[vmArgs.size()]), "-Xbootclasspath/a:" + pathToVEJDK());
        String[] javaArgs = buildJavaArgs(BootImageGenerator.class, prefixClassPath(fixupClassPath()),  vmArgsArray, systemProperties, generatorArgs);
        if (MONITOR.getValue() != null) {
            javaArgs = addArg(javaArgs, "-monitor=" + MONITOR.getValue());
        }
        if (MAXINE_IMAGE_ARGS.getValue() != null) {
            final List<String> args = MAXINE_IMAGE_ARGS.getValue();
            for (String arg : args) {
                javaArgs = addArg(javaArgs, arg);
            }
        }

        if (!checkEcho(javaArgs)) {
            exec(null, javaArgs, System.out, System.err, System.in);
        }
    }

    private static final String COM_SUN_MAX = ".com.sun.max".replace('.', File.separatorChar);
    private static final int OFFSET_TO_MAXINE = 3;

    private static String fixupClassPath() {
        /* The way that BootImageGenerator determines where the "prototype" native library
           is to be found is by taking the first element of the classpath that contains a com.sun.max package,
           and assuming a sibling relationship between the associated project and the Maxine Native project.
           Unfortunately, VE contains such a package, and is not in a sibling relationship (any more),
           so we must move VE from the front of the classpath before executing BinaryImageGenerator.
           This cannot be done in the Eclipse run configuration as the project containing the main class (i.e. this one)
           must always be first.
         */
        final String cp = System.getProperty("java.class.path");
        final String[] parts = cp.split(File.pathSeparator);
        final boolean[] defer = new boolean[parts.length];
        final StringBuilder sb = new StringBuilder();
        boolean needSep = false;
        for (int i = 0; i < parts.length; i++) {
            defer[i] = false;
            final File file = new File(parts[i] + COM_SUN_MAX);
            if (file.exists()) {
                final String[] nameParts = parts[i].split(File.separator);
                if (!nameParts[nameParts.length - OFFSET_TO_MAXINE].equals("maxine")) {
                    // some directory not a sibling of maxine and with a com.sun.max package
                    defer[i] = true;
                }
            }
            if (!defer[i]) {
                if (needSep) {
                    sb.append(File.pathSeparatorChar);
                }
                sb.append(parts[i]);
                needSep = true;
            }
        }
        for (int i = 0; i < parts.length; i++) {
            if (defer[i]) {
                if (needSep) {
                    sb.append(File.pathSeparatorChar);
                }
                sb.append(parts[i]);
            }
        }
        return sb.toString();
    }

    private static String prefixClassPath(String classPath) {
        if (PREFIX_CLASSPATH.getValue() != null) {
            return PREFIX_CLASSPATH.getValue() + File.pathSeparator + classPath;
        }
        return classPath;
    }

    /**
     * Path where the JDK override code lives, for putting on bootclasspath.
     * @return
     */
    private static String pathToVEJDK() {
        final File path = new File("com.oracle.max.ve.jdk/bin");
        return path.getAbsolutePath();
    }

    private static String[] addArg(String[] arguments, String argument) {
        final String [] result = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, result, 0, arguments.length);
        result[arguments.length] = argument;
        return result;
    }

    private static void asmImage(String[] arguments)  throws IOException, InterruptedException {
        String[] args = {"-image", maxineDir() + "/" + VMNative + "/generated/maxve/maxine.vm"};
        if (USE_ASM.getValue()) {
            args = addArg(args, "-asm");
        }
        if (!checkEcho(args)) {
            MemoryBootImage.main(args);
        }
    }

    private static boolean checkEcho(String[] arguments) {
        if (ECHO.getValue()) {
            for (int i = 0; i < arguments.length; i++) {
                System.out.print(arguments[i] + " ");
            }
            System.out.println("");
            return true;
        } else {
            return false;
        }
    }

    private static void maxineNative(String[] arguments)  throws IOException, InterruptedException {
        final String[] args = {"gmake", "OS=maxve", "substrate"};
        if (!checkEcho(args)) {
            exec(new File(maxineDir() + "/" + VMNative), args, System.out, System.err, System.in);
        }
    }

    /**
     * Return the path to the maxine directory relative to the cwd.
     * @return
     */
    private static String maxineDir() {
        return "../maxine";
    }

    private static void veNative(String[] arguments)  throws IOException, InterruptedException {
        String[] args = {"gmake"};
        if (VE_IMAGE_SUFFIX.getValue() != null) {
            args = addArg(args, "TARGET_NAME=" + VE_IMAGE_SUFFIX.getValue());
        }
        if (!checkEcho(args)) {
            exec(new File(VENative), args, System.out, System.err, System.in);
        }
    }

    private static String[] buildJavaArgs(Class<?> javaMainClass, String classpath, String[] vmArgs, String[] systemProperties, String[] args) {
        final LinkedList<String> cmd = new LinkedList<String>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-d64");
        cmd.add("-ea");
        cmd.add("-cp");
        cmd.add(classpath == null ? System.getProperty("java.class.path") : classpath);
        for (int i = 0; i < systemProperties.length; i++) {
            cmd.add("-D" + systemProperties[i]);
        }

        for (int i = 0; i < vmArgs.length; i++) {
            cmd.add(vmArgs[i]);
        }

        cmd.add(javaMainClass.getName());
        for (String arg : args) {
            cmd.add(arg);
        }
        return cmd.toArray(new String[0]);
    }

    private static void exec(File workingDir, String[] command, OutputStream out, OutputStream err, InputStream in) throws IOException, InterruptedException {
        Trace.line(1, "Executing process in directory: " + workingDir);
        for (String c : command) {
            Trace.line(1, "  " + c);
        }
        final Process process = Runtime.getRuntime().exec(command, null, workingDir);
        try {
            final Redirector stderr = Streams.redirect(process, process.getErrorStream(), err, command + " [stderr]");
            final Redirector stdout = Streams.redirect(process, process.getInputStream(), out, command + " [stdout]");
            final Redirector stdin = Streams.redirect(process, in, process.getOutputStream(), command + " [stdin]");
            final int exitValue = process.waitFor();
            stderr.close();
            stdout.close();
            stdin.close();
            if (exitValue != 0) {
                ProgramError.unexpected("execution of command: " + Arrays.toString(command) + " failed [exit code = " + exitValue + "]");
            }
        } finally {
            process.destroy();
        }
    }


}
