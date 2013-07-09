/**
 *
 */
package org.openthinclient.mountd;

import java.net.InetAddress;

public class Group {
    private final InetAddress address;
    private final int mask;
    private boolean readOnly = false;
    private final boolean wildcard;

    public Group() {
        wildcard = true;
        address = null;
        mask = 0;
    }

    public Group(InetAddress address, int mask) {
        this.address = address;
        this.mask = mask;
        wildcard = false;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getMask() {
        return mask;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public String toString() {
        if (isWildcard()) {
            return "*" + (readOnly ? "(ro)" : "(rw)");
        } else {
            return (null != address ? address.toString() : "")
                   + (0 != mask ? "/" + mask : "")
                   + (readOnly ? "(ro)" : "(rw)");
        }
    }
}
