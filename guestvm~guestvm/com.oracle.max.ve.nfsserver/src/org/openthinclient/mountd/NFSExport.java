/*******************************************************************************
 * openthinclient.org ThinClient suite
 *
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
/*
 * This code is based on: JNFSD - Free NFSD. Mark Mitchell 2001
 * markmitche11@aol.com http://hometown.aol.com/markmitche11
 */
package org.openthinclient.mountd;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author levigo
 */
public class NFSExport implements Serializable {

    private static final long serialVersionUID = 3257846571638207028L;

    private int cacheTimeout = 15000;
    private List<Group> groups;
    private final String name;

    private boolean revoked;

    private final File root;

    /**
     * Create an export from an exports-style export spec {@see man exports}.
     * There are two differences, however: the local root directory does not
     * necessarily have to be identical to the name under which it is visible.
     * Therefore a new first field is introduced: the local path name.
     * Furthermore the pipe character "|" instead of whitespace is used as
     * delimiter.
     * <p>
     * The full format is thus: <br>
     * <code>local-path-name|name-of-share|host[/network][(options)][|host[/network][(options)]]</code>
     * <p>
     * The following options is recognized (all other options are ignored):
     * <dl>
     * <dt>ro
     * <dd>NFSExport share read-only
     * <dt>rw
     * <dd>NFSExport stare read-write (the default)
     * </dl>
     *
     * @param spec
     * @throws UnknownHostException
     */
    public NFSExport(String spec) throws UnknownHostException {
        final String parts[] = spec.split("\\|");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Can't parse export spec: "
                                               + spec);
        }

        this.name = parts[1];
        this.root = new File(parts[0]);

        // parse hosts
        final Pattern p = Pattern.compile("([^\\s(]+)\\(([^\\s]+)\\)");

        groups = new ArrayList<Group>();
        for (int i = 2; i < parts.length; i++) {
            final Matcher m = p.matcher(parts[i]);
            if (!m.matches()) {
                throw new IllegalArgumentException("Can't parse export spec: "
                                                   + spec);
            }

            Group g = null;
            if (null != m.group(1) && m.group(1).length() > 0
                && m.group(1).equals("*")) {
                g = new Group();
            } else if (null != m.group(1) && m.group(1).length() > 0
                       && !m.group(1).equals("*")) {
                final String[] addrAndMask = m.group(1).split("/");
                switch (addrAndMask.length) {
                    case 2:
                        g = new Group(InetAddress.getByName(addrAndMask[0]),
                                      Integer.parseInt(addrAndMask[1]));
                        break;

                    case 1:
                        g = new Group(InetAddress.getByName(addrAndMask[0]), 0);
                        break;

                    default:
                        break;
                }
            }

            if (g != null && null != m.group(2) && m.group(2).length() > 0) {
                final String opts = m.group(2).toLowerCase();
                if (opts.indexOf("ro") >= 0) {
                    g.setReadOnly(true);
                }
            }
            if (g != null) {
                groups.add(g);
            }
        }
    }

    public NFSExport(String name, File root) {
        this.name = name;
        this.root = root;
    }

    public NFSExport(String name, File root, List<Group> groups) {
        this.name = name;
        this.root = root;
        this.groups = groups;
    }

    public int getCacheTimeout() {
        return cacheTimeout;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public String getName() {
        return name;
    }

    public File getRoot() {
        return root;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setCacheTimeout(int cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(root.getAbsolutePath());
        sb.append("|").append(name);
        if (null != groups) {
            for (final Group group : groups) {
                sb.append("|").append(group);
            }
        } else {
            sb.append("|*(rw)");
        }

        return sb.toString();
    }
}
