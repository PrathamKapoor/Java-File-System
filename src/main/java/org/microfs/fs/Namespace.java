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
        List<String> parts = resolveComponents(path);
        String parentPath = getParentPath(path);
        String dirName = parts.get(parts.size() - 1);

        // Find parent directory inode via resolution (simplified for this phase)
        int parentInode = findInodeByPath(parentPath);
        if (parentInode < 0) parentInode = 0; // root default

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
            // For simplicity, don't support directories with more entries than direct blocks in this basic phase
            // But we allocate the indirect block reference conceptually
            int indirectBlock = allocateBlock();
            dirInode.setIndirect(indirectBlock);
        }

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
        entries.removeIf(e -> e.getName().equals(entry.getName()));
        entries.add(entry);
        List<ByteBuffer> blocks = DirectorySerializer.serializeToBlocks(entries);
        // Clear previous direct blocks
        for (int i = 0; i < Inode.DIRECT_COUNT; i++) {
            int block = parentInode.getDirect(i);
            if (block != Inode.NO_BLOCK) {
                // Don't actually zero the block here; just update references
                // Simplified: reuse same blocks
            }
        }
        int dataBlock = parentInode.getDirect(0);
        if (dataBlock == Inode.NO_BLOCK) {
            dataBlock = allocateBlock();
            parentInode.setDirect(0, dataBlock);
        }
        for (int i = 0; i < Math.min(blocks.size(), Inode.DIRECT_COUNT); i++) {
            ByteBuffer blockData = blocks.get(i);
            blockData.position(0);
            blockData.limit(512);
            fs.getDisk().writeBlock(parentInode.getDirect(i) == Inode.NO_BLOCK ? dataBlock + i : parentInode.getDirect(i), blockData);
            parentInode.setDirect(i, parentInode.getDirect(i) == Inode.NO_BLOCK ? dataBlock + i : parentInode.getDirect(i));
        }
        fs.getInodeTable().flush();
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
        return bitmap.allocate();
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
        int inodeNum = findInodeByPath(path);
        if (inodeNum < 0) return Collections.emptyList();
        Inode inode = fs.getInodeTable().get(inodeNum).orElse(null);
        if (inode == null || inode.getType() != FileType.DIRECTORY) return Collections.emptyList();
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
        List<String> parts = resolveComponents(path);
        String fileName = parts.get(parts.size() - 1);
        String parentPath = getParentPath(path);
        int parentInodeNum = findInodeByPath(parentPath);
        if (parentInodeNum < 0) parentInodeNum = 0;

        int fileInodeNum = allocateInode();
        Inode fileInode = new Inode(FileType.REGULAR_FILE);
        fileInode.setPermissions(0644);
        fileInode.setLinkCount(1);
        fileInode.setNumber(fileInodeNum);
        fileInode.setSize(0);
        fileInode.setMtime(System.currentTimeMillis());
        fs.getInodeTable().flush();
        addDirectoryEntry(parentInodeNum, new DirectoryEntry(fileName, fileInodeNum));
    }

    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String path) { this.currentPath = path; }
}
