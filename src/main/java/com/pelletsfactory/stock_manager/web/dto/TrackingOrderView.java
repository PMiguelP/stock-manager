package com.pelletsfactory.stock_manager.web.dto;

import java.time.LocalDate;
import java.util.List;

public record TrackingOrderView(
        String orderReference,
        String clientName,
        double quantityKg,
        LocalDate orderDate,
        String lastUpdated,
        String status,
        String statusClass,
        String trackingCode,
        String currencyCode,
        String currencySymbol,
        double totalNet,
        double totalVat,
        double total,
        boolean cancelled,
        List<TrackingStageView> stages,
        List<TrackingItemView> items
) {
}
