package com.dozamigos.dozamigos.sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Pure diff between remote manifest and local managed ownership.
 * Unmanaged local files are ignored for REMOVED.
 * resourcepacks/ e shaderpacks/ nunca geram REMOVED — o jogador pode ter packs extras.
 * journeymap/data e similares sao runtime: ignorados no sync.
 * Se o pack ja foi aplicado (fileSha == want) e o jogo reescreveu o arquivo, nao pede UPDATED de novo.
 */
public final class UpdateDiff {
    private UpdateDiff() {}

    static boolean isUserContentPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String p = normalize(relativePath);
        return p.startsWith("resourcepacks/") || p.startsWith("shaderpacks/");
    }

    /** Mapas / cache gerados em runtime — nunca sync (mesmo se o manifest antigo ainda listar). */
    static boolean isRuntimeDataPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String p = normalize(relativePath);
        return p.startsWith("journeymap/data/")
                || p.startsWith("journeymap/icon/")
                || p.startsWith("journeymap/web/");
    }

    private static String normalize(String relativePath) {
        return relativePath.replace('\\', '/').toLowerCase();
    }

    public static UpdatePlan compute(
            PackManifest manifest,
            Map<String, String> ownedMods,
            Map<String, String> ownedFiles,
            Path modsDir,
            Path gameDir
    ) throws IOException {
        return compute(manifest, ownedMods, ownedFiles, Map.of(), modsDir, gameDir, null);
    }

    public static UpdatePlan compute(
            PackManifest manifest,
            Map<String, String> ownedMods,
            Map<String, String> ownedFiles,
            Map<String, String> appliedFileSha,
            Path modsDir,
            Path gameDir
    ) throws IOException {
        return compute(manifest, ownedMods, ownedFiles, appliedFileSha, modsDir, gameDir, null);
    }

    public static UpdatePlan compute(
            PackManifest manifest,
            Map<String, String> ownedMods,
            Map<String, String> ownedFiles,
            Path modsDir,
            Path gameDir,
            BiConsumer<Integer, Integer> progress
    ) throws IOException {
        return compute(manifest, ownedMods, ownedFiles, Map.of(), modsDir, gameDir, progress);
    }

    public static UpdatePlan compute(
            PackManifest manifest,
            Map<String, String> ownedMods,
            Map<String, String> ownedFiles,
            Map<String, String> appliedFileSha,
            Path modsDir,
            Path gameDir,
            BiConsumer<Integer, Integer> progress
    ) throws IOException {
        Map<String, ModEntry> remoteMods = remoteMods(manifest);
        Map<String, PackFileEntry> remoteFiles = remoteFiles(manifest);
        int total = Math.max(1, remoteMods.size() + remoteFiles.size());
        int[] done = {0};

        List<ModChange> changes = new ArrayList<>();
        changes.addAll(diffMods(remoteMods, ownedMods, modsDir, progress, done, total));
        changes.addAll(diffPackFiles(remoteFiles, ownedFiles, appliedFileSha, gameDir, progress, done, total));
        tick(progress, total, total);
        return new UpdatePlan(manifest.packVersion == null ? "" : manifest.packVersion, List.copyOf(changes));
    }

    /** Back-compat helper used by self-check (mods only). */
    public static UpdatePlan compute(PackManifest manifest, Map<String, String> owned, Path modsDir) throws IOException {
        return compute(manifest, owned, Map.of(), modsDir, modsDir.getParent() == null ? modsDir : modsDir.getParent());
    }

    private static Map<String, ModEntry> remoteMods(PackManifest manifest) {
        Map<String, ModEntry> remote = new LinkedHashMap<>();
        if (manifest.mods != null) {
            for (ModEntry entry : manifest.mods) {
                if (entry != null && entry.isValid()) {
                    remote.put(entry.id, entry);
                }
            }
        }
        return remote;
    }

    private static Map<String, PackFileEntry> remoteFiles(PackManifest manifest) {
        Map<String, PackFileEntry> remote = new LinkedHashMap<>();
        for (PackFileEntry entry : manifest.allPackFiles()) {
            if (entry != null && entry.isValid() && !isRuntimeDataPath(entry.path)) {
                remote.put(entry.id, entry);
            }
        }
        return remote;
    }

    private static List<ModChange> diffMods(
            Map<String, ModEntry> remote,
            Map<String, String> owned,
            Path modsDir,
            BiConsumer<Integer, Integer> progress,
            int[] done,
            int total
    ) throws IOException {
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
                    if (!HashUtil.equalsIgnoreCase(hash, want.sha256)) {
                        changes.add(ModChange.updatedMod(id, want.file, want));
                    }
                } else {
                    changes.add(ModChange.addedMod(want));
                }
            } else {
                String localHash = HashUtil.sha256(localPath);
                if (!HashUtil.equalsIgnoreCase(localHash, want.sha256) || !localFile.equals(want.file)) {
                    changes.add(ModChange.updatedMod(id, localFile, want));
                }
            }
            tick(progress, ++done[0], total);
        }

        for (Map.Entry<String, String> e : ownedCopy.entrySet()) {
            if (!remote.containsKey(e.getKey())) {
                changes.add(ModChange.removedMod(e.getKey(), e.getValue()));
            }
        }
        return changes;
    }

    private static List<ModChange> diffPackFiles(
            Map<String, PackFileEntry> remote,
            Map<String, String> owned,
            Map<String, String> appliedFileSha,
            Path gameDir,
            BiConsumer<Integer, Integer> progress,
            int[] done,
            int total
    ) throws IOException {
        List<ModChange> changes = new ArrayList<>();
        Map<String, String> ownedCopy = new LinkedHashMap<>(owned);
        Map<String, String> applied = appliedFileSha == null ? Map.of() : appliedFileSha;

        for (Map.Entry<String, PackFileEntry> e : remote.entrySet()) {
            String id = e.getKey();
            PackFileEntry want = e.getValue();
            String localRel = ownedCopy.get(id);
            Path localPath = localRel == null ? null : PathSafety.resolveUnder(gameDir, localRel);

            if (localRel == null || localPath == null || !Files.isRegularFile(localPath)) {
                Path byPath = PathSafety.resolveUnder(gameDir, want.path);
                if (Files.isRegularFile(byPath)) {
                    String hash = HashUtil.sha256(byPath);
                    if (!HashUtil.equalsIgnoreCase(hash, want.sha256)
                            && !alreadyApplied(applied, id, want.sha256)) {
                        changes.add(ModChange.updatedFile(id, want.path, want));
                    }
                } else {
                    changes.add(ModChange.addedFile(want));
                }
            } else {
                String localHash = HashUtil.sha256(localPath);
                boolean pathChanged = !localRel.replace('\\', '/').equals(want.path.replace('\\', '/'));
                if (pathChanged) {
                    changes.add(ModChange.updatedFile(id, localRel, want));
                } else if (!HashUtil.equalsIgnoreCase(localHash, want.sha256)
                        && !alreadyApplied(applied, id, want.sha256)) {
                    // ponytail: mods reescrevem config apos o apply; se o sha do pack ja foi aplicado, deixa quieto
                    changes.add(ModChange.updatedFile(id, localRel, want));
                }
            }
            tick(progress, ++done[0], total);
        }

        for (Map.Entry<String, String> e : ownedCopy.entrySet()) {
            if (!remote.containsKey(e.getKey())
                    && !isUserContentPath(e.getValue())
                    && !isRuntimeDataPath(e.getValue())) {
                changes.add(ModChange.removedFile(e.getKey(), e.getValue()));
            }
        }
        return changes;
    }

    private static boolean alreadyApplied(Map<String, String> applied, String id, String wantSha) {
        String got = applied.get(id);
        return got != null && HashUtil.equalsIgnoreCase(got, wantSha);
    }

    private static void tick(BiConsumer<Integer, Integer> progress, int done, int total) {
        if (progress != null) {
            progress.accept(done, total);
        }
    }
}
