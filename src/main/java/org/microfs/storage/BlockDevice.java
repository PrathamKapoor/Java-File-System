package org.microfs.storage;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Fixed-block virtual disk over a single image file.
 *
 * <p>On-disk layout: block 0 = superblock, then raw data blocks. Little-endian
 * encoding everywhere. All offsets are computed from {@link #BLOCK_SIZE_OFFSET}
 * so the layout stays deterministic across versions.</p>
 */
public final class BlockDevice implements AutoCloseable {

    /** Magic constant stored in the first 8 bytes ("MICROFS\0"). */
    static final byte[] MAGIC = {'M', 'I', 'C', 'R', 'O', 'F', 'S', 0};

    private static final long BLOCK_SIZE_OFFSET = 8L;
    private static final long BLOCK_COUNT_OFFSET = 12L;
    private static final int HEADER_SIZE = 20;

    private final FileChannel channel;
    private final int blockSize;
    private final long blockCount;

    private BlockDevice(FileChannel channel, int blockSize, long blockCount) {
        this.channel = channel;
        this.blockSize = blockSize;
        this.blockCount = blockCount;
    }

    /** Creates (or re-creates) an image with zeroed blocks and a valid header. */
    public static BlockDevice format(Path image, int blockSize, long blockCount) {
        validateGeometry(blockSize, blockCount);
        long totalBytes = HEADER_SIZE + blockCount * (long) blockSize;
        try (FileChannel ch = FileChannel.open(image, StandardOpenOption.CREATE,
                StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            header.put(MAGIC);
            header.putInt(blockSize);
            header.putLong(blockCount);
            header.flip();
            while (header.hasRemaining()) {
                ch.write(header);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot format image " + image, e);
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(image.toFile(), "rw");
            raf.setLength(totalBytes);
            raf.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot size image " + image, e);
        }
        return open(image);
    }

    public static BlockDevice open(Path image) {
        try (FileChannel probe = FileChannel.open(image, StandardOpenOption.READ)) {
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            readFully(probe, header, 0L);
            header.flip();
            byte[] magic = new byte[MAGIC.length];
            header.get(magic);
            if (!java.util.Arrays.equals(magic, MAGIC)) {
                throw new IllegalArgumentException("Not a MicroFS image: bad magic in " + image);
            }
            int blockSize = header.getInt();
            long blockCount = header.getLong();
            validateGeometry(blockSize, blockCount);
            return new BlockDevice(FileChannel.open(image, StandardOpenOption.READ, StandardOpenOption.WRITE), blockSize, blockCount);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot open image " + image, e);
        }
    }

    public int blockSize() {
        return blockSize;
    }

    public long blockCount() {
        return blockCount;
    }

    /** Reads one full block. Bounds are checked; partial reads are impossible by contract. */
    public ByteBuffer readBlock(long block) {
        checkBlock(block);
        ByteBuffer buffer = ByteBuffer.allocate(blockSize).order(ByteOrder.LITTLE_ENDIAN);
        try {
            readFully(channel, buffer, blockOffset(block));
        } catch (IOException e) {
            throw new UncheckedIOException("Read failed for block " + block, e);
        }
        buffer.flip();
        return buffer;
    }

    /** Writes one full block. Buffer must contain exactly blockSize bytes remaining. */
    public void writeBlock(long block, ByteBuffer data) {
        checkBlock(block);
        if (data.remaining() != blockSize) {
            throw new IllegalArgumentException("Expected " + blockSize + " bytes, got " + data.remaining());
        }
        try {
            while (data.hasRemaining()) {
                channel.write(data, blockOffset(block));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Write failed for block " + block, e);
        }
    }

    private static void readFully(FileChannel ch, ByteBuffer buffer, long position) throws IOException {
        while (buffer.hasRemaining()) {
            if (ch.read(buffer, position + (long) buffer.position()) < 0) {
                throw new EOFException("Unexpected end of image");
            }
        }
    }

    private long blockOffset(long block) {
        return HEADER_SIZE + block * (long) blockSize;
    }

    private void checkBlock(long block) {
        if (block < 0 || block >= blockCount) {
            throw new IndexOutOfBoundsException("Block " + block + " out of range 0.." + (blockCount - 1));
        }
    }

    private static void validateGeometry(int blockSize, long blockCount) {
        if (blockSize <= 0 || (blockSize & (blockSize - 1)) != 0 || blockSize < 64 || blockSize > 1 << 16) {
            throw new IllegalArgumentException("Block size must be a power of two in [64, 65536]: " + blockSize);
        }
        if (blockCount <= 0) {
            throw new IllegalArgumentException("Block count must be positive: " + blockCount);
        }
        if (blockCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Block count too large: " + blockCount);
        }
    }

    @Override
    public void close() {
        try {
            channel.force(true);
            channel.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Close failed", e);
        }
    }
}
