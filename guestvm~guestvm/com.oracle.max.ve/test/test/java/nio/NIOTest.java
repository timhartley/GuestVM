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
package test.java.nio;

import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import  java.util.*;


public class NIOTest {
    public static void main(String[] args)  throws Exception {
        final String[] ops = new String[10];
        final String[] values = new String[10];
        int opCount = 0;
        values[0] = "0";
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("op")) {
                ops[opCount++] = args[++i];
                values[opCount] = values[opCount-1];
            } else if (arg.equals("v")) {
                values[opCount] = args[++i];
            }
        }
        // Checkstyle: resume modified control variable check

        for (int j = 0; j < opCount; j++) {
            final String op = ops[j];
            final String value = values[j];
            if (op.equals("allocate")) {
                final ByteBuffer b = ByteBuffer.allocate(Integer.parseInt(value));
                System.out.println(b);
            } else if (op.equals("allocateDirect")) {
                final ByteBuffer b = ByteBuffer.allocateDirect(Integer.parseInt(value));
                System.out.println(b);
            } else if (op.equals("provider")) {
                final SelectorProvider provider = SelectorProvider.provider();
                System.out.println("provider=" + provider);
            } else if (op.equals("pipe")) {
                doPipe(Long.parseLong(value));
            } else if (op.equals("sysOut")) {
                doSysOut();
            }
        }
    }

    private static void doSysOut() throws Exception {
        WritableByteChannel out = Channels.newChannel (System.out);
        ByteBuffer buffer = ByteBuffer.allocate (100);
        buffer.put("Hello World via Channel\n".getBytes());
        buffer.flip();
        out.write(buffer);
    }

    private static void doPipe(long delay)  throws Exception {
        // wrap a channel around stdout
        WritableByteChannel out = Channels.newChannel (System.out);
        // start worker and get read end of channel
        ReadableByteChannel workerChannel = startWorker (10, delay);
        ByteBuffer buffer = ByteBuffer.allocate (100);

        int result = 0;
        while ((result = workerChannel.read (buffer)) >= 0) {
           buffer.flip();
           out.write (buffer);
           buffer.clear();
        }
        finish(result);
    }

    private static void finish(int result) {

    }

    private static ReadableByteChannel startWorker (int reps, long delay)
    throws Exception
 {
    Pipe pipe = Pipe.open();
    Writer worker = new Writer (pipe.sink(), delay);

    worker.start();

    return (pipe.source());
 }
    static class Writer extends Thread {
        Pipe.SinkChannel _channel;
        long _delay;
        Writer(Pipe.SinkChannel channel, long delay) {
            _channel = channel;
            _delay = delay;
        }

        @Override
        public void run() {
            if (_delay > 0) {
                try {
                    Thread.sleep(_delay * 1000);
                } catch (InterruptedException ex) {

                }
            }

            ByteBuffer buffer = ByteBuffer.allocate (128);
            try {
                for (int i = 0; i < 10; i++) {
                doSomeWork(buffer);
                while (_channel.write(buffer) > 0) {

                }
                }
                _channel.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private String [] products = {
                        "No good deed goes unpunished",
                        "To be, or what?",
                        "No matter where you go, there you are",
                        "Just say \"Yo\"",
                        "My karma ran over my dogma"
                     };

                     private Random rand = new Random();

        private void doSomeWork(ByteBuffer buffer) throws Exception {
            int product = rand.nextInt(products.length);

            buffer.clear();
            buffer.put(products[product].getBytes("US-ASCII"));
            buffer.put("\r\n".getBytes("US-ASCII"));
            buffer.flip();
        }
    }
 }
