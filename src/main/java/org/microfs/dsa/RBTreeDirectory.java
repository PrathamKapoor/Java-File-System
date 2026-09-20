package org.microfs.dsa;

import java.util.*;

/**
 * Red-Black Tree directory index for ordered operations.
 */
public class RBTreeDirectory implements DirectoryIndex {
    private final RedBlackTree<String, DirectoryEntry> tree = new RedBlackTree<>();

    @Override
    public void insert(DirectoryEntry entry) {
        if (tree.search(entry.getName()) != null) throw new IllegalArgumentException("Duplicate entry: " + entry.getName());
        tree.insert(entry.getName(), entry);
    }

    @Override
    public DirectoryEntry lookup(String name) {
        DirectoryEntry entry = tree.search(name);
        return entry;
    }

    @Override
    public boolean delete(String name) {
        return tree.delete(name) != null;
    }

    @Override
    public List<DirectoryEntry> listAll() {
        List<DirectoryEntry> result = new ArrayList<>(tree.size());
        for (String key : tree.orderedTraversal()) {
            result.add(tree.search(key));
        }
        return result;
    }

    @Override
    public int size() { return tree.size(); }

    @Override
    public boolean isEmpty() { return tree.isEmpty(); }

    @Override
    public String getRepresentation() { return "RB_TREE"; }

    public RedBlackTree<String, DirectoryEntry> getTree() { return tree; }
}
