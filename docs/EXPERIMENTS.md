# MicroFS Experimental Methodology

## Purpose

MicroFS is an experimental framework for investigating workload-adaptive filesystem data structures.

Every claim in this document is either:

* verified by tests
* measured experimentally on the repository's benchmark framework
* explicitly marked as deferred or hypothesized

No fabricated benchmark results are presented.

## Scientific Rules

Every optimization or data-structure choice must answer:

> What filesystem operation does this improve, and is the improvement measurable?

Every benchmark must control:

* same workload
* same seed
* same directory size
* same JVM environment
* same measurement method

Only the evaluated strategy/implementation changes between comparisons.

## Implemented Baselines

The following directory representations are implemented and benchmarked:

* `LinearDirectory` — sorted array, exact lookup by linear scan
* `HashDirectory` — `HashMap`-based exact lookup
* `RBTreeDirectory` — custom Red-Black Tree with invariant verification
* `HybridDirectory` — hash index + RB tree combined
* `AdaptiveDirectory` — configurable threshold-based switching (SMALL -> Hash, MEDIUM -> Hybrid, LARGE -> Hybrid)

## Benchmark Framework

`BenchmarkEngine` measures:

* insertion latency (ms)
* lookup latency (ms)
* deletion latency (ms)
* RB tree rotations/recolors (instrumentation)
* deterministic seed (42 for reproducibility)

Methodology:

1. Create directory index instance.
2. Insert `entryCount` entries.
3. Measure lookup iterations (`iterations` parameter).
4. Measure deletion of subset.
5. Report mean/median/min/max where applicable.

Warmup and measurement are included in the framework.

## Controlled Workloads

`WorkloadGenerator` produces deterministic operation sequences:

* `SMALL_FILE`
* `LARGE_FILE`
* `METADATA_HEAVY`
* `READ_HEAVY`
* `WRITE_HEAVY`
* `MIXED`
* `RANDOM`
* `DEVELOPER`

Each workload uses a fixed seed (`42`) and produces the same operations on repeated runs.

## Adaptive Directory Policy

The `AdaptiveDirectory` switches representation based on configurable thresholds.

Example tested policy (`AdaptiveDirectory(5, 50)`):

* `n < 5` -> Linear
* `5 <= n < 50` -> Hash
* `n >= 50` -> Hybrid

The benchmark framework supports comparing different threshold configurations.

Rebuild cost is measured by comparing insertion time before/after migration events.

The adaptive directory does not persist its representation choice; it rebuilds the index from master entries whenever the mode changes or mutations occur.

This is a correctness-first design.

## Trie Experiments

`Trie.search()` and `Trie.startsWith()` are implemented.

Benchmark comparison:

* `linear scan` — scan directory entries sequentially
* `trie` — traverse prefix index

Measured metrics:

* prefix length
* namespace size
* number of matches
* search latency
* memory overhead (approximate)

The Trie is treated as an auxiliary index, not authoritative persistent state.

## Cache Experiments

The `LRUBlockCache` framework exists but is not fully integrated in this phase.

If cache experiments are completed, they must measure:

* hit rate
* miss rate
* disk reads
* disk writes
* path lookup latency
* eviction count

Without full integration, this remains deferred.

## Allocation Experiments

`BlockAllocator` framework and `Bitmap` allocation exist.

Measured metrics (where implemented):

* allocation latency
* free latency
* fragmentation (number of free regions, average region size)
* sequential locality
* metadata overhead

The framework supports comparing:

* `BitmapFirstFit`
* potential future strategies (`NextFit`, `BestFit`, `Extent`)

Only implemented strategies are compared.

## Performance Metrics Definitions

Every metric is defined explicitly:

* `lookup latency`: time for exact name lookup across iterations
* `insert latency`: time to insert entries
* `delete latency`: time to delete subset
* `rebuild time`: time to rebuild index (adaptive migration)
* `rotations/recolors`: RB tree instrumentation counts
* `fragmentation ratio`: `(largest contiguous free region) / (total free blocks)`
* `hit rate`: `(cache hits) / (cache lookups)`

No vague "performance score" is defined.

## Environment Documentation

Every benchmark file must include:

* timestamp
* JVM version
* OS
* repository commit (if available)
* filesystem configuration (`blockSize`, `diskSize`)
* workload description and seed
* directory size
* operation count
* implementation names
* cache/allocation configuration
* result format version

The `results/benchmark_example.json` is explicitly labeled as illustrative (`ILLUSTRATIVE — NOT EXPERIMENTAL DATA`) and is not presented as an experimental result.

Actual experimental results are stored in `results/directory_benchmark_512.json` and generated from real benchmark engine executions.

## Scientific Discipline

Every conclusion is bounded:

* No universal speed claims against production filesystems.
* All comparisons are between implemented MicroFS structures and controlled workloads.
* Any inconclusive or noisy result is reported as inconclusive.
* Any deferred feature is explicitly marked.
* Any future optimization is marked as hypothesis or deferred work.

## Reproducibility Instructions

1. Clone repository.
2. `mvn clean compile`
3. `java -cp target/classes org.microfs.benchmark.BenchmarkEngine`
4. `java -cp target/classes org.microfs.benchmark.ExperimentRunner`
5. Inspect `results/*.json`
6. Inspect `workloads/*.workload`
7. Inspect `docs/EXPERIMENTS.md` (this file)

Results must use the same seed (`42`) for deterministic comparison.

## Final real-world latency validation

The final phase measures realistic namespace behavior rather than only aggregate directory totals. `RealWorldValidation` generates deterministic developer/student-like filenames at 100, 1,000, 10,000, and 50,000 entries, then measures exact lookup, prefix search, path canonicalization, ordered listing, and first-operation versus warmed latency with `System.nanoTime()`. Prefix search is compared as a linear scan and Trie traversal; exact lookup is compared across the existing directory representations where construction is practical.

Each sample retains raw nanoseconds and reports count, minimum, median, mean, p95, and maximum. Dataset generation, index construction/rebuild, and filesystem mount/remount are separate measurements. The persistence probe records actual mount/remount timing and the status of supported `mkdir`/`createFile` calls. Full post-remount namespace search is marked unavailable because complete namespace reconstruction is a documented current limitation. `find`, deletion, and real `stat` are marked unavailable because the current CLI does not implement them.

Generated artifacts from an actual run are stored under `results/usability/`. The CSV is suitable for plotting without additional infrastructure. No algorithm was changed for this phase, and no timing values are illustrative.
