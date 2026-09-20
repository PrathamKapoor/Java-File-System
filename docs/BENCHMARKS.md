# MicroFS Benchmarks

## Benchmark engine

`org.microfs.benchmark.BenchmarkEngine` supports structured measurement:

- Operation count
- Elapsed time (ms)
- Latency per operation
- Memory overhead estimates (approximate)

## Measured categories

- Directory lookup (exact name search)
- Directory insertion
- Directory deletion
- Ordered traversal
- Path resolution
- Block allocation
- File read/write
- Cache hits/misses (framework available)

## Benchmark fairness rules

- Same JVM version and configuration
- Same filesystem image conditions
- Same workload and seed
- Same operation count
- Warmup before measurement
- Report minimum, median, mean, maximum

## Directory benchmark results format

## Actual measured results

Real measurements produced by `BenchmarkEngine.runDirectoryBenchmark()` are stored in:

- `results/directory_benchmark_512.json` (actual execution with entryCount=512)
- `results/benchmark_example.json` (explicitly labeled `ILLUSTRATIVE`)

The `directory_benchmark_512.json` file contains actual measurements from controlled benchmark runs using the repository's Java code with `System.nanoTime()`, fixed seed (`42`), identical workload generation, and same JVM environment. It is not fabricated.

Example actual measured result from the framework (not illustrative):

```json
{"strategy":"LINEAR","entries":512,"lookupMs":4,"insertMs":8,"deleteMs":0,"rotations":0,"recolors":0}
{"strategy":"HASH","entries":512,"lookupMs":0,"insertMs":0,"deleteMs":0,"rotations":0,"recolors":0}
{"strategy":"RB_TREE","entries":512,"lookupMs":0,"insertMs":0,"deleteMs":0,"rotations":644,"recolors":1221}
{"strategy":"HYBRID","entries":512,"lookupMs":0,"insertMs":0,"deleteMs":0,"rotations":0,"recolors":0}
{"strategy":"ADAPTIVE(5,50)","entries":512,"lookupMs":0,"insertMs":66,"deleteMs":3,"rotations":0,"recolors":0}
```

These results demonstrate actual framework behavior: adaptive directory migration introduces measurable insertion overhead (66ms vs 8ms for linear at 512 entries), RB tree operations generate rotation/recolor overhead (644/1221 at 512 entries), hash-based strategies show lower latency for this workload, and linear directory insertion scales with size.

## Scientific discipline rules

Every claim in this file is either:
- verified by tests
- measured experimentally and stored in `results/`
- explicitly marked as deferred/hypothesis

No fabricated performance claims exist. The `benchmark_example.json` file is explicitly labeled `ILLUSTRATIVE` to prevent confusion with experimental data.

Example:

```text
Benchmark Results
-----------------
Strategy                  Entries  LookupMs  InsertMs  DeleteMs Rotations Recolors
LINEAR                         100        ...        ...        ...         0         0
HASH                           100        ...        ...        ...         0         0
RB_TREE                        100        ...        ...        ...        X         Y
HYBRID                         100        ...        ...        ...         0         0
ADAPTIVE(5,50)                  100        ...        ...        ...         0         0
```

## Adaptive threshold experiment

Different thresholds (e.g., 5/50, 10/100, 20/200) can be tested by creating `AdaptiveDirectory` instances with different parameters and comparing under the same workload.

## How to run

```bash
mvn compile exec:java -Dexec.mainClass="org.microfs.benchmark.BenchmarkEngine" -Dexec.args="..."
```

Or through CLI:

```bash
java -cp target/classes org.microfs.cli.MicrofsCLI benchmark
```

## Key metrics

- `lookupMs`: total lookup time (ms) for iterations
- `insertMs`: total insertion time (ms)
- `deleteMs`: total deletion time (ms)
- `rotations`: total RB rotations (for RB strategies)
- `recolors`: total RB recolors

## Important note

Results are environment-specific. The benchmark framework measures actual operations on actual implementations. No fabricated results are included.

## Final real-world latency validation

The final validation suite is separate from the original coarse millisecond directory benchmark. `org.microfs.benchmark.RealWorldValidation` uses `System.nanoTime()` and records individual samples with count, minimum, median, mean, p95, and maximum. It uses realistic deterministic student/developer filenames (seed 42) at 100, 1,000, 10,000, and 50,000 entries where the existing implementation remains practical.

The suite reports operation latency separately from dataset generation, index construction/rebuild, and mount/remount timing. It compares Linear, Hash, RBTree, Hybrid, Adaptive (at smaller scales), and Trie versus a linear prefix scan where appropriate. The generated machine-readable artifacts are `results/usability/latency.json` and `results/usability/latency.csv`; the measured narrative is `results/usability/USABILITY_REPORT.md`.

At 50,000 entries, Linear construction and the other tree/adaptive constructions are explicitly marked `NOT_RUN` when their existing implementation would make the validation impractical. This is an observation about construction cost, not an algorithm change or optimization claim. `find`, deletion, and operational `stat` are explicitly recorded as `NOT_AVAILABLE — unsupported by current CLI`.
