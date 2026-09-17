package org.microfs.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InodeTableTest {

    @Test
    void inodePersistsAcrossTableReload(@TempDir Path tmp) {
        Path img = tmp.resolve("disk.img");
        BlockDevice disk = BlockDevice.format(img, 512, 64);
        InodeTable table = new InodeTable(disk, 16);
        Inode original = table.allocate(FileType.REGULAR_FILE);
        original.setSize(7);
        original.setPermissions(0600);
        table.flush();

        InodeTable reloaded = new InodeTable(disk, 16);
        Inode copy = reloaded.get(original.getNumber()).orElseThrow();
        assertEquals(FileType.REGULAR_FILE, copy.getType());
        assertEquals(7, copy.getSize());
        assertEquals(0600, copy.getPermissions());
    }
}
