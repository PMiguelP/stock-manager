package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.FormulaProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.request.FormulaProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.services.FormulaProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.FormValidationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.animation.PauseTransition;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class FormulaController {
    private final FormulaProducaoService formulaService;
    private final StockService stockService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final FormValidationService formValidationService;
    private final I18nService i18nService;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private TableView<FormulaSimpleDTO> tblFormulas;
    @FXML private VBox vboxContainer;
    @FXML private TableColumn<FormulaSimpleDTO, String> colCodigo;
    @FXML private TableColumn<FormulaSimpleDTO, String> colNome;
    @FXML private TableColumn<FormulaSimpleDTO, String> colTipoPellet;
    @FXML private TableColumn<FormulaSimpleDTO, Boolean> colAtiva;
    @FXML private TableColumn<FormulaSimpleDTO, Void> colAcoes;

    private VBox drawerRoot;
    private ComboBox<TipoPelletItem> cmbTipoPellet;
    private TextField txtVersao;
    private VBox ingredientesContainer;
    private Label lblTotalKg;
    private Label lblWarning;
    private CheckBox chkAtiva;
    private List<IngredienteRow> ingredienteRows;
    private Label lblErroCriarTipoPellet;
    private Label lblErroCriarNome;

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<FormulaSimpleDTO> formulas = FXCollections.observableArrayList();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private boolean updatingSearch;

    public FormulaController(FormulaProducaoService formulaService,
                             StockService stockService,
                             NavigationService navigationService,
                             ToastService toastService,
                             FormValidationService formValidationService,
                             I18nService i18nService) {
        this.formulaService = formulaService;
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.formValidationService = formValidationService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        resetPaginationControls();
        configurarTabela();
        configurarComboBoxes();
        configurarPesquisaDinamica();
        configurarDrawerAdicionar();
        carregarFormulas();
    }

    private void resetPaginationControls() {
        lblPaginaStatus = null;
        cmbItemsPerPage = null;
        paginationButtons = null;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(e -> { paginaAtual = 0; carregarFormulas(); });
        txtSearch.textProperty().addListener((obs, old, val) -> {
            if (updatingSearch) return;
            searchDebounce.playFromStart();
        });
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> {
            if (updatingSearch) return;
            paginaAtual = 0;
            carregarFormulas();
        });
    }

    @FXML
    private void handleLimpar() {
        searchDebounce.stop();
        updatingSearch = true;
        txtSearch.clear();
        cmbFiltroStatus.setValue(null);
        updatingSearch = false;
        paginaAtual = 0;
        carregarFormulas();
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colCodigo.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().id() != null ? cd.getValue().id().toString() : "N/A"
        ));
        configurarColunaTexto(colCodigo);

        colNome.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colNome);

        colTipoPellet.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().tipoPelletNome() != null ? cd.getValue().tipoPelletNome() : "N/A"
        ));
        configurarColunaTexto(colTipoPellet);

        colAtiva.setCellValueFactory(cd -> new javafx.beans.property.SimpleBooleanProperty(
                cd.getValue().ativa() != null ? cd.getValue().ativa() : false
        ));
        colAtiva.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean ativa, boolean empty) {
                super.updateItem(ativa, empty);
                setGraphic(empty || ativa == null ? null : criarBadgeStatus(ativa));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().addAll("button-icon", "flat");
                btn.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btn.setTooltip(new Tooltip("Ver detalhes"));
                btn.setOnAction(ev -> handleAbrirDetalhes(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });

        tblFormulas.setFixedCellSize(48);
        tblFormulas.setItems(formulas);
    }

    private <T> void configurarColunaTexto(TableColumn<FormulaSimpleDTO, T> coluna) {
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

    // ── Data ──────────────────────────────────────────────────────────────────

    private void carregarFormulas() {
        try {
            String nome = (txtSearch != null && !txtSearch.getText().isEmpty()) ? txtSearch.getText() : null;
            Boolean ativa = (cmbFiltroStatus != null && cmbFiltroStatus.getValue() != null)
                    ? "Ativa".equals(cmbFiltroStatus.getValue()) : null;
            Page<FormulaSimpleDTO> page = formulaService.listarFormulasComFiltros(
                    paginaAtual + 1, itemsPerPage, nome, ativa, null, "nome", "ASC"
            );
            formulas.setAll(page.getContent());
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            mostrarErro(i18nService.translate("common.loadError") + ": " + e.getMessage());
        }
    }

    // ── Open drawers ──────────────────────────────────────────────────────────

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        navigationService.showModal(drawerRoot);
    }

    private void handleAbrirDetalhes(FormulaSimpleDTO formula) {
        try {
            FormulaProducaoResponseDTO d = formulaService.buscarPorId(formula.id());
            navigationService.showModal(criarDrawerEdicao(d));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("common.detailsLoadError") + ": " + e.getMessage());
        }
    }

    // ── Edit drawer ───────────────────────────────────────────────────────────

    private VBox criarDrawerEdicao(FormulaProducaoResponseDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("formulas.editTitle"), navigationService::hideModal);

        // Edit controls
        ComboBox<TipoPelletItem> cmbTipoPelletEdit = new ComboBox<>();
        cmbTipoPelletEdit.setMaxWidth(Double.MAX_VALUE);
        cmbTipoPelletEdit.setDisable(true);
        carregarTiposPellet(cmbTipoPelletEdit);
        Label lblErroTipoPelletEdit = formValidationService.createErrorLabel();
        formValidationService.attachComboAutoClear(cmbTipoPelletEdit, lblErroTipoPelletEdit);
        if (d.tipoPelletId() != null) {
            cmbTipoPelletEdit.getItems().stream()
                    .filter(item -> item.id().equals(d.tipoPelletId()))
                    .findFirst()
                    .ifPresent(cmbTipoPelletEdit::setValue);
        }

        TextField txtVersaoEdit = new TextField(d.nome());
        txtVersaoEdit.setDisable(true);
        Label lblErroNomeEdit = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtVersaoEdit, lblErroNomeEdit);

        Label lblTotalKgEdit = new Label("0.00 kg");
        lblTotalKgEdit.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#10b981;");
        Label lblWarningEdit = new Label(i18nService.translate("formulas.totalWarning"));
        lblWarningEdit.setStyle("-fx-text-fill:#f59e0b;-fx-font-weight:500;");
        lblWarningEdit.setVisible(true);

        VBox ingredientesContainerEdit = new VBox(12);
        List<IngredienteRow> ingredienteRowsEdit = new ArrayList<>();

        if (d.composicoes() != null && !d.composicoes().isEmpty()) {
            d.composicoes().forEach(c -> adicionarIngrediente(
                    ingredientesContainerEdit, ingredienteRowsEdit, c.materiaPrimaId(), c.quantidadePorKg(),
                    lblTotalKgEdit, lblWarningEdit, true));
            atualizarTotal(ingredienteRowsEdit, lblTotalKgEdit, lblWarningEdit);
        } else {
            adicionarIngrediente(ingredientesContainerEdit, ingredienteRowsEdit, null, null, lblTotalKgEdit, lblWarningEdit, true);
        }

        Button btnAddIngredient = UiFactory.drawerSecondaryAction(i18nService.translate("formulas.addIngredient"), "mdi2p-plus-circle-outline");
        btnAddIngredient.setVisible(false); btnAddIngredient.setManaged(false);
        btnAddIngredient.setOnAction(e -> adicionarIngrediente(
                ingredientesContainerEdit, ingredienteRowsEdit, null, null, lblTotalKgEdit, lblWarningEdit, false));

        CheckBox chkAtivaEdit = new CheckBox();
        chkAtivaEdit.setSelected(d.ativa() != null ? d.ativa() : true);
        chkAtivaEdit.setDisable(true);

        ScrollPane scrollPane = UiFactory.transparentScroll(new VBox(20,
                criarCampoFormularioComErro(i18nService.translate("formulas.pelletType"), cmbTipoPelletEdit, lblErroTipoPelletEdit),
                criarCampoFormularioComErro(i18nService.translate("formulas.formulaName"), txtVersaoEdit, lblErroNomeEdit),
                criarSecaoIngredientes(ingredientesContainerEdit, btnAddIngredient),
                criarCampoTotal(lblTotalKgEdit, lblWarningEdit),
                criarCampoStatus(chkAtivaEdit)
        ) {{ setPadding(new Insets(30)); }});
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button btnEliminar = UiFactory.drawerDangerAction(i18nService.translate("common.delete"), "mdi2d-delete-outline");
        Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        Button btnEditar   = UiFactory.drawerSecondaryAction(i18nService.translate("common.edit"), "mdi2p-pencil-outline");
        Button btnGuardar  = UiFactory.drawerPrimaryAction(i18nService.translate("formulas.save"), "mdi2c-content-save-outline");

        btnCancelar.setVisible(false); btnCancelar.setManaged(false);
        btnGuardar.setDisable(true);
        HBox footer = UiFactory.drawerActionFooter(btnEliminar, btnCancelar, btnEditar, btnGuardar);

        btnEditar.setOnAction(e -> {
            cmbTipoPelletEdit.setDisable(false);
            txtVersaoEdit.setDisable(false);
            chkAtivaEdit.setDisable(false);
            btnAddIngredient.setVisible(true); btnAddIngredient.setManaged(true);
            setIngredienteRowsDisabled(ingredienteRowsEdit, false);
            btnGuardar.setDisable(false);
            btnEditar.setVisible(false); btnEditar.setManaged(false);
            btnCancelar.setVisible(true); btnCancelar.setManaged(true);
        });

        btnCancelar.setOnAction(e -> navigationService.hideModal());

        btnGuardar.setOnAction(e -> {
            boolean valido = formValidationService.validateRequiredCombo(cmbTipoPelletEdit, lblErroTipoPelletEdit, i18nService.translate("formulas.pelletTypeRequired"));
            valido = formValidationService.validateRequiredText(txtVersaoEdit, lblErroNomeEdit, i18nService.translate("formulas.nameRequired")) && valido;
            if (!valido) return;
            handleAtualizarFormula(d.id(), cmbTipoPelletEdit, txtVersaoEdit, ingredienteRowsEdit, chkAtivaEdit);
        });

        btnEliminar.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(i18nService.translate("formulas.deleteTitle"));
            confirm.setHeaderText(i18nService.translate("common.confirmDelete"));
            confirm.setContentText(i18nService.translate("formulas.deleteWarning"));
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            try {
                formulaService.apagarFormula(d.id());
                carregarFormulas();
                navigationService.hideModal();
                mostrarSucesso(i18nService.translate("formulas.deleted"));
            } catch (Exception ex) {
                mostrarErro(i18nService.translate("formulas.deleteError") + ": " + ex.getMessage());
            }
        });

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    // ── Create drawer ─────────────────────────────────────────────────────────

    private void configurarDrawerAdicionar() {
        drawerRoot = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("formulas.newTitle"), navigationService::hideModal);

        cmbTipoPellet = new ComboBox<>();
        cmbTipoPellet.setMaxWidth(Double.MAX_VALUE);
        cmbTipoPellet.setPromptText(i18nService.translate("formulas.selectPelletType"));
        carregarTiposPellet(cmbTipoPellet);
        lblErroCriarTipoPellet = formValidationService.createErrorLabel();
        formValidationService.attachComboAutoClear(cmbTipoPellet, lblErroCriarTipoPellet);

        txtVersao = new TextField();
        lblErroCriarNome = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtVersao, lblErroCriarNome);

        lblTotalKg = new Label("0.00 kg");
        lblTotalKg.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#10b981;");

        lblWarning = new Label(i18nService.translate("formulas.totalWarning"));
        lblWarning.setStyle("-fx-text-fill:#f59e0b;-fx-font-weight:500;");
        lblWarning.setVisible(true);

        ingredientesContainer = new VBox(12);
        ingredienteRows = new ArrayList<>();
        adicionarIngrediente(ingredientesContainer, ingredienteRows, null, null, lblTotalKg, lblWarning, false);

        Button btnAddIngredient = UiFactory.drawerSecondaryAction(i18nService.translate("formulas.addIngredient"), "mdi2p-plus-circle-outline");
        btnAddIngredient.setOnAction(e -> adicionarIngrediente(
                ingredientesContainer, ingredienteRows, null, null, lblTotalKg, lblWarning, false));

        chkAtiva = new CheckBox();
        chkAtiva.setSelected(true);

        ScrollPane scrollPane = UiFactory.transparentScroll(new VBox(20,
                criarCampoFormularioComErro(i18nService.translate("formulas.pelletType"), cmbTipoPellet, lblErroCriarTipoPellet),
                criarCampoFormularioComErro(i18nService.translate("formulas.formulaName"), txtVersao, lblErroCriarNome),
                criarSecaoIngredientes(ingredientesContainer, btnAddIngredient),
                criarCampoTotal(lblTotalKg, lblWarning),
                criarCampoStatus(chkAtiva)
        ) {{ setPadding(new Insets(30)); }});
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button btnCriar = UiFactory.drawerPrimaryAction(i18nService.translate("formulas.create"), "mdi2c-content-save-outline");
        btnCriar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCriar, Priority.ALWAYS);
        btnCriar.setOnAction(e -> handleAdicionar());

        HBox footer = UiFactory.drawerFooter();
        footer.getChildren().add(btnCriar);
        drawerRoot.getChildren().addAll(header, scrollPane, footer);
    }

    // ── Ingredient helpers ────────────────────────────────────────────────────

    private void setIngredienteRowsDisabled(List<IngredienteRow> rows, boolean disabled) {
        for (IngredienteRow row : rows) {
            row.cmbMaterial().setDisable(disabled);
            row.txtQuantidade().setDisable(disabled);
            row.btnRemover().setDisable(disabled);
        }
    }

    private VBox criarSecaoIngredientes(VBox container, Button btnAdd) {
        VBox secao = new VBox(12);
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(i18nService.translate("formulas.ingredients"));
        label.getStyleClass().add("text-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(label, spacer, btnAdd);
        secao.getChildren().addAll(headerRow, container);
        return secao;
    }

    private VBox criarCampoTotal(Label lblTotal, Label lblWarn) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color:-color-bg-subtle;-fx-padding:20;-fx-border-radius:8;-fx-background-radius:8;");
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(i18nService.translate("formulas.totalPerKg") + ":");
        label.setStyle("-fx-font-size:14;-fx-text-fill:-color-fg-muted;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalRow.getChildren().addAll(label, spacer, lblTotal);
        box.getChildren().addAll(totalRow, lblWarn);
        return box;
    }

    private HBox criarCampoStatus(CheckBox checkbox) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(i18nService.translate("formulas.activeFormula"));
        label.getStyleClass().add("text-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button activeBtn = new Button(checkbox.isSelected()
                ? i18nService.translate("formulas.active")
                : i18nService.translate("formulas.inactive"));
        activeBtn.setStyle(checkbox.isSelected()
                ? "-fx-background-color:#10b981;-fx-text-fill:white;-fx-padding:8 20;"
                : "-fx-background-color:#6b7280;-fx-text-fill:white;-fx-padding:8 20;");
        checkbox.selectedProperty().addListener((obs, old, val) -> {
            activeBtn.setText(val ? i18nService.translate("formulas.active") : i18nService.translate("formulas.inactive"));
            activeBtn.setStyle(val
                    ? "-fx-background-color:#10b981;-fx-text-fill:white;-fx-padding:8 20;"
                    : "-fx-background-color:#6b7280;-fx-text-fill:white;-fx-padding:8 20;");
        });
        activeBtn.setOnAction(e -> checkbox.setSelected(!checkbox.isSelected()));
        box.getChildren().addAll(label, spacer, activeBtn);
        return box;
    }

    private void adicionarIngrediente(VBox container, List<IngredienteRow> rows,
                                      UUID materialId, Double quantidade,
                                      Label lblTotal, Label lblWarn, boolean disabled) {
        VBox ingredienteBox = new VBox(12);
        ingredienteBox.setStyle("-fx-background-color:-color-bg-subtle;-fx-padding:20;-fx-border-radius:8;-fx-background-radius:8;");

        Label lblMaterial = new Label(i18nService.translate("formulas.rawMaterial"));
        lblMaterial.getStyleClass().add("text-muted");
        ComboBox<MateriaPrimaItem> cmbMaterial = new ComboBox<>();
        cmbMaterial.setMaxWidth(Double.MAX_VALUE);
        cmbMaterial.setPromptText(i18nService.translate("formulas.selectRawMaterial"));
        cmbMaterial.setDisable(disabled);
        carregarMateriasPrimas(cmbMaterial);
        if (materialId != null) {
            cmbMaterial.getItems().stream().filter(i -> i.id().equals(materialId)).findFirst().ifPresent(cmbMaterial::setValue);
        }

        Label lblQuantidade = new Label(i18nService.translate("formulas.quantityPerKg"));
        lblQuantidade.getStyleClass().add("text-muted");
        TextField txtQuantidade = new TextField(quantidade != null ? String.valueOf(quantidade) : "0");
        txtQuantidade.setDisable(disabled);

        Button btnRemove = UiFactory.drawerDangerAction(i18nService.translate("formulas.removeIngredient"), "mdi2d-delete-outline");
        btnRemove.setStyle("-fx-text-fill:#ef4444;-fx-background-color:transparent;-fx-border-color:transparent;");
        btnRemove.setDisable(disabled);
        btnRemove.setOnAction(e -> {
            container.getChildren().remove(ingredienteBox);
            rows.removeIf(r -> r.container() == ingredienteBox);
            atualizarTotal(rows, lblTotal, lblWarn);
        });

        ingredienteBox.getChildren().addAll(lblMaterial, cmbMaterial, lblQuantidade, txtQuantidade, btnRemove);
        container.getChildren().add(ingredienteBox);

        IngredienteRow row = new IngredienteRow(ingredienteBox, cmbMaterial, txtQuantidade, btnRemove);
        rows.add(row);
        txtQuantidade.textProperty().addListener((obs, old, val) -> atualizarTotal(rows, lblTotal, lblWarn));
    }

    private void atualizarTotal(List<IngredienteRow> rows, Label lblTotal, Label lblWarn) {
        double total = 0.0;
        for (IngredienteRow row : rows) {
            try {
                double q = Double.parseDouble(row.txtQuantidade().getText().replace(",", "."));
                if (Double.isFinite(q)) total += q;
            } catch (NumberFormatException ignored) {}
        }
        lblTotal.setText(String.format("%.2f kg", total));
        if (Math.abs(total - 1.0) < 0.01) {
            lblTotal.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#10b981;");
            lblWarn.setVisible(false);
        } else {
            lblTotal.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#f59e0b;");
            lblWarn.setVisible(true);
        }
    }

    private void carregarTiposPellet(ComboBox<TipoPelletItem> combo) {
        try {
            var page = stockService.listarTiposPelletComFiltros(1, 100, null, null, "nome", "ASC");
            combo.setItems(FXCollections.observableArrayList(
                    page.getContent().stream().map(dto -> new TipoPelletItem(dto.id(), dto.nome())).toList()));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("formulas.loadPelletTypesError") + ": " + e.getMessage());
        }
    }

    private void carregarMateriasPrimas(ComboBox<MateriaPrimaItem> combo) {
        try {
            var page = stockService.listarMateriasPrimasComFiltros(1, 100, null, "kg", null, "nome", "ASC");
            combo.setItems(FXCollections.observableArrayList(
                    page.getContent().stream().map(dto -> new MateriaPrimaItem(dto.id(), dto.nome())).toList()));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("formulas.loadRawMaterialsError") + ": " + e.getMessage());
        }
    }

    // ── Form builders ─────────────────────────────────────────────────────────

    private VBox criarCampoFormularioComErro(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    // ── Save handlers ─────────────────────────────────────────────────────────

    private void handleAdicionar() {
        boolean valido = formValidationService.validateRequiredCombo(cmbTipoPellet, lblErroCriarTipoPellet, i18nService.translate("formulas.pelletTypeRequired"));
        valido = formValidationService.validateRequiredText(txtVersao, lblErroCriarNome, i18nService.translate("formulas.nameRequired")) && valido;
        if (!valido) return;
        if (ingredienteRows.isEmpty()) { mostrarErro(i18nService.translate("formulas.ingredientsRequired")); return; }
        try {
            formulaService.criarFormula(criarFormulaDTO(cmbTipoPellet, txtVersao, chkAtiva), lerIngredientes(ingredienteRows));
            paginaAtual = 0;
            carregarFormulas();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("formulas.created"));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("common.error") + ": " + e.getMessage());
        }
    }

    private void handleAtualizarFormula(UUID id, ComboBox<TipoPelletItem> cmbTipo, TextField txtVer,
                                        List<IngredienteRow> rows, CheckBox chkAtv) {
        try {
            formulaService.atualizarFormula(id, criarFormulaDTO(cmbTipo, txtVer, chkAtv), lerIngredientes(rows));
            carregarFormulas();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("formulas.updated"));
        } catch (Exception ex) {
            mostrarErro(i18nService.translate("formulas.updateError") + ": " + ex.getMessage());
        }
    }

    private FormulaProducaoRequestDTO criarFormulaDTO(ComboBox<TipoPelletItem> cmbTipo, TextField txtNome, CheckBox chkAtiva) {
        if (cmbTipo.getValue() == null) throw new IllegalArgumentException(i18nService.translate("formulas.selectPelletType"));
        return new FormulaProducaoRequestDTO(cmbTipo.getValue().id(), txtNome.getText(), chkAtiva.isSelected());
    }

    private Map<UUID, Double> lerIngredientes(List<IngredienteRow> rows) {
        if (rows.isEmpty()) throw new IllegalArgumentException(i18nService.translate("formulas.ingredientsRequired"));
        Map<UUID, Double> ingredientes = new LinkedHashMap<>();
        for (IngredienteRow row : rows) {
            if (row.cmbMaterial().getValue() == null)
                throw new IllegalArgumentException(i18nService.translate("formulas.allRawMaterialsRequired"));
            double quantidade;
            try {
                quantidade = Double.parseDouble(row.txtQuantidade().getText().replace(",", "."));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(i18nService.translate("formulas.allQuantitiesValid"));
            }
            if (!Double.isFinite(quantidade) || quantidade <= 0)
                throw new IllegalArgumentException(i18nService.translate("formulas.quantitiesPositive"));
            if (ingredientes.putIfAbsent(row.cmbMaterial().getValue().id(), quantidade) != null)
                throw new IllegalArgumentException(i18nService.translate("formulas.duplicateRawMaterial"));
        }
        double total = ingredientes.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(total) || Math.abs(total - 1.0) > 0.000001)
            throw new IllegalArgumentException(i18nService.translate("formulas.totalMustBeOneKg"));
        return ingredientes;
    }

    private void limparFormulario() {
        cmbTipoPellet.setValue(null);
        txtVersao.clear();
        ingredientesContainer.getChildren().clear();
        ingredienteRows.clear();
        adicionarIngrediente(ingredientesContainer, ingredienteRows, null, null, lblTotalKg, lblWarning, false);
        chkAtiva.setSelected(true);
        atualizarTotal(ingredienteRows, lblTotalKg, lblWarning);
        formValidationService.clearError(cmbTipoPellet, lblErroCriarTipoPellet);
        formValidationService.clearError(txtVersao, lblErroCriarNome);
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    private void configurarPaginacao(VBox container) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(20, 0, 20, 0));
        nav.setStyle("-fx-border-color:-color-border-muted;-fx-border-width:1 0 0 0;");

        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");
        HBox left = new HBox(lblPaginaStatus);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarFormulas(); });
        HBox center = new HBox(10, new Label(i18nService.translate("common.perPage")), cmbItemsPerPage);
        center.setAlignment(Pos.CENTER);
        HBox.setHgrow(center, Priority.ALWAYS);

        paginationButtons = new HBox(5);
        HBox right = new HBox(paginationButtons);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(right, Priority.ALWAYS);

        nav.getChildren().addAll(left, center, right);
        container.getChildren().add(nav);
    }

    private void atualizarBotoesPaginacao() {
        paginationButtons.getChildren().clear();
        Button prev = new Button();
        prev.setGraphic(new FontIcon("mdi2c-chevron-left"));
        prev.setDisable(paginaAtual == 0);
        prev.setOnAction(e -> { paginaAtual--; carregarFormulas(); });
        paginationButtons.getChildren().add(prev);
        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int fi = i;
                p.setOnAction(e -> { paginaAtual = fi; carregarFormulas(); });
                paginationButtons.getChildren().add(p);
            }
        }
        Button next = new Button();
        next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarFormulas(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<FormulaSimpleDTO> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText(i18nService.translate("common.showing") + " "
                + start + " " + i18nService.translate("common.to") + " "
                + end + " " + i18nService.translate("common.of") + " "
                + page.getTotalElements());
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    private HBox criarBadgeStatus(Boolean ativa) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        String color = ativa ? "#10b981" : "#6b7280";
        b.setStyle(String.format("-fx-background-radius:6;-fx-border-radius:6;-fx-border-width:1.5;-fx-background-color:%s20;-fx-border-color:%s;",
                color.replace("#", ""), color));
        Label l = new Label(ativa ? i18nService.translate("formulas.active") : i18nService.translate("formulas.inactive"));
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:500;");
        b.getChildren().add(l);
        return b;
    }

    private void configurarComboBoxes() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(i18nService.translate("formulas.active"), i18nService.translate("formulas.inactive")));
    }

    private void mostrarSucesso(String m) { toastService.showSuccess(i18nService.translate("common.success"), m); }
    private void mostrarErro(String m)    { toastService.showError(i18nService.translate("common.error"), m); }

    private record IngredienteRow(VBox container, ComboBox<MateriaPrimaItem> cmbMaterial,
                                   TextField txtQuantidade, Button btnRemover) {}
    private record TipoPelletItem(UUID id, String nome) { @Override public String toString() { return nome; } }
    private record MateriaPrimaItem(UUID id, String nome) { @Override public String toString() { return nome; } }
}
