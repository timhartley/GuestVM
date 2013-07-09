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
package com.sun.max.ve.net.dhcp;

/**
 * DHCP protocol.
 *
 * @author unknown, Mick Jordan (modifications)
 */
import java.util.Random;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.ip.*;
import com.sun.max.ve.net.udp.*;

public final class DHCP implements UDPUpcall {

    private static boolean _debug;
    private static Random _random;

    public static final int REQUEST = 1;
    public static final int REPLY = 2;

    public static final int CLIENT_PORT = 68;
    public static final int SERVER_PORT = 67;

    public static final int OP_OFFSET = 0;
    public static final int HTYPE_OFFSET = 1;
    public static final int HLEN_OFFSET = 2;
    public static final int HOPS_OFFSET = 3;
    public static final int XID_OFFSET = 4;
    public static final int SECS_OFFSET = 8;
    public static final int CIADDR_OFFSET = 12;
    public static final int YIADDR_OFFSET = 16;
    public static final int SIADDR_OFFSET = 20;
    public static final int GIADDR_OFFSET = 24;
    public static final int HWADDR_OFFSET = 28;
    public static final int SNAME_OFFSET = 44;
    public static final int FILE_OFFSET = 108;
    public static final int VEND_OFFSET = 236;
    public static final int OPTIONS_OFFSET = 240;

    protected Endpoint _endpoint;
    protected int _localPort;
    protected IPAddress _dest = new IPAddress(255, 255, 255, 255);
    protected int _destPort = SERVER_PORT;
    protected byte[] _myHWaddr;
    protected int _xid;
    protected static final int STATE_NULL = 0;
    protected static final int STATE_DISCOVER = 1;
    protected static final int STATE_OFFERED = 2;
    protected static final int STATE_REQUEST = 3;
    protected static final int STATE_COMPLETE = 4;
    protected int _state = STATE_NULL;
    private static final int INITIAL_DELAY = 4; // seconds for retransmission
    private static final int MAX_DELAY = 64;   // seconds before giving up
    private static int _delay = INITIAL_DELAY;
    protected int _secs = -1;
    protected long _leaseTime;

    protected static final int SUBNET_MASK_OPTION = 1;
    protected static final int NAME_SERVER_OPTION = 6;
    protected static final int ROUTER_OPTION = 3;
    protected static final int DHCP_MESSAGE_TYPE_OPTION = 53;
    protected static final int SERVER_IDENTIFIER_OPTION = 54;
    protected static final int REQUESTED_IPADDRESS_OPTION = 50;
    protected static final int IPADDRESS_LEASE_TIME_OPTION = 51;
    protected static final int DOMAIN_NAME_OPTION = 15;
    protected static final int END_OPTION = 255;
    protected static final int PAD_OPTION = 0;

    protected static final byte[] COOKIE = {(byte) 99, (byte) 130, (byte) 83, (byte) 99};
    protected static final int DHCP_DISCOVER = 1;
    protected static final byte[] DHCP_DISCOVER_BYTES = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) DHCP_DISCOVER};
    protected static final int  DHCP_OFFER = 2;
    protected static final byte[] DHCP_OFFER_BYTES = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) DHCP_OFFER};
    protected static final int DHCP_REQUEST = 3;
    protected static final byte[] DHCP_REQUEST_BYTES = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) DHCP_REQUEST};
    protected static final int DHCP_DECLINE = 4;
    protected static final byte[] DHCP_DECLINE_BYTES = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) DHCP_DECLINE};
    protected static final int DHCP_ACK = 5;
    protected static final byte[] DHCP_ACK_BYTES = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) DHCP_ACK};
    protected static final int DHCP_NAK = 6;
    protected static final byte[] DHCP_NAK_BYTES = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) DHCP_NAK};
    protected static final int DHCP_RELEASE = 7;
    protected static final byte[] DHCP_RELEASE_BYTES = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) DHCP_RELEASE};
    protected static final byte[] DHCP_INFORM = {(byte) DHCP_MESSAGE_TYPE_OPTION, (byte) 1, (byte) 8};
    protected static final byte[] DHCP_SERVER_IDENTIFIER_BYTES = {(byte) SERVER_IDENTIFIER_OPTION, (byte) 4};
    protected static final byte[] DHCP_REQUESTED_IPADDRESS_BYTES = {(byte) REQUESTED_IPADDRESS_OPTION, (byte) 4};

    private static IPAddress _gateway;
    private static IPAddress _netmask;
    private static IPAddress _resolver;
//    private static IPAddress _source;
    private static IPAddress _yours;
    @SuppressWarnings("unused")
    private static IPAddress _previous;
    private static IPAddress _serverIdentifier;
    private static String _domainName;
    private static DHCP _singleton;

    public static DHCP getDHCP(byte[] hwaddr, IPAddress previous) {
        if (_singleton == null) {
            _singleton = new DHCP(hwaddr, previous);
        }
        return _singleton;

    }
    private DHCP(byte[] hwaddr, IPAddress previous) {
        _debug = System.getProperty("max.ve.net.dhcp.debug") != null;
        _gateway = new IPAddress(0, 0, 0, 0);
        _netmask = new IPAddress(0, 0, 0, 0);
        _resolver = new IPAddress(0, 0, 0, 0);
        _random = new Random();
        _myHWaddr = hwaddr;
        _xid = _random.nextInt();
        _endpoint = new UDPEndpoint();
        _previous = previous;
        _localPort = UDP.register(this, CLIENT_PORT, false);
    }

    public synchronized IPAddress sendRequest() {
        _state = STATE_DISCOVER;
        Packet mp = null;
        final long startTime = System.currentTimeMillis();

        try {
            do {
                if (_state == STATE_DISCOVER) {
                    if (_secs < 0) {
                        _secs = 0;
                    } else {
                        _secs = (int) ((System.currentTimeMillis() - startTime) / 1000);
                    }
                    mp = getDHCPMemoryPacket();
                    mp.putBytes(DHCP_DISCOVER_BYTES, 0, OPTIONS_OFFSET, DHCP_DISCOVER_BYTES.length);
                    mp.putByte(END_OPTION, OPTIONS_OFFSET + DHCP_DISCOVER_BYTES.length);

                    UDP.output(mp, _localPort, _dest.addressAsInt(), _destPort, requiresSpace(), 0);

                    dhcpPrintln("output: DHCP Discover");
                    waitForStateChange();
                    if (_delay >= MAX_DELAY) {
                        // give up
                        break;
                    }

                    // continue
                } else if (_state == STATE_OFFERED) {
                    int offset = OPTIONS_OFFSET;
                    mp = getDHCPMemoryPacket();
                    mp.putBytes(DHCP_REQUEST_BYTES, 0, offset, DHCP_REQUEST_BYTES.length);
                    offset += DHCP_REQUEST_BYTES.length;
                    mp.putBytes(DHCP_SERVER_IDENTIFIER_BYTES, 0, offset, DHCP_SERVER_IDENTIFIER_BYTES.length);
                    offset += DHCP_SERVER_IDENTIFIER_BYTES.length;
                    mp.putInt(_serverIdentifier.addressAsInt(), offset);
                    offset += 4;
                    mp.putBytes(DHCP_REQUESTED_IPADDRESS_BYTES, 0, offset, DHCP_REQUESTED_IPADDRESS_BYTES.length);
                    offset += DHCP_REQUESTED_IPADDRESS_BYTES.length;
                    mp.putInt(_yours.addressAsInt(), offset);
                    offset += 4;

                    mp.putByte(END_OPTION, offset);

                    dhcpPrintln("output: DHCP Request server identifier " + _serverIdentifier + " my address " + _yours);
                    UDP.output(mp, _localPort, _dest.addressAsInt(), _destPort, requiresSpace(), 0);

                    waitForStateChange();
                }
            } while (_state != STATE_COMPLETE);
        } catch (InterruptedException ex) {
            return null;
        }
        return _yours;
    }

    /*
     * @return a basic memory packet that is formatted for DHCP
     */
    private Packet getDHCPMemoryPacket() {
        final Packet mp = Packet.get(14 + 20 + 8, requiresSpace());
        mp.putByte(REQUEST, OP_OFFSET);
        mp.putByte((byte) 1, HTYPE_OFFSET);
        mp.putByte((byte) 6, HLEN_OFFSET); // ether address size
        mp.putShort(_secs, SECS_OFFSET);
        mp.putInt(_xid, XID_OFFSET);
        mp.putBytes(_myHWaddr, 0, HWADDR_OFFSET, _myHWaddr.length);
        mp.putBytes(COOKIE, 0, VEND_OFFSET, COOKIE.length);
        return mp;
    }

    private boolean waitForStateChange() throws InterruptedException {
        final long currentState = _state;
        wait((_delay + randomization()) * 1000);
        if (_state == currentState) {
            // we timed out, try again
            _delay = _delay * 2;
            // Implementation decision : reuse the same xid for retransmissions
            return false;
        } else {
            return true;
        }
    }

    private static int randomization() {
        final int r = _random.nextInt(3);
        return r == 1 ? -1 : (r == 2 ? 1 : 0);
    }

    // Called by Udp when a packet arrives on our port.
    public synchronized void input(Packet pkt) {
        int newState = _state;

        if (_debug) {
            dhcpPrintln("input");
            dumpPktOptions(pkt);
        }

        if ((pkt.getInt(XID_OFFSET)) == _xid && checkCookie(pkt)) {
            // We are looking for DHCP_MESSAGE_TYPE option
            int index = findOption(DHCP_MESSAGE_TYPE_OPTION, pkt);
            if (index > 0) {
                final int type = pkt.getByte(index);
                if (type == DHCP_OFFER) {
                    if (_state == STATE_DISCOVER) {
                        // dumpPktOptions(mp);
                        _yours = new IPAddress(pkt.getInt(YIADDR_OFFSET));
                        dhcpPrintln("ipaddress: " + _yours.toString());
                        index = findOption(SUBNET_MASK_OPTION, pkt);
                        if (index > 0) {
                            _netmask = getIPAddressFromVendor(pkt, index);
                            dhcpPrintln("netmask: " + _netmask);
                        }
                        index = findOption(NAME_SERVER_OPTION, pkt);
                        if (index > 0) {
                            _resolver = getIPAddressFromVendor(pkt, index);
                            dhcpPrintln("name server: " + _resolver);
                        }
                        index = findOption(SERVER_IDENTIFIER_OPTION, pkt);
                        if (index > 0) {
                            _serverIdentifier = getIPAddressFromVendor(pkt, index);
                            dhcpPrintln("server identifier: " + _serverIdentifier);
                        }
                        index = findOption(ROUTER_OPTION, pkt);
                        if (index > 0) {
                            _gateway = getIPAddressFromVendor(pkt, index);
                            dhcpPrintln("gateway: " + _gateway);
                        }
                        index = findOption(IPADDRESS_LEASE_TIME_OPTION, pkt);
                        if (index > 0) {
                            _leaseTime = pkt.getInt(index);
                            dhcpPrintln("ip lease time: " + _leaseTime + " secs");
                        }
                        index = findOption(DOMAIN_NAME_OPTION, pkt);
                        if (index > 0) {
                            final int len = pkt.getByte(index - 1);
                            final StringBuilder sb = new StringBuilder(len);
                            for (int i = 0; i < len; i++) {
                                sb.append((char) pkt.getByte(index + i));
                            }
                            _domainName = sb.toString();
                        }
                        dhcpPrintln("transition to STATE_OFFERED");
                        newState = STATE_OFFERED;
                    } else {
                        dhcpPrintln("ignoring duplicate DHCP Offer");
                    }
                } else if (type == DHCP_ACK) {
                    newState = STATE_COMPLETE;
                    dhcpPrintln("ACK - discovery complete");
                } else if (type == DHCP_NAK) {
                    dhcpPrintln("NAK - restarting discovery");
                    newState = STATE_DISCOVER;
                }
            }
            final int oldState = _state;
            _state = newState;
            if (_state != oldState) {
                notify();
            }
        } else {
            dhcpPrintln(checkCookie(pkt) ? "xid mismatch" : "bad cookie");
        }
    }

    private void dhcpPrintln(String message) {
        if (_debug) {
            Debug.println("DHCP: " + message);
        }
    }

    private boolean checkCookie(Packet mp) {
        for (int i = 0; i < 4; i++) {
            if (mp.getByte(VEND_OFFSET + i) != (COOKIE[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    private int findOption(int optionType, Packet mp) {
        int i = OPTIONS_OFFSET;
        final int pktLength = mp.length();
        while (i < pktLength) {
            final int code = mp.getByte(i);

            if (code == PAD_OPTION) { /* pad */
                i++;
                continue;
            } else if (code == END_OPTION) { /* end */
                break;
            }

            i++;
            final int len = mp.getByte(i);
            i++;
            if (code == optionType) {
                return i;
            }
            i += len;
        }
        return -1;
    }

    private void dumpPktOptions(Packet pkt) {
        int i = OPTIONS_OFFSET;
        final int pktLength = pkt.length();
        int code = 0;
        dhcpPrintln("OPTIONS");
        while (i < pktLength) {
            code = pkt.getByte(i) & 0xFF;

            if (code == PAD_OPTION) { /* pad */
                dhcpPrintln("PAD");
                i++;
                continue;
            } else if (code == END_OPTION) { /* end */
                dhcpPrintln("END");
                break;
            }

            i++;
            final int len = pkt.getByte(i);
            i++;
            Debug.print("OPTION " + code + " len " + len);
            for (int j = 0; j < len; j++) {
                Debug.print("  " + (pkt.getByte(i + j) & 0xff));
            }
            Debug.println("");
            i += len;
        }
        if (code != END_OPTION) {
            dhcpPrintln("END not found");
        }
    }

    private IPAddress getIPAddressFromVendor(Packet mp, int index) {
        return new IPAddress(mp.getInt(index));
    }

    public IPAddress resolver() {
        return _resolver;
    }

    public IPAddress gateway() {
        return _gateway;
    }

    public IPAddress netmask() {
        return _netmask;
    }

    public String domainName() {
        return _domainName;
    }

    public int length() {
        return requiresSpace();
    }

    /*
     * BOOTP packet is 300 bytes in length
     */
    public static int requiresSpace() {
        return 300;
    }
}
