package org.microfs.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class FileOperationsTest {
    @TempDir Path temp;

    @Test void fileContentsAndDeletionSurviveRemount() throws Exception {
        Path image = temp.resolve("files.img");
        Filesystem fs = new Filesystem(image);
        fs.format(512, 200);
        fs.mount();
        Namespace ns = new Namespace(fs);
        ns.mkdir("/docs");
        ns.createFile("/docs/note.txt");
        ns.writeFile("/docs/note.txt", "hello filesystem".getBytes(StandardCharsets.UTF_8));
        fs.unmount();
        fs.mount();
        ns = new Namespace(fs);
        assertEquals("hello filesystem", new String(ns.readFile("/docs/note.txt"), StandardCharsets.UTF_8));
        int freeBefore = fs.getBlockBitmap().countFree();
        ns.deleteFile("/docs/note.txt");
        assertEquals(freeBefore + 1, fs.getBlockBitmap().countFree());
        fs.unmount();
        fs.mount();
        ns = new Namespace(fs);
        assertFalse(ns.ls("/docs").contains("note.txt"));
        fs.unmount();
    }

    @Test void nestedPathsMovesAndMetadataSurviveRemount() throws Exception {
        Path image = temp.resolve("moves.img");
        Filesystem fs = new Filesystem(image);
        fs.format(512, 200); fs.mount();
        Namespace ns = new Namespace(fs);
        ns.mkdir("/docs"); ns.mkdir("/archive"); ns.mkdir("/docs/nested");
        ns.createFile("/docs/nested/a.txt");
        ns.writeFile("/docs/nested/a.txt", "contents".getBytes(StandardCharsets.UTF_8));
        ns.move("/docs/nested/a.txt", "/archive/b.txt");
        ns.chmod("/archive/b.txt", 0600);
        assertEquals(0600, ns.stat("/archive/b.txt").getPermissions());
        Namespace initial = ns;
        assertThrows(IllegalArgumentException.class, () -> initial.createFile("/missing/a.txt"));
        fs.unmount(); fs.mount(); ns = new Namespace(fs);
        assertEquals("contents", new String(ns.readFile("/archive/b.txt"), StandardCharsets.UTF_8));
        assertEquals(0600, ns.stat("/archive/b.txt").getPermissions());
        assertFalse(ns.ls("/docs/nested").contains("a.txt"));
        fs.unmount();
    }

    @Test void overwriteReleasesOldBlocksAndRejectsOversize() throws Exception {
        Filesystem fs = new Filesystem(temp.resolve("overwrite.img"));
        fs.format(512, 200); fs.mount();
        Namespace ns = new Namespace(fs); ns.createFile("/f");
        int free = fs.getBlockBitmap().countFree();
        ns.writeFile("/f", new byte[700]);
        assertEquals(free - 2, fs.getBlockBitmap().countFree());
        ns.writeFile("/f", new byte[10]);
        assertEquals(free - 1, fs.getBlockBitmap().countFree());
        assertThrows(IllegalArgumentException.class, () -> ns.writeFile("/f", new byte[2049]));
        assertEquals(10, ns.readFile("/f").length);
        fs.unmount();
    }

    @Test void pathNormalizationHandlesParentComponents() {
        assertEquals("/home/note", PathResolver.canonicalize("/home/projects", "../note"));
        assertEquals("/", PathResolver.canonicalize("/", "../../"));
        assertEquals("/a/c", PathResolver.canonicalize("/", "/a/b/../c"));
    }
}
