package com.dozamigos.dozamigos.sync;

import com.google.gson.annotations.SerializedName;

/** Arquivo gerenciado do pack (path relativo ao game dir). */
public final class PackFileEntry {
    @SerializedName("id")
    public String id;

    /** Path relativo ao game dir. Ex: config/foo.toml, resourcepacks/pack.zip, options.txt */
    @SerializedName("path")
    public String path;

    @SerializedName("sha256")
    public String sha256;

    @SerializedName("url")
    public String url;

    public boolean isValid() {
        return id != null && !id.isBlank()
                && path != null && !path.isBlank()
                && url != null && !url.isBlank()
                && sha256 != null && !sha256.isBlank();
    }
}
