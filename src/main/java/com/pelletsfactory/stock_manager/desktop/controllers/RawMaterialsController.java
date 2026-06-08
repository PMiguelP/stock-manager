package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.MateriaPrimaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.FormValidationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RawMaterialsController {

    private final StockService stockService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;
    private final FormValidationService formValidationService;

    @FXML private VBox vboxContainer;
    @FXML private HBox alertBox;
    @FXML private Label lblAlertCount;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private TableView<MateriaPrimaRow> tblRawMaterials;
    @FXML private TableColumn<MateriaPrimaRow, String> colMaterialId;
    @FXML private TableColumn<MateriaPrimaRow, String> colName;
    @FXML private TableColumn<MateriaPrimaRow, String> colUnit;
    @FXML private TableColumn<MateriaPrimaRow, String> colStock;
    @FXML private TableColumn<MateriaPrimaRow, String> colMinimum;
    @FXML private TableColumn<MateriaPrimaRow, String> colStatus;
    @FXML private TableColumn<MateriaPrimaRow, Void> colActions;

    private VBox drawerAdicionar;
    private TextField txtNomeAdicionar;
    private ComboBox<String> cmbUnidadeAdicionar;
    private TextField txtStockAdicionar;
    private TextField txtMinimoAdicionar;
    private Label lblErroNomeAdicionar;
    private Label lblErroUnidadeAdicionar;
    private Label lblErroStockAdicionar;

    private PaginationControls pagination;
    private final ObservableList<MateriaPrimaRow> materiais = FXCollections.observableArrayList();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private boolean updatingSearch;

    public RawMaterialsController(StockService stockService,
                                  NavigationService navigationService,
                                  ToastService toastService,
                                  I18nService i18nService,
                                  FormValidationService formValidationService) {
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
        this.formValidationService = formValidationService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarMaterias, i18nService);
        cmbFiltroStatus.setItems(FXCollections.observableArrayList("Normal", "Low", "Critical"));
        cmbFiltroStatus.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String key) { return key == null ? "" : i18nService.translate(key); }
            @Override public String fromString(String s) { return s; }
        });
        configurarTabela();
        configurarPesquisaDinamica();
        configurarDrawerAdicionar();
        carregarMaterias();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(e -> { pagination.resetPage(); carregarMaterias(); });
        txtSearch.textProperty().addListener((obs, old, val) -> {
            if (updatingSearch) return;
            searchDebounce.playFromStart();
        });
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> {
            if (updatingSearch) return;
            pagination.resetPage();
            carregarMaterias();
        });
    }

    @FXML
    private void handleLimpar() {
        searchDebounce.stop();
        updatingSearch = true;
        txtSearch.clear();
        cmbFiltroStatus.setValue(null);
        updatingSearch = false;
        pagination.resetPage();
        carregarMaterias();
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colMaterialId.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().codigo()));
        configurarColunaTexto(colMaterialId);

        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colName);

        colUnit.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().unidade()));
        configurarColunaTexto(colUnit);

        colStock.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stockAtual()));
        configurarColunaTexto(colStock);

        colMinimum.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stockMinimo()));
        configurarColunaTexto(colMinimum);

        colStatus.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().status()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setGraphic(empty || status == null ? null : criarBadgeStatus(status));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEye = new Button();
            private final Button btnAdjust = new Button();
            private final HBox box = new HBox(4, btnEye, btnAdjust);
            {
                btnEye.getStyleClass().addAll("button-icon", "flat");
                btnEye.setGraphic(new FontIcon("mdi2e-eye-outline:18"));
                btnEye.setOnAction(e -> handleAbrirDetalhes(getTableView().getItems().get(getIndex())));

                btnAdjust.getStyleClass().addAll("button-icon", "flat");
                btnAdjust.setGraphic(new FontIcon("mdi2s-swap-vertical:18"));
                btnAdjust.setTooltip(new Tooltip(i18nService.translate("rawMaterials.adjustStock")));
                btnAdjust.setOnAction(e -> handleAbrirAjusteRapido(getTableView().getItems().get(getIndex())));

                box.setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setAlignment(Pos.CENTER);
            }
        });

        tblRawMaterials.setFixedCellSize(48);
        tblRawMaterials.setItems(materiais);
    }

    private <T> void configurarColunaTexto(TableColumn<MateriaPrimaRow, T> coluna) {
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void carregarMaterias() {
        try {
            String nome = (txtSearch != null && !txtSearch.getText().isBlank()) ? txtSearch.getText() : null;
            String status = cmbFiltroStatus.getValue();
            Page<MateriaPrimaSimpleDTO> page = stockService.listarMateriasPrimasComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, status, "nome", "ASC"
            );
            List<MateriaPrimaRow> rows = new ArrayList<>();
            for (int i = 0; i < page.getContent().size(); i++) {
                MateriaPrimaSimpleDTO item = page.getContent().get(i);
                String itemStatus = calcularStatus(item.stockAtual(), item.stockMinimo());
                rows.add(MateriaPrimaRow.from(item, pagination.currentPage(), pagination.pageSize(), i, itemStatus));
            }
            materiais.setAll(rows);
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            mostrarErro(i18nService.translate("rawMaterials.loadError") + ": " + e.getMessage());
        }
        try {
            atualizarAlerta();
        } catch (Exception ignored) {}
    }

    private void atualizarAlerta() {
        long count = stockService.contarMateriasAbaixoMinimo();
        if (count > 0) {
            alertBox.setVisible(true);
            alertBox.setManaged(true);
            lblAlertCount.setText(count + " " + i18nService.translate("rawMaterials.lowStockAlertCount"));
        } else {
            alertBox.setVisible(false);
            alertBox.setManaged(false);
        }
    }

    private String calcularStatus(Double stockAtual, Double stockMinimo) {
        if (stockAtual == null || stockMinimo == null || stockMinimo <= 0) return "Normal";
        if (stockAtual <= 0 || stockAtual < stockMinimo * 0.5) return "Critical";
        if (stockAtual < stockMinimo) return "Low";
        return "Normal";
    }

    private HBox criarBadgeStatus(String status) {
        String color = switch (status) {
            case "Critical" -> "#ef4444";
            case "Low" -> "#f59e0b";
            default -> "#22c55e";
        };
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle(String.format(
                "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;" +
                "-fx-background-color: %s20; -fx-border-color: %s;", color.replace("#", ""), color));
        Label l = new Label(i18nService.translate(status));
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
        b.getChildren().add(l);
        return b;
    }

    // ── Material stock adjustment ─────────────────────────────────────────────

    @FXML private void handleAdicionarMaterial() { abrirDrawerAjusteMaterial(true); }
    @FXML private void handleRemoverMaterial()   { abrirDrawerAjusteMaterial(false); }

    private void abrirDrawerAjusteMaterial(boolean adicionar) {
        VBox root = UiFactory.drawerRoot(480);
        String titulo = adicionar
                ? i18nService.translate("stock.addMaterialTitle")
                : i18nService.translate("stock.removeMaterialTitle");
        HBox header = UiFactory.drawerHeader(titulo, navigationService::hideModal);

        ComboBox<MateriaPrimaSimpleDTO> cmbTipo = new ComboBox<>();
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        cmbTipo.setPromptText(i18nService.translate("stock.selectType"));
        cmbTipo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(MateriaPrimaSimpleDTO m) {
                return m != null ? m.nome() + " (" + m.unidade() + ")" : "";
            }
            @Override public MateriaPrimaSimpleDTO fromString(String s) { return null; }
        });
        Label lblErroTipo = criarErroLabel();

        try {
            cmbTipo.getItems().setAll(
                    stockService.listarMateriasPrimasComFiltros(1, 100, null, null, null, "nome", "ASC").getContent());
        } catch (Exception e) {
            mostrarErro(e.getMessage());
            return;
        }

        cmbTipo.setOnAction(e -> { lblErroTipo.setVisible(false); lblErroTipo.setManaged(false); cmbTipo.setStyle(""); });

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("0.00");
        Label lblErroQtd = criarErroLabel();
        txtQtd.textProperty().addListener((obs, ov, nv) -> { lblErroQtd.setVisible(false); lblErroQtd.setManaged(false); txtQtd.setStyle(""); });

        Label lblErroGeral = criarErroGeralLabel();

        Label lblTipoLbl = new Label(i18nService.translate("rawMaterials.title"));
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
                if (adicionar) stockService.adicionarStockMateriaPrima(id, quantidade);
                else           stockService.subtrairStockMateriaPrima(id, quantidade);
                mostrarSucesso(i18nService.translate(adicionar ? "stock.addedSuccess" : "stock.removedSuccess"));
                navigationService.hideModal();
                carregarMaterias();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });
        footer.getChildren().add(btnConfirmar);
        root.getChildren().addAll(header, UiFactory.transparentScroll(form), footer);
        navigationService.showModal(root);
    }

    private void handleAbrirAjusteRapido(MateriaPrimaRow row) {
        VBox root = UiFactory.drawerRoot(480);
        HBox header = UiFactory.drawerHeader(row.nome(), navigationService::hideModal);

        Label lblCurrentLabel = new Label(i18nService.translate("stock.current"));
        lblCurrentLabel.getStyleClass().add("text-muted");
        Label lblCurrentValue = new Label(row.stockAtual() + " " + row.unidade());
        lblCurrentValue.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        ToggleGroup direcaoGroup = new ToggleGroup();
        ToggleButton btnAdd = new ToggleButton(i18nService.translate("stock.entry"));
        ToggleButton btnRemove = new ToggleButton(i18nService.translate("stock.exit"));
        btnAdd.setToggleGroup(direcaoGroup);
        btnRemove.setToggleGroup(direcaoGroup);
        btnAdd.setSelected(true);
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnRemove.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAdd, Priority.ALWAYS);
        HBox.setHgrow(btnRemove, Priority.ALWAYS);
        HBox toggleRow = new HBox(0, btnAdd, btnRemove);
        toggleRow.setMaxWidth(Double.MAX_VALUE);

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("0.00");
        Label lblQtdLabel = new Label(i18nService.translate("stock.quantityKg"));
        lblQtdLabel.getStyleClass().add("text-muted");
        Label lblErroQtd = criarErroLabel();
        txtQtd.textProperty().addListener((obs, ov, nv) -> { lblErroQtd.setVisible(false); lblErroQtd.setManaged(false); txtQtd.setStyle(""); });

        Label lblErroGeral = criarErroGeralLabel();

        VBox form = new VBox(20,
                new VBox(4, lblCurrentLabel, lblCurrentValue),
                toggleRow,
                new VBox(6, lblQtdLabel, txtQtd, lblErroQtd),
                lblErroGeral);
        form.setPadding(new Insets(30));

        HBox footer = UiFactory.drawerFooter();
        Button btnConfirmar = UiFactory.drawerPrimaryAction(
                i18nService.translate("rawMaterials.adjustStock"), "mdi2c-check");
        btnConfirmar.setPrefHeight(44);
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConfirmar, Priority.ALWAYS);
        btnConfirmar.setOnAction(e -> {
            lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
            Double quantidade = null;
            try {
                quantidade = Double.parseDouble(txtQtd.getText().replace(",", ".").trim());
                if (!Double.isFinite(quantidade) || quantidade <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                lblErroQtd.setText(i18nService.translate("stock.quantityInvalid"));
                lblErroQtd.setVisible(true); lblErroQtd.setManaged(true);
                txtQtd.setStyle("-fx-border-color: #ef4444;");
                return;
            }
            try {
                boolean adicionar = direcaoGroup.getSelectedToggle() == btnAdd;
                if (adicionar) stockService.adicionarStockMateriaPrima(row.id(), quantidade);
                else           stockService.subtrairStockMateriaPrima(row.id(), quantidade);
                mostrarSucesso(i18nService.translate(adicionar ? "stock.addedSuccess" : "stock.removedSuccess"));
                navigationService.hideModal();
                carregarMaterias();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });
        footer.getChildren().add(btnConfirmar);
        root.getChildren().addAll(header, UiFactory.transparentScroll(form), footer);
        navigationService.showModal(root);
    }

    // ── Create drawer ─────────────────────────────────────────────────────────

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        navigationService.showModal(drawerAdicionar);
    }

    private void configurarDrawerAdicionar() {
        drawerAdicionar = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("rawMaterials.title"), navigationService::hideModal);

        txtNomeAdicionar = new TextField();
        txtNomeAdicionar.setPromptText(i18nService.translate("rawMaterials.nameExample"));
        lblErroNomeAdicionar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNomeAdicionar, lblErroNomeAdicionar);

        cmbUnidadeAdicionar = new ComboBox<>(FXCollections.observableArrayList("kg", "ton", "m3", "l"));
        cmbUnidadeAdicionar.setEditable(true);
        cmbUnidadeAdicionar.setPromptText(i18nService.translate("rawMaterials.unit"));
        cmbUnidadeAdicionar.setMaxWidth(Double.MAX_VALUE);
        lblErroUnidadeAdicionar = formValidationService.createErrorLabel();
        formValidationService.attachComboAutoClear(cmbUnidadeAdicionar, lblErroUnidadeAdicionar);

        txtStockAdicionar = new TextField();
        txtStockAdicionar.setPromptText("0");
        lblErroStockAdicionar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtStockAdicionar, lblErroStockAdicionar);

        txtMinimoAdicionar = new TextField();
        txtMinimoAdicionar.setPromptText("0");

        VBox form = new VBox(20,
                criarCampoComErro(i18nService.translate("common.name") + " *", txtNomeAdicionar, lblErroNomeAdicionar),
                criarCampoComErro(i18nService.translate("rawMaterials.unit") + " *", cmbUnidadeAdicionar, lblErroUnidadeAdicionar),
                criarCampoComErro(i18nService.translate("stock.current") + " *", txtStockAdicionar, lblErroStockAdicionar),
                criarCampo(i18nService.translate("stock.minimum"), txtMinimoAdicionar)
        );
        form.setPadding(new Insets(30));

        ScrollPane scroll = UiFactory.transparentScroll(form);
        Button btnCreate = UiFactory.drawerPrimaryAction(i18nService.translate("rawMaterials.add"), "mdi2c-content-save-outline");
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        btnCreate.setOnAction(e -> handleAdicionar());

        HBox footer = UiFactory.drawerFooter();
        footer.getChildren().add(btnCreate);
        drawerAdicionar.getChildren().addAll(header, scroll, footer);
    }

    private void limparFormulario() {
        txtNomeAdicionar.clear();
        cmbUnidadeAdicionar.setValue(null);
        txtStockAdicionar.clear();
        txtMinimoAdicionar.clear();
        formValidationService.clearError(txtNomeAdicionar, lblErroNomeAdicionar);
        formValidationService.clearError(cmbUnidadeAdicionar, lblErroUnidadeAdicionar);
        formValidationService.clearError(txtStockAdicionar, lblErroStockAdicionar);
    }

    private void handleAdicionar() {
        boolean valido = formValidationService.validateRequiredText(txtNomeAdicionar, lblErroNomeAdicionar,
                i18nService.translate("common.name") + " " + i18nService.translate("common.required"));
        valido = formValidationService.validateRequiredCombo(cmbUnidadeAdicionar, lblErroUnidadeAdicionar,
                i18nService.translate("rawMaterials.unit") + " " + i18nService.translate("common.required")) && valido;

        Double stockAtual = null;
        if (!txtStockAdicionar.getText().isBlank()) {
            valido = formValidationService.validateRegex(txtStockAdicionar, lblErroStockAdicionar,
                    "[0-9]+(\\.[0-9]+)?([,][0-9]+)?", "Stock deve ser um número não negativo") && valido;
            if (valido) stockAtual = Double.parseDouble(txtStockAdicionar.getText().replace(",", "."));
        }
        if (!valido) return;

        try {
            Double stockMinimo = parseNumero(txtMinimoAdicionar.getText());
            stockService.criarMateriaPrima(new MateriaPrimaRequestDTO(
                    txtNomeAdicionar.getText().trim(),
                    cmbUnidadeAdicionar.getValue().trim(),
                    stockAtual,
                    stockMinimo
            ));
            pagination.resetPage();
            carregarMaterias();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("rawMaterials.created"));
        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    // ── Details drawer ────────────────────────────────────────────────────────

    private void handleAbrirDetalhes(MateriaPrimaRow row) {
        try {
            MateriaPrimaDetailsDTO d = stockService.obterDetalhesMateriaPrima(row.id());
            navigationService.showModal(criarDrawerDetalhes(d));
        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(MateriaPrimaDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("rawMaterials.title"), navigationService::hideModal);

        TextField txtNome = new TextField(d.nome() != null ? d.nome() : "");
        txtNome.setDisable(true);
        Label lblErroNome = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNome, lblErroNome);

        ComboBox<String> cmbUnidade = new ComboBox<>(FXCollections.observableArrayList("kg", "ton", "m3", "l"));
        cmbUnidade.setEditable(true);
        cmbUnidade.setValue(d.unidade());
        cmbUnidade.setMaxWidth(Double.MAX_VALUE);
        cmbUnidade.setDisable(true);
        Label lblErroUnidade = formValidationService.createErrorLabel();
        formValidationService.attachComboAutoClear(cmbUnidade, lblErroUnidade);

        TextField txtStock = new TextField(d.stockAtual() != null ? String.format("%.2f", d.stockAtual()) : "");
        txtStock.setDisable(true);
        Label lblErroStock = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtStock, lblErroStock);

        TextField txtMinimo = new TextField(d.stockMinimo() != null ? String.format("%.2f", d.stockMinimo()) : "");
        txtMinimo.setDisable(true);

        Label lblErroGeral = new Label();
        lblErroGeral.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12px;-fx-padding:6 10;-fx-background-color:#ef444418;-fx-background-radius:6;");
        lblErroGeral.setWrapText(true);
        lblErroGeral.setMaxWidth(Double.MAX_VALUE);
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);

        VBox form = new VBox(20,
                criarCampoComErro(i18nService.translate("common.name") + " *", txtNome, lblErroNome),
                criarCampoComErro(i18nService.translate("rawMaterials.unit") + " *", cmbUnidade, lblErroUnidade),
                criarCampoComErro(i18nService.translate("stock.current") + " *", txtStock, lblErroStock),
                criarCampo(i18nService.translate("stock.minimum"), txtMinimo),
                lblErroGeral
        );
        form.setPadding(new Insets(30));

        ScrollPane scroll = UiFactory.transparentScroll(form);

        Button btnEliminar = UiFactory.drawerDangerAction(i18nService.translate("common.delete"), "mdi2d-delete-outline");
        Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        Button btnEditar   = UiFactory.drawerSecondaryAction(i18nService.translate("common.edit"), "mdi2p-pencil-outline");
        Button btnGuardar  = UiFactory.drawerPrimaryAction(i18nService.translate("common.save"), "mdi2c-content-save-outline");

        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
        btnGuardar.setDisable(true);

        HBox footer = UiFactory.drawerActionFooter(btnEliminar, btnCancelar, btnEditar, btnGuardar);

        Control[] campos = {txtNome, cmbUnidade, txtStock, txtMinimo};

        btnEditar.setOnAction(e -> {
            for (Control c : campos) c.setDisable(false);
            btnGuardar.setDisable(false);
            btnEditar.setVisible(false);  btnEditar.setManaged(false);
            btnCancelar.setVisible(true); btnCancelar.setManaged(true);
        });

        btnCancelar.setOnAction(e -> {
            for (Control c : campos) c.setDisable(true);
            txtNome.setText(d.nome() != null ? d.nome() : "");
            cmbUnidade.setValue(d.unidade());
            txtStock.setText(d.stockAtual() != null ? String.format("%.2f", d.stockAtual()) : "");
            txtMinimo.setText(d.stockMinimo() != null ? String.format("%.2f", d.stockMinimo()) : "");
            lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
            btnGuardar.setDisable(true);
            btnCancelar.setVisible(false); btnCancelar.setManaged(false);
            btnEditar.setVisible(true);    btnEditar.setManaged(true);
        });

        btnGuardar.setOnAction(e -> {
            lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
            boolean valido = formValidationService.validateRequiredText(txtNome, lblErroNome,
                    i18nService.translate("common.name") + " " + i18nService.translate("common.required"));
            valido = formValidationService.validateRequiredCombo(cmbUnidade, lblErroUnidade,
                    i18nService.translate("rawMaterials.unit") + " " + i18nService.translate("common.required")) && valido;

            Double stockAtual = null;
            if (!txtStock.getText().isBlank()) {
                valido = formValidationService.validateRegex(txtStock, lblErroStock,
                        "[0-9]+(\\.[0-9]+)?([,][0-9]+)?", "Stock deve ser um número não negativo") && valido;
                if (valido) stockAtual = Double.parseDouble(txtStock.getText().replace(",", "."));
            }
            if (!valido) return;

            try {
                Double stockMinimo = parseNumero(txtMinimo.getText());
                stockService.atualizarMateriaPrima(d.id(), new MateriaPrimaRequestDTO(
                        txtNome.getText().trim(),
                        cmbUnidade.getValue().trim(),
                        stockAtual,
                        stockMinimo
                ));
                pagination.resetPage();
                carregarMaterias();
                navigationService.hideModal();
                mostrarSucesso(i18nService.translate("rawMaterials.updated"));
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });

        btnEliminar.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(i18nService.translate("common.delete"));
            confirm.setHeaderText(i18nService.translate("common.confirmDelete"));
            confirm.setContentText(d.nome());
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
            try {
                stockService.apagarMateriaPrima(d.id());
                pagination.resetPage();
                carregarMaterias();
                navigationService.hideModal();
                mostrarSucesso(i18nService.translate("rawMaterials.deleted"));
            } catch (Exception ex) {
                mostrarErro(ex.getMessage());
            }
        });

        root.getChildren().addAll(header, scroll, footer);
        return root;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox criarCampo(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private VBox criarCampoComErro(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    private Double parseNumero(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            double v = Double.parseDouble(raw.replace(",", "."));
            if (!Double.isFinite(v) || v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(i18nService.translate("stock.minimum") + " deve ser >= 0");
        }
    }

    private Label criarErroLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private Label criarErroGeralLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-color: #ef444420; -fx-background-radius: 6; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void mostrarErro(String m)    { toastService.showError(i18nService.translate("common.error"), m); }
    private void mostrarSucesso(String m) { toastService.showSuccess(i18nService.translate("common.success"), m); }

    private record MateriaPrimaRow(
            UUID id,
            String codigo,
            String nome,
            String unidade,
            String stockAtual,
            String stockMinimo,
            String status
    ) {
        static MateriaPrimaRow from(MateriaPrimaSimpleDTO dto, int paginaAtual, int itemsPerPage, int index, String status) {
            int seq = paginaAtual * itemsPerPage + index + 1;
            return new MateriaPrimaRow(
                    dto.id(),
                    String.format("RM-%03d", seq),
                    dto.nome(),
                    dto.unidade(),
                    dto.stockAtual() != null ? String.format("%.2f", dto.stockAtual()) : "-",
                    dto.stockMinimo() != null ? String.format("%.2f", dto.stockMinimo()) : "-",
                    status
            );
        }
    }
}
