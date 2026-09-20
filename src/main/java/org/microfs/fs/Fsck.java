package org.microfs.fs;

import org.microfs.storage.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Filesystem consistency checker (check only, no repair initially).
 */
public class Fsck {

    public static class Result {
        public boolean superblockOk;
        public final List<String> errors = new ArrayList<>();
        public int inodeChecked = 0;
        public int blockChecked = 0;
        public int errorsFound = 0;

        public Result(boolean superblockOk) {
            this.superblockOk = superblockOk;
        }

        public boolean isValid() {
            return superblockOk && errors.isEmpty();
        }
    }

    public static Result check(String diskImage) {
        Result result = new Result(true);
        try {
            Filesystem fs = new Filesystem(Paths.get(diskImage));
            fs.mount();

            // Superblock checks
            Superblock sb = fs.getSuperblock();
            if (sb == null) {
                result.superblockOk = false;
                result.errors.add("Superblock missing");
            } else {
                if (sb.getMagic() != 0x4D46) {
                    result.errors.add("Superblock bad magic: expected 0x4D46, got 0x" + Integer.toHexString(sb.getMagic()));
                }
                if (sb.getVersion() != 1) {
                    result.errors.add("Superblock unsupported version: " + sb.getVersion());
                }
                if (sb.getBlockSize() <= 0 || sb.getTotalBlocks() <= 0) {
                    result.errors.add("Superblock invalid geometry");
                }
            }

            // Bitmap checks
            Bitmap inodeBitmap = fs.getInodeBitmap();
            Bitmap blockBitmap = fs.getBlockBitmap();
            if (inodeBitmap == null) {
                result.errors.add("Inode bitmap missing");
            }
            if (blockBitmap == null) {
                result.errors.add("Block bitmap missing");
            }

            // Check a sample of inodes
            InodeTable inodeTable = fs.getInodeTable();
            int inodeCapacity = inodeTable.capacity();
            for (int i = 1; Math.min(i, inodeCapacity) > 0 && i < Math.min(inodeCapacity, 20); i++) {
                Optional<Inode> optInode = inodeTable.get(i);
                if (optInode.isPresent() && optInode.get() != null) {
                    Inode inode = optInode.get();
                    result.inodeChecked++;
                    if (inode.getType() == null) {
                        result.errors.add("Inode " + i + " has null type");
                    }
                    if (inode.getLinkCount() < 0) {
                        result.errors.add("Inode " + i + " has negative link count");
                    }
                    // Check direct block references
                    for (int b = 0; b < Inode.DIRECT_COUNT; b++) {
                        int block = inode.getDirect(b);
                        if (block != Inode.NO_BLOCK) {
                            if (block < 0 || block >= fs.getSuperblock().getTotalBlocks()) {
                                result.errors.add("Inode " + i + " references invalid block " + block);
                            }
                        }
                    }
                }
            }

            fs.unmount();
        } catch (Exception e) {
            result.superblockOk = false;
            result.errors.add("Exception during check: " + e.getMessage());
            result.errorsFound = result.errors.size();
        }
        result.errorsFound = result.errors.size();
        return result;
    }

    public static void main(String[] args) throws Exception {
        String image = args.length > 0 ? args[0] : "disk.img";
        Result result = check(image);
        System.out.println("FSCK result: " + (result.isValid() ? "PASS" : "FAIL"));
        System.out.println("Inodes checked: " + result.inodeChecked);
        System.out.println("Errors: " + result.errorsFound);
        for (String err : result.errors) {
            System.out.println("  - " + err);
        }
    }
}
