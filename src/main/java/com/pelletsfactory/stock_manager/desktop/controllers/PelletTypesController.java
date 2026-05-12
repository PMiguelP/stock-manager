package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.FormulaProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
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

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<TipoPelletRow> data = FXCollections.observableArrayList();

    public PelletTypesController(StockService stockService,
                                 FormulaProducaoService formulaService,
                                 NavigationService navigationService,
                                 ToastService toastService) {
        this.stockService = stockService;
        this.formulaService = formulaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        carregarPelletTypes();
    }

    @FXML
    private void handleRefresh() {
        carregarPelletTypes();
    }

    @FXML
    private void handleFiltrar() {
        paginaAtual = 0;
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
                    paginaAtual + 1, itemsPerPage, nome, null, "nome", "ASC"
            );

            List<TipoPelletRow> rows = new ArrayList<>();
            for (TipoPelletSimpleDTO item : page.getContent()) {
                TipoPelletDetailsDTO details = stockService.obterDetalhesTipoPellet(item.id());
                boolean formulaDefinida = formulaService.listarFormulasPorTipoPellet(item.id(), 1, 1).getTotalElements() > 0;
                rows.add(TipoPelletRow.from(item, details, formulaDefinida));
            }

            data.setAll(rows);
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            mostrarErro("Erro ao carregar tipos de pellet: " + e.getMessage());
        }
    }

    private void configurarPaginacao(VBox container) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(20, 0, 20, 0));
        nav.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");
        HBox left = new HBox(lblPaginaStatus); left.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(left, Priority.ALWAYS);

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarPelletTypes(); });
        HBox center = new HBox(10, new Label("Por página"), cmbItemsPerPage); center.setAlignment(Pos.CENTER); HBox.setHgrow(center, Priority.ALWAYS);

        paginationButtons = new HBox(5);
        HBox right = new HBox(paginationButtons); right.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(right, Priority.ALWAYS);

        nav.getChildren().addAll(left, center, right);
        container.getChildren().add(nav);
    }

    private void atualizarBotoesPaginacao() {
        paginationButtons.getChildren().clear();
        Button prev = new Button(); prev.setGraphic(new FontIcon("mdi2c-chevron-left"));
        prev.setDisable(paginaAtual == 0);
        prev.setOnAction(e -> { paginaAtual--; carregarPelletTypes(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int idx = i; p.setOnAction(e -> { paginaAtual = idx; carregarPelletTypes(); });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button(); next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarPelletTypes(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<?> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
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
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Pellet Type Details");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

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

