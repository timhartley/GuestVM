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
package com.sun.max.ve.net.tcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Puneeet Lakhina
 *
 */
public class CachingMap<K, V> extends ConcurrentHashMap<K, V> {

    /**
     *
     */
    private static final long serialVersionUID = -51271534294813025L;
    private int _cacheSize;
    private List<Entry<K, V>> _cache;

    public CachingMap(int cacheSize) {
        this._cacheSize = cacheSize;
        this._cache = new ArrayList<Entry<K, V>>();
    }

    static class Entry<K, V> implements Map.Entry<K, V> {

        private K _key;
        private V _value;

        public Entry(K key, V value) {
            this._key = key;
            this._value = value;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Map.Entry#getKey()
         */
        @Override
        public K getKey() {
            return _key;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Map.Entry#getValue()
         */
        @Override
        public V getValue() {
            return _value;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */
        @Override
        public V setValue(V value) {
            this._value = value;
            return this._value;
        }

    }



    /*
     * (non-Javadoc)
     *
     * @see java.util.HashMap#get(java.lang.Object)
     */
    @Override
    public V get(Object key) {
        for (Map.Entry<K, V> entry : _cache) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        final V val = super.get(key);
        addToCache((K) key, val);
        return val;
    }

    private void addToCache(K key, V value) {
        if(value == null) {
            return;
        }
        final Entry<K, V> entry = new Entry<K, V>(key, value);
        if (_cache.size() == _cacheSize) {
            // evict first element
            _cache.set(0, entry);
        } else {
            _cache.add(entry);
        }
    }


}
