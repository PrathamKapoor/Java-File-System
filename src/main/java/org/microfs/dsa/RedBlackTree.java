package org.microfs.dsa;

import java.util.*;

/**
 * A genuine Red-Black Tree implementation for ordered directory indexing.
 *
 * <p>Supports search, insertion, deletion, rotations, recoloring, minimum,
 * maximum, successor, predecessor, ordered traversal, and range traversal.
 *
 * <p>Invariants preserved:
 * 1. Every node is red or black.
 * 2. Root is black.
 * 3. Null leaves (NIL) are black.
 * 4. Red nodes have black children.
 * 5. Every root-to-leaf path has the same black height.</p>
 */
public class RedBlackTree<K extends Comparable<K>, V> {

    private enum Color { RED, BLACK }

    private static final class Node<K, V> {
        K key;
        V value;
        Node<K, V> left, right, parent;
        Color color;
        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.color = Color.RED;
        }
    }

    private final Node<K, V> nil = new Node<>(null, null);
    private Node<K, V> root;
    private int size = 0;
    private int rotations = 0;
    private int recolors = 0;

    public RedBlackTree() {
        nil.color = Color.BLACK;
        nil.left = nil.right = nil;
        root = nil;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return root == nil;
    }

    public V search(K key) {
        Node<K, V> node = findNode(key);
        return (node == nil) ? null : node.value;
    }

    private Node<K, V> findNode(K key) {
        Node<K, V> current = root;
        while (current != nil) {
            int cmp = key.compareTo(current.key);
            if (cmp == 0) return current;
            current = (cmp < 0) ? current.left : current.right;
        }
        return nil;
    }

    public void insert(K key, V value) {
        if (key == null) throw new IllegalArgumentException("Null key");
        Node<K, V> parent = nil;
        Node<K, V> current = root;
        while (current != nil) {
            parent = current;
            int cmp = key.compareTo(current.key);
            if (cmp == 0) {
                current.value = value;
                return;
            }
            current = (cmp < 0) ? current.left : current.right;
        }
        Node<K, V> node = new Node<>(key, value);
        node.parent = parent;
        node.left = nil; node.right = nil;
        if (parent == nil) {
            root = node;
        } else if (key.compareTo(parent.key) < 0) {
            parent.left = node;
        } else {
            parent.right = node;
        }
        node.color = Color.RED;
        fixInsert(node);
        size++;
    }

    private void fixInsert(Node<K, V> node) {
        while (node != root && node.parent.color == Color.RED) {
            if (node.parent == node.parent.parent.left) {
                Node<K, V> uncle = node.parent.parent.right;
                if (uncle.color == Color.RED) {
                    node.parent.color = Color.BLACK;
                    uncle.color = Color.BLACK;
                    node.parent.parent.color = Color.RED;
                    recolors += 3;
                    node = node.parent.parent;
                } else {
                    if (node == node.parent.right) {
                        node = node.parent;
                        rotateLeft(node);
                    }
                    node.parent.color = Color.BLACK;
                    node.parent.parent.color = Color.RED;
                    rotateRight(node.parent.parent);
                }
            } else {
                Node<K, V> uncle = node.parent.parent.left;
                if (uncle.color == Color.RED) {
                    node.parent.color = Color.BLACK;
                    uncle.color = Color.BLACK;
                    node.parent.parent.color = Color.RED;
                    recolors += 3;
                    node = node.parent.parent;
                } else {
                    if (node == node.parent.left) {
                        node = node.parent;
                        rotateRight(node);
                    }
                    node.parent.color = Color.BLACK;
                    node.parent.parent.color = Color.RED;
                    rotateLeft(node.parent.parent);
                }
            }
        }
        root.color = Color.BLACK;
    }

    private void rotateLeft(Node<K, V> x) {
        Node<K, V> y = x.right;
        x.right = y.left;
        if (y.left != nil) y.left.parent = x;
        y.parent = x.parent;
        if (x.parent == nil) root = y;
        else if (x == x.parent.left) x.parent.left = y;
        else x.parent.right = y;
        y.left = x;
        x.parent = y;
        rotations++;
    }

    private void rotateRight(Node<K, V> y) {
        Node<K, V> x = y.left;
        y.left = x.right;
        if (x.right != nil) x.right.parent = y;
        x.parent = y.parent;
        if (y.parent == nil) root = x;
        else if (y == y.parent.right) y.parent.right = x;
        else y.parent.left = x;
        x.right = y;
        y.parent = x;
        rotations++;
    }

    public V delete(K key) {
        Node<K, V> z = findNode(key);
        if (z == nil) return null;
        V oldValue = z.value;
        Node<K, V> y = z;
        Node<K, V> x;
        Color yOriginalColor = y.color;
        if (z.left == nil) {
            x = z.right;
            rbTransplant(z, z.right);
        } else if (z.right == nil) {
            x = z.left;
            rbTransplant(z, z.left);
        } else {
            y = minimum(z.right);
            yOriginalColor = y.color;
            x = y.right;
            if (y.parent == z) {
                if (x != nil) x.parent = y;
            } else {
                rbTransplant(y, y.right);
                y.right = z.right;
                y.right.parent = y;
            }
            rbTransplant(z, y);
            y.left = z.left;
            y.left.parent = y;
            y.color = z.color;
        }
        size--;
        if (yOriginalColor == Color.BLACK) {
            deleteFixup(x);
        }
        return oldValue;
    }

    private void deleteFixup(Node<K, V> x) {
        while (x != root && x.color == Color.BLACK) {
            if (x == x.parent.left) {
                Node<K, V> w = x.parent.right;
                if (w.color == Color.RED) {
                    w.color = Color.BLACK;
                    x.parent.color = Color.RED;
                    rotateLeft(x.parent);
                    w = x.parent.right;
                }
                if (w.left.color == Color.BLACK && w.right.color == Color.BLACK) {
                    w.color = Color.RED;
                    recolors++;
                    x = x.parent;
                } else {
                    if (w.right.color == Color.BLACK) {
                        w.left.color = Color.BLACK;
                        w.color = Color.RED;
                        rotateRight(w);
                        w = x.parent.right;
                    }
                    w.color = x.parent.color;
                    x.parent.color = Color.BLACK;
                    w.right.color = Color.BLACK;
                    rotateLeft(x.parent);
                    x = root;
                }
            } else {
                Node<K, V> w = x.parent.left;
                if (w.color == Color.RED) {
                    w.color = Color.BLACK;
                    x.parent.color = Color.RED;
                    rotateRight(x.parent);
                    w = x.parent.left;
                }
                if (w.right.color == Color.BLACK && w.left.color == Color.BLACK) {
                    w.color = Color.RED;
                    recolors++;
                    x = x.parent;
                } else {
                    if (w.left.color == Color.BLACK) {
                        w.right.color = Color.BLACK;
                        w.color = Color.RED;
                        rotateLeft(w);
                        w = x.parent.left;
                    }
                    w.color = x.parent.color;
                    x.parent.color = Color.BLACK;
                    w.left.color = Color.BLACK;
                    rotateRight(x.parent);
                    x = root;
                }
            }
        }
        x.color = Color.BLACK;
    }

    private void rbTransplant(Node<K, V> u, Node<K, V> v) {
        if (u.parent == nil) root = v;
        else if (u == u.parent.left) u.parent.left = v;
        else u.parent.right = v;
        v.parent = u.parent;
    }

    public K minimum() {
        return minimum(root).key;
    }
    private Node<K, V> minimum(Node<K, V> node) {
        while (node.left != nil) node = node.left;
        return node;
    }

    public K maximum() {
        return maximum(root).key;
    }
    private Node<K, V> maximum(Node<K, V> node) {
        while (node.right != nil) node = node.right;
        return node;
    }

    public K successor(K key) {
        Node<K, V> node = findNode(key);
        if (node == nil) return null;
        Node<K, V> succ = successor(node);
        return (succ == nil) ? null : succ.key;
    }
    private Node<K, V> successor(Node<K, V> node) {
        if (node.right != nil) return minimum(node.right);
        Node<K, V> p = node.parent;
        while (p != nil && node == p.right) {
            node = p;
            p = p.parent;
        }
        return p;
    }

    public K predecessor(K key) {
        Node<K, V> node = findNode(key);
        if (node == nil) return null;
        Node<K, V> pred = predecessor(node);
        return (pred == nil) ? null : pred.key;
    }
    private Node<K, V> predecessor(Node<K, V> node) {
        if (node.left != nil) return maximum(node.left);
        Node<K, V> p = node.parent;
        while (p != nil && node == p.left) {
            node = p;
            p = p.parent;
        }
        return p;
    }

    public List<K> orderedTraversal() {
        List<K> result = new ArrayList<>(size);
        inorder(root, result);
        return result;
    }

    private void inorder(Node<K, V> node, List<K> result) {
        if (node == nil) return;
        inorder(node.left, result);
        result.add(node.key);
        inorder(node.right, result);
    }

    public List<V> orderedValues() {
        List<V> result = new ArrayList<>(size);
        inorderValues(root, result);
        return result;
    }
    private void inorderValues(Node<K, V> node, List<V> result) {
        if (node == nil) return;
        inorderValues(node.left, result);
        result.add(node.value);
        inorderValues(node.right, result);
    }

    public List<Map.Entry<K, V>> rangeTraversal(K from, K to) {
        List<Map.Entry<K, V>> result = new ArrayList<>();
        rangeTraversal(root, from, to, result);
        return result;
    }

    private void rangeTraversal(Node<K, V> node, K from, K to, List<Map.Entry<K, V>> result) {
        if (node == nil) return;
        int cmpFrom = from.compareTo(node.key);
        int cmpTo = to.compareTo(node.key);
        if (cmpFrom < 0) rangeTraversal(node.left, from, to, result);
        if (cmpFrom <= 0 && cmpTo >= 0) result.add(new AbstractMap.SimpleEntry<>(node.key, node.value));
        if (cmpTo > 0) rangeTraversal(node.right, from, to, result);
    }

    public int getRotations() { return rotations; }
    public int getRecolors() { return recolors; }

    public boolean verifyInvariants() {
        return verifyInvariants(root);
    }

    private boolean verifyInvariants(Node<K, V> node) {
        if (node == nil) return true; // nil is black by convention
        if (node.color != Color.RED && node.color != Color.BLACK) return false;
        if (node == root && node.color != Color.BLACK) return false;
        if (node.color == Color.RED) {
            if (node.left.color == Color.RED || node.right.color == Color.RED) return false;
        }
        int leftBlackHeight = blackHeight(node.left);
        int rightBlackHeight = blackHeight(node.right);
        if (leftBlackHeight == MISMATCH || rightBlackHeight == MISMATCH || leftBlackHeight != rightBlackHeight) return false;
        return verifyInvariants(node.left) && verifyInvariants(node.right);
    }

    private static final int MISMATCH = Integer.MAX_VALUE;

    private int blackHeight(Node<K, V> node) {
        if (node == nil) return 1; // nil is black
        int left = blackHeight(node.left);
        int right = blackHeight(node.right);
        if (left == MISMATCH || right == MISMATCH || left != right) return MISMATCH;
        int bh = (node.color == Color.BLACK) ? 1 : 0;
        return bh + left;
    }

    public int getHeight() {
        return height(root);
    }
    private int height(Node<K, V> node) {
        if (node == nil) return 0;
        return 1 + Math.max(height(node.left), height(node.right));
    }
}
