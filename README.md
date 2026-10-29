# MicroFS

MicroFS is a small Java filesystem simulator built around a virtual disk image. It is an academic project for exploring how data structures support directory lookups, block allocation and persistent metadata. It is not an operating-system filesystem.

## Getting started

Requirements: JDK 21 or newer and Maven.

```sh
mvn test
mvn compile
java -cp target/classes org.microfs.cli.MicrofsCLI
```

At the `microfs>` prompt, create a **new** image and try the filesystem:

```text
format demo.img
mount demo.img
mkdir /projects
touch /projects/note.txt
write /projects/note.txt Hello from MicroFS
cat /projects/note.txt
mv /projects/note.txt /projects/report.txt
chmod 600 /projects/report.txt
stat /projects/report.txt
ls /projects
unmount
mount demo.img
cat /projects/report.txt
rm /projects/report.txt
unmount
exit
```

`format` overwrites the named image. To reopen an existing image, use `mount` without formatting it again. The default image is `disk.img`; the default geometry is 512-byte blocks and 1,000 blocks. Use the default block size: directory serialization currently assumes 512 bytes.

## Shell commands

| Command | Description |
| --- | --- |
| `format [image] [--block-size N] [--blocks N]` | Create a fresh image. |
| `mount [image]`, `unmount` | Open an image and flush/close it. |
| `mkdir <path>` | Create a directory; its parent must exist. |
| `touch <path>` | Create an empty file. |
| `write <path> <text>` | Replace an existing file's contents with UTF-8 text. |
| `cat <path>` | Display file contents as UTF-8 text. |
| `rm <path>` | Delete a regular file and release its blocks and inode. |
| `mv <source> <destination>` | Rename or move an entry to a new, unused path. |
| `chmod <mode> <path>` | Store a three-digit octal permission mode. |
| `stat [path]` | Show the inode, type, size, permissions and modification time. |
| `ls [path]`, `cd [path]`, `pwd` | List and navigate directories. |
| `exit`, `quit` | Leave the shell; unmount first. |

Permissions are stored metadata; the simulator does not enforce user access rights. `write` accepts text, replaces the whole file, and supports at most four direct blocks. `rm` handles regular files only. The shell's `df`, `fsck` and `benchmark` commands are placeholders. The separate checker can be run after unmounting:

```sh
java -cp target/classes org.microfs.fs.Fsck demo.img
```

## Data structures

- **Bitmap:** tracks inode and data-block allocation with one bit per resource. Allocation scans for a free bit; modified bitmaps are persisted.
- **Inode table:** fixed-size records hold file type, permissions, size, modification time and block pointers. A directory entry maps a name to an inode number.
- **Linear directory:** an `ArrayList` provides a simple baseline with linear lookup; the list is kept sorted on insertion.
- **Hash directory:** a `HashMap` supports expected constant-time exact lookup; listing sorts the entries.
- **Red-black tree:** a balanced ordered index supports logarithmic lookup. The current listing implementation traverses keys and searches for their values.
- **Hybrid directory:** combines the hash and red-black tree indexes for exact lookups and ordered listings, at the cost of maintaining both.
- **Adaptive directory:** switches between linear, hash and hybrid indexes at configurable size thresholds. Its current implementation rebuilds on each update, so construction is expensive.
- **Trie:** a separate character-based structure used to explore prefix search.

The directory indexes are experimental in-memory implementations used by the benchmark. The persistent `Namespace` currently reads directory entries and scans them during path lookup; it does not use the adaptive index for CLI operations.

## Measurements

The recorded results in [latency.csv](results/usability/latency.csv) are specific to the tested machine, Java runtime and generated workload. For example, at 10,000 entries the warm median for exact lookup was 28 µs for linear and 0.1 µs for hash. At 50,000 entries, linear prefix search had a median of about 715 µs and the standalone trie about 1.79 ms. These are in-memory measurements, not end-to-end filesystem timings. The data does not establish why the trie was slower in that run.

The benchmark code is in `org.microfs.benchmark`; `RealWorldValidation` writes results to a directory supplied on the command line. Use a fresh output directory because it overwrites result files.

```sh
java -cp target/classes org.microfs.benchmark.RealWorldValidation results/new-run
```

## Current limits

The filesystem is single-threaded and has no journaling or crash-recovery guarantee. Indirect file writes, directory deletion, symbolic links and hard links are not implemented. Directories have a four-block limit, and some operations do not provide transactional rollback after I/O errors. Large-scale indexing remains separate from the persistent namespace. The [technical guide](docs/FUNCTIONALITY.md) describes the original design; where it differs from the source or this README, the source is authoritative.
