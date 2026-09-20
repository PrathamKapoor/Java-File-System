package org.microfs.fs;

import org.microfs.dsa.DirectoryEntry;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Serializes and deserializes directory entries to/from disk blocks.
 *
 * <p>Format per entry (little-endian, variable length, 4-byte aligned):</p>
 * <pre>
 * 0..3  : inode number (int)
 * 4..5  : name length (short, max 255)
 * 6..N  : name bytes (UTF-8, length = name length)
 * next  : padding to 4-byte boundary
 * </pre>
 */
public final class DirectorySerializer {

    public static final int MAX_NAME_LENGTH = 255;

    public static int entrySize(DirectoryEntry entry) {
        int nameLen = entry.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        return 6 + nameLen + ((4 - (6 + nameLen) % 4) % 4);
    }

    public static List<DirectoryEntry> deserialize(ByteBuffer block) {
        List<DirectoryEntry> entries = new ArrayList<>();
        block.flip();
        while (block.remaining() >= 6) {
            int inode = block.getInt();
            if (inode <= 0) {
                // End of valid entries in this block
                break;
            }
            int nameLen = block.getShort() & 0xFFFF;
            if (nameLen < 1 || nameLen > MAX_NAME_LENGTH || block.remaining() < nameLen) {
                break;
            }
            byte[] nameBytes = new byte[nameLen];
            block.get(nameBytes);
            String name = new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8);
            if (name.isEmpty() || name.contains("\0")) {
                break;
            }
            int current = block.position();
            int pad = (4 - (current % 4)) % 4;
            if (pad > block.remaining()) {
                // Not enough padding bytes; stop reading
                break;
            }
            block.position(current + pad);
            entries.add(new DirectoryEntry(name, inode));
        }
        return entries;
    }

    public static ByteBuffer serialize(List<DirectoryEntry> entries) {
        int maxEntriesPerBlock = 512 / 6; // approximate; we'll just serialize sequentially
        ByteBuffer buf = ByteBuffer.allocate(512).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (DirectoryEntry entry : entries) {
            byte[] nameBytes = entry.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (nameBytes.length > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException("Name too long: " + entry.getName());
            }
            int size = 6 + nameBytes.length + ((4 - (6 + nameBytes.length) % 4) % 4);
            if (buf.remaining() < size) {
                break; // block full
            }
            buf.putInt(entry.getInode());
            buf.putShort((short) nameBytes.length);
            buf.put(nameBytes);
            int current = buf.position();
            int pad = (4 - (current % 4)) % 4;
            for (int i = 0; i < pad; i++) {
                buf.put((byte) 0);
            }
        }
        return buf;
    }

    /** Serializes entries across multiple blocks if needed (simplified). */
    public static List<ByteBuffer> serializeToBlocks(List<DirectoryEntry> entries) {
        List<ByteBuffer> blocks = new ArrayList<>();
        ByteBuffer current = ByteBuffer.allocate(512).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (DirectoryEntry entry : entries) {
            byte[] nameBytes = entry.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int size = 6 + nameBytes.length + ((4 - (6 + nameBytes.length) % 4) % 4);
            if (current.remaining() < size) {
                // Don't flip - caller will reset position
                current.position(0);
                current.limit(512);
                blocks.add(current);
                current = ByteBuffer.allocate(512).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            }
            current.putInt(entry.getInode());
            current.putShort((short) nameBytes.length);
            current.put(nameBytes);
            int pos = current.position();
            int pad = (4 - (pos % 4)) % 4;
            for (int i = 0; i < pad; i++) current.put((byte) 0);
        }
        // Don't flip - return buffer as-is with data written sequentially
        // Caller must reset position to 0 and set limit to capacity (512) before writing
        blocks.add(current);
        return blocks;
    }
}
