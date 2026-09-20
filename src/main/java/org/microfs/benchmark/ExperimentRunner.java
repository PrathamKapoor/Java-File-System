package org.microfs.benchmark;

import org.microfs.dsa.*;
import java.nio.file.*;
import java.time.Instant;

public class ExperimentRunner {

    public static void runExperiment(String experimentName, int entryCount, int operations, long seed) throws Exception {
        System.out.println("Running experiment: " + experimentName);
        System.out.println("Entries: " + entryCount + ", Operations: " + operations + ", Seed: " + seed);

        java.util.List<BenchmarkEngine.BenchmarkResult> benchResults = BenchmarkEngine.runDirectoryBenchmark(entryCount, Math.max(operations / 10, 100));
        for (BenchmarkEngine.BenchmarkResult r : benchResults) {
            System.out.println(r.strategy + ": entries=" + r.entries +
                " lookupMs=" + r.lookupMs +
                " insertMs=" + r.insertMs +
                " deleteMs=" + r.deleteMs);
        }
        System.out.println("Experiment complete: " + experimentName);
    }

    public static void main(String[] args) throws Exception {
        String[] experiments = {
            "directory_lookup_128",
            "directory_lookup_256",
            "directory_lookup_512",
            "directory_lookup_1024"
        };
        for (String exp : experiments) {
            int size = Integer.parseInt(exp.substring(exp.lastIndexOf('_') + 1));
            runExperiment(exp, size, size * 10, 42);
        }
        System.out.println("=== ALL EXPERIMENTS COMPLETE ===");
    }
}
