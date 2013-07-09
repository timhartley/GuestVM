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
package com.sun.max.ve.net.guk;

import com.sun.max.annotate.*;
import com.sun.max.ve.guk.*;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.device.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.reference.Reference;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * This class provides a singleton object that manages the interaction between the
 * low level GUK network driver and the JDK. It manages a ring of packet buffers
 * that are filled by the @see copyPacket upcall from uKernel. This call happens
 * in interrupt handler mode and so must return promptly without rescheduling.
 *
 * In interrupt handler mode, we are not in a proper Java thread context, in particular
 * code with safepoint instructions must not be executed, since that can cause
 * a block if a safepoint has been triggered.  So copyPacket is marked
 * as an interrupt handler and compiled specially. Further, since GC may be
 * ongoing when the interrupt happens the code must not touch any references
 * that might be moved by the GC, which means that the ring buffer and the
 * byte arrays must be allocated in the boot heap at image build time. Therefore,
 * the property that controls the ring buffer size is interpreted at image build time
 * and not run time. The default is defined by @see DEFAULT_RING_SIZE.
 * This value controls the maximum number of packets that can be processed
 * concurrently. As an additional control, it is possible to dial down the
 * effective concurrency using the "max.ve.net.device.mt" property. If this
 * property is set at runtime, its value limits the number of concurrently active
 * handlers. This is mostly a debugging aid; in particular if set to one, the
 * network stack will be single-threaded for input packets.
 *
 * The ring buffer is an array of PacketHandler objects, each of which contains
 * a Packet object, allocated at image build time, a thread and associated
 * completion object allocated at runtime device initialisation, and a volatile
 * boolean flag indicating whether the packet is currently being handled by the
 * thread. When a new packet comes in, it is assigned to the first free handler
 * or dropped if there are none. The thread that was interrupted by the
 * incoming packet will be marked as needing to be rescheduled, which
 * will be checked in the ukernel on return from the interrupt handler (copyPacket).
 * This will causes network thread to run in preference to compute-bound threads
 * (modulo other policies imposed by the scheduler).
 *
 * @author Mick Jordan
 *
 */
public final class GUKNetDevice implements NetDevice {

    private static final int MAC_ADDRESS_LENGTH = 6;
    private static final int MTU = 1514;
    private static final int DEFAULT_RING_SIZE = 4;
    private static final String RING_SIZE_PROPERTY = "max.ve.net.device.ringsize";
    private static final String DEBUG_PROPERTY = "max.ve.net.device.debug";
    private static final String MT_PROPERTY = "max.ve.net.device.mt";
    private static int _ringSize = DEFAULT_RING_SIZE;
    private static boolean _debug = false;
    private static GUKNetDevice _device;

    private static PacketHandler [] _ring;
    private static Handler _handler;
    private static long _dropCount;
    private static long _pktCount;
    private static long _truncateCount;
    private static Pointer _transmitBuffer;
    private static boolean _deviceActive;
    // these fields allow the actual handler concurrency to be controlled at runtime
    private static int _maxActiveHandlers;
    private static int _activeHandlers;

    static {
        final String ringSizeProperty = System.getProperty(RING_SIZE_PROPERTY);
        if (ringSizeProperty != null) {
            _ringSize = Integer.parseInt(ringSizeProperty);
        }
        _ring = new PacketHandler[_ringSize];
        for (int i = 0; i < _ringSize; i++) {
            final PacketHandler p = new PacketHandler(Packet.get(MTU));
            _ring[i] = p;
        }
    }

    private static class PacketHandler {
        static final int IN_USE = 1;
        static final int FREE = 0;
        static final int statusOffset = ClassActor.fromJava(PacketHandler.class).findFieldActor(SymbolTable.makeSymbol("_status"), null).offset();
        Pointer _self;
        Packet _packet;
        Thread _handlerThread;
        Pointer _completion;
        volatile int _status = IN_USE;  // prevents use until handler thread really started
        PacketHandler(Packet packet) {
            _packet = packet;
        }
    }

    private GUKNetDevice() {
        _debug = System.getProperty(DEBUG_PROPERTY) != null;
        final String mtProperty = System.getProperty(MT_PROPERTY);
        if (mtProperty != null) {
            _maxActiveHandlers = Integer.parseInt(mtProperty);
        }
        _transmitBuffer = Memory.allocate(Size.fromInt(MTU));

        for (int i = 0; i < _ringSize; i++) {
            final PacketHandler packetHandler = _ring[i];
            _ring[i]._self = Reference.fromJava(packetHandler).toOrigin();
            final DeviceHandler deviceHandler = new DeviceHandler(packetHandler);
            final Thread deviceThread = new Thread(deviceHandler, "NetPacketHandler-" + i);
            deviceThread.setDaemon(true);
            deviceThread.start();
            packetHandler._completion = GUKScheduler.createCompletion();
            packetHandler._handlerThread = deviceThread;
        }
        // Have to pass the address of copyPacket down to the kernel
        final ClassActor classActor = ClassActor.fromJava(getClass());
        final Word copyMethodAddress = CompilationScheme.Static.getCurrentTargetMethod(classActor.findLocalStaticMethodActor(SymbolTable.makeSymbol("copyPacket"))).getEntryPoint(CallEntryPoint.C_ENTRY_POINT);
        _deviceActive = maxve_netStart(copyMethodAddress);
        _device = this;
    }

    public static GUKNetDevice create() {
        if (_device == null) {
            new GUKNetDevice();
        }
        return _device;
    }

    public boolean active() {
        return _deviceActive;
    }

    public byte[] getMACAddress() {
        final Pointer nativeBytes = maxve_getMacAddress();
        if (nativeBytes.isZero()) {
            return null;
        }
        final byte[] result = new byte[MAC_ADDRESS_LENGTH];
        Memory.readBytes(nativeBytes, MAC_ADDRESS_LENGTH, result);
        return result;
    }

    public String getNICName() {
        final Pointer nativeBytes = maxve_getNicName();
        if (nativeBytes.isZero()) {
            return null;
        }
        try {
            return CString.utf8ToJava(nativeBytes);
        } catch (Utf8Exception ex) {
            return null;
        }
    }

    public int getMTU() {
        return MTU;
    }

    public void setReceiveMode(int mode) {
        ProgramError.unexpected("not implemented");
    }

    public synchronized void transmit(Packet pkt) {
        if (!_deviceActive) {
            if (_debug) {
                dprintln("device not active");
            }
            return;
        }
        int length = pkt.inlineLength();
        if (_debug) {
            dprintln("transmit " + length);
        }
        if (length > MTU) {
            length = MTU;
        }
        for (int i = 0; i < length; i++) {
            _transmitBuffer.writeByte(i, pkt.inlineGetByteIgnoringHeaderOffset(i));
        }
        GUK.guk_netfront_xmit(_transmitBuffer, length);
    }

    public void transmit1(Packet buf, int offset, int size) {
        ProgramError.unexpected("not implemented");
    }

    public void registerHandler(Handler handler) {
        _handler = handler;
    }

    public long dropCount() {
        return _dropCount;
    }

    public long pktCount() {
        return _pktCount;
    }

    public long truncateCount() {
        return _truncateCount;
    }

    static class DeviceHandler implements Runnable {
        private PacketHandler _packetHandler;
        DeviceHandler(PacketHandler packetHandler) {
            _packetHandler = packetHandler;
        }

        public void run() {
            _packetHandler._status = PacketHandler.FREE;
            while (true) {
                GUKScheduler.waitCompletion(_packetHandler._completion);
                if (_handler != null) {
                    if (_maxActiveHandlers > 0) {
                        handlerGateEntry();
                    }
                    _handler.handle(_packetHandler._packet);
                    if (_maxActiveHandlers > 0) {
                        handlerGateExit();
                    }
                }
                _packetHandler._status = PacketHandler.FREE;
            }
        }
    }

    /**
     * This is upcalled from the network handler. It must not block nor may it call
     * any methods that are compiled with safepoint code installed, because it
     * is in IRQ mode and may be running on a microkernel thread.
     * The safepoint register will be valid however, else we couldn't even call
     * {@link C_FUNCTION} annotated code.
     *
     * @param p address of the network packet
     * @param pktLength length of packet
     * @param ts time of this upcall
     */
    @VM_ENTRY_POINT
    @NO_SAFEPOINT_POLLS("network packet copy must be atomic")
    private static void copyPacket(Pointer p, int pktLength, long ts) {
        int length = pktLength;
        PacketHandler packetHandler = null;
        // try to find a free handler
        for (int i = 0; i < _ringSize; i++) {
            if (_ring[i]._self.compareAndSwapInt(PacketHandler.statusOffset, PacketHandler.FREE, PacketHandler.IN_USE) == PacketHandler.FREE) {
                packetHandler = _ring[i];
                break;
            }
        }
        // All Packet calls are inlined
        if (packetHandler != null) {
            final Packet pkt = packetHandler._packet;
            pkt.inlineSetTimeStamp(ts);
            pkt.inlineReset();
            if (length > pkt.inlineLength()) {
                length = pkt.inlineLength();
                _truncateCount++;
            }
            for (int i = 0; i < length; i++) {
                pkt.inlinePutByteIgnoringHdrOffset(p.readByte(i), i);
            }
            pkt.inlineSetLength(length);

            _pktCount++;
            GUKScheduler.complete(packetHandler._completion);
        } else {
            _dropCount++;
            // full, drop packet
        }
    }

    private static synchronized void handlerGateEntry() {
        while (_activeHandlers > _maxActiveHandlers) {
            try {
                GUKNetDevice.class.wait();
            } catch (InterruptedException ex) {
            }
        }
        _activeHandlers++;
    }

    private static synchronized void handlerGateExit() {
        _activeHandlers--;
        GUKNetDevice.class.notify();
    }

    private void dprintln(String m) {
        Debug.println("GUKNetDevice [" + Thread.currentThread().getName() + "] " + m);
    }

    @C_FUNCTION
    private static native boolean maxve_netStart(Word address);
    @C_FUNCTION
    private static native Pointer maxve_getMacAddress();
    @C_FUNCTION
    private static native Pointer maxve_getNicName();

}
