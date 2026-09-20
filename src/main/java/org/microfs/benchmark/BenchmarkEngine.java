package org.microfs.benchmark;

import org.microfs.dsa.*;
import java.util.*;

/**
 * Benchmark engine for directory strategies.
 */
public class BenchmarkEngine {

    public static class BenchmarkResult {
        public final String strategy;
        public final int entries;
        public final long lookupMs;
        public final long insertMs;
        public final long deleteMs;
        public final int rotations;
        public final int recolors;

        public BenchmarkResult(String strategy, int entries, long lookupMs, long insertMs,
                                 long deleteMs, int rotations, int recolors) {
            this.strategy = strategy;
            this.entries = entries;
            this.lookupMs = lookupMs;
            this.insertMs = insertMs;
            this.deleteMs = deleteMs;
            this.rotations = rotations;
            this.recolors = recolors;
        }
    }

    public static List<BenchmarkResult> runDirectoryBenchmark(int entryCount, int iterations) {
        List<BenchmarkResult> results = new ArrayList<>();
        Random rand = new Random(42);

        String[] strategies = {"LINEAR", "HASH", "RB_TREE", "HYBRID", "ADAPTIVE(5,50)"};
        for (String strategy : strategies) {
            DirectoryIndex index = createStrategy(strategy);
            long startInsert = System.nanoTime();
            for (int i = 0; i < entryCount; i++) {
                index.insert(new DirectoryEntry("entry" + i, i));
            }
            long insertMs = (System.nanoTime() - startInsert) / 1_000_000;

            long startLookup = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                index.lookup("entry" + rand.nextInt(entryCount));
            }
            long lookupMs = (System.nanoTime() - startLookup) / 1_000_000;

            long startDelete = System.nanoTime();
            for (int i = 0; i < Math.min(entryCount / 10, 10); i++) {
                index.delete("entry" + i);
            }
            long deleteMs = (System.nanoTime() - startDelete) / 1_000_000;

            int rotations = 0;
            int recolors = 0;
            if (index instanceof RBTreeDirectory) {
                rotations = ((RBTreeDirectory) index).getTree().getRotations();
                recolors = ((RBTreeDirectory) index).getTree().getRecolors();
            }

            results.add(new BenchmarkResult(strategy, entryCount, lookupMs, insertMs, deleteMs, rotations, recolors));
        }
        return results;
    }

    private static DirectoryIndex createStrategy(String name) {
        switch (name) {
            case "LINEAR": return new LinearDirectory();
            case "HASH": return new HashDirectory();
            case "RB_TREE": return new RBTreeDirectory();
            case "HYBRID": return new HybridDirectory();
            case "ADAPTIVE(5,50)": return new AdaptiveDirectory(5, 50);
            default: return new LinearDirectory();
        }
    }

    public static void printResults(List<BenchmarkResult> results) {
        System.out.println("Benchmark Results");
        System.out.println("-----------------");
        System.out.printf("%-20s %8s %10s %10s %10s %10s %10s%n",
                "Strategy", "Entries", "LookupMs", "InsertMs", "DeleteMs", "Rotations", "Recolors");
        for (BenchmarkResult r : results) {
            System.out.printf("%-20s %8d %10d %10d %10d %10d %10d%n",
                    r.strategy, r.entries, r.lookupMs, r.insertMs, r.deleteMs, r.rotations, r.recolors);
        }
    }
}
