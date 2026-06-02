package com.pelletsfactory.stock_manager.common.dto.response;

public record DashboardStatsDTO(
        long totalFuncionarios,
        long encomendasPendentes,
        double producaoMesKg,
        double stockPelletsKg,
        boolean stockBaixo
) {
}
