# MicroFS Testing

## Unit tests

- `RedBlackTreeTest`: insert, delete, ordered traversal, invariant verification
- `BitmapTest`: allocate, free, flush, persistence
- `InodeTableTest`: persistence across reload
- `PersistenceTest`: format, mount, unmount, superblock persistence

## Integration tests

Complete workflows:

- `format -> mount -> mkdir -> touch -> write -> read -> unmount -> mount -> read`
- `mkdir nested -> cp -> mv -> delete`
- `find traversal`
- `permission changes`

## Corruption tests

`Fsck.check()` detects:
- Bad superblock
- Missing bitmaps
- Invalid inode references

Explicit repair is deferred and would require opt-in.

## Serialization tests

Every persistent structure supports serialize/deserialize:
- `Superblock`
- `Inode`
- `Bitmap`
- Directory entries (through `DirectoryIndex` implementations)

## Property tests

- RB tree: random insert/delete sequences with invariant verification
- Bitmap: allocate/free cycles with count verification

## Running tests

```bash
mvn test
```

Note: JUnit dependency is configured in `pom.xml`. Tests use standard JUnit 5 annotations (`@Test`, `@BeforeEach`, etc.).
