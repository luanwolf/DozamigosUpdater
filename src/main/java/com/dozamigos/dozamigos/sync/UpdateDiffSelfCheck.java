package com.dozamigos.dozamigos.sync;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ponytail: one assert-based self-check for the diff logic; no test framework. */
public final class UpdateDiffSelfCheck {
    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("dozamigos-selfcheck");
        Path mods = tmp.resolve("mods");
        Path config = tmp.resolve("config");
        Files.createDirectories(mods);
        Files.createDirectories(config);

        Files.write(mods.resolve("keep-1.0.jar"), "keep".getBytes(StandardCharsets.UTF_8));
        Files.write(mods.resolve("old-1.0.jar"), "old-jar".getBytes(StandardCharsets.UTF_8));
        Files.write(mods.resolve("gone-1.0.jar"), "gone".getBytes(StandardCharsets.UTF_8));
        Files.write(config.resolve("old.toml"), "a=1".getBytes(StandardCharsets.UTF_8));

        Map<String, String> ownedMods = new LinkedHashMap<>();
        ownedMods.put("keep", "keep-1.0.jar");
        ownedMods.put("update_me", "old-1.0.jar");
        ownedMods.put("remove_me", "gone-1.0.jar");

        Map<String, String> ownedFiles = new LinkedHashMap<>();
        ownedFiles.put("demo-cfg", "config/old.toml");
        ownedFiles.put("gone-cfg", "config/gone.toml");

        PackManifest manifest = new PackManifest();
        manifest.packVersion = "2.0.0";
        manifest.mods = List.of(
                mod("keep", "keep-1.0.jar", sha(mods.resolve("keep-1.0.jar")), "http://x/keep"),
                mod("update_me", "new-2.0.jar", shaBytes("new-jar".getBytes(StandardCharsets.UTF_8)), "http://x/new"),
                mod("brand_new", "brand-1.0.jar", shaBytes("brand".getBytes(StandardCharsets.UTF_8)), "http://x/brand")
        );
        manifest.files = List.of(
                file("demo-cfg", "config/new.toml", shaBytes("a=2".getBytes(StandardCharsets.UTF_8)), "http://x/cfg"),
                file("fresh-cfg", "config/fresh.toml", shaBytes("fresh".getBytes(StandardCharsets.UTF_8)), "http://x/fresh")
        );

        UpdatePlan plan = UpdateDiff.compute(manifest, ownedMods, ownedFiles, mods, tmp);
        assertEq(6, plan.changes().size(), "change count");
        assertEq(2L, plan.count(ChangeKind.ADDED), "added");
        assertEq(2L, plan.count(ChangeKind.UPDATED), "updated");
        assertEq(2L, plan.count(ChangeKind.REMOVED), "removed");
        assertEq(1L, plan.count(ChangeKind.ADDED, FileKind.MOD), "added mods");
        assertEq(1L, plan.count(ChangeKind.ADDED, FileKind.FILE), "added files");
        assertEq(1L, plan.count(ChangeKind.REMOVED, FileKind.MOD), "removed mods");
        assertEq(1L, plan.count(ChangeKind.REMOVED, FileKind.FILE), "removed files");

        long fileChanges = plan.changes().stream().filter(c -> c.target() == FileKind.FILE).count();
        assertEq(3L, fileChanges, "file changes");

        // resourcepacks/shaderpacks: never REMOVED even if owned + missing from manifest
        Path rp = tmp.resolve("resourcepacks");
        Files.createDirectories(rp);
        Files.write(rp.resolve("user.zip"), "mine".getBytes(StandardCharsets.UTF_8));
        Map<String, String> ownedRp = new LinkedHashMap<>();
        ownedRp.put("user-rp", "resourcepacks/user.zip");
        ownedRp.put("gone-cfg", "config/gone.toml");
        PackManifest emptyFiles = new PackManifest();
        emptyFiles.packVersion = "3.0.0";
        emptyFiles.mods = List.of();
        emptyFiles.files = List.of();
        UpdatePlan keepUserPacks = UpdateDiff.compute(emptyFiles, Map.of(), ownedRp, mods, tmp);
        assertEq(1L, keepUserPacks.count(ChangeKind.REMOVED, FileKind.FILE), "only config removed");
        assertEq(0L, keepUserPacks.changes().stream()
                .filter(c -> c.kind() == ChangeKind.REMOVED && UpdateDiff.isUserContentPath(c.oldFile()))
                .count(), "no resourcepack removed");

        // config reescrito pelo mod apos apply: fileSha == want -> nao pede UPDATED de novo
        Files.write(config.resolve("drift.toml"), "local-rewrite".getBytes(StandardCharsets.UTF_8));
        Map<String, String> ownedDrift = Map.of("drift", "config/drift.toml");
        String packSha = shaBytes("from-pack".getBytes(StandardCharsets.UTF_8));
        Map<String, String> applied = Map.of("drift", packSha);
        PackManifest driftManifest = new PackManifest();
        driftManifest.packVersion = "4.0.0";
        driftManifest.mods = List.of();
        driftManifest.files = List.of(file("drift", "config/drift.toml", packSha, "http://x/drift"));
        UpdatePlan noLoop = UpdateDiff.compute(driftManifest, Map.of(), ownedDrift, applied, mods, tmp);
        assertEq(0, noLoop.changes().size(), "no rewrite loop");

        // pack mudou o sha -> UPDATED mesmo com fileSha antigo
        String newPackSha = shaBytes("pack-v2".getBytes(StandardCharsets.UTF_8));
        PackManifest bumped = new PackManifest();
        bumped.packVersion = "4.0.1";
        bumped.mods = List.of();
        bumped.files = List.of(file("drift", "config/drift.toml", newPackSha, "http://x/drift2"));
        UpdatePlan mustUpdate = UpdateDiff.compute(bumped, Map.of(), ownedDrift, applied, mods, tmp);
        assertEq(1L, mustUpdate.count(ChangeKind.UPDATED, FileKind.FILE), "pack bump updates");

        // journeymap/data no manifest remoto e ignorado
        PackManifest jm = new PackManifest();
        jm.packVersion = "5.0.0";
        jm.mods = List.of();
        jm.files = List.of(
                file("jm-map", "journeymap/data/mp/world/day/0,0.png", shaBytes("png".getBytes(StandardCharsets.UTF_8)), "http://x/map"),
                file("jm-cfg", "journeymap/config/5.9/journeymap.core.config", shaBytes("cfg".getBytes(StandardCharsets.UTF_8)), "http://x/jmcfg")
        );
        UpdatePlan jmPlan = UpdateDiff.compute(jm, Map.of(), Map.of(), mods, tmp);
        assertEq(1, jmPlan.changes().size(), "only journeymap config");
        if (!"jm-cfg".equals(jmPlan.changes().get(0).id())) {
            throw new AssertionError("journeymap config id: expected jm-cfg got " + jmPlan.changes().get(0).id());
        }

        System.out.println("UpdateDiffSelfCheck OK");
    }

    private static ModEntry mod(String id, String fileName, String sha, String url) {
        ModEntry e = new ModEntry();
        e.id = id;
        e.file = fileName;
        e.sha256 = sha;
        e.url = url;
        return e;
    }

    private static PackFileEntry file(String id, String path, String sha, String url) {
        PackFileEntry e = new PackFileEntry();
        e.id = id;
        e.path = path;
        e.sha256 = sha;
        e.url = url;
        return e;
    }

    private static String sha(Path path) throws Exception {
        return shaBytes(Files.readAllBytes(path));
    }

    private static String shaBytes(byte[] data) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static void assertEq(long expected, long actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " got " + actual);
        }
    }
}
