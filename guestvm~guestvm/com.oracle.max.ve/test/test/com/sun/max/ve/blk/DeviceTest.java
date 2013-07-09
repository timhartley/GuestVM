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
package test.com.sun.max.ve.blk;

import java.nio.ByteBuffer;
import java.util.*;
import com.sun.max.ve.blk.device.*;
import com.sun.max.ve.blk.guk.*;

public class DeviceTest {

    static boolean _verbose;
    static final int SEED = 24793;
    static int _runTime = 10;
    static boolean _done;
    static boolean _native;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        final String[] ops = new String[10];
        final String[] devices = new String[10];
        final String[] sectors = new String[10];
        int opCount = 0;

        devices[0] = "0";
        sectors[0] = "0";

        Filler filler = new IXFiller();

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("op")) {
                ops[opCount++] = args[++i];
                sectors[opCount] = sectors[opCount - 1];
                devices[opCount] = devices[opCount - 1];
            } else if (arg.equals("s")) {
                sectors[opCount] = args[++i];
            } else if (arg.equals("d")) {
                devices[opCount] = args[++i];
            } else if (arg.equals("v")) {
                _verbose = true;
            } else if (arg.equals("f")) {
                final String fillerName = args[++i];
                filler = (Filler) (Class.forName("test.com.sun.max.ve.blk.DeviceTest$" + fillerName + "Filler").newInstance());
            } else if (arg.equals("t")) {
                _runTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("n")) {
                _native = true;
            }
        }
        // Checkstyle: resume modified control variable check

        final int n = GUKBlkDevice.getDevices();
        final BlkDevice[] blkDevices = new BlkDevice[n];
        System.out.println("Devices: " + n);
        final int[] sectorCount = new int[n];
        for (int i = 0; i < n; i++) {
            blkDevices[i] = GUKBlkDevice.create(i);
            sectorCount[i] = blkDevices[i].getSectors();
            System.out.println("  device " + i + " has " + sectorCount[i] + " sectors");
        }

        for (int j = 0; j < opCount; j++) {
            final String op = ops[j];
            final long address = Long.parseLong(sectors[j]);
            final int device = Integer.parseInt(devices[j]);
            if (op.equals("ra")) {
                readAll(blkDevices[device], sectorCount[0], filler);
            } else if (op.equals("wa")) {
                writeAll(blkDevices[device], sectorCount[0], filler);
            } else if (op.equals("r")) {
                read(blkDevices[device], address);
            } else if (op.equals("rr")) {
                readRandom(blkDevices[device], sectorCount[0], filler);
            }
        }
    }

    abstract static class Filler {
        abstract void fill(byte[] data, Object xtra);
    }

    static class SNFiller extends Filler {
        int _sn;
        @Override
        void fill(byte[] data, Object xtra) {
            final int sn = xtra == null ? _sn : (Integer) xtra;
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (sn & 0xFF);
            }
            _sn++;
        }
    }

    static class IXFiller extends Filler {
        @Override
        void fill(byte[] data, Object xtra) {
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i & 0xFF);
            }
        }
    }

    static class RNFiller extends Filler {
        Random _random;

        RNFiller() {
            _random = new Random(SEED);
        }

        RNFiller(Random random) {
            _random = random;
        }

        @Override
        void fill(byte[] data, Object xtra) {
            final Random random = xtra == null ? _random : (Random) xtra;
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) random.nextInt(256);
            }
        }
    }

    private static void writeAll(BlkDevice device, int sectors, Filler filler) {
        final int sectorSize = device.getSectorSize();
        final byte[] data = new byte[sectorSize];
        for (int i = 0; i < sectorSize; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        for (int i = 0; i < sectors; i++) {
            filler.fill(data, null);
            device.write(i * sectorSize, ByteBuffer.wrap(data));
            if (_verbose) {
                System.out.println("wrote sector " + i);
            }
        }
    }

    private static void readAll(BlkDevice device, int sectors, Filler filler) {
        final int sectorSize = device.getSectorSize();
        final byte[] data = new byte[sectorSize];
        final byte[] checkData = new byte[sectorSize];
        for (int i = 0; i < sectors; i++) {
            readSector(device, data, checkData, i, filler, null);
        }
    }

    private static void readSector(BlkDevice device, byte[] data, byte[] checkData, int sector, Filler filler, Object xtra) {
        device.read(sector * device.getSectorSize(), ByteBuffer.wrap(data));
        filler.fill(checkData, xtra);
        for (int j = 0; j < data.length; j++) {
            if (data[j] != checkData[j]) {
                System.out.println("data mismatch: sector " + sector + ", offset " + j + "read " + data[j] + " check " + checkData[j]);
            }
        }
        if (_verbose) {
            System.out.println("read sector " + sector);
        }
    }

    private static void readRandom(BlkDevice device, int sectors, Filler filler) {
        final int sectorSize = device.getSectorSize();
        final byte[] data = new byte[sectorSize];
        final byte[] checkData = new byte[sectorSize];
        final Random random = new Random();
        final Timer timer = new Timer(true);
        timer.schedule(new RunTimerTask(), _runTime * 1000);

        while (!_done) {
            final int sector = random.nextInt(sectors);
            readSector(device, data, checkData, sector, filler, sector);
        }
        System.out.println("Test terminated");
    }

    static class RunTimerTask extends TimerTask {
        @Override
        public void run() {
            _done = true;
        }
    }

    private static void read(BlkDevice device, long sector) {
        final int sectorSize = device.getSectorSize();
        ByteBuffer byteBuffer = allocateBuffer(sectorSize);
        device.read(sector * sectorSize, byteBuffer);
        System.out.println("Contents of sector " + sector);
        int c = 0;
        for (int j = 0; j < sectorSize; j++) {
            System.out.print(" 0x" + Integer.toHexString(byteBuffer.get(j) & 0xFF));
            if (c++ == 16) {
                System.out.println();
                c = 0;
            }
        }
        System.out.println();
    }
    
    private static ByteBuffer allocateBuffer(int size) {
        if (_native) {
            return ByteBuffer.allocateDirect(size);
        } else {
            return ByteBuffer.allocate(size);
        }
    }

}
