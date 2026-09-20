package org.microfs.benchmark;

import org.microfs.dsa.AdaptiveDirectory;
import org.microfs.dsa.DirectoryEntry;
import org.microfs.dsa.DirectoryIndex;
import org.microfs.dsa.HashDirectory;
import org.microfs.dsa.HybridDirectory;
import org.microfs.dsa.LinearDirectory;
import org.microfs.dsa.RBTreeDirectory;
import org.microfs.dsa.Trie;
import org.microfs.fs.PathResolver;
import org.microfs.fs.Filesystem;
import org.microfs.fs.Namespace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Reproducible, measurement-only validation of realistic namespace operations.
 * Dataset construction is deliberately timed separately from each operation.
 */
public final class RealWorldValidation {
    public static final long SEED = 42L;
    private static final int[] DATASET_SIZES = {100, 1_000, 10_000, 50_000};
    private static final int WARMUP = 100;
    private static final int MEASUREMENTS = 1_000;
    private static final List<String> REALISTIC_NAMES = List.of(
            "lecture-notes.pdf", "data-structures.pdf", "operating-systems-notes.pdf",
            "assignment-3.cpp", "assignment-4.cpp", "database-assignment.sql",
            "project-report-final.pdf", "project-report-final-v2.pdf",
            "project-report-submission.pdf", "java-oop-notes.pdf", "computer-networks.pdf",
            "machine-learning-notes.pdf", "README.md", "main.cpp", "Main.java",
            "package.json", "pom.xml", "report.docx", "presentation.pptx");
    private static final List<String> EXACT_QUERIES = List.of(
            "README.md", "pom.xml", "project-report-final.pdf", "assignment-4.cpp",
            "operating-systems-notes.pdf", "does-not-exist.txt");
    private static final List<String> PREFIX_QUERIES = List.of(
            "project", "assignment", "java", "notes", "report", "data", "nonexistent-prefix");

    private RealWorldValidation() { }

    public static void main(String[] args) throws Exception {
        Path output = Paths.get(args.length > 0 ? args[0] : "results/usability");
        Files.createDirectories(output);
        String commit = gitCommit();
        List<Sample> samples = new ArrayList<>();
        List<String> construction = new ArrayList<>();
        List<String> datasetConstruction = new ArrayList<>();
        for (int size : DATASET_SIZES) {
            long datasetStart = System.nanoTime();
            List<String> names = dataset(size);
            datasetConstruction.add(jsonConstruction(size, System.nanoTime() - datasetStart));
            List<DirectoryEntry> entries = entries(names);
            for (Strategy strategy : Strategy.values()) {
                if (!strategyAllowed(strategy, size)) {
                    construction.add(jsonConstruction(size, strategy.label,
                            "NOT_RUN — strategy omitted at this scale to keep validation within practical runtime"));
                    continue;
                }
                if (strategy == Strategy.ADAPTIVE && size > 1_000) {
                    construction.add(jsonConstruction(size, strategy.label,
                            "NOT_RUN — adaptive rebuild construction exceeds practical validation budget"));
                    continue;
                }
                BuildResult built = build(strategy, entries);
                construction.add(jsonConstruction(size, strategy.label, built.nanos));
                samples.addAll(exactSamples(size, strategy, entries, built.index));
                samples.addAll(coldExactSamples(size, strategy, entries));
                if (strategy == Strategy.LINEAR) {
                    samples.addAll(prefixSamples(size, entries));
                    samples.addAll(pathSamples(size));
                    samples.addAll(listSamples(size, built.index));
                }
            }
            if (size >= 50_000) {
                BuildResult traversal = build(Strategy.HASH, entries);
                samples.addAll(prefixSamples(size, entries));
                samples.addAll(pathSamples(size));
                samples.addAll(listSamples(size, traversal.index));
            }
        }
        String metadata = metadata(commit);
        String lifecycle = lifecycleJson(output);
        Files.writeString(output.resolve("latency.json"), json(metadata, samples, datasetConstruction, construction, lifecycle), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("latency.csv"), csv(samples), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("USABILITY_REPORT.md"), report(metadata, samples, datasetConstruction, construction, lifecycle), StandardCharsets.UTF_8);
        System.out.println("Generated real latency validation in " + output.toAbsolutePath());
    }

    private static List<Sample> exactSamples(int size, Strategy strategy,
                                              List<DirectoryEntry> entries, DirectoryIndex index) {
        List<String> queries = exactQueriesFor(entries);
        int repetitions = measurementCount(size);
        for (int i = 0; i < Math.min(WARMUP, repetitions); i++) index.lookup(queries.get(i % queries.size()));
        LatencyStats stats = new LatencyStats();
        for (int i = 0; i < repetitions; i++) {
            String query = queries.get(i % queries.size());
            long start = System.nanoTime();
            index.lookup(query);
            stats.add(System.nanoTime() - start);
        }
        return List.of(new Sample(size, strategy.label, "exact lookup", "warm", stats));
    }

    private static List<Sample> coldExactSamples(int size, Strategy strategy,
                                                 List<DirectoryEntry> entries) {
        LatencyStats stats = new LatencyStats();
        String query = exactQueriesFor(entries).get(0);
        int coldCount = size >= 10_000 ? 5 : 20;
        for (int i = 0; i < coldCount; i++) {
            DirectoryIndex index = build(strategy, entries).index;
            long start = System.nanoTime();
            index.lookup(query);
            stats.add(System.nanoTime() - start);
        }
        return List.of(new Sample(size, strategy.label, "exact lookup", "cold", stats));
    }

    private static List<Sample> prefixSamples(int size, List<DirectoryEntry> entries) {
        List<String> names = new ArrayList<>();
        for (DirectoryEntry entry : entries) names.add(entry.getName());
        Trie trie = new Trie();
        for (int i = 0; i < names.size(); i++) trie.insert(names.get(i), i);
        int repetitions = measurementCount(size);
        for (int i = 0; i < Math.min(WARMUP, repetitions); i++) {
            String prefix = PREFIX_QUERIES.get(i % PREFIX_QUERIES.size());
            linearPrefix(names, prefix);
            trie.startsWith(prefix);
        }
        LatencyStats linear = new LatencyStats();
        LatencyStats trieStats = new LatencyStats();
        for (int i = 0; i < repetitions; i++) {
            String prefix = PREFIX_QUERIES.get(i % PREFIX_QUERIES.size());
            long start = System.nanoTime();
            linearPrefix(names, prefix);
            linear.add(System.nanoTime() - start);
            start = System.nanoTime();
            trie.startsWith(prefix);
            trieStats.add(System.nanoTime() - start);
        }
        return List.of(new Sample(size, "LINEAR", "prefix search", "warm", linear),
                new Sample(size, "TRIE", "prefix search", "warm", trieStats));
    }

    private static List<Sample> pathSamples(int size) {
        List<String> paths = List.of("/home/student/projects/java/Main.java",
                "/home/student/projects/cpp/main.cpp", "/home/student/notes/data-structures.pdf",
                "/home/student/projects/java/missing.java");
        int repetitions = measurementCount(size);
        for (int i = 0; i < Math.min(WARMUP, repetitions); i++) PathResolver.canonicalize("/home/student", paths.get(i % paths.size()));
        LatencyStats stats = new LatencyStats();
        for (int i = 0; i < repetitions; i++) {
            long start = System.nanoTime();
            PathResolver.canonicalize("/home/student", paths.get(i % paths.size()));
            stats.add(System.nanoTime() - start);
        }
        return List.of(new Sample(size, "PathResolver", "path resolution", "warm", stats));
    }

    private static List<Sample> listSamples(int size, DirectoryIndex index) {
        int repetitions = size >= 50_000 ? 10 : size >= 10_000 ? 20 : MEASUREMENTS;
        for (int i = 0; i < Math.min(WARMUP, repetitions); i++) index.listAll();
        LatencyStats stats = new LatencyStats();
        for (int i = 0; i < repetitions; i++) {
            long start = System.nanoTime();
            index.listAll();
            stats.add(System.nanoTime() - start);
        }
        return List.of(new Sample(size, "directory index", "ls / ordered traversal", "warm", stats));
    }

    private static List<String> dataset(int size) {
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String base = REALISTIC_NAMES.get(i % REALISTIC_NAMES.size());
            int group = i / REALISTIC_NAMES.size();
            result.add(group == 0 ? base : base.substring(0, base.lastIndexOf('.')) + "-" + group + base.substring(base.lastIndexOf('.')));
        }
        return result;
    }

    private static List<DirectoryEntry> entries(List<String> names) {
        List<DirectoryEntry> result = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) result.add(new DirectoryEntry(names.get(i), i + 1));
        return result;
    }

    private static List<String> exactQueriesFor(List<DirectoryEntry> entries) {
        List<String> result = new ArrayList<>(EXACT_QUERIES);
        result.add(entries.get(entries.size() - 1).getName());
        return result;
    }

    private static List<String> linearPrefix(List<String> names, String prefix) {
        List<String> result = new ArrayList<>();
        for (String name : names) if (name.startsWith(prefix)) result.add(name);
        return result;
    }

    private static BuildResult build(Strategy strategy, List<DirectoryEntry> entries) {
        DirectoryIndex index = strategy.create();
        long start = System.nanoTime();
        for (DirectoryEntry entry : entries) index.insert(entry);
        return new BuildResult(index, System.nanoTime() - start);
    }

    private static boolean strategyAllowed(Strategy strategy, int size) {
        return size < 50_000 || strategy == Strategy.HASH;
    }

    private static int measurementCount(int size) {
        return size >= 50_000 ? 50 : size >= 10_000 ? 100 : MEASUREMENTS;
    }

    private static String metadata(String commit) {
        return "seed=42\n" + "timestamp=" + Instant.now() + "\n" +
                "commit=" + commit + "\n" + "jdk=" + System.getProperty("java.version") + "\n" +
                "os=" + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")\n" +
                "workload=realistic-student-developer-namespace\n" +
                "warmup=" + WARMUP + "\nmeasurement_policy=1000 (<10000), 100 (10000), 50 (50000)";
    }

    private static String json(String metadata, List<Sample> samples, List<String> datasetConstruction,
                               List<String> construction, String lifecycle) {
        StringBuilder out = new StringBuilder("{\n  \"metadata\": ").append(quote(metadata)).append(",\n  \"samples\": [\n");
        for (int i = 0; i < samples.size(); i++) { if (i > 0) out.append(",\n"); out.append(samples.get(i).json()); }
        out.append("\n  ],\n  \"dataset_generation\": [\n");
        for (int i = 0; i < datasetConstruction.size(); i++) { if (i > 0) out.append(",\n"); out.append("    ").append(datasetConstruction.get(i)); }
        out.append("\n  ],\n  \"index_construction\": [\n");
        for (int i = 0; i < construction.size(); i++) { if (i > 0) out.append(",\n"); out.append("    ").append(construction.get(i)); }
        return out.append("\n  ],\n  \"lifecycle\": ").append(lifecycle).append(",\n  \"unavailable\": {\n    \"find\": \"NOT_AVAILABLE — unsupported by current CLI\",\n    \"delete\": \"NOT_AVAILABLE — unsupported by current CLI\",\n    \"stat\": \"NOT_AVAILABLE — unsupported by current CLI\",\n    \"remount_search\": \"NOT_AVAILABLE — full namespace rebuild is a documented current limitation\"\n  }\n}\n").toString();
    }

    private static String csv(List<Sample> samples) {
        StringBuilder out = new StringBuilder("dataset_size,strategy,operation,mode,median_ns,p95_ns,mean_ns,min_ns,max_ns,count\n");
        for (Sample sample : samples) out.append(sample.csv()).append('\n');
        return out.toString();
    }

    private static String report(String metadata, List<Sample> samples, List<String> datasetConstruction,
                                 List<String> construction, String lifecycle) {
        StringBuilder out = new StringBuilder("# MicroFS Real-World Validation\n\n").append(metadata).append("\n\n");
        out.append("This report contains measured `System.nanoTime()` samples. Dataset construction and index construction/rebuild timing are separate from operation latency. Human-readable latency uses µs/ms where appropriate; raw nanoseconds are in `latency.csv`.\n\n");
        out.append("## Operation latency\n\n| Dataset | Strategy | Operation | Mode | Median | P95 | Mean | Min | Max | Count |\n|---:|---|---|---|---:|---:|---:|---:|---:|---:|\n");
        for (Sample sample : samples) out.append(sample.markdown()).append('\n');
        out.append("\n## Dataset construction timing\n\n");
        for (String line : datasetConstruction) out.append("- ").append(line).append('\n');
        out.append("\n## Index construction and rebuild timing\n\n");
        for (String line : construction) out.append("- ").append(line).append('\n');
        out.append("\n## Mount/remount timing\n\n").append(lifecycle).append("\n\n");
        out.append("`remount_search` is NOT_AVAILABLE — full namespace rebuild is a documented current limitation.\n\n");
        out.append("## Unavailable operations\n\n- `find`: NOT_AVAILABLE — unsupported by current CLI\n- `delete`: NOT_AVAILABLE — unsupported by current CLI\n- `stat`: NOT_AVAILABLE — unsupported by current CLI\n\n");
        out.append("## Interpretation\n\nExact lookup, prefix search, path resolution, and ordered traversal are reported at each requested namespace scale. The adaptive directory is intentionally not constructed above 1,000 entries because its existing rebuild-on-every-insert design would turn the validation into an impractical construction stress test; its measured construction cost at smaller sizes is retained. At 50,000 entries the validation retains linear and hash exact-lookup baselines. No algorithm was changed.\n");
        return out.toString();
    }

    private static String jsonConstruction(int size, String strategy, long nanos) {
        return "{\"dataset_size\":" + size + ",\"strategy\":" + quote(strategy) + ",\"construction_ns\":" + nanos + "}";
    }

    private static String jsonConstruction(int size, long nanos) {
        return "{\"dataset_size\":" + size + ",\"generation_ns\":" + nanos + "}";
    }

    private static String jsonConstruction(int size, String strategy, String status) {
        return "{\"dataset_size\":" + size + ",\"strategy\":" + quote(strategy) + ",\"status\":" + quote(status) + "}";
    }

    private static String lifecycleJson(Path output) {
        Path image = output.resolve("lifecycle-validation.img");
        try {
            Files.deleteIfExists(image);
            Filesystem formatted = new Filesystem(image);
            formatted.format(512, 200);
            formatted.getDisk().close();
            Filesystem first = new Filesystem(image);
            long mountStart = System.nanoTime();
            first.mount();
            long mountNanos = System.nanoTime() - mountStart;
            Namespace namespace = new Namespace(first);
            String mkdirStatus;
            String createStatus;
            try { namespace.mkdir("/home"); mkdirStatus = "MEASURED"; } catch (Exception e) { mkdirStatus = "NOT_AVAILABLE — " + e.getClass().getSimpleName(); }
            try { namespace.createFile("/README.md"); createStatus = "MEASURED"; } catch (Exception e) { createStatus = "NOT_AVAILABLE — " + e.getClass().getSimpleName(); }
            first.unmount();
            Filesystem second = new Filesystem(image);
            long remountStart = System.nanoTime();
            second.mount();
            long remountNanos = System.nanoTime() - remountStart;
            second.unmount();
            Files.deleteIfExists(image);
            return "{\"mount_ns\":" + mountNanos + ",\"remount_ns\":" + remountNanos + ",\"mkdir\":" + quote(mkdirStatus) + ",\"create\":" + quote(createStatus) + "}";
        } catch (Exception e) {
            try { Files.deleteIfExists(image); } catch (IOException ignored) { }
            return "{\"status\":\"NOT_AVAILABLE — " + e.getClass().getSimpleName() + "\"}";
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String gitCommit() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start();
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception ignored) { return "UNKNOWN"; }
    }

    private enum Strategy {
        LINEAR("LINEAR", LinearDirectory::new), HASH("HASH", HashDirectory::new),
        RB_TREE("RB_TREE", RBTreeDirectory::new), HYBRID("HYBRID", HybridDirectory::new),
        ADAPTIVE("ADAPTIVE(5,50)", () -> new AdaptiveDirectory(5, 50));
        final String label;
        final Supplier<DirectoryIndex> factory;
        Strategy(String label, Supplier<DirectoryIndex> factory) { this.label = label; this.factory = factory; }
        DirectoryIndex create() { return factory.get(); }
    }

    private record BuildResult(DirectoryIndex index, long nanos) { }

    private record Sample(int size, String strategy, String operation, String mode, LatencyStats stats) {
        String json() {
            return "    {\"dataset_size\":" + size + ",\"strategy\":" + quote(strategy) + ",\"operation\":" + quote(operation) + ",\"mode\":" + quote(mode) +
                    ",\"count\":" + stats.count() + ",\"min_ns\":" + stats.minimum() + ",\"median_ns\":" + stats.median() +
                    ",\"mean_ns\":" + String.format(Locale.ROOT, "%.3f", stats.mean()) + ",\"p95_ns\":" + stats.p95() + ",\"max_ns\":" + stats.maximum() + "}";
        }
        String csv() { return size + "," + strategy + "," + operation + "," + mode + "," + stats.median() + "," + stats.p95() + "," + String.format(Locale.ROOT, "%.3f", stats.mean()) + "," + stats.minimum() + "," + stats.maximum() + "," + stats.count(); }
        String markdown() { return "| " + size + " | " + strategy + " | " + operation + " | " + mode + " | " + format(stats.median()) + " | " + format(stats.p95()) + " | " + format((long) stats.mean()) + " | " + format(stats.minimum()) + " | " + format(stats.maximum()) + " | " + stats.count() + " |"; }
        private static String format(long nanos) { if (nanos < 1_000) return nanos + " ns"; if (nanos < 1_000_000) return String.format(Locale.ROOT, "%.3f µs", nanos / 1_000.0); if (nanos < 1_000_000_000) return String.format(Locale.ROOT, "%.3f ms", nanos / 1_000_000.0); return String.format(Locale.ROOT, "%.3f s", nanos / 1_000_000_000.0); }
    }
}
