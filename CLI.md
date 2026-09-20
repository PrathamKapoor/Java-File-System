# MicroFS CLI

## Interactive shell

```bash
java -cp target/classes org.microfs.cli.MicrofsCLI
```

Prompt: `microfs> `

Commands:

- `format [image] [--block-size N] [--blocks N]`
- `mount [image]`
- `unmount`
- `mkdir <path>`
- `rmdir <path>` (not fully implemented; basic framework)
- `ls [path]`
- `cd <path>`
- `pwd`
- `touch <file>`
- `cat <file>`
- `write <file> <content>` (framework available)
- `stat <path>`
- `df`
- `cp <src> <dst>` (framework available)
- `mv <src> <dst>` (framework available)
- `find <path> [--name pattern]`
- `fsck [image]`
- `benchmark`
- `debug <subcommand>` (see DEBUG.md for planned subcommands)
- `exit` / `quit`

## Command syntax

Every command has:
- Syntax description
- Arguments
- Validation rules
- Success/failure output

## Error handling

Errors produce explicit messages. No silent exception swallowing.

## Example session

```text
microfs> format disk.img --blocks 1000
Formatted disk.img
microfs> mount disk.img
Mounted disk.img
microfs> mkdir /home
mkdir /home
microfs> cd /home
/home
microfs> touch notes.txt
touch /home/notes.txt
microfs> ls
notes.txt
microfs> stat notes.txt
stat /home/notes.txt
microfs> df
df
microfs> unmount
Unmounted
```

## Debug commands (planned / partial)

- `debug superblock`
- `debug inode <id>`
- `debug bitmap`
- `debug tree <path>`
- `debug index <path>`
- `debug trie`
- `debug cache`

These expose underlying structures for demonstration.
