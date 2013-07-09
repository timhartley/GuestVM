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
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link Exporter} which maintains a list of exports.
 *
 * @author levigo
 */
public class ListExporter implements Exporter {
    private final List<NFSExport> exports;
    private final static Logger logger = Logger.getLogger(ListExporter.class.getCanonicalName());

    public ListExporter() {
        this(null);
    }

    public ListExporter(NFSExport exports[]) {
        this.exports = new ArrayList<NFSExport>();
        if (null != exports) {
            for (NFSExport export : exports) {
                this.exports.add(export);
            }
        }
    }

    public void addExport(NFSExport e) {
        for (final NFSExport existing : exports) {
            if (existing.getName().equals(e.getName())) {
                throw new IllegalArgumentException(
                                                   "There is already an export with this name");
            }
        }

        exports.add(e);
    }

    public NFSExport getExport(InetAddress peer, String mountRequest) {
        String mountRequestNormalized = "";
        String exportNameNormalized = "";
        try {
            mountRequestNormalized = new File(mountRequest).getCanonicalPath();
        } catch (final IOException e1) {
            logger.log(Level.SEVERE, "Unable to normalize mount request: "
                                     + mountRequest, e1);
        }

        logger.fine("mount request normalized: " + mountRequestNormalized);

        for (final NFSExport export : exports) {
            try {
                exportNameNormalized = new File(export.getName()).getCanonicalPath();
            } catch (final IOException e1) {
                logger.log(Level.SEVERE, "Unable to normalize export name: "
                                         + export.getName(), e1);
            }
            logger.fine("export name normalized: " + exportNameNormalized);

            if (exportNameNormalized.equals(mountRequestNormalized)) {
                return export;
            }

            if (mountRequestNormalized.startsWith(exportNameNormalized)) {
                final String subdir = mountRequestNormalized.substring(exportNameNormalized.length());
                final String rootsubdir = export.getRoot() + subdir;

                if (new File(rootsubdir).isDirectory()) {
                    final String groups = export.toString().split("\\|")[2];
                    try {
                        final NFSExport subexport = new NFSExport(
                                                                  rootsubdir
                                                                          + "|"
                                                                          + mountRequestNormalized
                                                                          + "|"
                                                                          + groups);
                        return subexport;
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, "Unable to export sub: "
                                                 + rootsubdir + "|"
                                                 + mountRequestNormalized + "|"
                                                 + groups, e);
                    }
                }
            }
        }

        logger.fine("Exporting nothing for mount request: " + mountRequest);
        return null;
    }

    public List<NFSExport> getExports() {
        return exports;
    }

    public boolean removeExport(String share) {
        for (final NFSExport existing : exports) {
            if (existing.getName().equals(share)) {
                existing.setRevoked(true);
                exports.remove(existing);
                return true;
            }
        }

        return false;
    }
}
