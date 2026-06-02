package com.pelletsfactory.stock_manager.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centraliza cálculos financeiros compatíveis com o modelo atual baseado em Double.
 * Todos os valores monetários são arredondados a cêntimos antes de serem persistidos.
 */
public final class CalculationUtils {

    private static final int MONEY_SCALE = 2;

    private CalculationUtils() {
    }

    public static double subtotal(double quantity, double unitPrice) {
        requirePositive(quantity, "Quantidade");
        requireNonNegative(unitPrice, "Preço unitário");
        return money(decimal(quantity).multiply(decimal(unitPrice)));
    }

    public static double vat(double subtotal, double vatRate) {
        requireNonNegative(subtotal, "Subtotal");
        requirePercentage(vatRate, "Taxa de IVA");
        return money(decimal(subtotal)
                .multiply(decimal(vatRate))
                .divide(BigDecimal.valueOf(100), MONEY_SCALE + 4, RoundingMode.HALF_UP));
    }

    public static double total(double subtotal, double vat) {
        requireNonNegative(subtotal, "Subtotal");
        requireNonNegative(vat, "IVA");
        return money(decimal(subtotal).add(decimal(vat)));
    }

    public static double money(double value) {
        return money(decimal(value));
    }

    public static void requirePositive(Double value, String field) {
        ValidationUtils.requirePositive(value, field);
    }

    public static void requireNonNegative(Double value, String field) {
        ValidationUtils.requireNonNegative(value, field);
    }

    public static void requirePercentage(Double value, String field) {
        requireNonNegative(value, field);
        if (value > 100) {
            throw new IllegalArgumentException(field + " não pode ser superior a 100%");
        }
    }

    private static double money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal decimal(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Valor numérico inválido");
        }
        return BigDecimal.valueOf(value);
    }
}
