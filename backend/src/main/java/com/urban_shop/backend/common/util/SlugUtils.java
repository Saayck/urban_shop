package com.urban_shop.backend.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String normalizeOrGenerate(String slug, String source) {
        String value = slug == null || slug.isBlank() ? source : slug;
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("No se pudo generar un slug valido");
        }
        return normalized;
    }
}
