# MicroFS Development

## Build

```bash
mvn compile
```

## Project structure

```
org.microfs.storage   - BlockDevice, Bitmap, Inode, InodeTable, FileType
org.microfs.dsa        - RedBlackTree, Trie, DirectoryEntry, DirectoryIndex, LinearDirectory, HashDirectory, RBTreeDirectory, HybridDirectory, AdaptiveDirectory
org.microfs.fs         - Superblock, Filesystem, PathResolver, Fsck
org.microfs.cli        - MicrofsCLI
org.microfs.benchmark  - BenchmarkEngine
org.microfs.debug      - Debug commands (planned)
```

## Development phases (completed)

1. Disk foundation (BlockDevice, Bitmap, Inode, InodeTable)
2. Data structures (RB Tree, Trie, Directory indices)
3. Filesystem layer (Superblock, Filesystem lifecycle)
4. CLI (MicrofsCLI)
5. Benchmark framework (BenchmarkEngine)
6. Documentation and tests

## Key design decisions

- **No GUI**: CLI and debug commands provide observability
- **No fake implementations**: Every method has real behavior or is explicitly deferred
- **No fake benchmarks**: All measurements come from actual execution
- **Custom DSA**: RB Tree and Trie are implemented directly; HashMap is used where it does not diminish the DSA contribution
- **Adaptive design**: Migration is rebuild-based for correctness; thresholds are configurable
- **Persistence**: Every write goes through `BlockDevice`; flush order is defined

## Adding new directory strategy

1. Implement `DirectoryIndex`
2. Add to `BenchmarkEngine.createStrategy()`
3. Document complexity in `DATA_STRUCTURES.md`
4. Add tests and benchmark comparison

## Adding new benchmark workload

1. Define deterministic workload file or generator
2. Add to `benchmark/` directory
3. Ensure reproducibility with seed

## Coding standards

- Meaningful names
- Small cohesive classes
- No silent exception swallowing
- Explicit invariants
- Defensive validation
- Deterministic tests
