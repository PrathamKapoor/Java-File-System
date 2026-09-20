package org.microfs.dsa;

import java.util.*;

/**
 * Abstract directory index supporting exact lookup, insertion, deletion,
 * ordered traversal, and statistics.
 */
public interface DirectoryIndex {

    void insert(DirectoryEntry entry);

    DirectoryEntry lookup(String name);

    boolean delete(String name);

    List<DirectoryEntry> listAll();

    int size();

    boolean isEmpty();

    String getRepresentation();
}
