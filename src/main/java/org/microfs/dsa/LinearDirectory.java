package org.microfs.dsa;

import java.util.*;

/**
 * Linear directory index using sorted array/list.
 */
public class LinearDirectory implements DirectoryIndex {
    private final List<DirectoryEntry> entries = new ArrayList<>();

    @Override
    public void insert(DirectoryEntry entry) {
        if (lookup(entry.getName()) != null) throw new IllegalArgumentException("Duplicate entry: " + entry.getName());
        entries.add(entry);
        entries.sort(Comparator.comparing(DirectoryEntry::getName));
    }

    @Override
    public DirectoryEntry lookup(String name) {
        for (DirectoryEntry e : entries) {
            if (e.getName().equals(name)) return e;
        }
        return null;
    }

    @Override
    public boolean delete(String name) {
        Iterator<DirectoryEntry> it = entries.iterator();
        while (it.hasNext()) {
            DirectoryEntry e = it.next();
            if (e.getName().equals(name)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public List<DirectoryEntry> listAll() {
        return new ArrayList<>(entries);
    }

    @Override
    public int size() { return entries.size(); }

    @Override
    public boolean isEmpty() { return entries.isEmpty(); }

    @Override
    public String getRepresentation() { return "LINEAR"; }
}
