# MicroFS Algorithms

## Path Resolution (`org.microfs.fs.PathResolver`)

1. Tokenize path by `/`
2. Skip empty parts and `.`
3. Process `..` by removing previous token
4. For relative paths, prepend current directory tokens
5. For absolute paths, start from root
6. Build canonical string representation

Complexity: O(path length) token processing.

## RB Tree Insertion

Standard CLRS insertion with fixup:
- Insert as red leaf
- If parent is black, done
- If uncle is red, recolor parent/uncle/parent's parent and recurse
- Otherwise, perform rotations and recolor

Complexity: O(log n) amortized.

## RB Tree Deletion

Standard CLRS deletion with fixup:
- Transplant subtree
- If deleted node was black, fix black-height violation by traversing up and applying recolor/rotation rules

Complexity: O(log n) amortized.

## RB Tree Rotations

- Left rotation: promotes right child, demotes parent to left
- Right rotation: promotes left child, demotes parent to right

Both preserve BST ordering and update parent pointers.

## Trie Search / Insert / Remove

- Insert: traverse/create nodes per character; set end flag and value
- Search: traverse; return value if end flag set at final node
- StartsWith: find node for prefix; collect all descendants with end flag
- Remove: delete recursively; prune empty branches

Complexity: O(length of key) for insert/search/remove.

## Block Allocation (Bitmap)

Scan bits from first word. Find first zero bit in first non-full word. Set bit. Write back.

Complexity: O(number of words) per allocation = O(total elements / 64) worst case.

## Adaptive Migration

When size crosses threshold:
1. Determine new mode
2. Create new directory instance of target type
3. Rebuild by inserting all master entries
4. Replace current directory reference

Complexity: O(n) for n entries.
