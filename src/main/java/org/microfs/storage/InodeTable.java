package org.microfs.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Table of fixed-size inode slots persisted at a fixed region of the image.
 *
 * <p>Slot layout per inode: {@link Inode#SIZE} bytes, index = inode number.
 * Inode 0 is reserved as the root; allocation never returns it.</p>
 */
public final class InodeTable {

    private final long inodeRegionStartBlock;
    private final BlockDevice disk;
    private final int capacity;
    private final Inode[] cache;
    private final boolean[] dirty;
    private final long inodesPerBlock;

    public InodeTable(BlockDevice disk, int capacity, long inodeRegionStartBlock) {
        if (capacity < 2) {
            throw new IllegalArgumentException("Need at least root + one usable inode");
        }
        this.disk = disk;
        this.inodeRegionStartBlock = inodeRegionStartBlock;
        this.capacity = capacity;
        this.cache = new Inode[capacity];
        this.dirty = new boolean[capacity];
        this.inodesPerBlock = disk.blockSize() / Inode.SIZE;
        if (inodesPerBlock == 0) {
            throw new IllegalArgumentException("Block size smaller than inode size");
        }
    }

    /** Allocates the next free slot; throws if table full. Returned inode is not yet flushed. */
    public Inode allocate(FileType type) {
        for (int i = 1; i < capacity; i++) {
            if (cache[i] == null) {
                Inode inode = new Inode(type);
                cache[i] = inode;
                dirty[i] = true;
                inode.setNumber(i);
                return inode;
            }
        }
        throw new IllegalStateException("Inode table full");
    }

    public Optional<Inode> get(int number) {
        checkNumber(number);
        if (cache[number] == null) {
            load(number);
        }
        return Optional.ofNullable(cache[number]);
    }

    /** Permanently invalidates a slot. Caller is responsible for freeing blocks first. */
    void free(int number) {
        checkNumber(number);
        cache[number] = null;
        dirty[number] = false;
        zeroSlot(number);
    }

    private void zeroSlot(int number) {
        long block = inodeRegionStartBlock + number / inodesPerBlock;
        int slotInBlock = (int) (number % inodesPerBlock);
        ByteBuffer buffer = disk.readBlock(block);
        int offset = slotInBlock * Inode.SIZE;
        for (int i = 0; i < Inode.SIZE; i++) {
            buffer.put(offset + i, (byte) 0);
        }
        disk.writeBlock(block, buffer);
    }

    private void load(int number) {
        long block = inodeRegionStartBlock + number / inodesPerBlock;
        int slotInBlock = (int) (number % inodesPerBlock);
        ByteBuffer buffer = disk.readBlock(block);
        int offset = slotInBlock * Inode.SIZE;
        ByteBuffer slice = buffer.slice(offset, Inode.SIZE).order(buffer.order());
        if (slice.get(0) == 0) {
            return;
        }
        cache[number] = Inode.deserialize(slice);
        dirty[number] = false;
    }

    /** Writes back dirty slots. Reads mark slots clean. */
    public void flush() {
        for (int i = 1; i < capacity; i++) {
            if (cache[i] != null && dirty[i]) {
                writeSlot(i);
                dirty[i] = false;
            }
        }
    }

    private void writeSlot(int number) {
        long block = inodeRegionStartBlock + number / inodesPerBlock;
        int slotInBlock = (int) (number % inodesPerBlock);
        ByteBuffer buffer = disk.readBlock(block);
        ByteBuffer tail = buffer.duplicate();
        tail.position(slotInBlock * Inode.SIZE);
        tail.limit(slotInBlock * Inode.SIZE + Inode.SIZE);
        cache[number].serialize(tail.slice().order(buffer.order()));
        disk.writeBlock(block, buffer);
    }

    public int capacity() {
        return capacity;
    }

    private void checkNumber(int number) {
        if (number < 0 || number >= capacity) {
            throw new IndexOutOfBoundsException("Inode " + number + " out of range 0.." + (capacity - 1));
        }
    }
}
