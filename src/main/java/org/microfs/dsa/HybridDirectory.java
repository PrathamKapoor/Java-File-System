package org.microfs.dsa;

import java.util.*;

/**
 * Hybrid directory using both hash index and RB tree.
 */
public class HybridDirectory implements DirectoryIndex {
    private final HashDirectory hashDir = new HashDirectory();
    private final RBTreeDirectory rbDir = new RBTreeDirectory();

    @Override
    public void insert(DirectoryEntry entry) {
        hashDir.insert(entry);
        rbDir.insert(entry);
    }

    @Override
    public DirectoryEntry lookup(String name) {
        return hashDir.lookup(name);
    }

    @Override
    public boolean delete(String name) {
        boolean hashDel = hashDir.delete(name);
        boolean rbDel = rbDir.delete(name);
        return hashDel && rbDel;
    }

    @Override
    public List<DirectoryEntry> listAll() {
        return rbDir.listAll(); // ordered from RB
    }

    @Override
    public int size() { return hashDir.size(); }

    @Override
    public boolean isEmpty() { return hashDir.isEmpty(); }

    @Override
    public String getRepresentation() { return "HYBRID"; }

    public HashDirectory getHashDir() { return hashDir; }
    public RBTreeDirectory getRbDir() { return rbDir; }
}
