import org.microfs.storage.*;
import java.nio.file.*;
import java.nio.*;

public class InspectInode {
    public static void main(String[] args) throws Exception {
        BlockDevice disk = BlockDevice.open(Paths.get("fresh.img"));
        ByteBuffer buf = disk.readBlock(4);
        System.out.println("Block 4 content (first 20 bytes):");
        for (int i = 0; i < 20; i++) {
            System.out.printf("%02X ", buf.get(i));
        }
        System.out.println();
        System.out.println("First byte (file type): " + (buf.get(0) & 0xFF));
        System.out.println("Byte at offset 1: " + (buf.get(1) & 0xFF));
    }
}
