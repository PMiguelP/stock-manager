package com.pelletsfactory.stock_manager.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DecimalUtils {

    private DecimalUtils() {
    }

    public static BigDecimal fromDouble(Double value, int scale) {
        if (value == null) {
            return null;
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Valor monetário inválido");
        }
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    public static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
