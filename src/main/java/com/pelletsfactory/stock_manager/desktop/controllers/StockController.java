package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.services.FinanceiroService;
import com.pelletsfactory.stock_manager.common.services.MoedaService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class StockController {
    private final FinanceiroService financeiroService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final MoedaService moedaService;
    private final StockService stockService;
    private final I18nService i18nService;

    // Elementos de UI dos Cards
    @FXML private Label lblCurrentStock, lblMinThreshold, lblAvailableStock, lblReservedStock;
    @FXML private StackPane iconCurrentStock, iconMinThreshold, iconAvailableStock, iconReservedStock;
    @FXML private Button btnFilter, btnClear;

    // Tabela e Filtros
    @FXML private ComboBox<TipoMovimento> cmbFiltroTipo;
    @FXML private ComboBox<MoedaSimpleDTO> cmbFiltroMoeda;
    @FXML private TableView<MovimentoFinanceiroSimpleDTO> tblMovimentos;
    @FXML private VBox vboxContainer;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, TipoMovimento> colTipo;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Double> colValor;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, String> colMoeda;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Instant> colData;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Void> colAcoes;

    private PaginationControls pagination;

    private final ObservableList<MovimentoFinanceiroSimpleDTO> movimentos = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public StockController(FinanceiroService financeiroService,
                           NavigationService navigationService,
                           ToastService toastService,
                           MoedaService moedaService,
                           StockService stockService,
                           I18nService i18nService) {
        this.financeiroService = financeiroService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.moedaService = moedaService;
        this.stockService = stockService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarMovimentos, i18nService);
        configurarIcones();
        configurarTabela();
        configurarComboBoxes();
        carregarDadosEstatisticos();
        carregarMovimentos();
    }

    private void carregarDadosEstatisticos() {
        double stockPellets = stockService.calcularStockPelletsAtual();
        double stockMinimo = stockService.calcularStockPelletsMinimo();
        double stockMaterias = stockService.calcularStockMateriasPrimasKg();
        int alertas = stockService.verificarAlertasStock();

        lblCurrentStock.setText(String.format("%.0f kg", stockPellets));
        lblMinThreshold.setText(String.format("%.0f kg", stockMinimo));
        lblAvailableStock.setText(String.format("%.0f kg", stockMaterias));
        lblReservedStock.setText(String.valueOf(alertas));
    }

    private void configurarIcones() {
        setCardIcon(iconCurrentStock, "mdi2p-package-variant", "#4C7AF2");
        setCardIcon(iconMinThreshold, "mdi2a-alert-circle-outline", "#f59e0b");
        setCardIcon(iconAvailableStock, "mdi2c-check-circle-outline", "#22c55e");
        setCardIcon(iconReservedStock, "mdi2l-lock-outline", "#ef4444");

        setButtonIcon(btnFilter, "mdi2f-filter-outline");
        setButtonIcon(btnClear, "mdi2c-close-circle-outline");
    }

    private void setCardIcon(StackPane container, String literal, String color) {
        if (container == null) {
            return;
        }

        FontIcon icon = new FontIcon();
        icon.setIconLiteral(literal);
        icon.setIconSize(20);
        icon.setIconColor(javafx.scene.paint.Color.web(color));

        container.setMinSize(36, 36);
        container.setPrefSize(36, 36);
        container.setMaxSize(36, 36);
        container.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 8;");
        container.getChildren().setAll(icon);
    }

    private void setButtonIcon(Button button, String literal) {
        if (button == null) {
            return;
        }

        FontIcon icon = new FontIcon();
        icon.setIconLiteral(literal);
        icon.setIconSize(16);
        button.setGraphic(icon);
    }

    // --- LÓGICA DA TABELA (EXISTENTE) ---

    private void configurarTabela() {
        colTipo.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().tipoMovimento()));
        colTipo.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(TipoMovimento tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) setGraphic(null);
                else setGraphic(criarBadgeTipo(tipo));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colValor.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().valorTotal()));
        colValor.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                setText((empty || valor == null) ? null : String.format("%.2f", valor));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colMoeda.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().moedaCodigo()));
        configurarColunaTexto(colMoeda);

        colData.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().createdAt()));
        colData.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Instant data, boolean empty) {
                super.updateItem(data, empty);
                setText((empty || data == null) ? null : data.atZone(ZoneId.systemDefault()).format(DATETIME_FORMATTER));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setOnAction(event -> handleAbrirDetalhes(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblMovimentos.setFixedCellSize(48);
        tblMovimentos.setItems(movimentos);
    }

    private <T> void configurarColunaTexto(TableColumn<MovimentoFinanceiroSimpleDTO, T> coluna) {
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

    private void carregarMovimentos() {
        try {
            TipoMovimento tipo = cmbFiltroTipo.getValue();
            MoedaSimpleDTO moeda = cmbFiltroMoeda.getValue();
            Page<MovimentoFinanceiroSimpleDTO> page = financeiroService.listarMovimentosFinanceirosSimples(
                    pagination.pageNumberForService(), pagination.pageSize(), tipo, moeda != null ? moeda.id() : null, "createdAt", "DESC"
            );
            movimentos.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            mostrarErro(i18nService.translate("common.error") + ": " + e.getMessage());
        }
    }

    private HBox criarBadgeTipo(TipoMovimento tipo) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");

        String color = (tipo == TipoMovimento.ENTRADA) ? "#22c55e" : "#ef4444";
        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s; -fx-border-color: %s;", color + "20", color));

        FontIcon ic = new FontIcon(tipo == TipoMovimento.ENTRADA ? "mdi2a-arrow-down-circle" : "mdi2a-arrow-up-circle");
        ic.setIconColor(javafx.scene.paint.Color.web(color));
        Label l = new Label(tipo.name());
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");

        b.getChildren().addAll(ic, l);
        return b;
    }

    private void configurarComboBoxes() {
        cmbFiltroTipo.setItems(FXCollections.observableArrayList(TipoMovimento.values()));
        cmbFiltroMoeda.setItems(FXCollections.observableArrayList(moedaService.listarTodosSimplesDTO()));
        cmbFiltroMoeda.setConverter(new StringConverter<>() {
            @Override
            public String toString(MoedaSimpleDTO moeda) {
                return moeda == null ? "" : moeda.codigo();
            }

            @Override
            public MoedaSimpleDTO fromString(String codigo) {
                if (codigo == null || codigo.isBlank()) {
                    return null;
                }
                return cmbFiltroMoeda.getItems().stream()
                        .filter(m -> codigo.equalsIgnoreCase(m.codigo()))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void handleAbrirDetalhes(MovimentoFinanceiroSimpleDTO mov) {
        try {
            MovimentoFinanceiroResponseDTO detalhes = financeiroService.obterMovimentoFinanceiro(mov.id());
            VBox drawer = criarDrawerDetalhes(detalhes);
            navigationService.showModal(drawer);
        } catch (Exception e) {
            mostrarErro(i18nService.translate("common.detailsLoadError") + ": " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(MovimentoFinanceiroResponseDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("stock.movementDetails"), navigationService::hideModal);

        VBox content = new VBox(16);
        content.setPadding(new Insets(30));

        HBox tipoBox = new HBox(10);
        tipoBox.setAlignment(Pos.CENTER_LEFT);
        Label lblTipo = new Label(i18nService.translate("common.type"));
        lblTipo.getStyleClass().add("text-muted");
        lblTipo.setPrefWidth(140);
        tipoBox.getChildren().addAll(lblTipo, d.tipoMovimento() != null ? criarBadgeTipo(d.tipoMovimento()) : new Label("-"));

        String dataFormatada = d.createdAt() != null
                ? d.createdAt().atZone(ZoneId.systemDefault()).format(DATETIME_FORMATTER)
                : "-";
        String encomendaRelacionada = d.idEncomendaCliente() != null
                ? d.idEncomendaCliente().toString()
                : (d.idEncomendaFornecedor() != null ? d.idEncomendaFornecedor().toString() : "-");

        content.getChildren().addAll(
                tipoBox,
                criarCampoLeitura(i18nService.translate("common.value"), d.valorTotal() != null ? String.format("%.2f", d.valorTotal()) : "-"),
                criarCampoLeitura(i18nService.translate("common.currency"), valorOuVazio(d.moedaCodigo())),
                criarCampoLeitura(i18nService.translate("common.date"), dataFormatada),
                criarCampoLeitura(i18nService.translate("stock.relatedOrder"), encomendaRelacionada)
        );

        ScrollPane scrollPane = UiFactory.transparentScroll(content);

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    private HBox criarCampoLeitura(String label, String valor) {
        HBox campo = new HBox(10);
        campo.setAlignment(Pos.CENTER_LEFT);
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add("text-muted");
        lblLabel.setPrefWidth(140);
        Label lblValor = new Label(valorOuVazio(valor));
        lblValor.setStyle("-fx-font-weight: 500;");
        campo.getChildren().addAll(lblLabel, lblValor);
        return campo;
    }

    private String valorOuVazio(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private void mostrarErro(String m) { toastService.showError(i18nService.translate("common.error"), m); }

    @FXML private void handleFiltrar() { pagination.resetPage(); carregarMovimentos(); }
    @FXML private void handleMostrarTodos() {
        cmbFiltroTipo.setValue(null);
        cmbFiltroMoeda.setValue(null);
        handleFiltrar();
    }

    // ── Stock management drawers ──────────────────────────────────────────────

    @FXML private void handleAdicionarPellets() { abrirDrawerAjusteStock(true, true); }
    @FXML private void handleRemoverPellets()   { abrirDrawerAjusteStock(false, true); }
    @FXML private void handleAdicionarMaterial() { abrirDrawerAjusteStock(true, false); }
    @FXML private void handleRemoverMaterial()   { abrirDrawerAjusteStock(false, false); }

    private void abrirDrawerAjusteStock(boolean adicionar, boolean pellets) {
        VBox root = UiFactory.drawerRoot(480);
        String titulo = adicionar
                ? i18nService.translate(pellets ? "stock.addPelletsTitle" : "stock.addMaterialTitle")
                : i18nService.translate(pellets ? "stock.removePelletsTitle" : "stock.removeMaterialTitle");
        HBox header = UiFactory.drawerHeader(titulo, navigationService::hideModal);

        // Tipo selector
        ComboBox<Object> cmbTipo = new ComboBox<>();
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        cmbTipo.setPromptText(i18nService.translate("stock.selectType"));
        Label lblErroTipo = criarErroLabel();

        try {
            if (pellets) {
                var tipos = stockService.listarTiposPelletComFiltros(1, 100, null, null, "nome", "ASC").getContent();
                cmbTipo.setConverter(new StringConverter<>() {
                    @Override public String toString(Object o) { return o instanceof TipoPelletSimpleDTO t ? t.nome() : ""; }
                    @Override public Object fromString(String s) { return null; }
                });
                cmbTipo.getItems().setAll(tipos);
            } else {
                var mats = stockService.listarMateriasPrimasComFiltros(1, 100, null, null, null, "nome", "ASC").getContent();
                cmbTipo.setConverter(new StringConverter<>() {
                    @Override public String toString(Object o) { return o instanceof MateriaPrimaSimpleDTO m ? m.nome() + " (" + m.unidade() + ")" : ""; }
                    @Override public Object fromString(String s) { return null; }
                });
                cmbTipo.getItems().setAll(mats);
            }
        } catch (Exception e) {
            mostrarErro(e.getMessage());
            return;
        }

        cmbTipo.setOnAction(e -> { lblErroTipo.setVisible(false); lblErroTipo.setManaged(false); cmbTipo.setStyle(""); });

        // Quantidade
        TextField txtQtd = new TextField();
        txtQtd.setPromptText("0.00");
        Label lblErroQtd = criarErroLabel();
        txtQtd.textProperty().addListener((obs, ov, nv) -> { lblErroQtd.setVisible(false); lblErroQtd.setManaged(false); txtQtd.setStyle(""); });

        Label lblErroGeral = new Label();
        lblErroGeral.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-color: #ef444420; -fx-background-radius: 6; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1;");
        lblErroGeral.setWrapText(true);
        lblErroGeral.setMaxWidth(Double.MAX_VALUE);
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);

        Label lblTipoLbl = new Label(pellets ? i18nService.translate("pellet.type") : i18nService.translate("rawMaterials.title"));
        lblTipoLbl.getStyleClass().add("text-muted");
        Label lblQtdLbl = new Label(i18nService.translate("stock.quantityKg"));
        lblQtdLbl.getStyleClass().add("text-muted");

        VBox form = new VBox(20,
                new VBox(6, lblTipoLbl, cmbTipo, lblErroTipo),
                new VBox(6, lblQtdLbl, txtQtd, lblErroQtd),
                lblErroGeral
        );
        form.setPadding(new Insets(30));
        ScrollPane scroll = UiFactory.transparentScroll(form);

        HBox footer = UiFactory.drawerFooter();
        Button btnConfirmar = new Button(titulo);
        btnConfirmar.getStyleClass().add(adicionar ? "accent" : "danger");
        btnConfirmar.setPrefHeight(44);
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConfirmar, Priority.ALWAYS);
        btnConfirmar.setOnAction(e -> {
            lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
            boolean valido = true;
            if (cmbTipo.getValue() == null) {
                lblErroTipo.setText(i18nService.translate("stock.selectType"));
                lblErroTipo.setVisible(true); lblErroTipo.setManaged(true);
                cmbTipo.setStyle("-fx-border-color: #ef4444;");
                valido = false;
            }
            Double quantidade = null;
            try {
                quantidade = Double.parseDouble(txtQtd.getText().replace(",", ".").trim());
                if (!Double.isFinite(quantidade) || quantidade <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                lblErroQtd.setText(i18nService.translate("stock.quantityInvalid"));
                lblErroQtd.setVisible(true); lblErroQtd.setManaged(true);
                txtQtd.setStyle("-fx-border-color: #ef4444;");
                valido = false;
            }
            if (!valido) return;
            try {
                if (pellets) {
                    UUID id = ((TipoPelletSimpleDTO) cmbTipo.getValue()).id();
                    if (adicionar) stockService.adicionarStockPellet(id, quantidade);
                    else           stockService.subtrairStockPellet(id, quantidade);
                } else {
                    UUID id = ((MateriaPrimaSimpleDTO) cmbTipo.getValue()).id();
                    if (adicionar) stockService.adicionarStockMateriaPrima(id, quantidade);
                    else           stockService.subtrairStockMateriaPrima(id, quantidade);
                }
                String msg = i18nService.translate(adicionar ? "stock.addedSuccess" : "stock.removedSuccess");
                toastService.showSuccess(i18nService.translate("common.success"), msg);
                navigationService.hideModal();
                carregarDadosEstatisticos();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });
        footer.getChildren().add(btnConfirmar);
        root.getChildren().addAll(header, scroll, footer);
        navigationService.showModal(root);
    }

    private Label criarErroLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }
}
