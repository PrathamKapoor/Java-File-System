package org.microfs.storage;

import java.nio.ByteBuffer;

/**
 * On-disk inode: 64 bytes, little-endian.
 *
 * <p>Layout (offsets from zero):</p>
 * <pre>
 *   0  : type code (1 byte)
 *   1  : reserved (1 byte)
 *   2..3 : permissions (2 bytes, 9-bit Unix rwxrwxrwx, upper bits zero)
 *   4..7 : link count (int)
 *   8..15: size in bytes (long)
 *   16..23: mtime epoch millis (long)
 *   24..31: reserved for atime (long, currently zero)
 *   32..39: reserved for ctime (long, currently zero)
 *   40..43: direct block 0 (int)
 *   44..47: direct block 1 (int)
 *   48..51: direct block 2 (int)
 *   52..55: direct block 3 (int)
 *   56..59: single-indirect block (int, -1 = none)
 *   60..63: extent length (int, 0 = block-pointer mode)
 * </pre>
 */
public final class Inode {

    public static final int SIZE = 64;
    static final int NO_BLOCK = -1;
    public static final int DIRECT_COUNT = 4;

    private FileType type;
    private int permissions;
    private int linkCount;
    private long size;
    private long mtime;
    private final int[] direct = new int[DIRECT_COUNT];
    private int indirect;
    private int number;

    Inode(FileType type) {
        this.type = type;
        this.permissions = 0644;
        this.linkCount = type == FileType.DIRECTORY ? 2 : 1;
        this.mtime = 0L;
        this.indirect = NO_BLOCK;
        this.number = -1;
        java.util.Arrays.fill(direct, NO_BLOCK);
    }

    private Inode() {
    }

    public static Inode deserialize(ByteBuffer buffer) {
        Inode inode = new Inode();
        inode.type = FileType.fromCode(buffer.get());
        buffer.get();
        inode.permissions = buffer.getShort() & 0xFFFF;
        inode.linkCount = buffer.getInt();
        inode.size = buffer.getLong();
        inode.mtime = buffer.getLong();
        buffer.getLong();
        buffer.getLong();
        for (int i = 0; i < DIRECT_COUNT; i++) {
            inode.direct[i] = buffer.getInt();
        }
        inode.indirect = buffer.getInt();
        buffer.getInt();
        return inode;
    }

    public void serialize(ByteBuffer buffer) {
        buffer.put(type.code());
        buffer.put((byte) 0);
        buffer.putShort((short) (permissions & 0xFFFF));
        buffer.putInt(linkCount);
        buffer.putLong(size);
        buffer.putLong(mtime);
        buffer.putLong(0L);
        buffer.putLong(0L);
        for (int i = 0; i < DIRECT_COUNT; i++) {
            buffer.putInt(direct[i]);
        }
        buffer.putInt(indirect);
        buffer.putInt(0);
    }

    public FileType getType() {
        return type;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions & 0777;
    }

    public int getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(int linkCount) {
        if (linkCount < 0) {
            throw new IllegalArgumentException("Negative link count");
        }
        this.linkCount = linkCount;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        this.size = size;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public int getDirect(int index) {
        return direct[index];
    }

    public void setDirect(int index, int block) {
        direct[index] = block;
    }

    public int getIndirect() {
        return indirect;
    }

    public void setIndirect(int block) {
        indirect = block;
    }

    public int getNumber() {
        return number;
    }

    void setNumber(int number) {
        this.number = number;
    }

    /** Marks this inode deleted (link count 0, no blocks). Callers free blocks separately. */
    void clear() {
        type = null;
        java.util.Arrays.fill(direct, NO_BLOCK);
        indirect = NO_BLOCK;
        size = 0;
        linkCount = 0;
    }
}
