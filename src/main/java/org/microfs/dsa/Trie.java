package org.microfs.dsa;

import java.util.*;

/**
 * Trie for auxiliary namespace/search.
 *
 * <p>Supports insert(path/name), search(path/name), startsWith(prefix),
 * remove(path/name). Never affects persistent filesystem consistency.</p>
 */
public class Trie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
        int value; // inode reference or count
    }

    private final TrieNode root = new TrieNode();
    private int size = 0;

    public void insert(String name, int inodeRef) {
        TrieNode node = root;
        for (char c : name.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        if (!node.isEnd) size++;
        node.isEnd = true;
        node.value = inodeRef;
    }

    public Integer search(String name) {
        TrieNode node = root;
        for (char c : name.toCharArray()) {
            if (!node.children.containsKey(c)) return null;
            node = node.children.get(c);
        }
        return node.isEnd ? node.value : null;
    }

    public List<String> startsWith(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) return Collections.emptyList();
            node = node.children.get(c);
        }
        List<String> results = new ArrayList<>();
        collect(node, new StringBuilder(prefix), results);
        return results;
    }

    private void collect(TrieNode node, StringBuilder prefix, List<String> results) {
        if (node.isEnd) results.add(prefix.toString());
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            prefix.append(entry.getKey());
            collect(entry.getValue(), prefix, results);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    public boolean remove(String name) {
        return delete(root, name, 0);
    }

    private boolean delete(TrieNode node, String name, int depth) {
        if (depth == name.length()) {
            if (!node.isEnd) return false;
            node.isEnd = false;
            node.value = 0;
            size--;
            return node.children.isEmpty();
        }
        char ch = name.charAt(depth);
        TrieNode child = node.children.get(ch);
        if (child == null) return false;
        boolean shouldDeleteChild = delete(child, name, depth + 1);
        if (shouldDeleteChild) {
            node.children.remove(ch);
            return node.children.isEmpty() && !node.isEnd;
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean containsPrefix(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) return false;
            node = node.children.get(c);
        }
        return true;
    }
}
