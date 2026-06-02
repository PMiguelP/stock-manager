package com.pelletsfactory.stock_manager.web.dto;

public record TrackingItemView(
        String pelletType,
        double quantityKg,
        double unitPriceNet,
        double vatRate,
        double vatAmount,
        double subtotalNet,
        double total
) {
}
