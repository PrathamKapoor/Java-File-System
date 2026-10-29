package org.microfs.fs;

import org.microfs.dsa.*;
import org.microfs.storage.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Namespace layer managing persistent directory entries through the filesystem.
 */
public class Namespace {

    private final Filesystem fs;
    private String currentPath = "/";

    public Namespace(Filesystem fs) {
        this.fs = fs;
    }

    /** Creates a directory at the given absolute path. */
    public void mkdir(String path) throws Exception {
        path = PathResolver.canonicalize("/", path);
        validateNewPath(path);
        List<String> parts = resolveComponents(path);
        String parentPath = getParentPath(path);
        String dirName = parts.get(parts.size() - 1);

        // Find parent directory inode via resolution (simplified for this phase)
        int parentInode = findInodeByPath(parentPath);
        if (parentInode < 0) throw new IllegalArgumentException("Parent directory not found: " + parentPath);
        if (fs.getInodeTable().get(parentInode).orElseThrow().getType() != FileType.DIRECTORY) throw new IllegalArgumentException("Parent is not a directory");
        if (findInodeByPath(path) >= 0) throw new IllegalArgumentException("Already exists: " + path);
        checkEntryCapacity(parentInode, dirName, 1);

        int newInodeNum = allocateInode();
        Inode dirInode = new Inode(FileType.DIRECTORY);
        dirInode.setPermissions(0755);
        dirInode.setLinkCount(2); // . and ..
        dirInode.setNumber(newInodeNum);

        // Write . and .. entries to a block, then assign to inode
        List<DirectoryEntry> entries = new ArrayList<>();
        entries.add(new DirectoryEntry(".", newInodeNum));
        entries.add(new DirectoryEntry("..", parentInode));

        List<ByteBuffer> blocks = DirectorySerializer.serializeToBlocks(entries);
        int dataBlock = allocateBlock();
        for (int i = 0; i < Math.min(blocks.size(), Inode.DIRECT_COUNT); i++) {
            java.nio.ByteBuffer blockData = blocks.get(i);
            blockData.position(0);
            blockData.limit(512);
            fs.getDisk().writeBlock(dataBlock + i, blockData);
            dirInode.setDirect(i, dataBlock + i);
        }
        if (blocks.size() > Inode.DIRECT_COUNT) {
            int indirectBlock = allocateBlock();
            dirInode.setIndirect(indirectBlock);
        }

        fs.getInodeTable().put(dirInode);

        // Update parent directory entry
        addDirectoryEntry(parentInode, new DirectoryEntry(dirName, newInodeNum));

        fs.getInodeTable().flush();
        fs.getInodeBitmap().flush();
        fs.getBlockBitmap().flush();
    }

    private void addDirectoryEntry(int parentInodeNum, DirectoryEntry entry) throws Exception {
        Inode parentInode = fs.getInodeTable().get(parentInodeNum).orElse(null);
        if (parentInode == null) return;
        if (parentInode.getType() != FileType.DIRECTORY) {
            throw new IllegalArgumentException("Parent is not a directory");
        }
        // Read parent directory blocks
        List<DirectoryEntry> entries = readDirectoryEntries(parentInode);
        if (entries.stream().anyMatch(e -> e.getName().equals(entry.getName()))) throw new IllegalArgumentException("Already exists: " + entry.getName());
        entries.add(entry);
        writeDirectoryEntries(parentInode, entries);
    }

    private void writeDirectoryEntries(Inode inode, List<DirectoryEntry> entries) throws Exception {
        List<ByteBuffer> blocks = DirectorySerializer.serializeToBlocks(entries);
        if (blocks.size() > Inode.DIRECT_COUNT) throw new IllegalStateException("Directory is full");
        int missing = 0;
        for (int i = 0; i < blocks.size(); i++) if (inode.getDirect(i) == Inode.NO_BLOCK) missing++;
        if (fs.getBlockBitmap().countFree() < missing) throw new IllegalStateException("Not enough free blocks");
        for (int i = 0; i < blocks.size(); i++) {
            if (inode.getDirect(i) == Inode.NO_BLOCK) inode.setDirect(i, allocateBlock());
            ByteBuffer data = blocks.get(i); data.position(0); data.limit(512);
            fs.getDisk().writeBlock(inode.getDirect(i), data);
        }
        for (int i = blocks.size(); i < Inode.DIRECT_COUNT; i++) {
            int block = inode.getDirect(i);
            if (block != Inode.NO_BLOCK) {
                fs.getBlockBitmap().free(block - fs.getSuperblock().getDataRegionBlock());
                inode.setDirect(i, Inode.NO_BLOCK);
            }
        }
        inode.setMtime(System.currentTimeMillis());
        fs.getInodeTable().put(inode); fs.getInodeTable().flush(); fs.getBlockBitmap().flush();
    }

    private void checkEntryCapacity(int parentNumber, String name, int extraBlocks) throws Exception {
        Inode parent = fs.getInodeTable().get(parentNumber).orElseThrow();
        List<DirectoryEntry> entries = readDirectoryEntries(parent);
        entries.add(new DirectoryEntry(name, 0));
        int count = DirectorySerializer.serializeToBlocks(entries).size();
        if (count > Inode.DIRECT_COUNT) throw new IllegalStateException("Directory is full");
        int missing = extraBlocks;
        for (int i = 0; i < count; i++) if (parent.getDirect(i) == Inode.NO_BLOCK) missing++;
        if (fs.getBlockBitmap().countFree() < missing) throw new IllegalStateException("Not enough free blocks");
        if (fs.getInodeBitmap().countFree() < 1) throw new IllegalStateException("No free inodes");
    }

    private List<DirectoryEntry> readDirectoryEntries(Inode inode) throws Exception {
        List<DirectoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < Inode.DIRECT_COUNT; i++) {
            int block = inode.getDirect(i);
            if (block != Inode.NO_BLOCK) {
                ByteBuffer blockData = fs.getDisk().readBlock(block);
                entries.addAll(DirectorySerializer.deserialize(blockData));
            }
        }
        int indirect = inode.getIndirect();
        if (indirect != Inode.NO_BLOCK) {
            ByteBuffer indirectData = fs.getDisk().readBlock(indirect);
            // Indirect block contains block numbers; for simplicity in basic phase, treat as additional entries
            // Full indirect block processing is deferred; but we'll read any blocks it references
            for (int i = 0; i < indirectData.remaining() / 4 && i < 10; i++) {
                indirectData.getInt(); // skip for basic phase
            }
        }
        return entries;
    }

    private int findInodeByPath(String path) throws Exception {
        List<String> components = resolveComponents(path);
        int currentInode = 0; // root inode
        for (String component : components) {
            if (component.isEmpty()) continue;
            Inode dirInode = fs.getInodeTable().get(currentInode).orElse(null);
            if (dirInode == null || dirInode.getType() != FileType.DIRECTORY) return -1;
            List<DirectoryEntry> entries = readDirectoryEntries(dirInode);
            int found = -1;
            for (DirectoryEntry e : entries) {
                if (e.getName().equals(component)) {
                    found = e.getInode();
                    break;
                }
            }
            if (found < 0) return -1;
            currentInode = found;
        }
        return currentInode;
    }

    private int allocateInode() throws Exception {
        Bitmap bitmap = fs.getInodeBitmap();
        int inode = bitmap.allocate();
        return inode;
    }

    private int allocateBlock() throws Exception {
        Bitmap bitmap = fs.getBlockBitmap();
        return fs.getSuperblock().getDataRegionBlock() + bitmap.allocate();
    }

    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return "/";
        return path.substring(0, lastSlash);
    }

    private List<String> resolveComponents(String path) {
        List<String> tokens = new ArrayList<>();
        for (String tok : path.split("/")) {
            if (!tok.isEmpty() && !tok.equals(".") && !tok.equals("..")) tokens.add(tok);
        }
        return tokens;
    }

    public List<String> ls(String path) throws Exception {
        path = PathResolver.canonicalize("/", path);
        int inodeNum = findInodeByPath(path);
        if (inodeNum < 0) throw new IllegalArgumentException("Path not found: " + path);
        Inode inode = fs.getInodeTable().get(inodeNum).orElse(null);
        if (inode == null || inode.getType() != FileType.DIRECTORY) throw new IllegalArgumentException("Not a directory: " + path);
        List<DirectoryEntry> entries = readDirectoryEntries(inode);
        List<String> names = new ArrayList<>();
        for (DirectoryEntry e : entries) {
            if (!e.getName().equals(".") && !e.getName().equals("..")) {
                names.add(e.getName());
            }
        }
        names.sort(String::compareTo);
        return names;
    }

    public void createFile(String path) throws Exception {
        path = PathResolver.canonicalize("/", path);
        validateNewPath(path);
        List<String> parts = resolveComponents(path);
        String fileName = parts.get(parts.size() - 1);
        String parentPath = getParentPath(path);
        int parentInodeNum = findInodeByPath(parentPath);
        if (parentInodeNum < 0) throw new IllegalArgumentException("Parent directory not found: " + parentPath);
        if (fs.getInodeTable().get(parentInodeNum).orElseThrow().getType() != FileType.DIRECTORY) throw new IllegalArgumentException("Parent is not a directory");
        if (findInodeByPath(path) >= 0) throw new IllegalArgumentException("Already exists: " + path);
        checkEntryCapacity(parentInodeNum, fileName, 0);

        int fileInodeNum = allocateInode();
        Inode fileInode = new Inode(FileType.REGULAR_FILE);
        fileInode.setPermissions(0644);
        fileInode.setLinkCount(1);
        fileInode.setNumber(fileInodeNum);
        fileInode.setSize(0);
        fileInode.setMtime(System.currentTimeMillis());
        fs.getInodeTable().put(fileInode);
        fs.getInodeTable().flush();
        fs.getInodeBitmap().flush();
        addDirectoryEntry(parentInodeNum, new DirectoryEntry(fileName, fileInodeNum));
    }

    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String path) { this.currentPath = path; }

    private Inode requireFile(String path) throws Exception {
        path = PathResolver.canonicalize("/", path);
        int number = findInodeByPath(path);
        if (number < 0) throw new IllegalArgumentException("File not found: " + path);
        Inode inode = fs.getInodeTable().get(number).orElseThrow();
        if (inode.getType() != FileType.REGULAR_FILE) throw new IllegalArgumentException("Not a file: " + path);
        return inode;
    }

    public byte[] readFile(String path) throws Exception {
        return new FileStorage(fs).readFile(requireFile(path));
    }

    public void writeFile(String path, byte[] data) throws Exception {
        Inode inode = requireFile(path);
        inode.setMtime(System.currentTimeMillis());
        new FileStorage(fs).writeFile(inode, data);
    }

    public void deleteFile(String path) throws Exception {
        path = PathResolver.canonicalize("/", path);
        Inode inode = requireFile(path);
        if (inode.getIndirect() != Inode.NO_BLOCK) throw new IllegalStateException("Indirect files are not supported for deletion");
        int parentNum = findInodeByPath(getParentPath(path));
        Inode parent = fs.getInodeTable().get(parentNum).orElseThrow();
        List<DirectoryEntry> entries = readDirectoryEntries(parent);
        String name = resolveComponents(path).get(resolveComponents(path).size() - 1);
        if (!entries.removeIf(e -> e.getName().equals(name))) throw new IllegalArgumentException("File not found: " + path);
        writeDirectoryEntries(parent, entries);
        for (int i = 0; i < Inode.DIRECT_COUNT; i++) {
            int block = inode.getDirect(i);
            if (block != Inode.NO_BLOCK) fs.getBlockBitmap().free(block - fs.getSuperblock().getDataRegionBlock());
        }
        fs.getInodeTable().free(inode.getNumber());
        fs.getInodeBitmap().free(inode.getNumber());
        fs.getInodeBitmap().flush();
        fs.getBlockBitmap().flush();
    }

    public Inode stat(String path) throws Exception {
        path = PathResolver.canonicalize("/", path);
        int number = findInodeByPath(path);
        if (number < 0) throw new IllegalArgumentException("Path not found: " + path);
        return fs.getInodeTable().get(number).orElseThrow();
    }

    public void chmod(String path, int permissions) throws Exception {
        if (permissions < 0 || permissions > 0777) throw new IllegalArgumentException("Invalid permissions");
        Inode inode = stat(path);
        inode.setPermissions(permissions);
        fs.getInodeTable().put(inode); fs.getInodeTable().flush();
    }

    public void move(String source, String destination) throws Exception {
        source = PathResolver.canonicalize("/", source);
        destination = PathResolver.canonicalize("/", destination);
        validateNewPath(destination);
        if (source.equals("/") || destination.equals("/")) throw new IllegalArgumentException("Cannot move root");
        Inode inode = stat(source);
        if (findInodeByPath(destination) >= 0) throw new IllegalArgumentException("Destination already exists");
        if (inode.getType() == FileType.DIRECTORY && destination.startsWith(source + "/")) throw new IllegalArgumentException("Cannot move a directory into itself");
        Inode oldParent = stat(getParentPath(source));
        Inode newParent = stat(getParentPath(destination));
        if (newParent.getType() != FileType.DIRECTORY) throw new IllegalArgumentException("Destination parent is not a directory");
        String oldName = source.substring(source.lastIndexOf('/') + 1);
        String newName = destination.substring(destination.lastIndexOf('/') + 1);
        if (newName.isEmpty() || newName.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 255) throw new IllegalArgumentException("Invalid name");
        List<DirectoryEntry> oldEntries = readDirectoryEntries(oldParent);
        if (oldParent.getNumber() == newParent.getNumber()) {
            oldEntries.removeIf(e -> e.getName().equals(oldName));
            oldEntries.add(new DirectoryEntry(newName, inode.getNumber()));
            writeDirectoryEntries(oldParent, oldEntries);
        } else {
            List<DirectoryEntry> newEntries = readDirectoryEntries(newParent);
            newEntries.add(new DirectoryEntry(newName, inode.getNumber()));
            if (DirectorySerializer.serializeToBlocks(newEntries).size() > Inode.DIRECT_COUNT) throw new IllegalStateException("Destination directory is full");
            writeDirectoryEntries(newParent, newEntries);
            oldEntries.removeIf(e -> e.getName().equals(oldName));
            writeDirectoryEntries(oldParent, oldEntries);
            if (inode.getType() == FileType.DIRECTORY) {
                List<DirectoryEntry> moved = readDirectoryEntries(inode);
                moved.removeIf(e -> e.getName().equals(".."));
                moved.add(new DirectoryEntry("..", newParent.getNumber()));
                writeDirectoryEntries(inode, moved);
            }
        }
    }

    private static void validateNewPath(String path) {
        if (path.equals("/")) throw new IllegalArgumentException("Root is not a new entry");
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 255) throw new IllegalArgumentException("Name is too long");
    }
}
