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
package test.java.net.cs;

/**
 * Test programs for communication using UDP or TCP.
 *
 * This is a client-server system where client threads
 * send data to server threads, with optional acknowledgement.
 * The number of threads is variable as is the protocol used, i.e., UDP or TCP.
 *
 * The ServerThread class denotes a server which will be paired with
 * a ClientThread. ServerThread.PORT defines the port for the first thread,
 * with subsequent threads using ServerThread.PORT +1, etc.
 *
 * The ServerMain class is the main program entry point for a server. It
 * accepts the following arguments (optionally preceded by a '-'):
 *
 * bs n        set data block size to n (default 1000)
 * onerun    terminate after receiving one data block
 * check     validate the sent  data (default no check)
 * sync        validate (check) the data immediately on receipt (default false)
 * noack      do not ack the data
 * th n          create n server threads (default 1)
 * buffers n   create n buffers for received data
 * verbose   verbose output on what is happening
 * type t       protocol (UDP or TCP, default UDP)
 *
 * A server thread, which is identified with the thread name Sn, where n is the server id (0, 1, ...)
 * buffers received data in a set of buffers (default set size 100) each of size given by the bs option.
 * If sync && check the received data is validated immediately on receipt, before any ACK
 * packet is sent back to the client.  If check && !sync the data is validated after the ACK is sent.
 * To simulate an application, a consumer thread is registered with the server thread. The consumer
 * repeatedly calls the getData method which copies the next unread buffer into the buffer provided
 * by the consumer. If no buffers are available the consumer blocks. If the consumer does not process
 * the data fast enough the server thread will block until a buffer is available.
 * If onerun is set the thread terminates when the client stops sending, otherwise it loops waiting for
 * a new connection. N.B. in UDP mode the client sends a zero length packet to indicate the end of the session.
 *
 * The Client Main class is the main program entry point for a client.
 * It accepts essentially the same arguments as ServerMain, specifically
 * bs, noack, th, verbose, type and adds:
 *
 * delay d    wait d milliseconds between data transfers (default 0)
 *
 * @author Mick Jordan
 *
 *
*/
