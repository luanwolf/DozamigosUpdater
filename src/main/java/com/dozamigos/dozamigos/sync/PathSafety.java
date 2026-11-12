package com.dozamigos.dozamigos.sync;

import java.io.IOException;
import java.nio.file.Path;

public final class PathSafety {
    private PathSafety() {}

    /** Resolve path relativo ao gameDir; rejeita absolute / .. traversal. */
    public static Path resolveUnder(Path gameDir, String relative) throws IOException {
        if (relative == null || relative.isBlank()) {
            throw new IOException("path vazio");
        }
        String normalized = relative.replace('\\', '/').replaceAll("^/+", "");
        if (normalized.contains("..") || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            throw new IOException("path invalido: " + relative);
        }
        Path root = gameDir.toAbsolutePath().normalize();
        Path resolved = root.resolve(normalized).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("path fora do game dir: " + relative);
        }
        return resolved;
    }
}
