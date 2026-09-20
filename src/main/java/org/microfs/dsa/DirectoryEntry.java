package org.microfs.dsa;

import java.util.*;

/**
 * Directory entry: name -> inode reference.
 */
public final class DirectoryEntry {
    private final String name;
    private final int inode;

    public DirectoryEntry(String name, int inode) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Invalid name");
        if (inode < 0) throw new IllegalArgumentException("Invalid inode");
        this.name = name;
        this.inode = inode;
    }

    public String getName() { return name; }
    public int getInode() { return inode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DirectoryEntry)) return false;
        DirectoryEntry other = (DirectoryEntry) o;
        return inode == other.inode && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, inode);
    }

    @Override
    public String toString() {
        return name + " -> " + inode;
    }
}
