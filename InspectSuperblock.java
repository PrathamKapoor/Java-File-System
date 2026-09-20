import org.microfs.storage.*;
import java.nio.file.*;
import java.nio.*;

public class InspectSuperblock {
    public static void main(String[] args) throws Exception {
        BlockDevice disk = BlockDevice.open(Paths.get("fresh.img"));
        ByteBuffer superblockBuf = disk.readBlock(1);
        System.out.println("Superblock bytes (first 30):");
        for (int i = 0; i < 30; i++) {
            System.out.printf("%02X ", superblockBuf.get(i));
        }
        System.out.println();
        superblockBuf.flip();
        int magic = superblockBuf.getInt();
        int version = superblockBuf.getInt();
        int bs = superblockBuf.getInt();
        long total = superblockBuf.getLong();
        int inodeCap = superblockBuf.getInt();
        int inodeBitmapBlock = superblockBuf.getInt();
        int blockBitmapBlock = superblockBuf.getInt();
        int inodeTableBlock = superblockBuf.getInt();
        int dataRegionBlock = superblockBuf.getInt();
        int rootInode = superblockBuf.getInt();
        System.out.println("Magic: 0x" + Integer.toHexString(magic));
        System.out.println("Version: " + version);
        System.out.println("Block size: " + bs);
        System.out.println("Total blocks: " + total);
        System.out.println("Inode capacity: " + inodeCap);
        System.out.println("Inode bitmap block: " + inodeBitmapBlock);
        System.out.println("Block bitmap block: " + blockBitmapBlock);
        System.out.println("Inode table block: " + inodeTableBlock);
        System.out.println("Data region block: " + dataRegionBlock);
        System.out.println("Root inode: " + rootInode);
    }
}
