package com.pelletsfactory.stock_manager.common.utils;

import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;

public final class ProductionUnitUtils {

    public static final String KILOGRAM = "kg";

    private ProductionUnitUtils() {
    }

    public static void requireKilograms(MateriaPrima materiaPrima) {
        if (materiaPrima == null || !KILOGRAM.equalsIgnoreCase(materiaPrima.getUnidade())) {
            throw new IllegalArgumentException(
                    "A matéria-prima deve estar registada em kg para ser usada numa fórmula de produção"
            );
        }
    }
}
