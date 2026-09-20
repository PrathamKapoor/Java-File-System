import org.microfs.fs.*;
import java.nio.file.*;

public class FinalDemo {
    public static void main(String[] args) throws Exception {
        String image = "fresh5.img";
        Path path = Paths.get(image);
        if (Files.exists(path)) Files.delete(path);

        // Step 1: Format
        System.out.println("=== FORMAT ===");
        Filesystem fs = new Filesystem(path);
        fs.format(512, 200);
        System.out.println("Formatted: " + image);

        // Step 2: Mount, create namespace, unmount
        System.out.println("=== FIRST SESSION ===");
        fs = new Filesystem(path);
        fs.mount();
        Namespace ns = new Namespace(fs);
        ns.mkdir("/home");
        ns.mkdir("/home/user");
        ns.createFile("/home/user/notes.txt");
        System.out.println("Created /home/user/notes.txt");
        System.out.println("Directory contents: " + ns.ls("/home/user"));
        fs.unmount();
        System.out.println("Unmounted first session");

        // Step 3: Remount in new process
        System.out.println("=== SECOND SESSION ===");
        fs = new Filesystem(path);
        fs.mount();
        ns = new Namespace(fs);
        System.out.println("After remount, /home/user contents: " + ns.ls("/home/user"));
        System.out.println("File exists: " + ns.ls("/home/user").contains("notes.txt"));
        fs.unmount();
        System.out.println("Unmounted second session");

        // Step 4: FSCK
        System.out.println("=== FSCK ===");
        Fsck.Result result = Fsck.check(image);
        System.out.println("FSCK: " + (result.isValid() ? "PASS" : "FAIL"));
        for (String err : result.errors) System.out.println("  - " + err);
        System.out.println("Inodes checked: " + result.inodeChecked);

        System.out.println("=== DEMO COMPLETE ===");

        // Cleanup
        Files.deleteIfExists(path);
    }
}
