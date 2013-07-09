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
package test.java.lang;

import java.io.*;
import java.util.*;


public class ArgVarsTest {

    private static boolean _verbose = false;
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        int threshold = 100;
        String[] userArgs = null;
        boolean exec = false;
        boolean compress = false;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("t")) {
                threshold = Integer.parseInt(args[++i]);
            } else if (arg.equals("v")) {
                _verbose = true;
            } else if (arg.equals("c")) {
                compress = true;
            } else if (arg.equals("e")) {
                exec = true;
            } else if (arg.equals("f")) {
                userArgs = readFromFile(args[++i]);
            } else {
                final int userArgsLength = args.length - i;
                userArgs = new String[userArgsLength];
                for (int j = 0; j < userArgsLength; j++) {
                    userArgs[j] = args[i++];
                }
                break;
            }
        }
        String[] result = compress ? compressArgs(userArgs, threshold) : userArgs;
        if (result == null) {
            System.out.println("cannot compress");
        } else {
            for (String a : result) {
                System.out.println(a);
            }
            if (exec) {
                final String[] execArgs = new String[userArgs.length + 1];
                execArgs[0] = System.getProperty("java.home") + "/bin/java";
                System.arraycopy(userArgs, 0, execArgs, 1, userArgs.length);
                System.out.println("length of args: " + argsLength(userArgs));
                Process p = null;
                BufferedReader stdOut = null;
                BufferedReader stdErr = null;
                try {
                    p = Runtime.getRuntime().exec(execArgs);
                    p.waitFor();
                    stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    readFully(stdOut, true);
                    stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    readFully(stdErr, false);
                    System.out.println("waitFor returned " + p.waitFor());
                } catch (IOException ex) {
                    System.err.println(ex);
                } catch (InterruptedException ex) {
                    System.err.println(ex);
                } finally {
                    if (p != null) {
                        try {
                            stdOut.close();
                            stdErr.close();
                            p.destroy();
                        } catch (IOException ex) {

                        }
                    }
                }
            }
        }
    }

    private static int argsLength(String[] args) {
        int result = 0;
        for (String arg : args) {
            result += arg.length();
        }
        return result;
    }

    private static void readFully(BufferedReader in, boolean isStdOut) throws IOException {
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);
        }
    }


    private static String[] compressArgs(String[] xargs, int maxLength) {
        // We are not trying to be general here, just deal with the expected situation,
        // which is many long filenames with repeated prefixes.
        String[] args = xargs;
        int varNum = 0;
        int currentLength = argsLength(args);
        while (currentLength > maxLength) {
            int maxReduction = 0; // max reduction in this phase
            int maxReducerUpb = 0;
            Locator maxReducer = null;
            final List<Locator> candidates = new ArrayList<Locator>();
            // Find all occurrences of pathnames in args
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg != null) {
                    locateCandidates(candidates, args, i);
                }
            }
            /*
             * Examine each pathname and for each prefix, starting with the longest, find how many matches exist among
             * the complete list of pathnames. If during this process we achieve the required reduction we declare
             * victory. Otherwise, we choose the largest reduction and repeat.
             */
            for (Locator s : candidates) {
                if (s._parts.length > 2) {
                    int upb = s._parts.length - 2;
                    while (upb > 0) {
                        int pl = 0;
                        for (int i = 0; i <= upb; i++) {
                            pl += s._parts[i].length() + 1;
                        }
                        if (pl > 5) {
                            if (_verbose) {
                                System.out.println("checking " + concatParts(s, 0, upb, false) + ", reduction " + (pl - 5));
                            }
                            for (Locator c : candidates) {
                                if (s != c) {
                                    if (upb < c._parts.length && match(s, c, upb)) {
                                        s._matches[upb]++;
                                    }
                                }
                            }
                            if (_verbose) {
                                System.out.println("matches " + s._matches[upb]);
                            }
                            final int thisReduction = (pl - 5) * s._matches[upb];
                            // check if this will do
                            if (thisReduction >= currentLength - maxLength) {
                                args = doReduction(candidates, s, upb, args, varNum);
                                return args;
                            }
                            // check if this a max for this phase
                            if (thisReduction > maxReduction) {
                                maxReduction = thisReduction;
                                maxReducer = s;
                                maxReducerUpb = upb;
                            }
                        }
                        upb--;
                    }
                }
            }
            // We did not achieve the required reduction, so, unless we made no improvement,
            // apply the max and repeat the search
            if (maxReduction == 0) {
                // failure
                return null;
            }
            args = doReduction(candidates, maxReducer, maxReducerUpb, args, varNum);
            currentLength = argsLength(args);
            varNum++;
        }
        return args;
    }

    private static String[] doReduction(List<Locator> candidates, Locator s, int upb, String[] args, int varNum) {
        final String[] newArgs = new String[args.length + 1];
        newArgs[0] = "-XX:GVMArgVar:v" + varNum + "=" + concatParts(s, 0, upb, false);
        int nx = 1;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            String newArg = arg;
            if (arg != null) {
                int cx = findFirstLocatorForArgIndex(candidates, i);
                if (cx >= 0) {
                    newArg = ""; // reset
                    int ax = 0;
                    // This arg may contain several pathnames, zero or more that match our variable choice
                    while (true) {
                        final Locator c = candidates.get(cx);
                        // append text up to start of pathname
                        newArg += args[i].substring(ax, c._sx);
                        if (upb < c._parts.length && match(s, c, upb)) {
                            // append variable occurrence plus parts of pathname not matched
                            newArg += "${v" + varNum + "}" + concatParts(c, upb + 1, c._parts.length - 1, true);
                        } else {
                            // this pathname did not match our variable, so append it unmodified
                            newArg += concatParts(c, 0, c._parts.length - 1, false);
                        }
                        ax = c._ex;
                        cx++;
                        if (cx == candidates.size() || candidates.get(cx)._argIndex != i) {
                            // append text after final pathname
                            newArg += arg.substring(c._ex);
                            break;
                        }
                    }

                }
            }
            newArgs[nx++] = newArg;
        }
        return newArgs;
    }


    private static int findFirstLocatorForArgIndex(List<Locator> list, int argIndex) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)._argIndex == argIndex) {
                return i;
            }
        }
        return -1;
    }

    private static void locateCandidates(List<Locator> candidates, String[] args, int argIndex) {
        final String arg = args[argIndex];
        final int length = arg.length();
        int sx;
        int ex;
        int i = 0;
        while (i < length) {
            if (arg.charAt(i) == File.separatorChar) {
                int j = i;
                sx = i;
                ex = 0;
                while (j < length) {
                    final char ch = arg.charAt(j);
                    if (ch == ':' || ch == ';') {
                        ex = j;
                        break;
                    }
                    j++;
                }
                if (ex == 0) {
                    ex = length;
                }
                candidates.add(new Locator(Kind.PATHNAME, argIndex, arg, sx, ex));
                i = j;
            } else if ((arg.charAt(i) == '-') && (arg.charAt(i + 1) == 'D')) {
                ex = arg.indexOf('=', i);
                final int tx = ex < 0 ? length : ex;
                candidates.add(new Locator(Kind.PROPERTY, argIndex, arg, 2, tx));
                i = tx;
            }
            i++;
        }
    }

    private static boolean match(Locator la, Locator lb, int upb) {
        if (la._kind == lb._kind) {
            String[] a = la._parts;
            String[] b = lb._parts;
            for (int x = 0; x <= upb; x++) {
                if (!a[x].equals(b[x])) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static String concatParts(Locator locator, int lwb, int upb, boolean leadingSep) {
        if (lwb > upb) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        if (leadingSep && locator._kind == Kind.PATHNAME) {
            sb.append(File.separatorChar);
        }
        for (int i = lwb; i <= upb; i++) {
            sb.append(locator._parts[i]);
            if (i != upb) {
                sb.append(locator._kind == Kind.PATHNAME ? File.separatorChar : ".");
            }
        }
        return sb.toString();
    }

    static enum Kind {
        PATHNAME,
        PROPERTY
    };

    static class Locator {
        Kind _kind;
        int _argIndex;
        int _sx;
        int _ex;
        String[] _parts;
        int[] _matches;
        Locator(Kind kind, int argIndex, String arg, int sx, int ex) {
            _kind = kind;
            _argIndex = argIndex;
            _sx = sx;
            _ex = ex;
            _parts = arg.substring(sx, ex).split(File.separator);
            _matches = new int[_parts.length];
            for (int i = 0; i < _matches.length; i++) {
                _matches[i] = 0;
            }
        }
    }

    private static String[] readFromFile(String name) throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(name));
        final List<String> lines = new ArrayList<String>();
        try {
            while (true) {
                final String line = r.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                lines.add(line);
            }
        } finally {
            if (r != null) {
                r.close();
            }
        }
        return lines.toArray(new String[lines.size()]);
    }
}
