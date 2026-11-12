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
    private static volatile int progressPercent = 0;
    private static volatile String progressMessage = "";

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

    public static int progressPercent() {
        return progressPercent;
    }

    public static String progressMessage() {
        return progressMessage;
    }

    private static void progress(int percent, String message) {
        progressPercent = Math.max(0, Math.min(100, percent));
        if (message != null) {
            progressMessage = message;
        }
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
        progress(0, "Verificando atualizacoes...");
        return CompletableFuture.supplyAsync(() -> {
            try {
                UpdatePlan plan = checkNow();
                lastPlan = plan;
                progress(100, "Pronto");
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
        progress(2, "Baixando manifest...");
        String url = DozamigosConfig.manifestUrl();
        String json = HttpUtil.getText(url);
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

        progress(8, "Comparando arquivos...");
        ManagedState managed = state();
        Path mods = modsDir();
        Path game = gameDir();
        UpdatePlan plan = UpdateDiff.compute(
                manifest, managed.mods(), managed.files(), managed.fileSha(), mods, game,
                (done, total) -> {
                    // Diff ocupa 8%..85%
                    int pct = 8 + (int) ((done * 77L) / Math.max(1, total));
                    progress(pct, "Comparando... " + done + "/" + total);
                });
        progress(86, "Sincronizando estado...");
        adoptMatching(manifest, managed, mods, game);
        managed.save();
        progress(100, "Pronto");
        return plan;
    }

    private static void adoptMatching(PackManifest manifest, ManagedState managed, Path mods, Path game) throws IOException {
        List<ModEntry> modsList = manifest.mods;
        List<PackFileEntry> filesList = manifest.allPackFiles();
        int total = Math.max(1, modsList.size() + filesList.size());
        int done = 0;

        for (ModEntry entry : modsList) {
            if (entry != null && entry.isValid()) {
                Path path = mods.resolve(entry.file);
                if (Files.isRegularFile(path) && HashUtil.equalsIgnoreCase(HashUtil.sha256(path), entry.sha256)) {
                    managed.putMod(entry.id, entry.file);
                }
            }
            done++;
            progress(86 + (done * 14) / total, "Sincronizando... " + done + "/" + total);
        }
        for (PackFileEntry entry : filesList) {
            if (entry != null && entry.isValid() && !UpdateDiff.isRuntimeDataPath(entry.path)) {
                Path path = PathSafety.resolveUnder(game, entry.path);
                if (Files.isRegularFile(path) && HashUtil.equalsIgnoreCase(HashUtil.sha256(path), entry.sha256)) {
                    managed.putFile(entry.id, entry.path, entry.sha256);
                }
            }
            done++;
            progress(86 + (done * 14) / total, "Sincronizando... " + done + "/" + total);
        }
    }

    public static CompletableFuture<ApplyResult> applyAsync(UpdatePlan plan, Consumer<String> progressLabel) {
        progress(0, "Aplicando atualizacoes...");
        return CompletableFuture.supplyAsync(() -> applyNow(plan, progressLabel), WORKER);
    }

    public static ApplyResult applyNow(UpdatePlan plan, Consumer<String> progressLabel) {
        ManagedState managed = state();
        Path mods = modsDir();
        Path game = gameDir();
        List<String> errors = new ArrayList<>();
        int ok = 0;
        List<ModChange> changes = plan.changes();
        int total = Math.max(1, changes.size());

        for (int i = 0; i < changes.size(); i++) {
            ModChange change = changes.get(i);
            int pct = (i * 100) / total;
            String label = labelFor(change);
            progress(pct, label);
            if (progressLabel != null) {
                progressLabel.accept(label + "  (" + pct + "%)");
            }
            try {
                switch (change.target()) {
                    case MOD -> {
                        applyMod(change, managed, mods, null);
                        ok++;
                    }
                    case FILE -> {
                        applyPackFile(change, managed, game, null);
                        ok++;
                    }
                }
            } catch (Exception e) {
                DozamigosMod.LOGGER.error("Falha ao aplicar {}", change.id(), e);
                errors.add(change.id() + ": " + e.getMessage());
            }
        }

        managed.save();
        progress(100, "Concluido");
        if (progressLabel != null) {
            progressLabel.accept("Concluido");
        }
        return new ApplyResult(ok, List.copyOf(errors));
    }

    private static String labelFor(ModChange change) {
        return switch (change.kind()) {
            case ADDED, UPDATED -> "Baixando " + change.displayFile();
            case REMOVED -> "Removendo " + change.displayFile();
        };
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
                managed.putFile(change.id(), change.newFile(), change.sha256());
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
