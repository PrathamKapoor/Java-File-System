package org.microfs.fs;

import org.microfs.storage.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * Persistent superblock at block 0 (after header in BlockDevice).
 * Note: BlockDevice already writes a 20-byte header; superblock lives at block 1.
 */
public final class Superblock {

    public static final int BLOCK_NUMBER = 1;
    public static final int SIZE = 128;

    private final int magic;
    private final int version;
    private final int blockSize;
    private final long totalBlocks;
    private final int inodeCapacity;
    private final int inodeBitmapBlock;
    private final int blockBitmapBlock;
    private final int inodeTableBlock;
    private final int dataRegionBlock;
    private final int rootInode;

    private Superblock(int magic, int version, int blockSize, long totalBlocks,
                       int inodeCapacity, int inodeBitmapBlock, int blockBitmapBlock,
                       int inodeTableBlock, int dataRegionBlock, int rootInode) {
        this.magic = magic;
        this.version = version;
        this.blockSize = blockSize;
        this.totalBlocks = totalBlocks;
        this.inodeCapacity = inodeCapacity;
        this.inodeBitmapBlock = inodeBitmapBlock;
        this.blockBitmapBlock = blockBitmapBlock;
        this.inodeTableBlock = inodeTableBlock;
        this.dataRegionBlock = dataRegionBlock;
        this.rootInode = rootInode;
    }

    public static Superblock createDefault(BlockDevice disk) {
        int bs = disk.blockSize();
        long total = disk.blockCount();
        int inodeCapacity = 64; // small default
        int inodeBitmapBlock = 2;
        int blockBitmapBlock = 3;
        int inodeTableBlock = 4;
        int dataRegionBlock = (int) (inodeTableBlock + Math.ceil(inodeCapacity * 64.0 / bs));
        return new Superblock(0x4D46, 1, bs, total, inodeCapacity,
                inodeBitmapBlock, blockBitmapBlock, inodeTableBlock, dataRegionBlock, 0);
    }

    public static Superblock read(BlockDevice disk) {
        ByteBuffer buf = disk.readBlock(BLOCK_NUMBER);
        int magic = buf.getInt();
        int version = buf.getInt();
        int bs = buf.getInt();
        long total = buf.getLong();
        int inodeCapacity = buf.getInt();
        int inodeBitmapBlock = buf.getInt();
        int blockBitmapBlock = buf.getInt();
        int inodeTableBlock = buf.getInt();
        int dataRegionBlock = buf.getInt();
        int rootInode = buf.getInt();
        return new Superblock(magic, version, bs, total, inodeCapacity,
                inodeBitmapBlock, blockBitmapBlock, inodeTableBlock, dataRegionBlock, rootInode);
    }

    public void write(BlockDevice disk) {
        int bs = disk.blockSize();
        ByteBuffer buf = ByteBuffer.allocate(bs).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(magic);
        buf.putInt(version);
        buf.putInt(blockSize);
        buf.putLong(totalBlocks);
        buf.putInt(inodeCapacity);
        buf.putInt(inodeBitmapBlock);
        buf.putInt(blockBitmapBlock);
        buf.putInt(inodeTableBlock);
        buf.putInt(dataRegionBlock);
        buf.putInt(rootInode);
        // Fill rest with zeros
        while (buf.hasRemaining()) {
            buf.put((byte) 0);
        }
        buf.flip();
        disk.writeBlock(BLOCK_NUMBER, buf);
    }

    public int getMagic() { return magic; }
    public int getVersion() { return version; }
    public int getBlockSize() { return blockSize; }
    public long getTotalBlocks() { return totalBlocks; }
    public int getInodeCapacity() { return inodeCapacity; }
    public int getInodeBitmapBlock() { return inodeBitmapBlock; }
    public int getBlockBitmapBlock() { return blockBitmapBlock; }
    public int getInodeTableBlock() { return inodeTableBlock; }
    public int getDataRegionBlock() { return dataRegionBlock; }
    public int getRootInode() { return rootInode; }
}
