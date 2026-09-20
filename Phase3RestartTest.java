import org.microfs.fs.*;
import java.nio.file.*;

public class Phase3RestartTest {
    public static void main(String[] args) throws Exception {
        String image = "phase3_restart.img";
        Path p = Paths.get(image);
        if (Files.exists(p)) Files.delete(p);

        // Process A: create filesystem and data
        System.out.println("=== PROCESS A START ===");
        Filesystem fs = new Filesystem(p);
        fs.format(512, 300);
        System.out.println("A: Created filesystem");

        fs = new Filesystem(p);
        fs.mount();
        Namespace ns = new Namespace(fs);
        ns.mkdir("/home");
        ns.mkdir("/home/user");
        ns.mkdir("/home/user/projects");
        ns.createFile("/home/user/notes.txt");
        System.out.println("A: Created /home/user/notes.txt");
        System.out.println("A: /home/user contents: " + ns.ls("/home/user"));
        System.out.println("A: /home contents: " + ns.ls("/home"));
        fs.unmount();
        System.out.println("=== PROCESS A END ===");

        // Process B: remount and verify (simulated by new JVM objects)
        System.out.println("=== PROCESS B START ===");
        fs = new Filesystem(p);
        fs.mount();
        ns = new Namespace(fs);
        System.out.println("B: Remounted");
        System.out.println("B: / contents: " + ns.ls("/"));
        System.out.println("B: /home contents: " + ns.ls("/home"));
        System.out.println("B: /home/user contents: " + ns.ls("/home/user"));
        System.out.println("B: notes.txt exists: " + ns.ls("/home/user").contains("notes.txt"));
        fs.unmount();

        // FSCK
        System.out.println("=== FSCK ===");
        Fsck.Result result = Fsck.check(image);
        System.out.println("FSCK: " + (result.isValid() ? "PASS" : "FAIL"));
        for (String err : result.errors) System.out.println("  - " + err);
        System.out.println("=== PROCESS B END ===");

        System.out.println("=== PHASE 3 RESTART TEST COMPLETE ===");
        Files.deleteIfExists(p);
    }
}
