package org.microfs.storage;

import java.nio.ByteBuffer;

/**
 * Persistent allocation bitmap over a contiguous run of disk blocks.
 *
 * <p>Bit i of the run maps to element i. Bit set = allocated. The region is
 * addressed by its first block index; element count may be smaller than the
 * bit capacity of the run, in which case trailing bits stay zero.</p>
 */
public final class Bitmap implements AutoCloseable {

    private final BlockDevice disk;
    private final long firstBlock;
    private final int blocksInRun;
    private final int elementCount;
    private final long[] bits;
    private boolean dirty;

    public Bitmap(BlockDevice disk, long firstBlock, int blocksInRun, int elementCount) {
        if (blocksInRun <= 0) {
            throw new IllegalArgumentException("blocksInRun must be positive");
        }
        if (elementCount < 0 || (long) elementCount > (long) blocksInRun * disk.blockSize() * 8L) {
            throw new IllegalArgumentException("elementCount exceeds bitmap capacity");
        }
        this.disk = disk;
        this.firstBlock = firstBlock;
        this.blocksInRun = blocksInRun;
        this.elementCount = elementCount;
        this.bits = new long[(elementCount + 63) >> 6];
        load();
    }

    private void load() {
        int bitsPerBlock = disk.blockSize() * Byte.SIZE;
        int wordsRead = 0;
        for (int b = 0; b < blocksInRun && wordsRead < bits.length; b++) {
            ByteBuffer buffer = disk.readBlock(firstBlock + b);
            for (int i = 0; i < bitsPerBlock && wordsRead < bits.length; i += 64) {
                bits[wordsRead++] = buffer.getLong(i >> 3);
            }
        }
    }

    /** Returns the index of the first free element, or throws when exhausted. */
    public int allocate() {
        for (int w = 0; w < bits.length; w++) {
            long word = bits[w];
            if (word != -1L) {
                int bit = Long.numberOfTrailingZeros(~word);
                int index = (w << 6) + bit;
                if (index >= elementCount) {
                    break;
                }
                bits[w] = word | (1L << bit);
                dirty = true;
                return index;
            }
        }
        throw new IllegalStateException("Bitmap exhausted");
    }

    public void free(int index) {
        checkIndex(index);
        long mask = 1L << (index & 63);
        if ((bits[index >> 6] & mask) == 0) {
            throw new IllegalStateException("Element " + index + " is already free");
        }
        bits[index >> 6] &= ~mask;
        dirty = true;
    }

    public boolean isAllocated(int index) {
        checkIndex(index);
        return (bits[index >> 6] & (1L << (index & 63))) != 0;
    }

    public int countFree() {
        int allocated = 0;
        for (long word : bits) {
            allocated += Long.bitCount(word);
        }
        return elementCount - allocated;
    }

    public int capacity() {
        return elementCount;
    }

    /** Writes the bit image back to disk if modified. */
    public void flush() {
        if (!dirty) {
            return;
        }
        int bitsPerBlock = disk.blockSize() * Byte.SIZE;
        int wordsWritten = 0;
        for (int b = 0; b < blocksInRun; b++) {
            ByteBuffer buffer = ByteBuffer.allocate(disk.blockSize())
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < bitsPerBlock && wordsWritten <= bits.length; i += 64) {
                if (wordsWritten < bits.length) {
                    buffer.putLong(bits[wordsWritten++]);
                } else {
                    buffer.putLong(0L);
                }
            }
            disk.writeBlock(firstBlock + b, buffer.flip());
        }
        dirty = false;
    }

    @Override
    public void close() {
        flush();
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= elementCount) {
            throw new IndexOutOfBoundsException("Element " + index + " out of range 0.." + (elementCount - 1));
        }
    }
}
