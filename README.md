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
