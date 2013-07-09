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
package com.sun.max.ve.net.dns;

/**
 * @author unknown, Mick Jordan (modifications)
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.max.ve.net.Packet;
import com.sun.max.ve.net.debug.Debug;
import com.sun.max.ve.net.ip.IPAddress;
import com.sun.max.ve.net.udp.UDP;
import com.sun.max.ve.net.udp.UDPUpcall;


public class DNS implements UDPUpcall {
    // Currently unused fields commented out

    // DNS Resource record type codes
    private static final int TYPE_A =  1;
    private static final int TYPE_PTR = 12;
    /*
    private static final int TYPE_NS =  2;
    private static final int TYPE_CNAME =  5;
    private static final int TYPE_SOA =  6;
    private static final int TYPE_HINFO = 13;
    private static final int TYPE_MX = 15;
    private static final int TYPE_TXT = 16;
    private static final int TYPE_AAAA = 28;
    private static final int TYPE_SRV = 33;
    private static final int TYPE_NAPTR = 35;
    */

    // DNS Resource record class codes
    private static final int CLASS_INTERNET = 1;

    // DNS packet header field offsets
    private static final int IDENT_OFFSET = 0;
    private static final int FLAGS_OFFSET = 2;
    private static final int NUMQ_OFFSET = 4;
    private static final int NUMANS_OFFSET = 6;
    private static final int NUMAUTH_OFFSET = 8;
    //private static final int NUMADD_OFFSET = 10;
    private static final int DNS_HDR_SIZE = 12;
    private static final int DNS_PORT = 53;
    private static final int INPUT_PORT = 11213;

    // DNS response codes
    /*
    private static final int NO_ERROR  = 0;
    private static final int FORMAT_ERROR    1;
    private static final int SERVER_FAILURE = 2;
    private static final int NAME_ERROR  = 3;
    private static final int NOT_IMPL  = 4;
    private static final int REFUSED  = 5;
    */

    private static final short QR_BIT = (short) 0x8000;
    //private static final short AA_BIT = (short) 0x0400;
    //private static final short TC_BIT = (short) 0x0200;
    private static final short RD_BIT = (short) 0x0100;
    //private static final short RA_BIT = (short) 0x0080;
    private static final byte COMPRESSED_NAME_FLAG = (byte) 0xC0;
    private static final short COMPRESSED_NAME_MASK = (byte) 0x3FF;

    private static final int  TIMEOUT = 2000;
    private static final int RETRY_COUNT = 3;

    private static final String IN_ADDR_ARPA = "in-addr.arpa";

    private static boolean _debug;

    private static final Map<String, IPAddress[]> _dnsTable = Collections.synchronizedMap(new HashMap<String, IPAddress[]>());
    private static final Map<IPAddress, String> _dnsReverseTable = Collections.synchronizedMap(new HashMap<IPAddress, String>());
    private static final Map<Integer, Response> _results = Collections.synchronizedMap(new HashMap<Integer, Response>());

    private IPAddress _serverAddress;
    private String _domainName;
    private boolean _initialized = false;
    private int _localPort;
    private int _destPort = DNS_PORT;
    private short  _nextIdent;
    private static DNS _singleton;

    static class Response {
        boolean _haveReply;
        // union!
        IPAddress[] _ipAddresses;  // TYPE_A
        String _hostname;         // TYPE_PTR
    }

    public DNS(IPAddress server, String domainName)  {
        _debug = System.getProperty("max.ve.net.dns.debug") != null;
        if (_debug) {
            dnsPrintln("server: " +  server);
        }
        _serverAddress = server;
        _domainName = domainName;
        _localPort = UDP.register(this, INPUT_PORT, false);
        _singleton = this;
        _dnsTable.put("localhost", new IPAddress[] {IPAddress.loopback()});
        _dnsReverseTable.put(IPAddress.loopback(), "localhost");
        _initialized = true;
    }

    public static DNS getDNS() {
        return _singleton;
    }

    /**
     * Find the host name for the given IP address.
     * @param ipAddressName an IP address
     * @return the host name or null if not found
     */

    public String reverseLookup(IPAddress ipAddress) {
        final String reverse = IPAddress.toReverseString(ipAddress.addressAsInt());
        return (String) lookup(reverse + "." + IN_ADDR_ARPA, true);
    }

    /**
     * Find the host name for the given IP address.
     *
     * @param ipAddressName
     *            an IP address
     * @return the host name or null if not found
     */

    public String reverseLookup(String ipAddressName) {
        String result = _dnsReverseTable.get(ipAddressName);
        if (result == null) {
            try {
                final IPAddress ipAddress = IPAddress.parse(ipAddressName);
                result = reverseLookup(ipAddress);
                _dnsReverseTable.put(ipAddress, result);
            } catch (NumberFormatException ex) {
            }
        }
        return result;
    }

    /**
     * Find an IP address for the given host name.
     *
     * @param hostname
     * @return
     */
    public IPAddress lookupOne(String hostname) {
        final IPAddress[] ipAddresses = lookup(hostname);
        if (ipAddresses == null) {
            return null;
        } else {
            return ipAddresses[0];
        }
    }

    /**
     * Find the IP address(es) for the given host name.
     * @param hostname
     * @return an array of IPAddress or null if not found
     */
    public IPAddress[] lookup(final String hostname) {
        IPAddress[] result = null;
        if (_initialized) {
            result = (IPAddress[]) _dnsTable.get(hostname);
            if (result != null) {
                if (_debug) {
                    dnsPrintln("lookup: cached " + hostname + " " + result);
                }
            } else {
                String xhostname = hostname;
                // check for partial name
                final int ix = xhostname.indexOf('.');
                if (ix  > 0) {
                    final String tail = xhostname.substring(ix + 1) + ".";
                    if (_domainName.startsWith(tail)) {
                        xhostname += _domainName.substring(tail.length() - 1);
                    }
                } else {
                    xhostname += "." + _domainName;
                }
                result = (IPAddress[]) lookup(xhostname, false);
                if (result != null) {
                    _dnsTable.put(xhostname, result);
                }
            }
        }
        return result;
    }

    private Object lookup(String hostname, boolean reverse) {
        Object result = null;
        int retries = 0;
        short ident;
        if (_debug) {
            dnsPrintln("lookup: query " + hostname);
        }
        do {
            final Packet mp = Packet.get(14 + 20 + 8, requiresSpace());

            synchronized (this) {
                ident = ++_nextIdent;
            }
            final Response response = new Response();
            _results.put((int) ident, response);

            mp.putShort((short) ident, IDENT_OFFSET);
            mp.putShort(RD_BIT, FLAGS_OFFSET);
            mp.putShort((short) 1, NUMQ_OFFSET);
            mp.putShort((short) 0, NUMANS_OFFSET);
            mp.putShort(0, NUMAUTH_OFFSET);

            final byte[] name = encodeName(hostname);
            mp.putBytes(name, 0, DNS_HDR_SIZE, name.length);
            mp.putShort((short) (reverse ? TYPE_PTR : TYPE_A), DNS_HDR_SIZE
                    + name.length);
            mp.putShort((short) CLASS_INTERNET, DNS_HDR_SIZE + name.length + 2);

            UDP.output(mp, _localPort, _serverAddress.addressAsInt(),
                    _destPort, DNS_HDR_SIZE + name.length + 4, 0);
            synchronized (response) {
                try {
                    response.wait(TIMEOUT);
                } catch (InterruptedException ex) {
                }
            }
            _results.remove(response);
            if (response._haveReply) {
                result = reverse ? response._hostname : response._ipAddresses;
                break;
            } else {
                if (_debug) {
                    dnsPrintln("lookup of " + hostname + " timed out");
                }
            }
        } while (++retries < RETRY_COUNT);
        return result;
    }

    @SuppressWarnings("unused")
    public void input(Packet pkt) {
        if (_debug) {
            dnsPrintln("input");
        }
        final int flags = pkt.getShort(FLAGS_OFFSET);
        if ((flags & QR_BIT) == 0) {
            return;
        }

        final int queries = pkt.getShort(NUMQ_OFFSET);
        final int answers = pkt.getShort(NUMANS_OFFSET);

        if (answers != 0) {
            final int ident = pkt.getShort(IDENT_OFFSET);
            final Response response = _results.get(ident);
            if (response != null) {
                int offset = DNS_HDR_SIZE;
                if (_debug) {
                    dnsPrintln("queries " + queries + ", answers " + answers);
                }
                final List<IPAddress> ipAddressList = new ArrayList<IPAddress>();
                for (int i = 0; i < queries + answers; i++) {
                    int nameOffset = offset;
                    int lenBytes = pkt.getByte(offset);
                    if ((lenBytes & COMPRESSED_NAME_FLAG) != 0) {
                        nameOffset = pkt.getShort(offset) & COMPRESSED_NAME_MASK;
                        offset += 2;
                    } else {
                        while ((lenBytes = pkt.getByte(offset)) != 0) {
                            offset += lenBytes + 1; // plus 1 for the actual length byte itself
                        }
                        offset++; /* skip over trailing zero */
                    }
                    final int type = pkt.getShort(offset);
                    offset += 2;
                    // int clazz = pkt.getShort(offset);
                    offset += 2;
                    if (i >= queries) {
                        // int ttl = pkt.getInt(offset);
                        offset += 4;
                        final int datalen = pkt.getShort(offset);
                        offset += 2;
                        if (_debug) {
                            dnsPrintln("type " + type);
                        }
                        if (type == TYPE_A || type == TYPE_PTR) {
                            response._haveReply = true;
                            if (type == TYPE_A) {
                                ipAddressList.add(new IPAddress(pkt.getInt(offset)));
                            } else {
                                response._hostname = decodeName(pkt, offset);
                            }
                        }
                        // Don't process other types, just skip over data
                        offset += datalen;
                    }
                }
                response._ipAddresses = ipAddressList.toArray(new IPAddress[ipAddressList.size()]);
                synchronized (response) {
                    response.notify();
                }
            }
        }
    }

    private String decodeName(Packet pkt, int off) {
        int lenBytes;
        int offset = off;
        boolean first = true;
        final StringBuilder result = new StringBuilder();
        while ((lenBytes = pkt.getByte(offset)) != 0) {
            if (!first) {
                result.append('.');
            } else {
                first = false;
            }
            offset++;
            for (int i = 0; i < lenBytes; i++) {
                result.append((char) pkt.getByte(offset + i));
            }
            offset += lenBytes;
        }
        return result.toString();
    }

    private void dnsPrintln(String message) {
        if (_debug) {
            Debug.println("DNS: " + message);
        }
    }

    public byte[] encodeName(String name) {
        final int octets = name.length();
        int dots = 0;
        for (int oct = 0; oct < octets; oct++) {
            if (name.charAt(oct) == '.') {
                dots++;
            }
        }
        final byte[] encoded = new byte[octets + 2 /* one more length byte than dots + ending '0' */];
        int namelen = 0;
        int lenptr = 0;
        // Checkstyle: stop multiple variable declaration check
        for (int enc = 1, oct = 0; oct < octets; oct++, enc++) {
            if (name.charAt(oct) != '.') {
                namelen++;
                encoded[enc] = (byte) name.charAt(oct);
            } else {
                encoded[lenptr] = (byte) namelen;
                namelen = 0;
                lenptr = enc;
            }
        }
        // Checkstyle: resume multiple variable declaration check
        encoded[lenptr] = (byte) namelen;
        encoded[encoded.length - 1] = 0;
        return encoded;
    }

    public int length() {
        return requiresSpace();
    }

    public static int requiresSpace() {
        return 512;
    }
}



