package org.microfs.fs;

import org.junit.jupiter.api.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceRestartTest {

    private static final Path TEST_IMAGE = Paths.get("restart.img");

    @BeforeEach
    void setup() throws Exception {
        if (Files.exists(TEST_IMAGE)) Files.delete(TEST_IMAGE);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (Files.exists(TEST_IMAGE)) Files.delete(TEST_IMAGE);
    }

    @Test
    void testFullRestartCycle() throws Exception {
        // Phase 1: Create and populate
        Filesystem fs = new Filesystem(TEST_IMAGE);
        fs.format(512, 200);
        fs.unmount();

        fs = new Filesystem(TEST_IMAGE);
        fs.mount();
        Namespace ns = new Namespace(fs);
        ns.mkdir("/home");
        ns.mkdir("/home/user");
        ns.createFile("/home/user/notes.txt");
        fs.unmount();

        // Phase 2: Remount and verify
        fs = new Filesystem(TEST_IMAGE);
        fs.mount();
        ns = new Namespace(fs);
        assertNotNull(ns.ls("/home"));
        assertTrue(ns.ls("/home/user").contains("notes.txt"));
        fs.unmount();
    }

    @Test
    void testFsckAfterRestart() throws Exception {
        Filesystem fs = new Filesystem(TEST_IMAGE);
        fs.format(512, 200);
        fs.unmount();
        fs.mount();
        fs.unmount();

        Fsck.Result result = Fsck.check(TEST_IMAGE.toString());
        assertTrue(result.isValid(), "FSCK should pass after restart: " + result.errors);
    }
}
