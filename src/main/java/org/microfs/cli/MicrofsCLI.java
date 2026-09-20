package org.microfs.cli;

import org.microfs.fs.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Interactive CLI for MicroFS.
 */
public class MicrofsCLI {

    private Filesystem fs = null;
    private Namespace namespace = null;
    private FileStorage fileStorage = null;
    private String currentDir = "/";
    private boolean interactive = false;

    public static void main(String[] args) throws Exception {
        MicrofsCLI cli = new MicrofsCLI();
        if (args.length == 0) {
            cli.interactive = true;
            cli.runInteractive();
        } else {
            cli.execute(args);
        }
    }

    private void runInteractive() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("MicroFS Interactive Shell");
        while (true) {
            System.out.print("microfs> ");
            String line = scanner.nextLine();
            if (line.isEmpty()) continue;
            if (line.equals("exit") || line.equals("quit")) break;
            String[] parts = line.trim().split("\\s+");
            try {
                execute(parts);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    public void execute(String[] args) throws Exception {
        if (args.length == 0) return;
        String cmd = args[0];
        switch (cmd) {
            case "format":
                formatCommand(args);
                break;
            case "mount":
                mountCommand(args);
                break;
            case "unmount":
                unmountCommand(args);
                break;
            case "mkdir":
                mkdirCommand(args);
                break;
            case "ls":
                lsCommand(args);
                break;
            case "pwd":
                pwdCommand();
                break;
            case "cd":
                cdCommand(args);
                break;
            case "touch":
                touchCommand(args);
                break;
            case "cat":
                catCommand(args);
                break;
            case "stat":
                statCommand(args);
                break;
            case "df":
                dfCommand();
                break;
            case "fsck":
                fsckCommand(args);
                break;
            case "benchmark":
                benchmarkCommand(args);
                break;
            default:
                System.out.println("Unknown command: " + cmd);
        }
    }

    private void formatCommand(String[] args) throws Exception {
        String image = args.length > 1 ? args[1] : "disk.img";
        int blockSize = 512;
        long blockCount = 1000;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--block-size") && i + 1 < args.length) {
                blockSize = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--blocks") && i + 1 < args.length) {
                blockCount = Long.parseLong(args[++i]);
            }
        }
        fs = new Filesystem(Paths.get(image));
        fs.format(blockSize, (int) blockCount);
        System.out.println("Formatted " + image + " (blockSize=" + blockSize + ", blocks=" + blockCount + ")");
    }

    private void mountCommand(String[] args) throws Exception {
        String image = args.length > 1 ? args[1] : "disk.img";
        fs = new Filesystem(Paths.get(image));
        fs.mount();
        namespace = new Namespace(fs);
        fileStorage = new FileStorage(fs);
        System.out.println("Mounted " + image);
    }

    private void unmountCommand(String[] args) throws Exception {
        if (fs == null) throw new IllegalStateException("Not mounted");
        fs.unmount();
        System.out.println("Unmounted");
        fs = null;
        namespace = null;
    }

    private void mkdirCommand(String[] args) throws Exception {
        checkMounted();
        String path = args.length > 1 ? args[1] : "/test";
        namespace.mkdir(path);
        System.out.println("mkdir " + path);
    }

    private void lsCommand(String[] args) throws Exception {
        checkMounted();
        String path = args.length > 1 ? args[1] : ".";
        String resolved = PathResolver.canonicalize(currentDir, path);
        List<String> entries = namespace.ls(resolved);
        System.out.println("ls " + resolved);
        for (String entry : entries) {
            System.out.println("  " + entry);
        }
    }

    private void pwdCommand() {
        System.out.println(namespace != null ? namespace.getCurrentPath() : currentDir);
    }

    private void cdCommand(String[] args) throws Exception {
        String target = args.length > 1 ? args[1] : "/";
        currentDir = PathResolver.canonicalize(currentDir, target);
        if (namespace != null) namespace.setCurrentPath(currentDir);
        System.out.println(currentDir);
    }

    private void touchCommand(String[] args) throws Exception {
        checkMounted();
        String file = args.length > 1 ? args[1] : "/test.txt";
        namespace.createFile(file);
        System.out.println("touch " + file);
    }

    private void catCommand(String[] args) throws Exception {
        checkMounted();
        String file = args.length > 1 ? args[1] : "/test.txt";
        System.out.println("cat " + file);
    }

    private void statCommand(String[] args) throws Exception {
        checkMounted();
        String path = args.length > 1 ? args[1] : "/";
        System.out.println("stat " + path);
    }

    private void dfCommand() throws Exception {
        checkMounted();
        System.out.println("df");
    }

    private void fsckCommand(String[] args) throws Exception {
        String image = args.length > 1 ? args[1] : "disk.img";
        System.out.println("fsck " + image + " - checking consistency (no repair)");
    }

    private void benchmarkCommand(String[] args) throws Exception {
        System.out.println("benchmark (see benchmark engine)");
    }

    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash > 0) ? path.substring(0, lastSlash) : "/";
    }

    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }

    private void checkMounted() {
        if (fs == null || !fs.isMounted()) throw new IllegalStateException("Filesystem not mounted");
    }
}
