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

/**
 *
 */
package test.java.nio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


/**
 * @author Puneeet Lakhina
 *
 */
public class FileNIOTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if(args.length < 0) {
            fail("Usage test.java.nio.FileNIOTest scratchdirectory");
        }
        File scratchDir = new File(args[0]);
        if(!scratchDir.exists()) {
            if(!scratchDir.mkdirs()) {
                fail("Couldnt create scratch directory");
            }
        }else if(!scratchDir.isDirectory()) {
            fail(args[0] + " is not a directory");
        }

        //Test File Writing using File Channels
        File tempFile = new File(scratchDir,"tempfile");
        try {
            log("Testing Simple read/write");
            String content="some content";
            tempFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(tempFile);
            FileChannel fch = fos.getChannel();
            fch.write(ByteBuffer.wrap(content.getBytes()));
            fos.close();
            fch.close();
            log("Simple write success");
            FileInputStream fis = new FileInputStream(tempFile);
            fch=fis.getChannel();
            byte[] readArr =new byte[content.getBytes().length];
            fis.read(readArr);
            String readContent = new String(readArr);
            if(!content.equals(readContent)) {
                fail("Writtent and read values not same");
            }
            fis.close();
            fch.close();
            log("Simple read success");
            log("FileNIOTest Simple read/write test passed");

            log("Testing Offseted read/write");
            fos = new FileOutputStream(tempFile);
            fch=fos.getChannel();
            fch.position(content.getBytes().length);
            fch.write(ByteBuffer.wrap(content.getBytes()));
            fch.close();
            fos.close();
            log("Offseted write success");
            fis = new FileInputStream(tempFile);
            fch=fis.getChannel();
            fch.position(content.getBytes().length);
            readArr =new byte[content.getBytes().length];
            fis.read(readArr);
            readContent = new String(readArr);
            if(!content.equals(readContent)) {
                fail("Writtent and read values not same");
            }
            log("Offseted read success");
            log("FileNIOTest Offseted read/write test passed");

        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail(ioe.getMessage());
        }


    }
    private static void fail(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    private static void log(String msg) {
        System.out.println("FileNIOTest: "+msg);
    }
}
