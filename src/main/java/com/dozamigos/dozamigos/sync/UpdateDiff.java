package com.dozamigos.dozamigos.sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure diff between remote manifest and local managed ownership.
 * Unmanaged local files are ignored for REMOVED.
 */
public final class UpdateDiff {
    private UpdateDiff() {}

    public static UpdatePlan compute(
            PackManifest manifest,
            Map<String, String> ownedMods,
            Map<String, String> ownedFiles,
            Path modsDir,
            Path gameDir
    ) throws IOException {
        List<ModChange> changes = new ArrayList<>();
        changes.addAll(diffMods(manifest, ownedMods, modsDir));
        changes.addAll(diffPackFiles(manifest, ownedFiles, gameDir));
        return new UpdatePlan(manifest.packVersion == null ? "" : manifest.packVersion, List.copyOf(changes));
    }

    /** Back-compat helper used by self-check (mods only). */
    public static UpdatePlan compute(PackManifest manifest, Map<String, String> owned, Path modsDir) throws IOException {
        return compute(manifest, owned, Map.of(), modsDir, modsDir.getParent() == null ? modsDir : modsDir.getParent());
    }

    private static List<ModChange> diffMods(PackManifest manifest, Map<String, String> owned, Path modsDir) throws IOException {
        Map<String, ModEntry> remote = new LinkedHashMap<>();
        if (manifest.mods != null) {
            for (ModEntry entry : manifest.mods) {
                if (entry != null && entry.isValid()) {
                    remote.put(entry.id, entry);
                }
            }
        }

        List<ModChange> changes = new ArrayList<>();
        Map<String, String> ownedCopy = new LinkedHashMap<>(owned);

        for (Map.Entry<String, ModEntry> e : remote.entrySet()) {
            String id = e.getKey();
            ModEntry want = e.getValue();
            String localFile = ownedCopy.get(id);
            Path localPath = localFile == null ? null : modsDir.resolve(localFile);

            if (localFile == null || localPath == null || !Files.isRegularFile(localPath)) {
                Path byName = modsDir.resolve(want.file);
                if (Files.isRegularFile(byName)) {
                    String hash = HashUtil.sha256(byName);
                    if (HashUtil.equalsIgnoreCase(hash, want.sha256)) {
                        continue;
                    }
                    changes.add(ModChange.updatedMod(id, want.file, want));
                } else {
                    changes.add(ModChange.addedMod(want));
                }
                continue;
            }

            String localHash = HashUtil.sha256(localPath);
            if (!HashUtil.equalsIgnoreCase(localHash, want.sha256) || !localFile.equals(want.file)) {
                changes.add(ModChange.updatedMod(id, localFile, want));
            }
        }

        for (Map.Entry<String, String> e : ownedCopy.entrySet()) {
            if (!remote.containsKey(e.getKey())) {
                changes.add(ModChange.removedMod(e.getKey(), e.getValue()));
            }
        }
        return changes;
    }

    private static List<ModChange> diffPackFiles(PackManifest manifest, Map<String, String> owned, Path gameDir) throws IOException {
        Map<String, PackFileEntry> remote = new LinkedHashMap<>();
        for (PackFileEntry entry : manifest.allPackFiles()) {
            if (entry != null && entry.isValid()) {
                remote.put(entry.id, entry);
            }
        }

        List<ModChange> changes = new ArrayList<>();
        Map<String, String> ownedCopy = new LinkedHashMap<>(owned);

        for (Map.Entry<String, PackFileEntry> e : remote.entrySet()) {
            String id = e.getKey();
            PackFileEntry want = e.getValue();
            String localRel = ownedCopy.get(id);
            Path localPath = localRel == null ? null : PathSafety.resolveUnder(gameDir, localRel);

            if (localRel == null || localPath == null || !Files.isRegularFile(localPath)) {
                Path byPath = PathSafety.resolveUnder(gameDir, want.path);
                if (Files.isRegularFile(byPath)) {
                    String hash = HashUtil.sha256(byPath);
                    if (HashUtil.equalsIgnoreCase(hash, want.sha256)) {
                        continue;
                    }
                    changes.add(ModChange.updatedFile(id, want.path, want));
                } else {
                    changes.add(ModChange.addedFile(want));
                }
                continue;
            }

            String localHash = HashUtil.sha256(localPath);
            if (!HashUtil.equalsIgnoreCase(localHash, want.sha256)
                    || !localRel.replace('\\', '/').equals(want.path.replace('\\', '/'))) {
                changes.add(ModChange.updatedFile(id, localRel, want));
            }
        }

        for (Map.Entry<String, String> e : ownedCopy.entrySet()) {
            if (!remote.containsKey(e.getKey())) {
                changes.add(ModChange.removedFile(e.getKey(), e.getValue()));
            }
        }
        return changes;
    }
}
