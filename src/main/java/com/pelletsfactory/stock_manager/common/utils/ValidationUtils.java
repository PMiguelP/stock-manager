package com.pelletsfactory.stock_manager.common.utils;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static void requirePositive(Double value, String field) {
        if (value == null || !Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(field + " deve ser maior que zero");
        }
    }

    public static void requireNonNegative(Double value, String field) {
        if (value == null || !Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(field + " não pode ser negativo");
        }
    }
}
