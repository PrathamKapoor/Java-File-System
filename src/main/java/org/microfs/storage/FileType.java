package org.microfs.storage;

/** Filesystem object types. Unknown values are rejected on read. */
public enum FileType {
    REGULAR_FILE((byte) 1),
    DIRECTORY((byte) 2);

    private final byte code;

    FileType(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static FileType fromCode(byte code) {
        for (FileType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown file type code: " + code);
    }
}
