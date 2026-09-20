# MicroFS Technical Specification & Functionality Guide

This document provides the complete, consolidated technical specification of **MicroFS**: architecture, on-disk format, data structures, algorithms, filesystem operations, CLI commands, benchmark results, and testing procedures.

---

## 1. Overview & Research Focus

MicroFS is a persistent Java filesystem simulator running over a virtual block device (`disk.img`). It explores:

> **Core Research Question:**  
> *Can a workload-adaptive combination of classical data structures reduce namespace lookup, path resolution, and storage-management overhead compared with fixed-structure baseline designs?*

### Key Capabilities
- **On-Disk Persistence**: Structured binary layout with superblock, inode table, allocation bitmaps, and data blocks.
- **Pluggable & Adaptive Directory Indexing**: Dynamically transitions between Linear, Hash, and Red-Black Tree indexes based on entry volume.
- **Algorithmic Integrity**: Custom, self-balancing Red-Black Tree with invariant verification; auxiliary Trie for prefix search; bitwise persistent allocation bitmaps.
- **Robust Filesystem Tools**: Interactive CLI shell, FSCK read-only consistency checker, and reproducible latency benchmark suite.

---

## 2. System Architecture & Layering

MicroFS is organized into distinct layers with strict separation of concerns:

```
┌────────────────────────────────────────────────────────┐
│                   CLI (MicrofsCLI)                     │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│          Filesystem Layer (Filesystem, Namespace)      │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
┌─────────────▼──────────────┐ ┌───────────▼─────────────┐
│  PathResolver & Directory  │ │ FileStorage (Direct /   │
│  Index (org.microfs.dsa)   │ │ Indirect Block Mapping) │
└─────────────┬──────────────┘ └───────────┬─────────────┘
              │                            │
┌─────────────▼────────────────────────────▼─────────────┐
│       Inode & Table Layer (Inode, InodeTable)          │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│      Storage Management (Superblock, Bitmap)           │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│      Virtual Disk / I/O Engine (BlockDevice)           │
└───────────────────────────┬────────────────────────────┘
                            │
                     [ disk.img ]
```

### Separation of Concerns
- `org.microfs.storage`: Fixed-block virtual disk, raw inode slots, and persistent bitmaps. Knows nothing about directories or tree structures.
- `org.microfs.dsa`: Pure in-memory data structures (Red-Black Tree, Trie, Directory indexing strategies). Knows nothing about disk blocks or file channels.
- `org.microfs.fs`: Filesystem coordinator. Manages superblock, mounts, unmounts, directory serialization, path resolution, and FSCK.
- `org.microfs.cli`: Command-line interface and interactive REPL.
- `org.microfs.benchmark`: Workload generators, latency statistical accumulators, and reproducibility test harnesses.
- `org.microfs.debug`: Standalone diagnostic utilities and restart verification programs.

### Persistence & Flush Lifecycle
Metadata consistency is guaranteed by a strict flush order on unmount:
1. **Inode Table** (`inodeTable.flush()`): Writes all dirty inode slots (0 to `capacity - 1`) to disk.
2. **Allocation Bitmaps** (`inodeBitmap.flush()`, `blockBitmap.flush()`): Synchronizes allocated bit masks.
3. **Superblock** (`superblock.write(disk)`): Rewrites superblock geometry and region pointers at Block 1.
4. **Virtual Disk** (`disk.close()`): Forces channel synchronization (`channel.force(true)`) and closes file handles.

---

## 3. On-Disk Filesystem Format

MicroFS uses little-endian byte ordering throughout all structures.

```
Disk Block Layout:
┌─────────────────┬──────────────┬──────────────┬──────────────┬──────────────────┬─────────────────┐
│ Block 0         │ Block 1      │ Block 2      │ Block 3      │ Block 4..11      │ Block 12..N     │
│ BlockDevice Hdr │ Superblock   │ Inode Bitmap │ Block Bitmap │ Inode Table      │ Data Region     │
│ (20 bytes)      │ (128 bytes)  │ (1 block)    │ (1 block)    │ (8 blocks / 64)  │ (Data blocks)   │
└─────────────────┴──────────────┴──────────────┴──────────────┴──────────────────┴─────────────────┘
```

### Block 0: BlockDevice Header (20 bytes)
- Offset 0..7: Magic identifier bytes `MICROFS\0`
- Offset 8..11: Block size (32-bit signed int, power of 2 between 64 and 65,536; default: 512)
- Offset 12..19: Total block count (64-bit signed long)

### Block 1: Superblock (128 bytes)
- Offset 0..3: Filesystem magic `0x4D46`
- Offset 4..7: Format version (`1`)
- Offset 8..11: Block size (bytes)
- Offset 12..19: Total blocks on device
- Offset 20..23: Inode capacity (default: 64)
- Offset 24..27: Inode bitmap starting block (default: 2)
- Offset 28..31: Block bitmap starting block (default: 3)
- Offset 32..35: Inode table starting block (default: 4)
- Offset 36..39: Data region starting block (default: 12)
- Offset 40..43: Root inode index (`0`)
- Offset 44..127: Reserved / zero-padding

### Inode Layout (Fixed 64 bytes)
Every inode slot is exactly 64 bytes:

| Offset | Size (bytes) | Field | Description |
|---|---:|---|---|
| 0 | 1 | `type` | `1` = Regular File, `2` = Directory |
| 1 | 1 | Reserved | Zero padding |
| 2..3 | 2 | `permissions` | 9-bit Unix style (`0755` dir, `0644` file) |
| 4..7 | 4 | `linkCount` | Number of hard links |
| 8..15 | 8 | `size` | File size in bytes |
| 16..23 | 8 | `mtime` | Epoch timestamp in milliseconds |
| 24..31 | 8 | Reserved | Reserved for atime |
| 32..39 | 8 | Reserved | Reserved for ctime |
| 40..43 | 4 | `direct[0]` | Absolute disk block number for direct block 0 |
| 44..47 | 4 | `direct[1]` | Absolute disk block number for direct block 1 |
| 48..51 | 4 | `direct[2]` | Absolute disk block number for direct block 2 |
| 52..55 | 4 | `direct[3]` | Absolute disk block number for direct block 3 |
| 56..59 | 4 | `indirect` | Absolute disk block number for single-indirect pointer (`-1` = none) |
| 60..63 | 4 | `extentLength`| Reserved for extent allocation mode (`0` = block pointers) |

### Directory Entry Serialization Format (`DirectorySerializer`)
Directories are stored as packed, variable-length entries inside data blocks with 4-byte alignment:

```
┌──────────────┬──────────────────┬────────────────────────────┬──────────────────┐
│ Inode Number │ Name Length      │ UTF-8 Name Bytes           │ Padding          │
│ (4 bytes)    │ (2 bytes, 1-255) │ (length = Name Length)     │ (0-3 zero bytes) │
└──────────────┴──────────────────┴────────────────────────────┴──────────────────┘
```
- End-of-entries in a block is reached when remaining bytes `< 6` or `nameLen < 1` (zero padding).
- Root inode `0` is a valid entry (representing `.` or `..`).

---

## 4. Data Structures: Implementation vs. Frameworks

| Component | Status | Implementation Class | Primary Role |
|---|---|---|---|
| **Red-Black Tree** | **Implemented** | `org.microfs.dsa.RedBlackTree` | Balanced BST with invariant checks & instrumentation |
| **Trie** | **Implemented** | `org.microfs.dsa.Trie` | In-memory prefix search & autocompletion |
| **Bitmap** | **Implemented** | `org.microfs.storage.Bitmap` | Persistent bitwise allocation tracking |
| **Linear Directory** | **Implemented** | `org.microfs.dsa.LinearDirectory` | Baseline linear scan strategy ($O(n)$) |
| **Hash Directory** | **Implemented** | `org.microfs.dsa.HashDirectory` | Constant-time exact lookup strategy ($O(1)$) |
| **RB-Tree Directory**| **Implemented** | `org.microfs.dsa.RBTreeDirectory` | Ordered tree directory index ($O(\log n)$) |
| **Hybrid Directory** | **Implemented** | `org.microfs.dsa.HybridDirectory` | Combined Hash lookup + Trie prefix search |
| **Adaptive Directory**| **Implemented** | `org.microfs.dsa.AdaptiveDirectory`| Dynamic scale-driven strategy transitions |
| **LRU Cache** | **Framework** | Interface / Framework | Standalone caching model; not wired into disk I/O path |
| **Block Allocation** | **Framework** | Extensible Framework | Basic bitmap allocation active; pluggable policies framed |

### Asymptotic Complexity Comparison

| Index Strategy | Exact Lookup | Prefix Search | Ordered Listing | Space Overhead |
|---|---|---|---|---|
| **LinearDirectory** | $O(n)$ | $O(n)$ | $O(n \log n)$ (sort needed) | Minimal ($O(n)$ array) |
| **HashDirectory** | $O(1)$ avg, $O(n)$ worst | $O(n)$ | $O(n \log n)$ (sort needed) | Hash table overhead (~$2\times$) |
| **RBTreeDirectory** | $O(\log n)$ | $O(\log n + k)$ | $O(n)$ (in-order traversal) | 3 pointers + color per node |
| **HybridDirectory** | $O(1)$ avg | $O(K + k)$ | $O(n \log n)$ | Hash table + Trie node overhead |
| **AdaptiveDirectory**| $O(1)$ to $O(\log n)$ | $O(n)$ to $O(K + k)$ | $O(n)$ to $O(n \log n)$ | Varies with active state |

*Where $n$ = entry count, $K$ = prefix length, $k$ = matching results count.*

---

## 5. Algorithmic Implementations

### Red-Black Tree Balancing & Invariants
`RedBlackTree<K, V>` adheres strictly to CLRS self-balancing properties:
1. Every node is either Red or Black.
2. The root is always Black.
3. Every leaf (`nil`) is Black.
4. If a node is Red, both its children are Black (no two consecutive Red nodes).
5. For each node, all simple paths from the node to descendant leaves contain the same number of Black nodes (*black-height*).

- **Instrumentation**: Measures exact left/right rotation counts, recoloring operations, tree height, and black-height.
- **Verification**: `verifyInvariants()` asserts all 5 properties across the entire tree during testing.

### Adaptive Directory Migration
`AdaptiveDirectory` monitors directory size and migrates between strategies across configurable thresholds:
- **`SMALL`** ($< 5$ entries): `LinearDirectory` (minimal memory footprint).
- **`MEDIUM`** ($5 \le n < 50$ entries): `HashDirectory` (fast exact lookups).
- **`LARGE`** ($n \ge 50$ entries): `HybridDirectory` (Hash + Trie) or `RBTreeDirectory`.

When a threshold boundary is crossed during insertion or deletion, the active index is rebuilt from the master entry list in $O(n)$ time while preserving all inode numbers.

### Path Resolution
`PathResolver.resolve(currentDir, inputPath)`:
1. Normalizes separators and tokenizes by `/`.
2. Filters out empty tokens and current-directory markers (`.`).
3. Handles parent-directory markers (`..`) by popping from a canonical path stack.
4. Returns an absolute canonical path string in $O(L)$ time ($L$ = path length).

---

## 6. Filesystem Lifecycle & Consistency

### Formatting
`Filesystem.format(blockSize, blockCount)`:
1. Initializes `BlockDevice` with zeroed storage and a valid 20-byte header.
2. Writes the default `Superblock` at Block 1.
3. Allocates inode 0 for root in the inode bitmap.
4. Allocates block 0 of the data region (`superblock.getDataRegionBlock()`) for the root directory.
5. Populates root directory entries `.` (inode 0) and `..` (inode 0).
6. Serializes the root directory inode (mode `0755`, link count 2) into slot 0 of the inode table.

### FSCK Consistency Checker
`Fsck.check(imagePath)` performs read-only structural validation:
- Validates superblock magic (`0x4D46`), version (`1`), and positive geometry.
- Asserts presence and integrity of inode and block bitmaps.
- Validates inode table slots: verifies type codes, non-negative link counts, and ensures all referenced direct blocks fall within valid disk boundaries.

---

## 7. Interactive CLI Reference

Run the interactive shell:
```bash
java -cp target/classes org.microfs.cli.MicrofsCLI
```

### Command Table

| Command | Arguments | Description |
|---|---|---|
| `format` | `[image] [--block-size B] [--blocks N]` | Formats a virtual disk image (defaults: `disk.img`, 512 bytes, 1000 blocks) |
| `mount` | `[image]` | Mounts an existing disk image (default: `disk.img`) |
| `unmount` | *(none)* | Flushes all metadata and unmounts the current image |
| `mkdir` | `<path>` | Creates a new directory at the specified absolute or relative path |
| `ls` | `[path]` | Lists directory contents (defaults to current working directory) |
| `cd` | `[path]` | Changes the current directory (defaults to `/`) |
| `pwd` | *(none)* | Displays the current working directory |
| `touch` | `[path]` | Creates a regular file inode (defaults to `/test.txt`) |
| `cat` | `[path]` | Reads and displays file content (defaults to `/test.txt`) |
| `stat` | `[path]` | Displays file/directory metadata (defaults to `/`) |
| `df` | *(none)* | Displays disk capacity, block size, and allocated block counts |
| `fsck` | `[image]` | Runs consistency checks on a virtual disk image |
| `benchmark` | *(none)* | Executes the built-in benchmark runner |
| `exit` / `quit` | *(none)* | Exits the interactive shell |

---

## 8. Benchmark Results & Key Empirical Findings

Real-world validation was executed on Windows 11 with OpenJDK 25 over realistic student-developer namespaces (100 to 50,000 entries).

### 5-Row Key Benchmark Results

| Workload / Operation | Scale (Entries) | Strategy | Warm Median Latency | Key Empirical Takeaway |
|---|---:|---|---:|---|
| **Exact Lookup** | 50,000 | Hash | **~100 ns** | Near-instant constant-time ($O(1)$) retrieval at scale |
| **Exact Lookup Baseline** | 10,000 | Linear | **28.0 µs** | Linear scan exhibits severe latency degradation ($O(n)$) |
| **Prefix Search Comparison** | 50,000 | Linear vs. Trie | Linear: **~715 µs**<br>Trie: **~1.79 ms** | **Critical DSA Lesson:** Trie was slower than linear scanning on short prefixes due to pointer hopping & cache misses vs contiguous memory scanning |
| **Ordered Directory Listing** | 50,000 | RB-Tree | **~44.8 ms** | Full in-order traversal yields sorted output without external sort pass |
| **Dynamic Index Construction** | 1,000 | Adaptive | **~197.6 ms** | Dynamic representation migration incurs measurable re-indexing overhead |

### Detailed Analysis: Trie vs. Linear Prefix Search
In theoretical computer science, a Trie provides $O(K + k)$ prefix search, which asymptotically dominates $O(n \cdot K)$ linear scanning. However, modern CPU architectures introduce physical constraints:
- **Linear Directory**: Entries are packed contiguously in memory. Scanning utilizes CPU hardware prefetchers, achieves near-100% L1/L2 cache line utilization, and requires zero pointer dereferencing.
- **Trie Directory**: Each character transition traverses a heap-allocated node pointer. Across 50,000 entries, node pointer chasing causes frequent CPU cache misses and TLB misses, resulting in **~1.79 ms** latency compared to **~715 µs** for contiguous linear scanning.

---

## 9. Build, Test, & Verification

### Build Requirements
- JDK 21 or later
- Apache Maven 3.8+

### Commands

Compile the project:
```bash
mvn compile
```

Run unit and integration test suite (14/14 tests pass):
```bash
mvn test
```

Build the release package:
```bash
mvn clean test package
```
*Output: `target/microfs-0.1.0-SNAPSHOT.jar`*

### Diagnostic Programs (`org.microfs.debug`)
- `FinalDemo`: End-to-end demonstration of format, mount, multi-level directory creation, unmount, remount, and FSCK.
- `Phase3RestartTest`: Multi-process restart simulation verifying persistence across clean JVM re-initializations.
- `RunBenchmark`: Multi-scale directory benchmark across Linear, Hash, RB-Tree, Hybrid, and Adaptive strategies.
- `InspectInode`: Raw byte inspector for inode table blocks.
- `InspectSuperblock`: Raw byte inspector for superblock geometry.

Run any diagnostic program:
```bash
java -cp target/classes org.microfs.debug.FinalDemo
java -cp target/classes org.microfs.debug.Phase3RestartTest
```

---

## 10. Scope & Limitations

- **Concurrency**: MicroFS is single-threaded. Concurrency controls (read/write locking per inode) are deferred.
- **Academic Simulator**: Designed for algorithmic measurement, not as a replacement for OS filesystems (NTFS, ext4, APFS).
- **Deferred Features**: Crash-recovery journaling, extent allocation, hard links, and symlinks are omitted from the current release.
