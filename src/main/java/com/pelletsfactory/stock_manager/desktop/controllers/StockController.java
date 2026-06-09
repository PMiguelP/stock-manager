package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StockController {

    private final NavigationService navigationService;
    private final ToastService toastService;
    private final StockService stockService;
    private final I18nService i18nService;

    @FXML private Label lblCurrentStock, lblMinThreshold, lblAvailableStock, lblReservedStock;
    @FXML private StackPane iconCurrentStock, iconMinThreshold, iconAvailableStock, iconReservedStock;
    @FXML private VBox vboxContainer;
    @FXML private TextField txtSearch;
    @FXML private TableView<TipoPelletSimpleDTO> tblPellets;
    @FXML private TableColumn<TipoPelletSimpleDTO, String> colNome, colDiametro, colStockAtual, colStockMin;
    @FXML private TableColumn<TipoPelletSimpleDTO, TipoPelletSimpleDTO> colStatus;
    @FXML private TableColumn<TipoPelletSimpleDTO, Void> colAcoes;

    private PaginationControls pagination;
    private final ObservableList<TipoPelletSimpleDTO> pellets = FXCollections.observableArrayList();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private boolean updatingSearch;

    public StockController(NavigationService navigationService,
                           ToastService toastService,
                           StockService stockService,
                           I18nService i18nService) {
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.stockService = stockService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        configurarIcones();
        carregarDadosEstatisticos();
        pagination = new PaginationControls(10, this::carregarPellets, i18nService);
        configurarTabela();
        configurarPesquisaDinamica();
        carregarPellets();
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

    private void configurarTabela() {
        colNome.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nome()));
        colDiametro.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().diametroMm() != null ? cd.getValue().diametroMm() + " mm" : "-"));
        colStockAtual.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%.0f kg", cd.getValue().stockAtual() != null ? cd.getValue().stockAtual() : 0)));
        colStockMin.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%.0f kg", cd.getValue().stockMinimo() != null ? cd.getValue().stockMinimo() : 0)));

        colStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TipoPelletSimpleDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                boolean alerta = item.stockAtual() != null && item.stockMinimo() != null
                        && item.stockAtual() < item.stockMinimo();
                String icon  = alerta ? "mdi2a-alert-outline:14"     : "mdi2c-check-circle-outline:14";
                String cor   = alerta ? "#ef4444"                     : "#22c55e";
                String label = alerta ? i18nService.translate("stock.alert") : i18nService.translate("stock.ok");
                setGraphic(UiFactory.statusBadge(label, icon, cor));
                setPadding(new Insets(6, 10, 6, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnAdd = new Button();
            private final Button btnRem = new Button();
            private final HBox box = new HBox(4, btnAdd, btnRem);
            {
                btnAdd.getStyleClass().addAll("button-icon", "flat");
                btnAdd.setGraphic(new FontIcon("mdi2p-plus-circle-outline:18"));
                btnAdd.setTooltip(new Tooltip(i18nService.translate("stock.addPellets")));
                btnRem.getStyleClass().addAll("button-icon", "flat");
                btnRem.setGraphic(new FontIcon("mdi2m-minus-circle-outline:18"));
                btnRem.setTooltip(new Tooltip(i18nService.translate("stock.removePellets")));
                btnAdd.setOnAction(ev -> abrirDrawerAjustePelletsComTipo(getTableView().getItems().get(getIndex()), true));
                btnRem.setOnAction(ev -> abrirDrawerAjustePelletsComTipo(getTableView().getItems().get(getIndex()), false));
                box.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setPadding(new Insets(4, 8, 4, 8));
            }
        });

        for (TableColumn<TipoPelletSimpleDTO, ?> col : java.util.List.of(colNome, colDiametro, colStockAtual, colStockMin)) {
            ((TableColumn<TipoPelletSimpleDTO, Object>) col).setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? null : item.toString());
                    setPadding(new Insets(8, 10, 8, 10));
                    setAlignment(Pos.CENTER_LEFT);
                }
            });
        }

        tblPellets.setFixedCellSize(48);
        tblPellets.setItems(pellets);
    }

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(e -> carregarPellets());
        txtSearch.textProperty().addListener((obs, old, val) -> {
            if (updatingSearch) return;
            pagination.resetPage();
            searchDebounce.playFromStart();
        });
    }

    private void carregarPellets() {
        try {
            String nome = (txtSearch != null && !txtSearch.getText().isBlank()) ? txtSearch.getText().trim() : null;
            Page<TipoPelletSimpleDTO> page = stockService.listarTiposPelletComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, "nome", "ASC");
            pellets.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    @FXML
    private void handleLimpar() {
        searchDebounce.stop();
        updatingSearch = true;
        txtSearch.clear();
        updatingSearch = false;
        pagination.resetPage();
        carregarPellets();
    }

    private void configurarIcones() {
        setCardIcon(iconCurrentStock, "mdi2p-package-variant", "#4C7AF2");
        setCardIcon(iconMinThreshold, "mdi2a-alert-circle-outline", "#f59e0b");
        setCardIcon(iconAvailableStock, "mdi2c-check-circle-outline", "#22c55e");
        setCardIcon(iconReservedStock, "mdi2l-lock-outline", "#ef4444");
    }

    private void setCardIcon(StackPane container, String literal, String color) {
        if (container == null) return;
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

    @FXML private void handleAdicionarPellets() { abrirDrawerAjustePellets(true, null); }
    @FXML private void handleRemoverPellets()   { abrirDrawerAjustePellets(false, null); }

    private void abrirDrawerAjustePelletsComTipo(TipoPelletSimpleDTO tipo, boolean adicionar) {
        abrirDrawerAjustePellets(adicionar, tipo);
    }

    private void abrirDrawerAjustePellets(boolean adicionar, TipoPelletSimpleDTO preselect) {
        VBox root = UiFactory.drawerRoot(480);
        String titulo = adicionar
                ? i18nService.translate("stock.addPelletsTitle")
                : i18nService.translate("stock.removePelletsTitle");
        HBox header = UiFactory.drawerHeader(titulo, navigationService::hideModal);

        ComboBox<TipoPelletSimpleDTO> cmbTipo = new ComboBox<>();
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        cmbTipo.setPromptText(i18nService.translate("stock.selectType"));
        cmbTipo.setConverter(new StringConverter<>() {
            @Override public String toString(TipoPelletSimpleDTO o) { return o != null ? o.nome() : ""; }
            @Override public TipoPelletSimpleDTO fromString(String s) { return null; }
        });
        Label lblErroTipo = criarErroLabel();

        try {
            cmbTipo.getItems().setAll(
                    stockService.listarTiposPelletComFiltros(1, 100, null, null, "nome", "ASC").getContent());
        } catch (Exception e) {
            mostrarErro(e.getMessage());
            return;
        }

        if (preselect != null) {
            cmbTipo.getItems().stream().filter(t -> t.id().equals(preselect.id())).findFirst()
                    .ifPresent(cmbTipo::setValue);
        }

        cmbTipo.setOnAction(e -> { lblErroTipo.setVisible(false); lblErroTipo.setManaged(false); cmbTipo.setStyle(""); });

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

        Label lblTipoLbl = new Label(i18nService.translate("pellet.type"));
        lblTipoLbl.getStyleClass().add("text-muted");
        Label lblQtdLbl = new Label(i18nService.translate("stock.quantityKg"));
        lblQtdLbl.getStyleClass().add("text-muted");

        VBox form = new VBox(20,
                new VBox(6, lblTipoLbl, cmbTipo, lblErroTipo),
                new VBox(6, lblQtdLbl, txtQtd, lblErroQtd),
                lblErroGeral);
        form.setPadding(new Insets(30));

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
                UUID id = cmbTipo.getValue().id();
                if (adicionar) stockService.adicionarStockPellet(id, quantidade);
                else           stockService.subtrairStockPellet(id, quantidade);
                toastService.showSuccess(i18nService.translate("common.success"),
                        i18nService.translate(adicionar ? "stock.addedSuccess" : "stock.removedSuccess"));
                navigationService.hideModal();
                carregarDadosEstatisticos();
                carregarPellets();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });
        footer.getChildren().add(btnConfirmar);
        root.getChildren().addAll(header, UiFactory.transparentScroll(form), footer);
        navigationService.showModal(root);
    }

    private Label criarErroLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void mostrarErro(String m) { toastService.showError(i18nService.translate("common.error"), m); }
}
