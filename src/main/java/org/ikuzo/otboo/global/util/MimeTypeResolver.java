package org.ikuzo.otboo.global.util;

import java.util.Locale;
import java.util.Map;

public final class MimeTypeResolver {

    private static final Map<String, String> EXT_TO_MIME = Map.ofEntries(
        Map.entry("jpg",  "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("png",  "image/png"),
        Map.entry("gif",  "image/gif"),
        Map.entry("webp", "image/webp"),
        Map.entry("bmp",  "image/bmp"),
        Map.entry("svg",  "image/svg+xml"),
        Map.entry("ico",  "image/x-icon"),
        Map.entry("tif",  "image/tiff"),
        Map.entry("tiff", "image/tiff")
    );

    private MimeTypeResolver() {}

    public static String resolveFromExtension(String filename, String fallback) {
        if (filename == null || filename.isBlank()) return fallback;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return fallback;

        String ext = filename.substring(idx + 1).toLowerCase(Locale.ROOT).trim();
        return EXT_TO_MIME.getOrDefault(ext, fallback);
    }
}
