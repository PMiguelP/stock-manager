package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.FormulaProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PelletTypesController {

    private final StockService stockService;
    private final FormulaProducaoService formulaService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;

    @FXML private VBox vboxContainer;
    @FXML private TextField txtFiltroNome;
    @FXML private TableView<TipoPelletRow> tblPelletTypes;
    @FXML private TableColumn<TipoPelletRow, String> colCodigo;
    @FXML private TableColumn<TipoPelletRow, String> colNome;
    @FXML private TableColumn<TipoPelletRow, String> colDiametro;
    @FXML private TableColumn<TipoPelletRow, String> colCalorifico;
    @FXML private TableColumn<TipoPelletRow, String> colStockAtual;
    @FXML private TableColumn<TipoPelletRow, String> colStockMinimo;
    @FXML private TableColumn<TipoPelletRow, String> colCustoKg;
    @FXML private TableColumn<TipoPelletRow, Boolean> colFormula;
    @FXML private TableColumn<TipoPelletRow, Void> colAcoes;

    private PaginationControls pagination;

    private final ObservableList<TipoPelletRow> data = FXCollections.observableArrayList();

    public PelletTypesController(StockService stockService,
                                 FormulaProducaoService formulaService,
                                 NavigationService navigationService,
                                 ToastService toastService,
                                 I18nService i18nService) {
        this.stockService = stockService;
        this.formulaService = formulaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarPelletTypes, i18nService);
        configurarTabela();
        carregarPelletTypes();
    }

    @FXML
    private void handleFiltrar() {
        pagination.resetPage();
        carregarPelletTypes();
    }

    @FXML
    private void handleMostrarTodos() {
        txtFiltroNome.clear();
        handleFiltrar();
    }

    private void configurarTabela() {
        colCodigo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().codigo()));
        configurarColunaTexto(colCodigo);

        colNome.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colNome);

        colDiametro.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().diametro()));
        configurarColunaTexto(colDiametro);

        colCalorifico.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().calorifico()));
        configurarColunaTexto(colCalorifico);

        colStockAtual.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stockAtual()));
        configurarColunaTexto(colStockAtual);

        colStockMinimo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stockMinimo()));
        configurarColunaTexto(colStockMinimo);

        colCustoKg.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().custoKg()));
        configurarColunaTexto(colCustoKg);

        colFormula.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().formulaDefinida()));
        colFormula.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean definida, boolean empty) {
                super.updateItem(definida, empty);
                setGraphic(empty || definida == null ? null : criarBadgeFormula(definida));
                setAlignment(Pos.CENTER_LEFT);
                setPadding(new Insets(8, 10, 8, 10));
            }
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setOnAction(event -> handleAbrirDetalhes(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblPelletTypes.setFixedCellSize(48);
        tblPelletTypes.setItems(data);
    }

    private <T> void configurarColunaTexto(TableColumn<TipoPelletRow, T> coluna) {
        coluna.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    private void carregarPelletTypes() {
        try {
            String nome = (txtFiltroNome != null && !txtFiltroNome.getText().isBlank()) ? txtFiltroNome.getText() : null;
            Page<TipoPelletSimpleDTO> page = stockService.listarTiposPelletComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, "nome", "ASC"
            );

            List<TipoPelletRow> rows = new ArrayList<>();
            for (TipoPelletSimpleDTO item : page.getContent()) {
                TipoPelletDetailsDTO details = stockService.obterDetalhesTipoPellet(item.id());
                boolean formulaDefinida = formulaService.listarFormulasPorTipoPellet(item.id(), 1, 1).getTotalElements() > 0;
                rows.add(TipoPelletRow.from(item, details, formulaDefinida));
            }

            data.setAll(rows);
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            mostrarErro("Erro ao carregar tipos de pellet: " + e.getMessage());
        }
    }

    private HBox criarBadgeFormula(Boolean definida) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");

        String color = definida ? "#22c55e" : "#f59e0b";
        String text = definida ? "Defined" : "Missing";
        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s20; -fx-border-color: %s;",
                color.replace("#", ""), color));
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
        b.getChildren().add(l);
        return b;
    }

    private void handleAbrirDetalhes(TipoPelletRow row) {
        try {
            TipoPelletDetailsDTO d = stockService.obterDetalhesTipoPellet(row.id());
            VBox drawer = criarDrawerDetalhes(d, row.formulaDefinida());
            navigationService.showModal(drawer);
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(TipoPelletDetailsDTO d, boolean formulaDefinida) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Pellet Type Details", navigationService::hideModal);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);

        addDetalhe(grid, 0, "Name", d.nome());
        addDetalhe(grid, 1, "Diameter", d.diametroMm() != null ? d.diametroMm() + " mm" : "-");
        addDetalhe(grid, 2, "Calorific Value", d.poderCalorifico() != null ? d.poderCalorifico() + " kWh/ton" : "-");
        addDetalhe(grid, 3, "Current Stock", d.stockAtual() != null ? d.stockAtual() + " tons" : "-");
        addDetalhe(grid, 4, "Minimum Stock", d.stockMinimo() != null ? d.stockMinimo() + " tons" : "-");
        String custo = d.custoAtualPorKg() != null ? String.format("%.2f", d.custoAtualPorKg()) : "-";
        addDetalhe(grid, 5, "Cost per kg", d.moedaCodigo() != null ? d.moedaCodigo() + " " + custo : custo);
        addDetalhe(grid, 6, "Formula", formulaDefinida ? "Defined" : "Missing");

        VBox body = new VBox(20, grid);
        body.setPadding(new Insets(25));

        root.getChildren().addAll(header, body);
        return root;
    }

    private void addDetalhe(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        Label val = new Label(value);
        val.getStyleClass().add("text-strong");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private void mostrarErro(String m) {
        toastService.showError("Erro", m);
    }

    private record TipoPelletRow(
            UUID id,
            String codigo,
            String nome,
            String diametro,
            String calorifico,
            String stockAtual,
            String stockMinimo,
            String custoKg,
            Boolean formulaDefinida
    ) {
        static TipoPelletRow from(TipoPelletSimpleDTO simple, TipoPelletDetailsDTO details, boolean formulaDefinida) {
            String codigo = simple.id() != null ? "PLT-" + simple.id().toString().substring(0, 6).toUpperCase() : "N/A";
            String diametro = simple.diametroMm() != null ? simple.diametroMm() + " mm" : "-";
            String calorifico = details.poderCalorifico() != null ? details.poderCalorifico() + " kWh/ton" : "-";
            String stockAtual = simple.stockAtual() != null ? simple.stockAtual() + " tons" : "-";
            String stockMinimo = simple.stockMinimo() != null ? simple.stockMinimo() + " tons" : "-";
            String custoKg = details.custoAtualPorKg() != null
                    ? (details.moedaCodigo() != null ? details.moedaCodigo() + " " : "") + String.format("%.2f", details.custoAtualPorKg())
                    : "-";
            return new TipoPelletRow(simple.id(), codigo, simple.nome(), diametro, calorifico, stockAtual, stockMinimo, custoKg, formulaDefinida);
        }
    }
}
