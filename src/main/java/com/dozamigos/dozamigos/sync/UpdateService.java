package com.dozamigos.dozamigos.sync;

import com.dozamigos.dozamigos.DozamigosMod;
import com.dozamigos.dozamigos.config.DozamigosConfig;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraftforge.fml.loading.FMLPaths;

public final class UpdateService {
    private static final Gson GSON = new Gson();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dozamigos-updater");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicBoolean CHECKING = new AtomicBoolean(false);
    private static volatile UpdatePlan lastPlan = new UpdatePlan("", List.of());
    private static volatile String lastError = null;

    private UpdateService() {}

    public static UpdatePlan lastPlan() {
        return lastPlan;
    }

    public static String lastError() {
        return lastError;
    }

    public static boolean isBusy() {
        return CHECKING.get();
    }

    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static Path modsDir() {
        return gameDir().resolve("mods");
    }

    public static ManagedState state() {
        return new ManagedState(gameDir());
    }

    public static CompletableFuture<UpdatePlan> checkAsync() {
        if (!CHECKING.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(lastPlan);
        }
        lastError = null;
        return CompletableFuture.supplyAsync(() -> {
            try {
                UpdatePlan plan = checkNow();
                lastPlan = plan;
                return plan;
            } catch (Exception e) {
                lastError = e.getMessage() == null ? e.toString() : e.getMessage();
                DozamigosMod.LOGGER.error("Falha ao checar updates Dozamigos", e);
                throw new RuntimeException(lastError, e);
            } finally {
                CHECKING.set(false);
            }
        }, WORKER);
    }

    public static UpdatePlan checkNow() throws IOException {
        String url = DozamigosConfig.MANIFEST_URL.get();
        if (url == null || url.isBlank()) {
            throw new IOException("Configure manifestUrl em config/dozamigos-client.toml");
        }
        String json = HttpUtil.getText(url.trim());
        PackManifest manifest = GSON.fromJson(json, PackManifest.class);
        if (manifest == null) {
            throw new IOException("Manifest invalido");
        }
        if (manifest.mods == null) {
            manifest.mods = List.of();
        }
        if (manifest.files == null) {
            manifest.files = List.of();
        }
        if (manifest.configs == null) {
            manifest.configs = List.of();
        }

        ManagedState managed = state();
        Path mods = modsDir();
        Path game = gameDir();
        UpdatePlan plan = UpdateDiff.compute(manifest, managed.mods(), managed.files(), mods, game);
        adoptMatching(manifest, managed, mods, game);
        managed.save();
        return plan;
    }

    private static void adoptMatching(PackManifest manifest, ManagedState managed, Path mods, Path game) throws IOException {
        for (ModEntry entry : manifest.mods) {
            if (entry == null || !entry.isValid()) {
                continue;
            }
            Path path = mods.resolve(entry.file);
            if (Files.isRegularFile(path) && HashUtil.equalsIgnoreCase(HashUtil.sha256(path), entry.sha256)) {
                managed.putMod(entry.id, entry.file);
            }
        }
        for (PackFileEntry entry : manifest.allPackFiles()) {
            if (entry == null || !entry.isValid()) {
                continue;
            }
            Path path = PathSafety.resolveUnder(game, entry.path);
            if (Files.isRegularFile(path) && HashUtil.equalsIgnoreCase(HashUtil.sha256(path), entry.sha256)) {
                managed.putFile(entry.id, entry.path);
            }
        }
    }

    public static CompletableFuture<ApplyResult> applyAsync(UpdatePlan plan, Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> applyNow(plan, progress), WORKER);
    }

    public static ApplyResult applyNow(UpdatePlan plan, Consumer<String> progress) {
        ManagedState managed = state();
        Path mods = modsDir();
        Path game = gameDir();
        List<String> errors = new ArrayList<>();
        int ok = 0;

        for (ModChange change : plan.changes()) {
            try {
                switch (change.target()) {
                    case MOD -> {
                        applyMod(change, managed, mods, progress);
                        ok++;
                    }
                    case FILE -> {
                        applyPackFile(change, managed, game, progress);
                        ok++;
                    }
                }
            } catch (Exception e) {
                DozamigosMod.LOGGER.error("Falha ao aplicar {}", change.id(), e);
                errors.add(change.id() + ": " + e.getMessage());
            }
        }

        managed.save();
        return new ApplyResult(ok, List.copyOf(errors));
    }

    private static void applyMod(ModChange change, ManagedState managed, Path mods, Consumer<String> progress) throws Exception {
        switch (change.kind()) {
            case ADDED, UPDATED -> {
                if (progress != null) {
                    progress.accept("Baixando mod " + change.newFile());
                }
                Path target = mods.resolve(change.newFile());
                HttpUtil.downloadTo(change.url(), target);
                String hash = HashUtil.sha256(target);
                if (!HashUtil.equalsIgnoreCase(hash, change.sha256())) {
                    Files.deleteIfExists(target);
                    throw new IOException("SHA-256 nao confere para " + change.newFile());
                }
                if (change.oldFile() != null && !change.oldFile().equals(change.newFile())) {
                    Files.deleteIfExists(mods.resolve(change.oldFile()));
                }
                managed.putMod(change.id(), change.newFile());
            }
            case REMOVED -> {
                if (progress != null) {
                    progress.accept("Removendo mod " + change.oldFile());
                }
                if (change.oldFile() != null) {
                    Files.deleteIfExists(mods.resolve(change.oldFile()));
                }
                managed.removeMod(change.id());
            }
        }
    }

    private static void applyPackFile(ModChange change, ManagedState managed, Path game, Consumer<String> progress) throws Exception {
        switch (change.kind()) {
            case ADDED, UPDATED -> {
                if (progress != null) {
                    progress.accept("Baixando " + change.newFile());
                }
                Path target = PathSafety.resolveUnder(game, change.newFile());
                HttpUtil.downloadTo(change.url(), target);
                String hash = HashUtil.sha256(target);
                if (!HashUtil.equalsIgnoreCase(hash, change.sha256())) {
                    Files.deleteIfExists(target);
                    throw new IOException("SHA-256 nao confere para " + change.newFile());
                }
                if (change.oldFile() != null && !change.oldFile().replace('\\', '/').equals(change.newFile().replace('\\', '/'))) {
                    Files.deleteIfExists(PathSafety.resolveUnder(game, change.oldFile()));
                }
                managed.putFile(change.id(), change.newFile());
            }
            case REMOVED -> {
                if (progress != null) {
                    progress.accept("Removendo " + change.oldFile());
                }
                if (change.oldFile() != null) {
                    Files.deleteIfExists(PathSafety.resolveUnder(game, change.oldFile()));
                }
                managed.removeFile(change.id());
            }
        }
    }

    public record ApplyResult(int applied, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }
}
