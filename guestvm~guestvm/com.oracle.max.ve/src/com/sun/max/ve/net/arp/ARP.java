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
package com.sun.max.ve.net.arp;
/**
   Address Resolution Protocol layer.

   Solves the question: have IP address, what is ether address?

   notes:

   The arp entries are stored in two arrays.  One array for ether
   addresses, another for associated ip addresses.  Insert new
   entries at index nextFree, wrapping around as necessary.

   *** The ARP rfc specifies that entries have a lifetime of no
   more than 30 minutes.  We don't implement that policy yet. ***

   The sendPacket() code ensures that we don't send multiple ARP
   requests for the same IP address.  The RFC says we should hold
   on to the *latest* packet for transmission when an ARP reply
   comes in.  Instead we currently hold on to the *first* packet for
   transmission.  Might want to fix this.

   sritchie -- Oct 95

   @author Mick Jordan (modifications)
*/

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.ve.net.*;
import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.device.*;
import com.sun.max.ve.net.ip.*;
import com.sun.max.ve.net.protocol.ether.*;

/**
 * The ARP protocol implementation.
 */
public final class ARP extends TimerTask implements NetDevice.Handler {

    private static ARP _singleton;

    // used to timeout entries in the ARP cache
    private Timer _timer;
    private static long _arpCacheReapInterval = 60000;     // tick every minute
    private static int _maxTicks = 20;                                 // 20 minute timeout for known entries
    private static int _maxInProgressTicks = 3;                   // 3 minute timeout for in progress entries

    // ethernet driver we'll send packets to
    private Ether _ether  = null;

    private static final int ARP_TIMEOUT = 1000;
    private static final int DEFAULT_MAX_ARP_TIMEOUTS = 5;
    private int _maxTimeOuts = DEFAULT_MAX_ARP_TIMEOUTS;


    private static final int ARPOP_REQUEST = 1;
    private static final int ARPOP_REPLY   = 2;

    private static final int ETHERTYPE_IP  = 0x800;
    //private static final int ETHERTYPE_ARP = 0x806;
    private static final int ARPHRD_ETHER  = 1;

    // These are the header offsets.
    private static final int HRD_OFFSET    = 0;  // hardware address type
    private static final int PROT_OFFSET   = 2;  // protocol address type
    private static final int HLEN_OFFSET   = 4;  // hardware address length
    private static final int PLEN_OFFSET   = 5;  // protocol address length
    private static final int OP_OFFSET     = 6;  // operation code
    private static final int SRCETH_OFFSET = 8;  // source hardware address
    private static final int SRCIP_OFFSET  = 14; // source IP address
    private static final int DSTETH_OFFSET = 18; // target hardware address
    private static final int DSTIP_OFFSET  = 24; // target IP address

    public static class CacheEntry {
        private int _ticks;
        private int _ipAddr;
        private long _ethAddress;

        CacheEntry(int ipAddr, long ethAddress) {
            _ipAddr = ipAddr;
            _ethAddress = ethAddress;
        }

        public long getEthAddress() {
            return _ethAddress;
        }

        public IPAddress getIPAddress() {
            return new IPAddress(_ipAddr);
        }
    }

    private static Map<Integer, CacheEntry> _cache = Collections.synchronizedMap(new HashMap<Integer, CacheEntry>());

    public static ARP getARP(Ether ether) {
        if (_singleton == null) {
            _singleton = new ARP(ether);
        }
        return _singleton;
    }

    private ARP(Ether ether) {
        _ether = ether;
        _debug = System.getProperty("max.ve.net.arp.debug") != null;
        final String timeOutProperty = System.getProperty("max.ve.net.arp.maxtimeouts");
        if (timeOutProperty != null) {
            _maxTimeOuts = Integer.parseInt(timeOutProperty);
        }
        _timer = new Timer("ARP Cache Timer", true);
        _timer.scheduleAtFixedRate(this, _arpCacheReapInterval, _arpCacheReapInterval);

        _singleton = this;
    }

    private long resolve(int ipAddr, Packet pkt, boolean nonBlocking) {
        long result = 0;
        // look for the IP address in our cache.
        CacheEntry entry = _cache.get(ipAddr);

        if (entry == null) {
            // We don't have the destination ethernet address so try to find it
            // insert this IP addr into our cache as "in-progress"
            entry = new CacheEntry(ipAddr, 0);
            _cache.put(ipAddr, entry);
            if (nonBlocking) {
                new Thread(new ARPThread(ipAddr, pkt)).start();
            } else {
                if (doArp(pkt, ipAddr,  _maxTimeOuts)) {
                    result = entry._ethAddress;
                }
            }
        } else {
            if (entry._ethAddress != 0) {
                // we have the Ethernet address in our cache, so immediately
                // return it to the caller.
                result = entry._ethAddress;
            } else {
                // We currently have an ARP request on the wire for this IP
                // address, and we are not going to generate another.
                // So we drop the packet.
                // TODO: reconsider this
            }
        }
        return result;
    }

    /**
     * Get the ethernet address for the given IP address.
     * If we do not have the address in the cache,
     * this method will block until either the address is discovered
     * or the ARP request times out.
     * @param ip_addr IP address
     * @return the ethernet address or 0 if not found
     */
    public long resolve(int ip_addr) {
        return resolve(ip_addr, null, false);
    }

    /**
     * Get the ethernet address for the given IP address.
     * If we do not have the address in the cache,
     * this method will spawn a thread to resolve the address
     * and, if that is successful, will then send the original packet.
     * @param ip_addr IP address
     * @param pkt packet to send
     * @return the ethernet address or 0 if not found
     */
   public long nonBlockingResolve(int ip_addr, Packet pkt) {
        return resolve(ip_addr, pkt, true);
    }

    public void handle(Packet pkt) {
        pkt.shiftHeader(14);
        input(pkt);
    }

    /**
     * Process a received ARP packet from the network. This packet is either a
     * broadcast "who is xxx?" or a response to one of our ARP request
     * broadcasts.
     *
     * @param pkt
     *            The packet
     */
    void input(Packet pkt) {
        // get hardware type from arp header
        int i = pkt.getShort(HRD_OFFSET);
        if (i != ARPHRD_ETHER) {
            err("unsupported hardware type: " + i);
            return;
        }

        // get protocol type from arp header
        i = pkt.getShort(PROT_OFFSET);
        if (i != ETHERTYPE_IP) {
            err("unsupported protocol type: " + i);
            return;
        }

        // get source IP address and ethernet address
        int src_ip = pkt.getInt(SRCIP_OFFSET);
        long src_ethAddress = pkt.getEthAddrAsLong(SRCETH_OFFSET);

        if (_debug) {
            dprint("ARP packet from " + IPAddress.toString(src_ip));
        }

        // if we already have this IP/ether translation, update
        // our old copy of the ether address with the new one.
        CacheEntry entry = _cache.get(src_ip);
        boolean updated = false;
        if (entry != null) {
            if (_debug) {
                dprint("updating " + IPAddress.toString(src_ip) + " " + Long.toString(src_ethAddress, 16));
            }
            updated = true;

            long ethAddress = entry._ethAddress;
            entry._ethAddress = src_ethAddress;

            if (ethAddress == 0) {
                // notify any waiting thread that a new mapping has arrived
                synchronized (entry) {
                    entry.notify();
                }
            }
        }

        // get target IP address and see if it's for us.
        int dest_ip = pkt.getInt(DSTIP_OFFSET);
        if (dest_ip == IP.getLocalAddress()) {
            // this packet is for us.
            if (updated == false) {
                // insert the new IP/ether mapping into our cache.
                entry = new CacheEntry(dest_ip, src_ethAddress);
                _cache.put(src_ip, entry);
            }

            // get ARP packet type
            i = pkt.getShort(OP_OFFSET);
            if (i == ARPOP_REQUEST) {
                if (_debug) {
                    dprint("replying to ARPOP_REQUEST from " + IPAddress.toString(src_ip));
                }

                // we got an ARP request for this machine, let's reply to it.
                sendArpReply(src_ethAddress, src_ip);
            }
        } else {
            // not for us, so we're done.
            if (_debug) {
                dprint("ignoring ARP target "+IPAddress.toString(dest_ip));
            }
        }
    }

    // Check the ARP cache and flush stale entries.
    @Override
    public void run() {
        Set<Map.Entry<Integer,CacheEntry>> values = _cache.entrySet();
        synchronized (_cache) {
            Iterator<Map.Entry<Integer,CacheEntry>> iter = values.iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer,CacheEntry> mapEntry =  iter.next();
                CacheEntry entry = mapEntry.getValue();
                entry._ticks ++;
                if ((entry._ethAddress != 0 && entry._ticks >= _maxTicks) || (entry._ethAddress == 0 && entry._ticks >= _maxInProgressTicks)) {
                    if (_debug) {
                        dprint("cache entry " + IPAddress.toString(mapEntry.getKey())
                                + " expired");
                    }
                    iter.remove();
                }
            }
        }
    }

    public CacheEntry[] getArpCache(){
        Collection<CacheEntry> values = _cache.values();
        return values.toArray(new CacheEntry[values.size()]);
    }

    /**
     *  Send a broadcast ARP request packet to the net.
     * @param ip_addr
     * @param local_ip
     */
    private void sendArp(int ip_addr, int local_ip) {
        Packet pkt = Packet.get(14, 28);
        // Build an ARP request packet.

        pkt.putShort(ARPHRD_ETHER, HRD_OFFSET);// hardware type ETHERNET
        pkt.putShort(ETHERTYPE_IP, PROT_OFFSET); // protocol type IP
        pkt.putByte(6, HLEN_OFFSET); // ethernet addr length
        pkt.putByte(4, PLEN_OFFSET); // IP addr length
        pkt.putShort(ARPOP_REQUEST, OP_OFFSET);// operation code
        pkt.putEthAddr(_ether.getMacAddress(), SRCETH_OFFSET);
        pkt.putInt(local_ip, SRCIP_OFFSET); // src IP addr
        pkt.putEthAddr(0, DSTETH_OFFSET); // unknown dest ether addr
        pkt.putInt(ip_addr, DSTIP_OFFSET); // dest IP addr

        // broadcast this ARP packet

        _ether.transmitARPBroadcast(pkt);
        dprint("sendArp: ipaddr " + IPAddress.toString(ip_addr) + " local_ip "
                + IPAddress.toString(local_ip));
    }

    /**
     *  Send an Arp reply with own ethernet address and matching IP.
     * @param src_eth
     * @param src_ip
     */

    void sendArpReply(long src_eth, int src_ip) {
      sendGeneralArpReply(src_eth, src_ip, _ether.getMacAddress(), IP.getLocalAddress(), src_eth);
    }

    /**
     *  Send a general Arp reply with a given ethernet address and matching IP.
     * @param src_eth
     * @param src_ip
     * @param local_eth
     * @param local_ip
     * @param dest_eth
     */
    void sendGeneralArpReply(long src_eth, int src_ip, byte[] local_eth,
            int local_ip, long dest_eth) {

        // Allocate a new packet for the reply.
        Packet p = Packet.get(14, 28);

        // Build an ARP reply packet.

        p.putShort(ARPHRD_ETHER, HRD_OFFSET); // hardware type ETHERNET
        p.putShort(ETHERTYPE_IP, PROT_OFFSET); // protocol type IP
        p.putByte(6, HLEN_OFFSET); // ethernet addr length
        p.putByte(4, PLEN_OFFSET); // IP addr length
        p.putShort(ARPOP_REPLY, OP_OFFSET); // operation code
        p.putEthAddr(local_eth, SRCETH_OFFSET);
        p.putInt(local_ip, SRCIP_OFFSET);
        p.putEthAddr(src_eth, DSTETH_OFFSET);
        p.putInt(src_ip, DSTIP_OFFSET);

        // write the packet out the network
        _ether.transmitARPBroadcast(p);
    }

    /**
     * Called from an ArpThread or checkForIP() to perform an ARP request.
     *
     * @param originalPkt
     *            The original packet we're arping in order  to send. May
     *            be null if all you care about is the return value of the
     *            method.
     * @param ip_addr
     *            The IP address we're ARP'ing for
     * @param local_ip
     *            The source IP address for the ARP header
     * @param maxTimeouts
     *            How many times to send an ARP request
     *
     * @return True if address resolved
     */
    boolean doArp(Packet originalPkt, int ip_addr, int maxTimeouts) {
        boolean found = false;
        int timeouts;
        CacheEntry entry = _cache.get(ip_addr);
        if (entry == null) {
            ProgramError.unexpected("doArp, no cache entry");
        }

        for (timeouts = 0; timeouts < maxTimeouts; timeouts++) {
            // broadcast an ARP request packet
            try {
                if (_debug) {
                    dprint("doArp: sendArp: ip_addr "
                            + IPAddress.toString(ip_addr) + " local_ip "
                            + IPAddress.toString(IP.getLocalAddress()));
                }
                sendArp(ip_addr, IP.getLocalAddress());
                synchronized (entry) {
                    entry.wait((timeouts + 1) * ARP_TIMEOUT);
                }
            } catch (InterruptedException e) {
                    return false;
            }

            // check if we have received a response.
            if (entry._ethAddress != 0) {
                found = true;
                if (_debug) {
                  dprint("found " + IPAddress.toString(ip_addr) + " as " + Long.toString(entry._ethAddress, 16));
                }

                if (originalPkt != null) {
                    // now we can send the packet
                    if (_debug) {
                        dprint("sending originalPkt: to "
                                + IPAddress.toString(ip_addr) + " using "
                                + Long.toString(entry._ethAddress, 16));
                    }
                    _ether.transmit(entry._ethAddress, ETHERTYPE_IP, originalPkt);
                }
                break;
            }

            // if we get here, then we don't yet have an answer
            // to the ARP request. Loop around and try again.
            if (_debug) {
                dprint("timeout " + timeouts + " for " + IPAddress.toString(ip_addr));
            }
        }

        return found;
    }

    /**
     *  Check for the existence of given IP address.
     * @param addr
     * @param maxTimeouts
     * @return Returns true if the host addr is found, false if we timeout.
     */
    public boolean checkForIP(int addr, int maxTimeouts) {

        CacheEntry entry = _cache.get(addr);
        if (entry != null) {
            if (entry._ethAddress != 0) {
                return true;
            }
            // TODO: in progress what to do?
            return false;
        } else {
             // insert this IP addr into our cache as "in-progress"
             _cache.put(addr, new CacheEntry(addr, 0));
        }

        return doArp(null, addr, maxTimeouts);
    }

    public static void setCacheEntryTimeout(int ticks) {
        _maxTicks = ticks;
    }

    public static void setCacheReapInterval(int secs) {
        _arpCacheReapInterval = secs * 1000;
    }

    private static int _threadCount;
    class ARPThread extends Thread {
        private Packet _pkt;
        private int _ipAddr;
        ARPThread(int ipAddr, Packet pkt) {
            super("ARP" + _threadCount++);
            _ipAddr = ipAddr;
            _pkt = pkt;
        }

        @Override
        public void run() {
            doArp(_pkt, _ipAddr, _maxTimeOuts);
        }
    }

    private static boolean _debug;

    public static boolean setDebug(boolean d) {
        boolean p = _debug;
        _debug = p;
        return p;
    }

    private static void dprint(String mess) {
        if (_debug) {
            err(mess);
        }
    }

     private static void err(String mess) {
        Debug.println("ARP: " + mess);
    }

}

