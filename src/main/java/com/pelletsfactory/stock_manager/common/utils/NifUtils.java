package com.pelletsfactory.stock_manager.common.utils;

public final class NifUtils {

    private NifUtils() {
    }

    public static String normalizeRequired(String nif) {
        String normalized = normalizeNullable(nif);
        if (normalized == null || !normalized.matches("\\d{9}")) {
            throw new IllegalArgumentException("NIF deve ter exatamente 9 dígitos");
        }
        return normalized;
    }

    public static String normalizeNullable(String nif) {
        if (nif == null || nif.isBlank()) {
            return null;
        }
        return nif.trim().replaceFirst("(?i)^PT", "");
    }
}
