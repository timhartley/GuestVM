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
package com.sun.max.ve.process;

import java.util.*;

public class HadoopProcessFilters extends VEProcessFilter {

    private BashProcessFilter _bashFilter = new BashProcessFilter();
    private ChmodProcessFilter _chmodFilter = new ChmodProcessFilter();
    private WhoamiProcessFilter _whoamiFilter = new WhoamiProcessFilter();
    private Map<String, VEProcessFilter> _map = new HashMap<String, VEProcessFilter>(5);

    public HadoopProcessFilters() {
        _map.put(_bashFilter.names()[0], _bashFilter);
        _map.put(_chmodFilter.names()[0], _chmodFilter);
        _map.put(_whoamiFilter.names()[0], _whoamiFilter);
    }

    @Override
    public int exec(byte[] prog, byte[] argBlock, int argc, byte[] envBlock, int envc, byte[] dir) {
        return _map.get(VEProcessFilter.stripNull(prog)).exec(prog, argBlock, argc, envBlock, envc, dir);
    }

    @Override
    public String[] names() {
        return _map.keySet().toArray(new String[_map.size()]);
    }

}
