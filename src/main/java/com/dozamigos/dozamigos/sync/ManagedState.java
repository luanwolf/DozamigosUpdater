package com.dozamigos.dozamigos.sync;

import com.dozamigos.dozamigos.DozamigosMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks which files this updater owns so user-added files are never marked REMOVED. */
public final class ManagedState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

    private final Path file;
    /** id -> mods/filename */
    private Map<String, String> mods = new LinkedHashMap<>();
    /** id -> relative path from game dir */
    private Map<String, String> files = new LinkedHashMap<>();

    public ManagedState(Path gameDir) {
        this.file = gameDir.resolve("config").resolve("dozamigos").resolve("managed.json");
        load();
    }

    public Map<String, String> mods() {
        return mods;
    }

    public Map<String, String> files() {
        return files;
    }

    public void putMod(String id, String filename) {
        mods.put(id, filename);
    }

    public void removeMod(String id) {
        mods.remove(id);
    }

    public void putFile(String id, String relativePath) {
        files.put(id, relativePath.replace('\\', '/'));
    }

    public void removeFile(String id) {
        files.remove(id);
    }

    public void load() {
        mods = new LinkedHashMap<>();
        files = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("mods") || obj.has("files") || obj.has("configs")) {
                if (obj.has("mods") && obj.get("mods").isJsonObject()) {
                    Map<String, String> parsed = GSON.fromJson(obj.get("mods"), MAP_TYPE);
                    if (parsed != null) {
                        mods = new LinkedHashMap<>(parsed);
                    }
                }
                if (obj.has("files") && obj.get("files").isJsonObject()) {
                    Map<String, String> parsed = GSON.fromJson(obj.get("files"), MAP_TYPE);
                    if (parsed != null) {
                        files.putAll(parsed);
                    }
                }
                // legacy key
                if (obj.has("configs") && obj.get("configs").isJsonObject()) {
                    Map<String, String> parsed = GSON.fromJson(obj.get("configs"), MAP_TYPE);
                    if (parsed != null) {
                        files.putAll(parsed);
                    }
                }
            } else {
                Map<String, String> parsed = GSON.fromJson(obj, MAP_TYPE);
                if (parsed != null) {
                    mods = new LinkedHashMap<>(parsed);
                }
            }
        } catch (Exception e) {
            DozamigosMod.LOGGER.warn("Falha ao ler managed.json, comecando limpo", e);
            mods = new LinkedHashMap<>();
            files = new LinkedHashMap<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.add("mods", GSON.toJsonTree(mods, MAP_TYPE));
            root.add("files", GSON.toJsonTree(files, MAP_TYPE));
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            DozamigosMod.LOGGER.error("Falha ao salvar managed.json", e);
        }
    }
}
