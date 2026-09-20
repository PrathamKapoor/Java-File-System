package org.microfs.dsa;

import java.util.*;

/**
 * Adaptive directory representation that switches based on size thresholds.
 */
public class AdaptiveDirectory implements DirectoryIndex {
    public enum Mode { SMALL, MEDIUM, LARGE }

    private Mode currentMode = Mode.SMALL;
    private final int smallThreshold;
    private final int mediumThreshold;
    private final List<DirectoryEntry> masterEntries = new ArrayList<>();
    private DirectoryIndex currentDir;

    public AdaptiveDirectory(int smallThreshold, int mediumThreshold) {
        if (smallThreshold < 1 || mediumThreshold <= smallThreshold) {
            throw new IllegalArgumentException("Thresholds must satisfy 1 <= small < medium");
        }
        this.smallThreshold = smallThreshold;
        this.mediumThreshold = mediumThreshold;
        this.currentDir = new LinearDirectory();
    }

    @Override
    public void insert(DirectoryEntry entry) {
        if (masterEntries.stream().anyMatch(e -> e.getName().equals(entry.getName()))) {
            throw new IllegalArgumentException("Duplicate entry: " + entry.getName());
        }
        masterEntries.add(entry);
        rebuildCurrent();
        migrateIfNeeded();
    }

    @Override
    public DirectoryEntry lookup(String name) {
        return currentDir.lookup(name);
    }

    @Override
    public boolean delete(String name) {
        boolean removed = masterEntries.removeIf(e -> e.getName().equals(name));
        if (removed) {
            rebuildCurrent();
            migrateIfNeeded();
        }
        return removed;
    }

    @Override
    public List<DirectoryEntry> listAll() {
        return currentDir.listAll();
    }

    @Override
    public int size() {
        return masterEntries.size();
    }

    @Override
    public boolean isEmpty() {
        return masterEntries.isEmpty();
    }

    @Override
    public String getRepresentation() {
        return "ADAPTIVE(" + currentMode + ")";
    }

    private void rebuildCurrent() {
        DirectoryIndex newDir;
        switch (currentMode) {
            case SMALL: newDir = new LinearDirectory(); break;
            case MEDIUM: newDir = new HashDirectory(); break;
            case LARGE: newDir = new HybridDirectory(); break;
            default: throw new IllegalStateException();
        }
        for (DirectoryEntry e : masterEntries) {
            newDir.insert(e);
        }
        currentDir = newDir;
    }

    private void migrateIfNeeded() {
        int s = size();
        Mode target;
        if (s == 0) target = Mode.SMALL;
        else if (s < smallThreshold) target = Mode.SMALL;
        else if (s < mediumThreshold) target = Mode.MEDIUM;
        else target = Mode.LARGE;

        if (target == currentMode) return;
        currentMode = target;
        rebuildCurrent();
    }

    public Mode getCurrentMode() { return currentMode; }
    public int getSmallThreshold() { return smallThreshold; }
    public int getMediumThreshold() { return mediumThreshold; }
}
