package org.microfs.fs;

import org.microfs.storage.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Manages file block mapping and basic read/write operations.
 */
public class FileStorage {

    private final Filesystem fs;

    public FileStorage(Filesystem fs) {
        this.fs = fs;
    }

    /** Reads data from a file given its inode. Returns byte array. */
    public byte[] readFile(Inode inode) throws Exception {
        long size = inode.getSize();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("File too large for basic read");
        }
        byte[] result = new byte[(int) size];
        int offset = 0;
        int blockSize = fs.getDisk().blockSize();

        // Read direct blocks
        for (int i = 0; i < Inode.DIRECT_COUNT && offset < size; i++) {
            int blockNum = inode.getDirect(i);
            if (blockNum == Inode.NO_BLOCK) break;
            ByteBuffer block = fs.getDisk().readBlock(blockNum);
            int toRead = Math.min(blockSize, (int) (size - offset));
            for (int j = 0; j < toRead; j++) {
                if (offset + j < result.length) result[offset + j] = block.get(j);
            }
            offset += toRead;
        }

        // Read single indirect block if needed
        if (offset < size) {
            int indirectBlock = inode.getIndirect();
            if (indirectBlock != Inode.NO_BLOCK) {
                ByteBuffer indirectBuf = fs.getDisk().readBlock(indirectBlock);
                int maxBlocks = (indirectBuf.capacity() - indirectBuf.position()) / 4;
                for (int j = 0; j < maxBlocks && offset < size; j++) {
                    int blockNum = indirectBuf.getInt();
                    if (blockNum == Inode.NO_BLOCK) break;
                    ByteBuffer block = fs.getDisk().readBlock(blockNum);
                    int toRead = Math.min(blockSize, (int) (size - offset));
                    for (int k = 0; k < toRead; k++) {
                        if (offset + k < result.length) result[offset + k] = block.get(k);
                    }
                    offset += toRead;
                }
            }
        }
        return result;
    }

    /** Writes data to a file given its inode and data bytes. Simplified for basic phase. */
    public void writeFile(Inode inode, byte[] data) throws Exception {
        long size = data.length;
        inode.setSize(size);
        int blockSize = fs.getDisk().blockSize();
        int offset = 0;

        // Clear previous direct blocks (simplified: don't reuse old blocks for simplicity)
        for (int i = 0; i < Inode.DIRECT_COUNT; i++) {
            inode.setDirect(i, Inode.NO_BLOCK);
        }
        inode.setIndirect(Inode.NO_BLOCK);

        // Allocate and write direct blocks
        List<Integer> allocatedBlocks = new ArrayList<>();
        for (int i = 0; i < Inode.DIRECT_COUNT && offset < size; i++) {
            int newBlock = fs.getBlockBitmap().allocate();
            inode.setDirect(i, newBlock);
            allocatedBlocks.add(newBlock);
            ByteBuffer block = ByteBuffer.allocate(blockSize).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int toWrite = Math.min(blockSize, data.length - offset);
            for (int j = 0; j < toWrite; j++) {
                block.put(data[offset + j]);
            }
            // Pad rest with zeros
            for (int j = toWrite; j < blockSize; j++) block.put((byte) 0);
            fs.getDisk().writeBlock(newBlock, block.flip());
            offset += toWrite;
        }

        // For simplicity, if file exceeds direct blocks, don't fully implement indirect yet
        // But allocate indirect conceptually
        if (offset < size) {
            // Not fully implemented in basic phase; document limitation
            System.err.println("WARNING: File exceeds direct block capacity; multi-block indirect not fully implemented in basic phase");
            // Allocate indirect block and fill with new block numbers conceptually
            int indirectBlock = fs.getBlockBitmap().allocate();
            inode.setIndirect(indirectBlock);
        }

        fs.getInodeTable().flush();
        fs.getInodeBitmap().flush();
        fs.getBlockBitmap().flush();
    }
}
