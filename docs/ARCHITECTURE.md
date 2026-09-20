# MicroFS Architecture

## Layer overview

```
CLI (MicrofsCLI)
  ↓
Filesystem (org.microfs.fs.Filesystem)
  ↓
Path Resolver (org.microfs.fs.PathResolver)
  ↓
Namespace / Directory Index (org.microfs.dsa.DirectoryIndex)
  ↓
Inode Layer (org.microfs.storage.Inode / InodeTable)
  ↓
Storage Manager (org.microfs.fs.Superblock / Bitmap)
  ↓
Block Allocator (BlockDevice / Bitmap)
  ↓
Virtual Disk (disk.img)
```

## Components

- **BlockDevice**: fixed-block virtual disk over a file, little-endian, header + block array
- **Superblock**: persistent filesystem metadata at block 1
- **Bitmap**: persistent bit allocation image for inodes and data blocks
- **InodeTable**: fixed-size inode slots persisted in disk region
- **Inode**: 64-byte on-disk structure (type, permissions, link count, size, mtime, direct blocks, indirect block, extent length)
- **DirectoryIndex**: interface implemented by Linear, Hash, RBTree, Hybrid, Adaptive
- **RedBlackTree**: genuine RB tree with invariant verification and instrumentation
- **Trie**: auxiliary prefix search, never authoritative for persistent namespace
- **Cache framework**: LRU block/path cache interfaces (framework implemented where appropriate)
- **Filesystem**: mount/unmount lifecycle, superblock validation, bitmap management
- **PathResolver**: tokenizes paths, handles `.`, `..`, absolute/relative resolution
- **MicrofsCLI**: interactive shell and command dispatcher
- **Fsck**: consistency checker for superblock, bitmaps, inode references
- **BenchmarkEngine**: structured benchmark framework with JSON/CSV output support

## Data flow

```
mkdir /home/user
  → resolve path → find parent inode
  → allocate inode (bitmap)
  → initialize directory inode
  → add entry to parent directory index
  → persist changes (flush)
```

## Persistence

Every persistent component writes explicitly through `BlockDevice`.
Flush order on unmount:

1. Inode table (`flush`)
2. Bitmaps (`flush`)
3. Superblock (`write`)
4. Disk (`close` with `force`)

## Separation of concerns

- Storage layer (`org.microfs.storage`) knows nothing about directories
- DSA layer (`org.microfs.dsa`) knows nothing about disk
- Filesystem layer (`org.microfs.fs`) connects them but does not implement DSA algorithms directly
- CLI layer (`org.microfs.cli`) handles user interaction and delegates to filesystem

## Adaptive design

`AdaptiveDirectory` monitors size thresholds. When crossing thresholds, it rebuilds the active directory index from a master entry list and switches mode (SMALL=Linear, MEDIUM=Hash, LARGE=Hybrid). Migration preserves all entries and inode references.
