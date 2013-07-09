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
package com.sun.max.ve.net;

import com.sun.max.ve.net.debug.*;
import com.sun.max.ve.net.icmp.*;
import com.sun.max.ve.net.ip.*;

//
// Class used to maintain a list of default routers.
//

class RouterListEntry {

    //
    // IP Address of this router.
    //

    int Router;

    //
    // Use for linking to lists.
    //

    public RouterListEntry next;

    //
    // The preference value for this router. Greater is more preference.
    //

    int preference;

    //
    // The lifetime this router can be used. In seconds.
    //

    int lifetime;

    //
    // time in milliseconds when this entry was validated.
    // Value returned from System.currentTimeMillis()
    //

    long time;

    //
    // This boolean indicates whether the gateway is determined to be dead.
    // If it indicates that the gateway is dead, then the time field above
    // indicates when we decided that the gateway was dead.
    //

    boolean dead;

}

class RedirectRoute {

    RedirectRoute next;

    int destination;
    int gateway;

}

public class Route {

    //
    // This is the current default router. It may be setup by DHCP or any
    // other means initially. Later on, our dead gateway detection scheme
    // will pick a router from our list of default routers. This list may
    // have been obtained from DHCP, ICMP Router Discovery etc.
    //

    private static int currentDefaultRouter;

    private static int OurIpAddress;
    private static int OurNetmask;
    private static int OurNetwork;

    private static RouterListEntry DefaultRouterList;

    //
    // MAX_PREFERENCE is given to the router configured in when this class is
    // constructed. This is usually obtained from DHCP etc.
    //

    private static int MAX_PREFERENCE = 0x7fffffff;
    private static int MAX_LIFETIME = 0x7fffffff;


    //
    // The linked list of route cache entries created by ICMP Redirect
    // messages. We do not allow this list to exceed MAX_REDIRECT_ROUTE entries.
    //

    private static RedirectRoute redirect;

    private static int MAX_REDIRECT_ROUTES = 4;

    //----------------------------------------------------------------------

    Route(int ipaddr, int netmask, int defaultrouter) {

        if (defaultrouter != 0) {

            //
            // If we have a router configured from DHCP or someother means,
            // that router gets maximum preference and endless lifetime.
            //

            addDefaultRouter(defaultrouter, MAX_PREFERENCE, MAX_LIFETIME);

        }

        OurIpAddress = ipaddr;
        OurNetmask = netmask;
        OurNetwork = OurIpAddress & OurNetmask;

        try {
            ICMP.sendRouterSolicit();
        } catch (Exception e) {
        }

    }

    //----------------------------------------------------------------------

    Route(int ipaddr, int netmask) throws InterruptedException {

        this(ipaddr, netmask, 0);

    }

    //----------------------------------------------------------------------

    //
    // Returns the gateway IP address for the given destination.
    //

    public static int getRoute(int dest) {

        //
        // Check the ICMP redirected routing list, if we have any
        // entries.
        //

        if (redirect != null) {

            RedirectRoute rdr = redirect;
            RedirectRoute prev = null;
            int entries = 0;

            while (rdr != null) {

                if (rdr.destination == dest) {

                    //
                    // If we already have a route to this destination, update
                    // the gateway and bump up the entry to the head of the
                    // list.
                    //

                    if (prev != null) {        // Not head of list.
                        prev.next = rdr.next;
                        rdr.next = redirect;
                        redirect = rdr;
                    }

                    return rdr.gateway;

                }

                entries++;
                prev = rdr;
                rdr = rdr.next;

            }

        }


        return currentDefaultRouter;

    }

    //----------------------------------------------------------------------

    //
    // Scans the DefaultRouterList and sets the non dead router with the
    // highest priority to be the currentDefaultRouter.
    //

    private static void setCurrentDefaultRouter() {

        RouterListEntry        rentry = DefaultRouterList;

        while (rentry != null) {

            if (rentry.dead == false) {
                currentDefaultRouter = rentry.Router;
                return;
            }

            rentry = rentry.next;

        }

        //
        // All routers dead? We could set the currentDefaultRouter to 0.
        // Maybe it would be better to set it to the router of maximum
        // preference, even if it is dead.
        //

        if (DefaultRouterList != null) {

            currentDefaultRouter = DefaultRouterList.Router;

        } else {

            currentDefaultRouter = 0;

        }
    }

    //----------------------------------------------------------------------

    //
    // Adds a new default router. Ensure that the router added here is not
    // already in the list.
    //

    private static void addNewDefaultRouter(int router, int pref, int life) {

        RouterListEntry nr = new RouterListEntry();
        nr.Router = router;
        nr.preference = pref;
        nr.lifetime = life;
        nr.time = System.currentTimeMillis();
        nr.dead = false;

        RouterListEntry        rentry_prev = null;
        RouterListEntry        rentry = DefaultRouterList;

        while (rentry != null) {

            if (rentry.preference <= pref) {
                nr.next = rentry;
                if (rentry_prev != null) {
                    rentry_prev.next = nr;
                } else {
                    DefaultRouterList = nr;
                }
                return;
            }
            rentry_prev = rentry;
            rentry = rentry.next;
        }

        if (rentry_prev != null) {
            rentry_prev.next = nr;
        } else {
            DefaultRouterList = nr;
        }
        return;
    }

    //----------------------------------------------------------------------

    //
    // Add a default router to the default routers list. Checks if the router
    // specified is already in the list.
    //

    public static void addDefaultRouter(int router, int pref, int life) {

        //
        // First check if this is for a router already in the list. If so,
        // just update the entry.
        //

        RouterListEntry        rentry_prev = null;
        RouterListEntry        rentry = DefaultRouterList;

        while (rentry != null) {
            if (rentry.Router == router) {
                if (rentry.preference == MAX_PREFERENCE) {

                    //
                    // This entry was config'ed in by DHCP. Dont mess with it.
                    //
                    setCurrentDefaultRouter();
                    return;

                } else if (rentry.preference != pref) {

                    //
                    // The preference value changed for this entry.
                    // Delete the old preference entry and add a brand new one.
                    //
                    if (rentry_prev != null) {
                        rentry_prev.next = rentry.next;
                    } else {
                        DefaultRouterList = rentry.next;
                    }
                    addNewDefaultRouter(router, pref, life);
                    setCurrentDefaultRouter();
                    return;

                } else {

                    //
                    // Just update the times for this default router entry.
                    //
                    rentry.time = System.currentTimeMillis();
                    rentry.lifetime = life;
                    setCurrentDefaultRouter();
                    return;

                }
            }
            rentry_prev = rentry;
            rentry = rentry.next;
        }

        //
        // New entry.
        //

        addNewDefaultRouter(router, pref, life);
        setCurrentDefaultRouter();

        return;

    }

    //----------------------------------------------------------------------

    //
    // Called by OutstandingPing when it decides that a router is not
    // responding to ICMP_ECHOREQs.
    //

    public static void markRouterDead(int router) {

        markDefaultRouterDead(router);

        //
        // FIXIT. Need to check the static routing table here.
        //

        //
        // Flush the redirect routing cache here.
        //

        RedirectRoute rdr = redirect;
        RedirectRoute prev = null;
        RedirectRoute rdrtmp = null;

        while (rdr != null) {

            if (rdr.gateway == router) {

                if (prev != null) {        // Not head of list.
                    prev.next = rdr.next;
                } else {
                    redirect = rdr.next;
                }

                rdrtmp = rdr;
                rdr = rdr.next;
                rdrtmp.next = null;
                continue;

            }

            prev = rdr;
            rdr = rdr.next;

        }

    }

    //----------------------------------------------------------------------

    //
    // Check the defaultRouterList and if this router is present in that
    // list, mark it dead. Then reset the currentDefaultRouter.
    //

    private static void markDefaultRouterDead(int router) {

        RouterListEntry        rentry = DefaultRouterList;
        while (rentry != null) {

            if (rentry.Router == router) {

                // mark the router dead.
                rentry.dead = true;

                // reset the currentDefaultRouter.
                setCurrentDefaultRouter();
                break;
            }

            rentry = rentry.next;
        }

    }

    //----------------------------------------------------------------------

    public static void markRouterAlive(int router) {


        // Check default router list first.

        RouterListEntry        rentry = DefaultRouterList;

        while (rentry != null) {

            if (rentry.Router == router) {

                //
                // mark the router alive.
                // reset the current default router if necessary.
                //

                rentry.dead = false;
                setCurrentDefaultRouter();
                break;

            }

            rentry = rentry.next;

        }

        //
        // FIXIT. Check static route table now to mark route as UP.
        //

    }

    //----------------------------------------------------------------------

    //
    // Check the route to this destination. Called from protocol code
    // when the protocol thinks the gateway may be dead.
    //

    public static void checkRoute(int destination) {

        //
        // First, if the destination is local, ignore this call.
        //

        if ((destination & OurNetmask) == OurNetwork)
            return;

        //
        // Next, get the first hop router to this destination.
        //

        int router = getRoute(destination);

        //
        // Now, if we already have a ICMP echoreq pending to this router
        // just return.
        //

        if (GatewayPingCheck.isInList(router) == true) {

            return;

        }

        //
        // Create a new OutstandingPing for this router. OutstandingPing will
        // send a ICMP_ECHOREQ and manage replies from the router.
        //

        GatewayPingCheck.create(router);

        // System.err.println(">>>> CheckRoute: router "
        //                         + IP.addrToString(router)
        //                         + ", dest " + IP.addrToString(destination));
        // dumpRoutingInfo();
    }

    //----------------------------------------------------------------------

    //
    // Called from the ICMP class when we get an ICMP Redirect message.
    //

    public static void redirectRoute(int dest, int gway) {

        RedirectRoute rdr = redirect;
        RedirectRoute prev = null;
        int entries = 0;

        Debug.println("redirectRoute: dest " +
                        IPAddress.toString(dest) +
                        ", gway " +
                        IPAddress.toString(gway));

        while (rdr != null) {

            if (rdr.destination == dest) {

                //
                // If we already have a route to this destination, update
                // the gateway and bump up the entry to the head of the
                // list.
                //

                rdr.gateway = gway;

                if (prev != null) {        // Not head of list.
                    prev.next = rdr.next;
                    rdr.next = redirect;
                    redirect = rdr;
                }

                return;

            }

            entries++;
            prev = rdr;
            rdr = rdr.next;

        }

        rdr = new RedirectRoute();

        if (rdr == null) {
            return;
        }

        rdr.destination = dest;
        rdr.gateway = gway;

        rdr.next = redirect;
        redirect = rdr;

        if (entries == MAX_REDIRECT_ROUTES) {

            //
            // bump off the last entry.
            //

            rdr = redirect;
            prev = null;
            while (rdr != null) {

                if (rdr.next == null) {

                    prev.next = null;
                    break;

                }
                prev = rdr;
                rdr = rdr.next;

            }

        }

        return;

    }

    //----------------------------------------------------------------------

    @Override
    public String toString() {
        return "Route: currentDefaultRouter " +
                                        IPAddress.toString(currentDefaultRouter);
    }

    //----------------------------------------------------------------------

    public static void dumpRoutingInfo() {

        Debug.println( "Route: currentDefaultRouter " +
                                        IPAddress.toString(currentDefaultRouter));

        Debug.println( " DefaultRouterList");
        RouterListEntry        rentry = DefaultRouterList;
        while(rentry != null) {
            Debug.println( "     : " + IPAddress.toString(rentry.Router)
                                   + ", pref " + rentry.preference
                                   + ", time " + rentry.time
                                   + ", lifetime " + rentry.lifetime
                                   + ", dead " + rentry.dead);

            rentry = rentry.next;
        }

        // TODO OutstandingPing.dump();

        Debug.println(" Redirect Route List: max " + MAX_REDIRECT_ROUTES);
        RedirectRoute rdr = redirect;

        if (rdr == null)
            Debug.println(" < No Entries >");

        while(rdr != null) {

            Debug.println("     : Dest "
                              + IPAddress.toString(rdr.destination)
                              + ", gateway "
                              + IPAddress.toString(rdr.gateway)
                                );
            rdr = rdr.next;

        }

    }

    //----------------------------------------------------------------------
}
