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
        if (disk != null) {
            try { disk.close(); } catch (Exception ignored) {}
            disk = null;
        }
        this.disk = BlockDevice.format(imagePath, blockSize, blockCount);
        this.superblock = Superblock.createDefault(disk);
        this.superblock.write(disk);
        // Initialize root inode (inode 0) as directory using InodeTable mechanism
        try {
            this.inodeBitmap = new Bitmap(disk, superblock.getInodeBitmapBlock(), 1, superblock.getInodeCapacity());
            this.inodeBitmap.allocate(); // reserve inode 0
            this.inodeBitmap.flush();

            int blockBitmapCapacity = (int) (disk.blockCount() - superblock.getDataRegionBlock());
            if (blockBitmapCapacity < 1) blockBitmapCapacity = 64;
            this.blockBitmap = new Bitmap(disk, superblock.getBlockBitmapBlock(), 1, blockBitmapCapacity);
            this.blockBitmap.allocate(); // reserve data block 0 for root directory
            this.blockBitmap.flush();

            this.inodeTable = new InodeTable(disk, superblock.getInodeCapacity(), superblock.getInodeTableBlock());
            // Initialize root directory inode directly through table
            Inode rootInode = new Inode(FileType.DIRECTORY);
            rootInode.setPermissions(0755);
            rootInode.setLinkCount(2);
            rootInode.setNumber(0);
            rootInode.setSize(0);
            rootInode.setMtime(System.currentTimeMillis());
            // Initialize directory entries (. and ..) in root inode's direct block
            int rootDataBlock = superblock.getDataRegionBlock();
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
            this.inodeTable.put(rootInode);
            this.inodeTable.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize root inode", e);
        }
    }

    public void mount() {
        if (mounted) throw new IllegalStateException("Already mounted");
        if (disk != null) {
            try { disk.close(); } catch (Exception ignored) {}
            disk = null;
        }
        this.disk = BlockDevice.open(imagePath);
        this.superblock = Superblock.read(disk);
        // Initialize bitmaps at superblock locations
        this.inodeBitmap = new Bitmap(disk, superblock.getInodeBitmapBlock(), 1, superblock.getInodeCapacity());
        int blockBitmapCapacity = (int) (disk.blockCount() - superblock.getDataRegionBlock());
        if (blockBitmapCapacity < 1) blockBitmapCapacity = 64;
        this.blockBitmap = new Bitmap(disk, superblock.getBlockBitmapBlock(), 1, blockBitmapCapacity);
        this.inodeTable = new InodeTable(disk, superblock.getInodeCapacity(), superblock.getInodeTableBlock());
        this.mounted = true;
    }

    public void unmount() {
        if (!mounted && disk == null) throw new IllegalStateException("Not mounted");
        if (disk != null) {
            if (inodeTable != null) inodeTable.flush();
            if (inodeBitmap != null) inodeBitmap.flush();
            if (blockBitmap != null) blockBitmap.flush();
            if (superblock != null) superblock.write(disk);
            try {
                disk.close();
            } catch (Exception ignored) {
            }
            disk = null;
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
