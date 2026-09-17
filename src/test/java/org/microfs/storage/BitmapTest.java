package org.microfs.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitmapTest {

    @Test
    void allocationSurvivesReload(@TempDir Path tmp) {
        Path img = tmp.resolve("disk.img");
        BlockDevice disk = BlockDevice.format(img, 512, 8);
        Bitmap first = new Bitmap(disk, 0, 1, 512);
        int a = first.allocate();
        int b = first.allocate();
        first.free(a);
        first.flush();
        first.close();

        BlockDevice reopened = BlockDevice.open(img);
        Bitmap second = new Bitmap(reopened, 0, 1, 512);
        assertTrue(second.isAllocated(b));
        assertFalse(second.isAllocated(a));
        assertEquals(511, second.countFree());
        assertEquals(a, second.allocate());
        second.close();
    }

    @Test
    void exhaustingBitmapThrowsAndLeavesStateConsistent(@TempDir Path tmp) {
        Path img = tmp.resolve("disk.img");
        BlockDevice disk = BlockDevice.format(img, 512, 8);
        Bitmap bitmap = new Bitmap(disk, 0, 1, 8);
        for (int i = 0; i < 8; i++) {
            assertEquals(i, bitmap.allocate());
        }
        assertThrows(IllegalStateException.class, bitmap::allocate);
        assertEquals(0, bitmap.countFree());
        bitmap.free(3);
        assertEquals(3, bitmap.allocate());
        bitmap.close();
    }

    @Test
    void freeOfUnallocatedBitIsRejected(@TempDir Path tmp) {
        Path img = tmp.resolve("disk.img");
        BlockDevice disk = BlockDevice.format(img, 512, 8);
        Bitmap bitmap = new Bitmap(disk, 0, 1, 512);
        assertThrows(IllegalStateException.class, () -> bitmap.free(7));
        bitmap.close();
    }
}
