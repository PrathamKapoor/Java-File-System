package org.microfs.benchmark;

import java.util.*;

/**
 * Workload generator for controlled filesystem experiments.
 */
public class WorkloadGenerator {

    public enum WorkloadClass {
        SMALL_FILE,
        LARGE_FILE,
        METADATA_HEAVY,
        READ_HEAVY,
        WRITE_HEAVY,
        MIXED,
        RANDOM,
        DEVELOPER
    }

    public static class WorkloadOperation {
        public final String type; // CREATE, LOOKUP, READ, WRITE, DELETE, RENAME, LIST, PREFIX
        public final String path;
        public final int dataSize; // for WRITE
        public final int seed;

        public WorkloadOperation(String type, String path, int dataSize) {
            this.type = type;
            this.path = path;
            this.dataSize = dataSize;
            this.seed = 42;
        }
    }

    public static List<WorkloadOperation> generateWorkload(WorkloadClass wcls, int size, int seed) {
        List<WorkloadOperation> ops = new ArrayList<>();
        Random rand = new Random(seed);
        switch (wcls) {
            case SMALL_FILE:
                for (int i = 0; i < size; i++) {
                    ops.add(new WorkloadOperation("CREATE", "/file" + i + ".txt", 0));
                    if (i % 2 == 0) ops.add(new WorkloadOperation("LOOKUP", "/file" + i + ".txt", 0));
                }
                break;
            case LARGE_FILE:
                ops.add(new WorkloadOperation("CREATE", "/large.bin", 4096));
                ops.add(new WorkloadOperation("READ", "/large.bin", 4096));
                ops.add(new WorkloadOperation("WRITE", "/large.bin", 8192));
                break;
            case METADATA_HEAVY:
                for (int i = 0; i < size; i++) {
                    ops.add(new WorkloadOperation("CREATE", "/meta" + i, 0));
                    if (i % 3 == 0) ops.add(new WorkloadOperation("DELETE", "/meta" + i, 0));
                }
                break;
            case READ_HEAVY:
                for (int i = 0; i < size; i++) {
                    ops.add(new WorkloadOperation("LOOKUP", "/file" + (i % 10) + ".txt", 0));
                }
                break;
            case WRITE_HEAVY:
                for (int i = 0; i < size; i++) {
                    ops.add(new WorkloadOperation("WRITE", "/w" + i + ".txt", 512));
                }
                break;
            case MIXED:
                for (int i = 0; i < size; i++) {
                    int r = rand.nextInt(10);
                    if (r < 3) ops.add(new WorkloadOperation("CREATE", "/m" + i, 0));
                    else if (r < 6) ops.add(new WorkloadOperation("LOOKUP", "/m" + (i % 10), 0));
                    else ops.add(new WorkloadOperation("DELETE", "/m" + (i - 10), 0));
                }
                break;
            case DEVELOPER:
                ops.add(new WorkloadOperation("CREATE", "/project/src/Main.java", 0));
                ops.add(new WorkloadOperation("CREATE", "/project/src/App.java", 0));
                ops.add(new WorkloadOperation("WRITE", "/project/src/Main.java", 1024));
                ops.add(new WorkloadOperation("READ", "/project/src/Main.java", 1024));
                ops.add(new WorkloadOperation("LOOKUP", "/project/src/App.java", 0));
                ops.add(new WorkloadOperation("DELETE", "/project/src/App.java", 0));
                break;
            case RANDOM:
                for (int i = 0; i < size; i++) {
                    String name = "/r" + rand.nextInt(20);
                    int op = rand.nextInt(4);
                    if (op == 0) ops.add(new WorkloadOperation("CREATE", name, 0));
                    else if (op == 1) ops.add(new WorkloadOperation("LOOKUP", name, 0));
                    else if (op == 2) ops.add(new WorkloadOperation("WRITE", name, 256));
                    else ops.add(new WorkloadOperation("DELETE", name, 0));
                }
                break;
        }
        return ops;
    }
}
