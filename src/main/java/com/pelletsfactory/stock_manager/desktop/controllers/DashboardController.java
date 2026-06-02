package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.DashboardChartsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.DashboardStatsDTO;
import com.pelletsfactory.stock_manager.common.services.DashboardService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    private final DashboardService dashboardService;
    private final I18nService i18nService;
    private final NavigationService navigationService;
    private final FuncionarioController funcionarioController;
    private final OrdersController ordersController;
    private final BatchesController batchesController;

    @FXML private Label lblTotalFuncionarios;
    @FXML private Label lblFuncionariosAtivos;
    @FXML private Label lblProducaoMes;
    @FXML private Label lblStockAtual;
    @FXML private Label lblSubtotalFuncionarios;
    @FXML private Label lblSubtotalAtivos;
    @FXML private Label lblSubtotalProducao;
    @FXML private Label lblAlertaStock;
    @FXML private StackPane iconTotalFuncionarios;
    @FXML private StackPane iconEncomendasPendentes;
    @FXML private StackPane iconProducaoMensal;
    @FXML private StackPane iconStockAtual;
    @FXML private AreaChart<String, Number> chartDailyProduction;
    @FXML private AreaChart<String, Number> chartMonthlyProduction;
    @FXML private BarChart<String, Number> chartOrdersPerMonth;
    @FXML private PieChart chartMateriasPrimas;
    @FXML private LineChart<String, Number> chartPelletProduction;
    @FXML private BubbleChart<Number, Number> chartFinancialMovements;

    public DashboardController(DashboardService dashboardService,
                               I18nService i18nService,
                               NavigationService navigationService,
                               FuncionarioController funcionarioController,
                               OrdersController ordersController,
                               BatchesController batchesController) {
        this.dashboardService = dashboardService;
        this.i18nService = i18nService;
        this.navigationService = navigationService;
        this.funcionarioController = funcionarioController;
        this.ordersController = ordersController;
        this.batchesController = batchesController;
    }

    @FXML
    public void initialize() {
        configurarIconesResumo();
        carregarEstatisticas();
        carregarGraficos();
    }

    private void carregarGraficos() {
        limparGraficos();
        try {
            DashboardChartsDTO charts = dashboardService.obterGraficos();
            carregarProducaoDiaria(charts);
            carregarProducaoMensal(charts);
            carregarEncomendasMensais(charts);
            carregarMateriasPrimas(charts);
            carregarProducaoPellets(charts);
            carregarMovimentosFinanceiros(charts);
        } catch (RuntimeException ignored) {
            // Um painel vazio é preferível a apresentar valores incorretos.
        }
    }

    private void limparGraficos() {
        chartDailyProduction.getData().clear();
        chartMonthlyProduction.getData().clear();
        chartOrdersPerMonth.getData().clear();
        chartMateriasPrimas.getData().clear();
        chartPelletProduction.getData().clear();
        chartFinancialMovements.getData().clear();
    }

    private void carregarProducaoDiaria(DashboardChartsDTO charts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(i18nService.translate("dashboard.produced"));
        charts.dailyProduction().forEach(point ->
                series.getData().add(new XYChart.Data<>(point.day(), point.producedKg())));
        chartDailyProduction.getData().add(series);
    }

    private void carregarProducaoMensal(DashboardChartsDTO charts) {
        XYChart.Series<String, Number> planned = new XYChart.Series<>();
        planned.setName(i18nService.translate("dashboard.planned"));
        XYChart.Series<String, Number> produced = new XYChart.Series<>();
        produced.setName(i18nService.translate("dashboard.produced"));
        charts.monthlyProduction().forEach(point -> {
            planned.getData().add(new XYChart.Data<>(point.month(), point.plannedKg()));
            produced.getData().add(new XYChart.Data<>(point.month(), point.producedKg()));
        });
        chartMonthlyProduction.getData().addAll(planned, produced);
    }

    private void carregarEncomendasMensais(DashboardChartsDTO charts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(i18nService.translate("dashboard.orders"));
        charts.ordersPerMonth().forEach(point ->
                series.getData().add(new XYChart.Data<>(point.month(), point.orders())));
        chartOrdersPerMonth.getData().add(series);
    }

    private void carregarMateriasPrimas(DashboardChartsDTO charts) {
        charts.rawMaterialsStockKg().forEach(point ->
                chartMateriasPrimas.getData().add(new PieChart.Data(point.material(), point.stockKg())));
    }

    private void carregarProducaoPellets(DashboardChartsDTO charts) {
        charts.pelletProductionByMonth().forEach(pellet -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(pellet.pelletType());
            pellet.points().forEach(point ->
                    series.getData().add(new XYChart.Data<>(point.month(), point.producedKg())));
            chartPelletProduction.getData().add(series);
        });
    }

    private void carregarMovimentosFinanceiros(DashboardChartsDTO charts) {
        XYChart.Series<Number, Number> entries = new XYChart.Series<>();
        entries.setName(i18nService.translate("dashboard.entries"));
        XYChart.Series<Number, Number> exits = new XYChart.Series<>();
        exits.setName(i18nService.translate("dashboard.exits"));
        charts.weeklyFinancialMovements().forEach(point -> {
            adicionarBolha(entries, point.week(), point.entries());
            adicionarBolha(exits, point.week(), point.exits());
        });
        chartFinancialMovements.getData().addAll(entries, exits);
    }

    private void adicionarBolha(XYChart.Series<Number, Number> series, int week, long count) {
        if (count > 0) {
            series.getData().add(new XYChart.Data<>(week, count, Math.sqrt(count)));
        }
    }

    private void configurarIconesResumo() {
        setIcon(iconTotalFuncionarios, "mdi2a-account-group", "#cbd5e1");
        setIcon(iconEncomendasPendentes, "mdi2c-cart-outline", "#22c55e");
        setIcon(iconProducaoMensal, "mdi2f-factory", "#eab308");
        setIcon(iconStockAtual, "mdi2p-package-variant", "#ef4444");
    }

    private void setIcon(StackPane container, String iconLiteral, String color) {
        if (container == null) {
            return;
        }
        FontIcon icon = new FontIcon();
        icon.setIconLiteral(iconLiteral);
        icon.setIconSize(30);
        icon.setIconColor(Color.web(color));
        container.getChildren().setAll(icon);
    }

    private void carregarEstatisticas() {
        try {
            DashboardStatsDTO stats = dashboardService.obterEstatisticas();
            lblTotalFuncionarios.setText(String.valueOf(stats.totalFuncionarios()));
            lblSubtotalFuncionarios.setText(stats.totalFuncionarios() + " no sistema");
            lblFuncionariosAtivos.setText(String.valueOf(stats.encomendasPendentes()));
            lblSubtotalAtivos.setText("encomendas pendentes");
            lblProducaoMes.setText(String.format("%.3f ton", stats.producaoMesKg() / 1000.0));
            lblSubtotalProducao.setText("");
            lblStockAtual.setText(String.format("%.3f ton", stats.stockPelletsKg() / 1000.0));
            lblAlertaStock.setText(stats.stockBaixo() ? "Nível Baixo" : "Nível Normal");
            lblAlertaStock.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (stats.stockBaixo() ? "#ef4444" : "#22c55e") + ";");
        } catch (Exception e) {
            lblTotalFuncionarios.setText("—");
            lblSubtotalFuncionarios.setText("");
            lblFuncionariosAtivos.setText("—");
            lblSubtotalAtivos.setText("");
            lblProducaoMes.setText("—");
            lblSubtotalProducao.setText("");
            lblStockAtual.setText("—");
            lblAlertaStock.setText("");
        }
    }

    @FXML
    private void handleCriarEncomenda() {
        navigationService.navigateTo("/orders");
        ordersController.openCreateModal();
    }

    @FXML
    private void handleAdicionarFuncionario() {
        navigationService.navigateTo("/funcionarios");
        funcionarioController.openCreateModal();
    }

    @FXML
    private void handleRegistarLote() {
        navigationService.navigateTo("/batches");
        batchesController.openCreateModal();
    }
}
