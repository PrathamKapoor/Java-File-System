# MicroFS Data Structures

## Tree (Filesystem Namespace)

The filesystem namespace is a tree of directories and files. Each directory node maintains entries (`name -> inode`). The root inode represents `/`.

- **Purpose**: Hierarchical namespace
- **Operations**: insert, delete, traversal, parent lookup
- **Where used**: `Filesystem` namespace layer, `PathResolver`

## Red-Black Tree (`org.microfs.dsa.RedBlackTree`)

Genuine RB tree implementation with full rotation/recolor logic.

- **Purpose**: Ordered directory indexing for medium/large directories
- **Operations**: insert, delete, search, minimum, maximum, successor, predecessor, ordered traversal, range traversal
- **Invariants**: every node is red/black; root is black; nil leaves black; red nodes have black children; consistent black height
- **Instrumentation**: rotation count, recolor count, height, node count, invariant verification
- **Where used**: `RBTreeDirectory`, `AdaptiveDirectory` (LARGE mode)

## Hash Index (`org.microfs.dsa.HashDirectory`)

Standard `HashMap`-based index.

- **Purpose**: Average O(1) exact lookup
- **Operations**: insert, lookup, delete, list (sorted for determinism)
- **Where used**: `HashDirectory`, `HybridDirectory`, `AdaptiveDirectory` (MEDIUM mode)

Why `HashMap` is acceptable: it is a production-tested hash table. The project's DSA contribution is demonstrated through the custom RB tree, Trie, adaptive framework, and LRU design.

## Trie (`org.microfs.dsa.Trie`)

Auxiliary prefix index.

- **Purpose**: Path prefix search, filename autocomplete, `find` acceleration, namespace experimentation
- **Operations**: insert, search, startsWith, remove
- **Distinction**: never authoritative for persistent namespace; does not affect inode consistency
- **Where used**: `Trie` (standalone), could be integrated with path lookup for prefix search

## Bitmap (`org.microfs.storage.Bitmap`)

Persistent bit image.

- **Purpose**: Inode allocation, block allocation tracking
- **Operations**: allocate (first free), free, isAllocated, countFree, flush
- **Where used**: `InodeBitmap`, `BlockBitmap`

## Directory Index Interface (`org.microfs.dsa.DirectoryIndex`)

Abstraction for substitutable directory representations.

Implementations:
- `LinearDirectory`
- `HashDirectory`
- `RBTreeDirectory`
- `HybridDirectory`
- `AdaptiveDirectory`

## Adaptive Directory (`org.microfs.dsa.AdaptiveDirectory`)

Switches representation by size:

- `SMALL` (< smallThreshold): Linear
- `MEDIUM` (< mediumThreshold): Hash
- `LARGE` (>= mediumThreshold): Hybrid (Hash + RB Tree)

Migration rebuilds active index from master entry list. Thresholds are configurable for benchmarking.

## LRU Cache (framework)

Conceptual framework defined. Actual LRU block cache can be built using a `HashMap` + doubly linked list. Not fully implemented as persistent feature in this release but framework exists.

## Doubly Linked List (concept)

Used conceptually in LRU eviction framework. Not independently exposed but referenced in design.
