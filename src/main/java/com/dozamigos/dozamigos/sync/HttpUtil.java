package com.dozamigos.dozamigos.sync;

import com.dozamigos.dozamigos.config.DozamigosConfig;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class HttpUtil {
    private HttpUtil() {}

    public static String getText(String url) throws IOException {
        HttpURLConnection conn = open(url);
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    public static void downloadTo(String url, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName().toString() + ".dozamigos.tmp");
        HttpURLConnection conn = open(url);
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static HttpURLConnection open(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(DozamigosConfig.CONNECT_TIMEOUT_MS.get());
        conn.setReadTimeout(DozamigosConfig.READ_TIMEOUT_MS.get());
        conn.setRequestProperty("User-Agent", "Dozamigos-Updater/1.0 (Minecraft Forge)");
        conn.setRequestProperty("Accept", "application/octet-stream, application/json, */*");
        String token = DozamigosConfig.GITHUB_TOKEN.get();
        if (token != null && !token.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.trim());
        }
        int code = conn.getResponseCode();
        if (code >= 400) {
            throw new IOException("HTTP " + code + " ao baixar " + urlString);
        }
        return conn;
    }
}
