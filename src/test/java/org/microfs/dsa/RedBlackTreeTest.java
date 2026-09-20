package org.microfs.dsa;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RedBlackTreeTest {

    @Test
    void testInsertSearchDelete() {
        RedBlackTree<String, Integer> tree = new RedBlackTree<>();
        tree.insert("a", 1);
        tree.insert("b", 2);
        tree.insert("c", 3);
        assertEquals(1, tree.search("a"));
        assertEquals(2, tree.search("b"));
        assertEquals(3, tree.search("c"));
        assertNull(tree.search("d"));
    }

    @Test
    void testOrderedTraversal() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        tree.insert(3, "c");
        tree.insert(1, "a");
        tree.insert(2, "b");
        List<Integer> ordered = tree.orderedTraversal();
        assertEquals(Arrays.asList(1, 2, 3), ordered);
    }

    @Test
    void testInvariants() {
        RedBlackTree<Integer, String> tree = new RedBlackTree<>();
        Random rand = new Random(42);
        for (int i = 0; i < 100; i++) {
            tree.insert(rand.nextInt(1000), "v" + i);
        }
        assertTrue(tree.verifyInvariants(), "RB invariants violated");
    }

    @Test
    void testDelete() {
        RedBlackTree<String, Integer> tree = new RedBlackTree<>();
        tree.insert("a", 1);
        tree.insert("b", 2);
        assertEquals(1, tree.delete("a"));
        assertNull(tree.search("a"));
        assertNotNull(tree.search("b"));
    }
}
