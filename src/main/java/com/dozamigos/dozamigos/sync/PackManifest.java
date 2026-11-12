package com.dozamigos.dozamigos.sync;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public final class PackManifest {
    @SerializedName("packVersion")
    public String packVersion = "";

    @SerializedName("mods")
    public List<ModEntry> mods = new ArrayList<>();

    /** Arquivos do pack (config, resourcepacks, options.txt, etc.). */
    @SerializedName("files")
    public List<PackFileEntry> files = new ArrayList<>();

    /** Legacy alias de {@link #files}. Ainda lido no parse. */
    @SerializedName("configs")
    public List<PackFileEntry> configs = new ArrayList<>();

    public List<PackFileEntry> allPackFiles() {
        List<PackFileEntry> all = new ArrayList<>();
        if (files != null) {
            all.addAll(files);
        }
        if (configs != null) {
            all.addAll(configs);
        }
        return all;
    }
}
