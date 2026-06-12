package com.openlinkhub.digitalhuman.funasr.service;

import java.util.Locale;
import java.util.Map;

final class AudioFormatResolver {

    private static final Map<String, String> EXTENSION_FORMATS = Map.ofEntries(
            Map.entry("pcm", "pcm"),
            Map.entry("wav", "wav"),
            Map.entry("mp3", "mp3"),
            Map.entry("opus", "opus"),
            Map.entry("speex", "speex"),
            Map.entry("aac", "aac"),
            Map.entry("m4a", "aac"),
            Map.entry("amr", "amr")
    );

    private AudioFormatResolver() {
    }

    static String resolve(String formatOverride, String filename, String contentType, String defaultFormat) {
        if (hasText(formatOverride)) {
            return normalize(formatOverride);
        }

        String filenameFormat = fromFilename(filename);
        if (filenameFormat != null) {
            return filenameFormat;
        }

        String contentTypeFormat = fromContentType(contentType);
        if (contentTypeFormat != null) {
            return contentTypeFormat;
        }

        return defaultFormat;
    }

    static boolean shouldTranscodeToPcm(String formatOverride, String filename, String contentType) {
        if (hasText(formatOverride)) {
            String normalizedOverride = normalize(formatOverride);
            if (!normalizedOverride.equals("aac") && !normalizedOverride.equals("m4a") && !normalizedOverride.equals("mp4")) {
                return false;
            }
        }
        return isM4aContainer(filename) || isM4aContentType(contentType);
    }

    private static String fromFilename(String filename) {
        if (!hasText(filename)) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return EXTENSION_FORMATS.get(normalize(filename.substring(dotIndex + 1)));
    }

    private static String fromContentType(String contentType) {
        if (!hasText(contentType)) {
            return null;
        }
        String normalized = normalize(contentType);
        if (normalized.contains("x-m4a") || normalized.contains("mp4")) {
            return "aac";
        }
        for (String format : EXTENSION_FORMATS.values()) {
            if (normalized.contains(format)) {
                return format;
            }
        }
        return null;
    }

    private static boolean isM4aContainer(String filename) {
        if (!hasText(filename)) {
            return false;
        }
        String normalized = normalize(filename);
        return normalized.endsWith(".m4a") || normalized.endsWith(".mp4");
    }

    private static boolean isM4aContentType(String contentType) {
        if (!hasText(contentType)) {
            return false;
        }
        String normalized = normalize(contentType);
        return normalized.contains("x-m4a") || normalized.contains("mp4");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
