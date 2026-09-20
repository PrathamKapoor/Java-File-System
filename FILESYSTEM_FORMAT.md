# MicroFS Filesystem Format

## Disk image layout

```
Block 0: BlockDevice header (20 bytes)
  - Magic: "MICROFS\0" (8 bytes)
  - Block size (int, 4 bytes)
  - Block count (long, 8 bytes)

Block 1: Superblock (128 bytes)
  - Magic (4 bytes) - 0x4D46
  - Version (4 bytes) - 1
  - Block size (4 bytes)
  - Total block count (8 bytes)
  - Inode capacity (4 bytes)
  - Inode bitmap block (4 bytes)
  - Block bitmap block (4 bytes)
  - Inode table start block (4 bytes)
  - Data region start block (4 bytes)
  - Root inode number (4 bytes)
  - Reserved (remaining bytes)

Following blocks:
  - Inode bitmap region
  - Block bitmap region
  - Inode table region
  - Data region
```

## Block size

Power of two in [64, 65536] bytes.

## Inode

Fixed 64 bytes, little-endian:

| Offset | Size | Field |
|--------|------|-------|
| 0 | 1 | Type code (1=REGULAR_FILE, 2=DIRECTORY) |
| 1 | 1 | Reserved |
| 2-3 | 2 | Permissions (9-bit Unix style) |
| 4-7 | 4 | Link count |
| 8-15 | 8 | Size (bytes) |
| 16-23 | 8 | Mtime (epoch millis) |
| 24-31 | 8 | Reserved (atime) |
| 32-39 | 8 | Reserved (ctime) |
| 40-43 | 4 | Direct block 0 |
| 44-47 | 4 | Direct block 1 |
| 48-51 | 4 | Direct block 2 |
| 52-55 | 4 | Direct block 3 |
| 56-59 | 4 | Single indirect block (-1 = none) |
| 60-63 | 4 | Extent length (0 = block pointer mode) |

## Endianness

All disk structures use `LittleEndian`.

## Compatibility

Images with different magic or version are rejected. Block size and total block count must match the header. Inode capacity must be positive.
