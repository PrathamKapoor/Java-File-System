package org.microfs.fs;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceTest {

    private static final Path TEST_IMAGE = Paths.get("test_disk.img");

    @BeforeEach
    void setup() throws Exception {
        if (Files.exists(TEST_IMAGE)) Files.delete(TEST_IMAGE);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (Files.exists(TEST_IMAGE)) Files.delete(TEST_IMAGE);
    }

    @Test
    void testFormatAndMount() throws Exception {
        Filesystem fs = new Filesystem(TEST_IMAGE);
        fs.format(512, 200);
        assertTrue(Files.exists(TEST_IMAGE));
        fs.unmount();
        fs.mount();
        assertTrue(fs.isMounted());
        fs.unmount();
    }

    @Test
    void testSuperblockPersistence() throws Exception {
        Filesystem fs = new Filesystem(TEST_IMAGE);
        fs.format(512, 200);
        fs.unmount();
        fs.mount();
        assertNotNull(fs.getSuperblock());
        assertEquals(0x4D46, fs.getSuperblock().getMagic());
        fs.unmount();
    }
}
