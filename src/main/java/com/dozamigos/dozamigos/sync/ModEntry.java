package com.dozamigos.dozamigos.sync;

import com.google.gson.annotations.SerializedName;

public final class ModEntry {
    @SerializedName("id")
    public String id;

    @SerializedName("file")
    public String file;

    @SerializedName("sha256")
    public String sha256;

    @SerializedName("url")
    public String url;

    public boolean isValid() {
        return id != null && !id.isBlank()
                && file != null && !file.isBlank()
                && url != null && !url.isBlank()
                && sha256 != null && !sha256.isBlank();
    }
}
