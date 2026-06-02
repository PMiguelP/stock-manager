package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.List;

public record DashboardChartsDTO(
        List<DailyProductionPoint> dailyProduction,
        List<MonthlyProductionPoint> monthlyProduction,
        List<MonthlyOrdersPoint> ordersPerMonth,
        List<RawMaterialStockPoint> rawMaterialsStockKg,
        List<PelletProductionSeries> pelletProductionByMonth,
        List<WeeklyFinancialPoint> weeklyFinancialMovements
) {
    public record DailyProductionPoint(String day, double producedKg) {}

    public record MonthlyProductionPoint(String month, double plannedKg, double producedKg) {}

    public record MonthlyOrdersPoint(String month, long orders) {}

    public record RawMaterialStockPoint(String material, double stockKg) {}

    public record PelletProductionSeries(String pelletType, List<MonthlyPelletProductionPoint> points) {}

    public record MonthlyPelletProductionPoint(String month, double producedKg) {}

    public record WeeklyFinancialPoint(int week, long entries, long exits) {}
}
