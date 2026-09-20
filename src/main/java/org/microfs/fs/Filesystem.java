package org.microfs.fs;

import org.microfs.dsa.*;
import org.microfs.storage.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Core filesystem instance managing a mounted disk image.
 */
public class Filesystem {

    private final Path imagePath;
    private BlockDevice disk;
    private Superblock superblock;
    private Bitmap inodeBitmap;
    private Bitmap blockBitmap;
    private InodeTable inodeTable;
    private boolean mounted = false;

    public Filesystem(Path imagePath) {
        this.imagePath = imagePath;
    }

    public void format(int blockSize, long blockCount) {
        if (mounted) throw new IllegalStateException("Filesystem already mounted");
        this.disk = BlockDevice.format(imagePath, blockSize, blockCount);
        this.superblock = Superblock.createDefault(disk);
        this.superblock.write(disk);
        // Initialize root inode (inode 0) as directory using InodeTable mechanism
        try {
            Bitmap inodeBitmapInit = new Bitmap(disk, superblock.getInodeBitmapBlock(), 2, superblock.getInodeCapacity());
            inodeBitmapInit.allocate(); // reserve inode 0
            inodeBitmapInit.flush();

            InodeTable inodeTableInit = new InodeTable(disk, superblock.getInodeCapacity(), superblock.getInodeTableBlock());
            // Initialize root directory inode directly through table
            Inode rootInode = new Inode(FileType.DIRECTORY);
            rootInode.setPermissions(0755);
            rootInode.setLinkCount(2);
            rootInode.setNumber(0);
            rootInode.setSize(0);
            rootInode.setMtime(System.currentTimeMillis());
            // Initialize directory entries (. and ..) in root inode's direct block
            int rootDataBlock = (int) superblock.getDataRegionBlock();
            List<org.microfs.dsa.DirectoryEntry> rootEntries = new ArrayList<>();
            rootEntries.add(new org.microfs.dsa.DirectoryEntry(".", 0));
            rootEntries.add(new org.microfs.dsa.DirectoryEntry("..", 0));
            List<java.nio.ByteBuffer> rootBlocks = org.microfs.fs.DirectorySerializer.serializeToBlocks(rootEntries);
            for (int i = 0; i < Math.min(rootBlocks.size(), 4); i++) {
                java.nio.ByteBuffer blockToWrite = rootBlocks.get(i);
                blockToWrite.position(0);
                blockToWrite.limit(512);
                disk.writeBlock(rootDataBlock + i, blockToWrite);
                rootInode.setDirect(i, rootDataBlock + i);
            }
            // Manually serialize and write inode 0 to inode table region
            java.nio.ByteBuffer inodeBuf = java.nio.ByteBuffer.allocate(Inode.SIZE).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            rootInode.serialize(inodeBuf);
            inodeBuf.flip();
            // InodeTable uses slot-based storage; for simplicity, write at slot 0 of first inode block
            long inodeRegionStart = superblock.getInodeTableBlock();
            java.nio.ByteBuffer inodeTableBlock = java.nio.ByteBuffer.allocate(disk.blockSize()).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            inodeTableBlock.put(inodeBuf);
            // Pad rest
            while (inodeTableBlock.hasRemaining()) inodeTableBlock.put((byte) 0);
            inodeTableBlock.flip();
            disk.writeBlock(inodeRegionStart, inodeTableBlock);
            inodeTableInit.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize root inode", e);
        }
    }

    public void mount() {
        if (mounted) throw new IllegalStateException("Already mounted");
        this.disk = BlockDevice.open(imagePath);
        this.superblock = Superblock.read(disk);
        // Initialize bitmaps at superblock locations
        this.inodeBitmap = new Bitmap(disk, superblock.getInodeBitmapBlock(), 2, superblock.getInodeCapacity());
        int blockBitmapCapacity = (int) (disk.blockCount() - superblock.getDataRegionBlock());
        if (blockBitmapCapacity < 1) blockBitmapCapacity = 64;
        this.blockBitmap = new Bitmap(disk, superblock.getBlockBitmapBlock(), 2, blockBitmapCapacity);
        this.inodeTable = new InodeTable(disk, superblock.getInodeCapacity(), superblock.getInodeTableBlock());
        this.mounted = true;
    }

    public void unmount() {
        if (!mounted) throw new IllegalStateException("Not mounted");
        if (disk != null) {
            inodeTable.flush();
            inodeBitmap.flush();
            blockBitmap.flush();
            superblock.write(disk);
        }
        this.mounted = false;
    }

    public boolean isMounted() { return mounted; }
    public BlockDevice getDisk() { return disk; }
    public Superblock getSuperblock() { return superblock; }
    public Bitmap getInodeBitmap() { return inodeBitmap; }
    public Bitmap getBlockBitmap() { return blockBitmap; }
    public InodeTable getInodeTable() { return inodeTable; }
}
