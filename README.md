# MicroFS

MicroFS is a persistent Java filesystem simulator that uses workload-adaptive data structures for namespace indexing and storage management, with an experimental framework for measuring algorithmic tradeoffs.

## What it is

MicroFS simulates a filesystem over a virtual disk image (`disk.img`). It supports:

- Persistent disk format with superblock, inode table, bitmaps, and data blocks
- Multiple directory representations: Linear, Hash, RB-Tree, Hybrid, Adaptive
- Red-Black Tree, Trie, LRU cache (framework), block allocation framework
- CLI with core filesystem commands
- Benchmark engine and workload framework
- FSCK consistency checker

## What it investigates

Can a workload-adaptive combination of classical data structures reduce namespace lookup, path resolution, and storage-management overhead compared with fixed-structure baseline designs?

## Build

```bash
mvn compile
```

## Run

```bash
mvn compile exec:java -Dexec.mainClass="org.microfs.cli.MicrofsCLI" -Dexec.args="format disk.img"
```

Or interactively:

```bash
java -cp target/classes org.microfs.cli.MicrofsCLI
```

## Key commands

```text
format [image]
mount [image]
unmount
mkdir <path>
ls [path]
cd <path>
pwd
touch <file>
cat <file>
stat <path>
df
fsck [image]
benchmark
```

## Data structures

- **RedBlackTree**: genuine RB tree with insert/delete/rotations/recolor/min/max/successor/predecessor/invariant verification
- **Trie**: auxiliary prefix search
- **HashDirectory / LinearDirectory / RBTreeDirectory / HybridDirectory / AdaptiveDirectory**: directory indexing strategies
- **Bitmap**: persistent inode/block allocation bitmap

## Tests

```bash
mvn test
```

## Practical Performance

MicroFS includes a reproducible latency validation suite covering realistic namespace sizes and filesystem operations. Measurements include exact lookup, prefix search, path resolution, directory listing, file and directory creation probes, cold versus warm execution, and persistence mount/remount timing. Results are specific to the tested MicroFS implementation, JDK, environment, and workload; they are not general filesystem performance claims.

Run the final validation with the available JDK using:

```text
java -cp target/validation-classes org.microfs.benchmark.RealWorldValidation results/usability
```

See `results/usability/USABILITY_REPORT.md` and `results/usability/latency.csv` for the measured run. `find`, deletion, and operational `stat` are recorded as unavailable because the current CLI does not support them.

## Documentation

See `docs/` (or this directory) for:

- `ARCHITECTURE.md`
- `DATA_STRUCTURES.md`
- `ALGORITHMS.md`
- `BENCHMARKS.md`
- `CLI.md`
- `FILESYSTEM_FORMAT.md`

## Limitations

- Single-threaded core
- Not a replacement for NTFS/ext4/APFS
- Benchmark results are environment-specific
- Advanced features (journaling, symlinks, hard links) are deferred

## License

MIT (or as configured)
