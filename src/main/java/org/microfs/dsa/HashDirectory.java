package org.microfs.dsa;

import java.util.*;

/**
 * Hash-based directory index using HashMap for exact lookup.
 */
public class HashDirectory implements DirectoryIndex {
    private final Map<String, DirectoryEntry> index = new HashMap<>();

    @Override
    public void insert(DirectoryEntry entry) {
        if (index.containsKey(entry.getName())) throw new IllegalArgumentException("Duplicate entry: " + entry.getName());
        index.put(entry.getName(), entry);
    }

    @Override
    public DirectoryEntry lookup(String name) {
        return index.get(name);
    }

    @Override
    public boolean delete(String name) {
        return index.remove(name) != null;
    }

    @Override
    public List<DirectoryEntry> listAll() {
        List<DirectoryEntry> result = new ArrayList<>(index.values());
        result.sort(Comparator.comparing(DirectoryEntry::getName));
        return result;
    }

    @Override
    public int size() { return index.size(); }

    @Override
    public boolean isEmpty() { return index.isEmpty(); }

    @Override
    public String getRepresentation() { return "HASH"; }
}
