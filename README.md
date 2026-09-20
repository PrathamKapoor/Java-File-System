# MicroFS

MicroFS is a persistent Java filesystem simulator that uses workload-adaptive data structures for namespace indexing and storage management, with an experimental framework for measuring algorithmic tradeoffs.

## What it is

MicroFS simulates a filesystem over a virtual disk image (`disk.img`). It supports:

- Persistent disk format with superblock, inode table, bitmaps, and data blocks
- Multiple directory representations: Linear, Hash, RB-Tree, Hybrid, Adaptive
- Implemented core data structures: Red-Black Tree, Trie, persistent Bitmaps, directory strategies
- Standalone / partial frameworks: LRU cache (framework interface), block allocation framework
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

The interactive CLI supports these commands:

| Command | Purpose |
|---|---|
| `format [image]` | Create a new virtual disk image. Optional `--block-size` and `--blocks` options configure its geometry. |
| `mount [image]` | Mount an existing virtual disk image. |
| `unmount` | Flush filesystem metadata and unmount the current image. |
| `mkdir <path>` | Create a directory at the supplied path. |
| `ls [path]` | List entries in a directory; defaults to the current directory. |
| `cd [path]` | Change the shell's current directory; defaults to `/`. |
| `pwd` | Print the current directory. |
| `touch [file]` | Create a file inode; defaults to `/test.txt`. |
| `cat [file]` | Invoke the file-display command; defaults to `/test.txt`. |
| `stat [path]` | Invoke the metadata-display command; defaults to `/`. |
| `df` | Invoke the filesystem usage command. |
| `fsck [image]` | Run the read-only filesystem consistency-check command. |
| `benchmark` | Select the benchmark command entry point. |
| `exit` / `quit` | Leave the interactive shell. |

The command names above are the commands currently registered by `MicrofsCLI`; advanced operations such as `find`, deletion, and a fully populated `stat` output are not implemented in this release.

## Data structures

### Implemented
- **RedBlackTree**: Genuine self-balancing Red-Black Tree with insertion, deletion, rotations, recoloring, min/max, successor/predecessor, and invariant verification.
- **Trie**: Character-level Trie for auxiliary prefix searching and namespace autocompletion.
- **Bitmap**: Persistent inode and data block allocation bitmaps with bitwise scan-and-set and disk synchronization.
- **Directory Indexing Strategies**:
  - `LinearDirectory`: Unsorted list baseline ($O(n)$ search).
  - `HashDirectory`: Hash table indexing for fast exact lookups ($O(1)$ average).
  - `RBTreeDirectory`: Ordered tree index supporting range queries and ordered listings ($O(\log n)$).
  - `HybridDirectory`: Dual index combining hash table lookups with trie prefix searches.
  - `AdaptiveDirectory`: Dynamically transitions between Linear, Hash, and RB-Tree based on entry count and query patterns.

### Framework / Partial
- **LRU Cache**: Interface and standalone eviction cache framework; ready for integration into a future page/buffer cache layer, but not part of the primary disk runtime.
- **Block Allocation Strategies**: Basic bitmap allocation integrated into disk formatting and file storage; extensible framework provided for pluggable allocation policies.

## Tests

```bash
mvn test
```

## Practical Performance & Key Benchmark Results

MicroFS includes a reproducible latency validation suite covering realistic namespace sizes and filesystem operations (`results/usability/USABILITY_REPORT.md`, measured on JDK 25, Windows 11):

| Workload / Operation | Scale (Entries) | Strategy | Warm Median Latency | Key Observation / DSA Takeaway |
|---|---:|---|---:|---|
| Exact lookup | 50,000 | Hash | ~100 ns | Near-instant constant-time ($O(1)$) retrieval at scale |
| Exact lookup baseline | 10,000 | Linear | 28.0 µs | Linear scan exhibits severe latency degradation ($O(n)$) |
| Prefix search comparison | 50,000 | Linear vs. Trie | Linear: ~715 µs<br>Trie: ~1.79 ms | **Teaching point:** Trie was slower than linear prefix search due to pointer hopping & cache misses vs contiguous array scanning |
| Ordered directory listing | 50,000 | RB-Tree | ~44.8 ms | Full in-order traversal yields sorted output without external sort pass |
| Dynamic index construction | 1,000 | Adaptive | ~197.6 ms | Rebuilding representations during scale transition incurs measurable construction cost |

> **Key DSA Insight:** While asymptotic theory predicts that a Trie should outperform linear scanning for prefix search, hardware cache locality dominates at short prefix lengths. Iterating contiguous byte arrays in L1/L2 cache outpaces node-by-node pointer traversal across scattered heap-allocated Trie nodes.

Run the final validation suite using:

```text
java -cp target/validation-classes org.microfs.benchmark.RealWorldValidation results/usability
```

See `results/usability/latency.csv` and `results/usability/latency.json` for full raw measurement logs.

## Documentation

Comprehensive technical documentation is consolidated in a single specification:

- [Technical Specification & Functionality Guide](file:///C:/Projects/ADS/docs/FUNCTIONALITY.md) — Complete coverage of system architecture, on-disk format, data structures, algorithms, CLI reference, and empirical benchmark findings.

## Limitations

- Single-threaded core
- Not a replacement for production filesystems (NTFS/ext4/APFS)
- Benchmark results are environment-specific
- Advanced features (journaling, symlinks, hard links) are deferred

## License

MIT (or as configured)
