package com.dozamigos.dozamigos.sync;

public record ModChange(
        ChangeKind kind,
        FileKind target,
        String id,
        String oldFile,
        String newFile,
        String sha256,
        String url
) {
    public static ModChange addedMod(ModEntry entry) {
        return new ModChange(ChangeKind.ADDED, FileKind.MOD, entry.id, null, entry.file, entry.sha256, entry.url);
    }

    public static ModChange updatedMod(String id, String oldFile, ModEntry entry) {
        return new ModChange(ChangeKind.UPDATED, FileKind.MOD, id, oldFile, entry.file, entry.sha256, entry.url);
    }

    public static ModChange removedMod(String id, String oldFile) {
        return new ModChange(ChangeKind.REMOVED, FileKind.MOD, id, oldFile, null, null, null);
    }

    public static ModChange addedFile(PackFileEntry entry) {
        return new ModChange(ChangeKind.ADDED, FileKind.FILE, entry.id, null, entry.path, entry.sha256, entry.url);
    }

    public static ModChange updatedFile(String id, String oldPath, PackFileEntry entry) {
        return new ModChange(ChangeKind.UPDATED, FileKind.FILE, id, oldPath, entry.path, entry.sha256, entry.url);
    }

    public static ModChange removedFile(String id, String oldPath) {
        return new ModChange(ChangeKind.REMOVED, FileKind.FILE, id, oldPath, null, null, null);
    }

    public String displayFile() {
        return newFile != null ? newFile : oldFile;
    }

    public String targetLabel() {
        return target == FileKind.FILE ? "file" : "mod";
    }
}
