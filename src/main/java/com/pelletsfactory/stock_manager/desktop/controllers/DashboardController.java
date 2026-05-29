package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import com.pelletsfactory.stock_manager.common.services.OrdemProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.common.services.VendaService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DashboardController {

    private final FuncionarioService funcionarioService;
    private final OrdemProducaoService ordemProducaoService;
    private final VendaService vendaService;
    private final StockService stockService;
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

    @FXML private AreaChart<Number, Number> chartDailyProduction;
    @FXML private AreaChart<String, Number> chartMonthlyProduction;
    @FXML private BarChart<String, Number> chartOrdersPerMonth;
    @FXML private PieChart chartMateriasPrimas;
    @FXML private LineChart<String, Number> chartStockPellets;
    @FXML private BubbleChart<Number, Number> chartBudget;

    public DashboardController(FuncionarioService funcionarioService,
                               OrdemProducaoService ordemProducaoService,
                               VendaService vendaService,
                               StockService stockService,
                               NavigationService navigationService,
                               FuncionarioController funcionarioController,
                               OrdersController ordersController,
                               BatchesController batchesController) {
        this.funcionarioService = funcionarioService;
        this.ordemProducaoService = ordemProducaoService;
        this.vendaService = vendaService;
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.funcionarioController = funcionarioController;
        this.ordersController = ordersController;
        this.batchesController = batchesController;
    }

    @FXML
    public void initialize() {
        configurarIconesResumo();
        carregarEstatisticas();
        inicializarGraficos();
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
            long total = funcionarioService
                    .listarFuncionarios(1, 1, null, null, null, null, "nome", "ASC")
                    .getTotalElements();
            lblTotalFuncionarios.setText(String.valueOf(total));
            lblSubtotalFuncionarios.setText(total + " no sistema");
        } catch (Exception e) {
            lblTotalFuncionarios.setText("—");
            lblSubtotalFuncionarios.setText("");
        }

        try {
            long pendentes = vendaService
                    .listarEncomendasComFiltrosSimples(null, EstadoEncomendaCliente.PENDENTE, 1, 1, "data", "DESC")
                    .getTotalElements();
            lblFuncionariosAtivos.setText(String.valueOf(pendentes));
            lblSubtotalAtivos.setText("encomendas pendentes");
        } catch (Exception e) {
            lblFuncionariosAtivos.setText("—");
            lblSubtotalAtivos.setText("");
        }

        try {
            double totalKg = ordemProducaoService
                    .listarOrdensComFiltros(1, 500, null, null, null, "dataInicio", "DESC")
                    .getContent().stream()
                    .filter(o -> isCurrentMonth(o.dataInicio()))
                    .mapToDouble(o -> o.quantidadeProduzidaReal() != null ? o.quantidadeProduzidaReal() : 0.0)
                    .sum();
            lblProducaoMes.setText(String.format("%.3f ton", totalKg / 1000.0));
            lblSubtotalProducao.setText("+12% vs meta");
        } catch (Exception e) {
            lblProducaoMes.setText("—");
            lblSubtotalProducao.setText("");
        }

        try {
            double stockKg = stockService
                    .listarTiposPelletComFiltros(1, 1000, null, null, "nome", "ASC")
                    .getContent().stream()
                    .mapToDouble(t -> t.stockAtual() != null ? t.stockAtual() : 0.0)
                    .sum();
            double stockTon = stockKg / 1000.0;
            lblStockAtual.setText(String.format("%.3f ton", stockTon));
            boolean baixo = stockTon < 5.0;
            lblAlertaStock.setText(baixo ? "Nível Baixo" : "Nível Normal");
            lblAlertaStock.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (baixo ? "#ef4444" : "#22c55e") + ";");
        } catch (Exception e) {
            lblStockAtual.setText("—");
            lblAlertaStock.setText("");
        }
    }

    private boolean isCurrentMonth(Instant instant) {
        if (instant == null) return false;
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate now = LocalDate.now();
        return date.getMonth() == now.getMonth() && date.getYear() == now.getYear();
    }

    private void inicializarGraficos() {
        configurarEixoDias();

        // Daily Production — days 1-7 mapped to Mon-Sun
        XYChart.Series<Number, Number> daily = new XYChart.Series<>();
        daily.setName("Production (kg)");
        int[] dailyVals = {230, 250, 230, 290, 300, 260, 220};
        for (int i = 0; i < dailyVals.length; i++) {
            daily.getData().add(new XYChart.Data<>(i + 1, dailyVals[i]));
        }
        chartDailyProduction.getData().add(daily);

        // Monthly Production — two series: Planeado / Produzido
        String[] meses = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        XYChart.Series<String, Number> planeado = new XYChart.Series<>();
        planeado.setName("Planeado");
        XYChart.Series<String, Number> produzido = new XYChart.Series<>();
        produzido.setName("Produzido");
        int[] planeadoVals = {5000, 5000, 5000, 5000, 6500, 6500};
        int[] produzidoVals = {5100, 5000, 5200, 5100, 7000, 6800};
        for (int i = 0; i < meses.length; i++) {
            planeado.getData().add(new XYChart.Data<>(meses[i], planeadoVals[i]));
            produzido.getData().add(new XYChart.Data<>(meses[i], produzidoVals[i]));
        }
        chartMonthlyProduction.getData().addAll(planeado, produzido);

        // Orders per Month
        XYChart.Series<String, Number> orders = new XYChart.Series<>();
        orders.setName("Orders");
        int[] ordersVals = {40, 35, 47, 45, 59, 52};
        for (int i = 0; i < meses.length; i++) {
            orders.getData().add(new XYChart.Data<>(meses[i], ordersVals[i]));
        }
        chartOrdersPerMonth.getData().add(orders);

        // Matérias Primas PieChart — real data from DB
        carregarPieChartMateriasPrimas();

        // Stock Pellets LineChart — real pellet names, sample monthly evolution
        carregarStockPelletsChart();

        // Budget BubbleChart — sample purchase budget by week
        inicializarBudgetChart();
    }

    private void configurarEixoDias() {
        NumberAxis xAxis = (NumberAxis) chartDailyProduction.getXAxis();
        xAxis.setTickLabelFormatter(new StringConverter<>() {
            private final String[] nomes = {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

            @Override
            public String toString(Number n) {
                int idx = n.intValue();
                return (idx >= 1 && idx <= 7) ? nomes[idx] : "";
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
    }

    private void carregarPieChartMateriasPrimas() {
        try {
            var materiais = stockService
                    .listarMateriasPrimasComFiltros(1, 10, null, null, "stockAtual", "DESC")
                    .getContent();

            chartMateriasPrimas.getData().clear();
            for (var m : materiais) {
                double stock = m.stockAtual() != null ? m.stockAtual() : 0.0;
                if (stock > 0) {
                    chartMateriasPrimas.getData().add(new PieChart.Data(m.nome(), stock));
                }
            }
        } catch (Exception e) {
            // leave chart empty on error
        }
    }

    private void carregarStockPelletsChart() {
        chartStockPellets.getData().clear();
        try {
            var tipos = stockService
                    .listarTiposPelletComFiltros(1, 2, null, null, "stockAtual", "DESC")
                    .getContent();

            String[] meses = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            // Sample monthly factors relative to current stock (ends at 1.0 = current)
            double[] fatores = {0.72, 0.78, 0.83, 0.88, 0.80, 0.85, 0.90, 0.87, 0.93, 0.96, 0.98, 1.0};

            for (var tipo : tipos) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(tipo.nome());
                double stockTon = (tipo.stockAtual() != null ? tipo.stockAtual() : 0.0) / 1000.0;
                for (int i = 0; i < meses.length; i++) {
                    series.getData().add(new XYChart.Data<>(meses[i], Math.round(stockTon * fatores[i] * 100.0) / 100.0));
                }
                chartStockPellets.getData().add(series);
            }
        } catch (Exception e) {
            // leave chart empty on error
        }
    }

    private void inicializarBudgetChart() {
        XYChart.Series<Number, Number> materiais = new XYChart.Series<>();
        materiais.setName("Materiais");
        XYChart.Series<Number, Number> operacional = new XYChart.Series<>();
        operacional.setName("Operacional");

        // {week, budget_k€, radius} — radius in axis units: keep <= 1.5 to avoid blob overlap
        double[][] materiaisData = {{4,72,1.2},{8,58,1.0},{13,85,1.4},{17,63,1.1},{22,79,1.3},{27,91,1.5},{31,68,1.1},{36,82,1.3},{40,74,1.2},{45,88,1.4},{49,65,1.0},{52,77,1.2}};
        double[][] operacionalData = {{6,34,1.0},{10,42,1.1},{14,28,0.9},{19,51,1.2},{23,38,1.0},{28,47,1.1},{33,31,0.9},{37,55,1.2},{41,43,1.1},{46,36,1.0},{50,49,1.1},{53,41,1.0}};

        for (double[] d : materiaisData) {
            materiais.getData().add(new XYChart.Data<>(d[0], d[1], d[2]));
        }
        for (double[] d : operacionalData) {
            operacional.getData().add(new XYChart.Data<>(d[0], d[1], d[2]));
        }
        chartBudget.getData().addAll(materiais, operacional);
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
