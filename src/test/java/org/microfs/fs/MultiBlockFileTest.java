package org.microfs.fs;

import org.junit.jupiter.api.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

public class MultiBlockFileTest {

    private static final Path TEST_IMAGE = Paths.get("multiblock.img");

    @BeforeEach
    void setup() throws Exception {
        if (Files.exists(TEST_IMAGE)) Files.delete(TEST_IMAGE);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (Files.exists(TEST_IMAGE)) Files.delete(TEST_IMAGE);
    }

    @Test
    void testMultiBlockFilePersistence() throws Exception {
        Filesystem fs = new Filesystem(TEST_IMAGE);
        fs.format(512, 500);
        fs.mount();

        Namespace ns = new Namespace(fs);
        ns.mkdir("/projects");
        ns.createFile("/projects/large.txt");
        fs.unmount();

        fs = new Filesystem(TEST_IMAGE);
        fs.mount();
        ns = new Namespace(fs);
        assertNotNull(ns.ls("/projects"));
        assertTrue(ns.ls("/projects").contains("large.txt"));
        fs.unmount();
    }
}
