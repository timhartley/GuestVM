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
package com.sun.max.tele.debug.maxve.xen.dump;

import com.sun.max.tele.debug.maxve.xen.dump.NotesSection.DescriptorType;

/**
 * The Xen Version descriptor from the notes section of the ELF dump
 *
 * @author Puneeet Lakhina
 *
 */
public class XenVersionDescriptor extends NotesSectionDescriptor {

    public XenVersionDescriptor() {
        super(DescriptorType.XEN_VERSION);
    }

    public static final int EXTRA_VERSION_LENGTH = 16;
    public static final int CAPABILITIES_LENGTH = 1024;
    public static final int CHANGESET_LENGTH = 64;
    private long majorVersion;
    private long minorVersion;
    private String extraVersion;
    private CompileInfo compileInfo;
    private String capabilities;
    private String changeSet;
    private long platformParamters;
    private long pageSize;

    static class CompileInfo {
        public static final int COMPILE_INFO_COMPILER_LENGTH = 64;
        public static final int COMPILE_INFO_COMPILE_BY_LENGTH = 16;
        public static final int COMPILE_INFO_COMPILER_DOMAIN_LENGTH = 32;
        public static final int COMPILE_INFO_COMPILE_DATE_LENGTH = 32;
        private String compiler;
        private String compiledBy;
        private String compileDomain;
        private String compileDate;

    }



    public void setCompileInfo(String compiler,String compiledby,String compiledomain,String compileDate) {
        compileInfo = new CompileInfo();
        compileInfo.compileDate = compileDate;
        compileInfo.compiler=compiler;
        compileInfo.compileDomain=compiledomain;
        compileInfo.compiledBy=compiledby;
    }




    /**
     * @return the majorVersion
     */
    public long getMajorVersion() {
        return majorVersion;
    }




    /**
     * @param majorVersion the majorVersion to set
     */
    public void setMajorVersion(long majorVersion) {
        this.majorVersion = majorVersion;
    }




    /**
     * @return the minorVersion
     */
    public long getMinorVersion() {
        return minorVersion;
    }




    /**
     * @param minorVersion the minorVersion to set
     */
    public void setMinorVersion(long minorVersion) {
        this.minorVersion = minorVersion;
    }




    /**
     * @return the extraVersion
     */
    public String getExtraVersion() {
        return extraVersion;
    }




    /**
     * @param extraVersion the extraVersion to set
     */
    public void setExtraVersion(String extraVersion) {
        this.extraVersion = extraVersion;
    }




    /**
     * @return the capabilities
     */
    public String getCapabilities() {
        return capabilities;
    }




    /**
     * @param capabilities the capabilities to set
     */
    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }




    /**
     * @return the changeSet
     */
    public String getChangeSet() {
        return changeSet;
    }




    /**
     * @param changeSet the changeSet to set
     */
    public void setChangeSet(String changeSet) {
        this.changeSet = changeSet;
    }




    /**
     * @return the platformParamters
     */
    public long getPlatformParamters() {
        return platformParamters;
    }




    /**
     * @param platformParamters the platformParamters to set
     */
    public void setPlatformParamters(long platformParamters) {
        this.platformParamters = platformParamters;
    }




    /**
     * @return the pageSize
     */
    public long getPageSize() {
        return pageSize;
    }




    /**
     * @param pageSize the pageSize to set
     */
    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }




    /**
     * @return the compileInfo
     */
    public CompileInfo getCompileInfo() {
        return compileInfo;
    }


}
