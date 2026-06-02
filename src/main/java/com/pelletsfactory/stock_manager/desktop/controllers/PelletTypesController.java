package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.TipoPelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.FormulaProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.FormValidationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import java.math.BigDecimal;
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
    private final FormValidationService formValidationService;

    // Create drawer fields
    private VBox drawerCriar;
    private TextField txtNomeCriar, txtDiametroCriar, txtCalorificoCriar, txtStockAtualCriar, txtStockMinimoCriar, txtCustoCriar;
    private Label lblErroNomeCriar, lblErroDiametroCriar, lblErroCalorificoCriar, lblErroStockAtualCriar, lblErroStockMinimoCriar, lblErroCustoCriar;

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
                                 I18nService i18nService,
                                 FormValidationService formValidationService) {
        this.stockService = stockService;
        this.formulaService = formulaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
        this.formValidationService = formValidationService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarPelletTypes, i18nService);
        configurarTabela();
        configurarDrawerCriar();
        carregarPelletTypes();
    }

    @FXML
    private void handleAbrirModal() {
        limparFormularioCriar();
        navigationService.showModal(drawerCriar);
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
            VBox drawer = criarDrawerEdicao(d, row.formulaDefinida());
            navigationService.showModal(drawer);
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerEdicao(TipoPelletDetailsDTO d, boolean formulaDefinida) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("pelletTypes.editTitle"), navigationService::hideModal);

        TextField txtNome = new TextField(d.nome() != null ? d.nome() : "");
        Label lblErroNome = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNome, lblErroNome);

        TextField txtDiametro = new TextField(d.diametroMm() != null ? String.valueOf(d.diametroMm()) : "");
        Label lblErroDiametro = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtDiametro, lblErroDiametro);

        TextField txtCalorifico = new TextField(d.poderCalorifico() != null ? String.valueOf(d.poderCalorifico()) : "");
        Label lblErroCalorifico = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtCalorifico, lblErroCalorifico);

        TextField txtStockAtual = new TextField(d.stockAtual() != null ? String.format("%.2f", d.stockAtual()) : "");
        Label lblErroStockAtual = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtStockAtual, lblErroStockAtual);

        TextField txtStockMinimo = new TextField(d.stockMinimo() != null ? String.format("%.2f", d.stockMinimo()) : "");
        Label lblErroStockMinimo = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtStockMinimo, lblErroStockMinimo);

        TextField txtCusto = new TextField(d.custoAtualPorKg() != null ? d.custoAtualPorKg().toPlainString() : "");
        Label lblErroCusto = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtCusto, lblErroCusto);

        Label lblErroGeral = criarErroGeral();

        String numRegex = "[0-9]+(\\.[0-9]+)?";

        VBox form = new VBox(20,
                criarCampo(i18nService.translate("common.name") + " *", txtNome, lblErroNome),
                criarCampo(i18nService.translate("pelletTypes.diameter") + " *", txtDiametro, lblErroDiametro),
                criarCampo(i18nService.translate("pelletTypes.calorificValue") + " *", txtCalorifico, lblErroCalorifico),
                criarCampo(i18nService.translate("stock.current") + " *", txtStockAtual, lblErroStockAtual),
                criarCampo(i18nService.translate("stock.minimumShort") + " *", txtStockMinimo, lblErroStockMinimo),
                criarCampo(i18nService.translate("pelletTypes.costKg") + " *", txtCusto, lblErroCusto),
                lblErroGeral
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox footer = UiFactory.drawerFooter();

        Button btnEliminar = new Button(i18nService.translate("common.delete"));
        btnEliminar.getStyleClass().addAll("button-outlined", "danger");
        btnEliminar.setPrefHeight(44);

        Button btnGuardar = new Button(i18nService.translate("common.save"));
        btnGuardar.getStyleClass().add("accent");
        btnGuardar.setPrefHeight(44);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnGuardar, Priority.ALWAYS);

        footer.getChildren().addAll(btnEliminar, btnGuardar);

        btnGuardar.setOnAction(e -> {
            lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
            boolean valido = formValidationService.validateRequiredText(txtNome, lblErroNome, i18nService.translate("common.nameRequired"));
            valido = formValidationService.validateRequiredText(txtDiametro, lblErroDiametro, i18nService.translate("pelletTypes.diameterRequired")) && valido;
            if (!txtDiametro.getText().isBlank()) {
                valido = formValidationService.validateRegex(txtDiametro, lblErroDiametro, numRegex, i18nService.translate("common.invalidNumber")) && valido;
                valido = validarPositivo(txtDiametro, lblErroDiametro, i18nService.translate("pelletTypes.mustBePositive")) && valido;
            }
            valido = formValidationService.validateRequiredText(txtCalorifico, lblErroCalorifico, i18nService.translate("pelletTypes.calorificRequired")) && valido;
            if (!txtCalorifico.getText().isBlank()) {
                valido = formValidationService.validateRegex(txtCalorifico, lblErroCalorifico, numRegex, i18nService.translate("common.invalidNumber")) && valido;
                valido = validarPositivo(txtCalorifico, lblErroCalorifico, i18nService.translate("pelletTypes.mustBePositive")) && valido;
            }
            valido = formValidationService.validateRequiredText(txtStockAtual, lblErroStockAtual, i18nService.translate("pelletTypes.stockRequired")) && valido;
            if (!txtStockAtual.getText().isBlank()) {
                valido = formValidationService.validateRegex(txtStockAtual, lblErroStockAtual, numRegex, i18nService.translate("common.invalidNumber")) && valido;
                valido = validarNaoNegativo(txtStockAtual, lblErroStockAtual, i18nService.translate("pelletTypes.mustBeNonNegative")) && valido;
            }
            valido = formValidationService.validateRequiredText(txtStockMinimo, lblErroStockMinimo, i18nService.translate("pelletTypes.stockMinRequired")) && valido;
            if (!txtStockMinimo.getText().isBlank()) {
                valido = formValidationService.validateRegex(txtStockMinimo, lblErroStockMinimo, numRegex, i18nService.translate("common.invalidNumber")) && valido;
                valido = validarNaoNegativo(txtStockMinimo, lblErroStockMinimo, i18nService.translate("pelletTypes.mustBeNonNegative")) && valido;
            }
            valido = formValidationService.validateRequiredText(txtCusto, lblErroCusto, i18nService.translate("pelletTypes.costRequired")) && valido;
            if (!txtCusto.getText().isBlank()) {
                valido = formValidationService.validateRegex(txtCusto, lblErroCusto, numRegex, i18nService.translate("common.invalidNumber")) && valido;
                valido = validarPositivo(txtCusto, lblErroCusto, i18nService.translate("pelletTypes.mustBePositive")) && valido;
            }
            if (valido) {
                Double sa = parseDoubleOuNull(txtStockAtual.getText());
                Double sm = parseDoubleOuNull(txtStockMinimo.getText());
                if (sa != null && sm != null && sm > sa) {
                    lblErroStockMinimo.setText(i18nService.translate("pelletTypes.stockMinExceedsActual"));
                    lblErroStockMinimo.setVisible(true); lblErroStockMinimo.setManaged(true);
                    txtStockMinimo.setStyle("-fx-border-color: #ef4444;");
                    valido = false;
                }
            }
            if (!valido) return;
            try {
                TipoPelletRequestDTO dto = new TipoPelletRequestDTO(
                        txtNome.getText().trim(),
                        parseDoubleOuNull(txtDiametro.getText()),
                        parseDoubleOuNull(txtCalorifico.getText()),
                        parseDoubleOuNull(txtStockAtual.getText()),
                        parseDoubleOuNull(txtStockMinimo.getText()),
                        parseBigDecimalOuNull(txtCusto.getText()),
                        null
                );
                stockService.atualizarTipoPellet(d.id(), dto);
                carregarPelletTypes();
                navigationService.hideModal();
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("pelletTypes.updated"));
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });

        btnEliminar.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(i18nService.translate("common.delete"));
            confirm.setHeaderText(i18nService.translate("common.confirmDelete"));
            confirm.setContentText("\"" + d.nome() + "\"");
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
            try {
                stockService.apagarTipoPellet(d.id());
                carregarPelletTypes();
                navigationService.hideModal();
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("pelletTypes.deleted"));
            } catch (Exception ex) {
                toastService.showError(i18nService.translate("common.error"), ex.getMessage());
            }
        });

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private Label criarErroGeral() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-color: #ef444420; -fx-background-radius: 6; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void configurarDrawerCriar() {
        drawerCriar = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("pelletTypes.newTitle"), navigationService::hideModal);

        txtNomeCriar = new TextField(); txtNomeCriar.setPromptText("ex: Pellet Industrial 6mm");
        lblErroNomeCriar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNomeCriar, lblErroNomeCriar);

        txtDiametroCriar = new TextField(); txtDiametroCriar.setPromptText("ex: 6.0");
        lblErroDiametroCriar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtDiametroCriar, lblErroDiametroCriar);

        txtCalorificoCriar = new TextField(); txtCalorificoCriar.setPromptText("ex: 4800");
        lblErroCalorificoCriar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtCalorificoCriar, lblErroCalorificoCriar);

        txtStockAtualCriar = new TextField(); txtStockAtualCriar.setPromptText("0");
        lblErroStockAtualCriar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtStockAtualCriar, lblErroStockAtualCriar);

        txtStockMinimoCriar = new TextField(); txtStockMinimoCriar.setPromptText("0");
        lblErroStockMinimoCriar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtStockMinimoCriar, lblErroStockMinimoCriar);

        txtCustoCriar = new TextField(); txtCustoCriar.setPromptText("ex: 0.25");
        lblErroCustoCriar = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtCustoCriar, lblErroCustoCriar);

        Label lblErroGeral = criarErroGeral();

        VBox form = new VBox(20,
                criarCampo(i18nService.translate("common.name") + " *", txtNomeCriar, lblErroNomeCriar),
                criarCampo(i18nService.translate("pelletTypes.diameter") + " *", txtDiametroCriar, lblErroDiametroCriar),
                criarCampo(i18nService.translate("pelletTypes.calorificValue") + " *", txtCalorificoCriar, lblErroCalorificoCriar),
                criarCampo(i18nService.translate("stock.current") + " *", txtStockAtualCriar, lblErroStockAtualCriar),
                criarCampo(i18nService.translate("stock.minimumShort") + " *", txtStockMinimoCriar, lblErroStockMinimoCriar),
                criarCampo(i18nService.translate("pelletTypes.costKg") + " *", txtCustoCriar, lblErroCustoCriar),
                lblErroGeral
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox footer = UiFactory.drawerFooter();
        Button btnCriar = new Button(i18nService.translate("pelletTypes.save"));
        btnCriar.getStyleClass().add("accent");
        btnCriar.setPrefHeight(44);
        btnCriar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCriar, Priority.ALWAYS);
        btnCriar.setOnAction(e -> handleCriarTipoPellet(lblErroGeral));
        footer.getChildren().add(btnCriar);

        drawerCriar.getChildren().addAll(header, scrollPane, footer);
    }

    private void handleCriarTipoPellet(Label lblErroGeral) {
        lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
        String numRegex = "[0-9]+(\\.[0-9]+)?";
        boolean valido = formValidationService.validateRequiredText(txtNomeCriar, lblErroNomeCriar, i18nService.translate("common.nameRequired"));
        // Diâmetro: obrigatório, número, > 0
        valido = formValidationService.validateRequiredText(txtDiametroCriar, lblErroDiametroCriar, i18nService.translate("pelletTypes.diameterRequired")) && valido;
        if (!txtDiametroCriar.getText().isBlank()) {
            valido = formValidationService.validateRegex(txtDiametroCriar, lblErroDiametroCriar, numRegex, i18nService.translate("common.invalidNumber")) && valido;
            valido = validarPositivo(txtDiametroCriar, lblErroDiametroCriar, i18nService.translate("pelletTypes.mustBePositive")) && valido;
        }
        // Poder calorífico: obrigatório, número, > 0
        valido = formValidationService.validateRequiredText(txtCalorificoCriar, lblErroCalorificoCriar, i18nService.translate("pelletTypes.calorificRequired")) && valido;
        if (!txtCalorificoCriar.getText().isBlank()) {
            valido = formValidationService.validateRegex(txtCalorificoCriar, lblErroCalorificoCriar, numRegex, i18nService.translate("common.invalidNumber")) && valido;
            valido = validarPositivo(txtCalorificoCriar, lblErroCalorificoCriar, i18nService.translate("pelletTypes.mustBePositive")) && valido;
        }
        // Stock atual: obrigatório, número, >= 0
        valido = formValidationService.validateRequiredText(txtStockAtualCriar, lblErroStockAtualCriar, i18nService.translate("pelletTypes.stockRequired")) && valido;
        if (!txtStockAtualCriar.getText().isBlank()) {
            valido = formValidationService.validateRegex(txtStockAtualCriar, lblErroStockAtualCriar, numRegex, i18nService.translate("common.invalidNumber")) && valido;
            valido = validarNaoNegativo(txtStockAtualCriar, lblErroStockAtualCriar, i18nService.translate("pelletTypes.mustBeNonNegative")) && valido;
        }
        // Stock mínimo: obrigatório, número, >= 0
        valido = formValidationService.validateRequiredText(txtStockMinimoCriar, lblErroStockMinimoCriar, i18nService.translate("pelletTypes.stockMinRequired")) && valido;
        if (!txtStockMinimoCriar.getText().isBlank()) {
            valido = formValidationService.validateRegex(txtStockMinimoCriar, lblErroStockMinimoCriar, numRegex, i18nService.translate("common.invalidNumber")) && valido;
            valido = validarNaoNegativo(txtStockMinimoCriar, lblErroStockMinimoCriar, i18nService.translate("pelletTypes.mustBeNonNegative")) && valido;
        }
        // Custo: obrigatório, número, > 0
        valido = formValidationService.validateRequiredText(txtCustoCriar, lblErroCustoCriar, i18nService.translate("pelletTypes.costRequired")) && valido;
        if (!txtCustoCriar.getText().isBlank()) {
            valido = formValidationService.validateRegex(txtCustoCriar, lblErroCustoCriar, numRegex, i18nService.translate("common.invalidNumber")) && valido;
            valido = validarPositivo(txtCustoCriar, lblErroCustoCriar, i18nService.translate("pelletTypes.mustBePositive")) && valido;
        }
        // Stock mínimo <= Stock atual
        if (valido) {
            Double sa = parseDoubleOuNull(txtStockAtualCriar.getText());
            Double sm = parseDoubleOuNull(txtStockMinimoCriar.getText());
            if (sa != null && sm != null && sm > sa) {
                lblErroStockMinimoCriar.setText(i18nService.translate("pelletTypes.stockMinExceedsActual"));
                lblErroStockMinimoCriar.setVisible(true); lblErroStockMinimoCriar.setManaged(true);
                txtStockMinimoCriar.setStyle("-fx-border-color: #ef4444;");
                valido = false;
            }
        }
        if (!valido) return;
        try {
            TipoPelletRequestDTO dto = new TipoPelletRequestDTO(
                    txtNomeCriar.getText().trim(),
                    parseDoubleOuNull(txtDiametroCriar.getText()),
                    parseDoubleOuNull(txtCalorificoCriar.getText()),
                    parseDoubleOuNull(txtStockAtualCriar.getText()),
                    parseDoubleOuNull(txtStockMinimoCriar.getText()),
                    parseBigDecimalOuNull(txtCustoCriar.getText()),
                    null
            );
            stockService.criarTipoPellet(dto);
            pagination.resetPage();
            carregarPelletTypes();
            navigationService.hideModal();
            toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("pelletTypes.created"));
        } catch (Exception e) {
            lblErroGeral.setText(e.getMessage() != null ? e.getMessage() : i18nService.translate("common.saveError"));
            lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
        }
    }

    private void limparFormularioCriar() {
        txtNomeCriar.clear(); txtDiametroCriar.clear(); txtCalorificoCriar.clear();
        txtStockAtualCriar.clear(); txtStockMinimoCriar.clear(); txtCustoCriar.clear();
        formValidationService.clearError(txtNomeCriar, lblErroNomeCriar);
        formValidationService.clearError(txtDiametroCriar, lblErroDiametroCriar);
        formValidationService.clearError(txtCalorificoCriar, lblErroCalorificoCriar);
        formValidationService.clearError(txtStockAtualCriar, lblErroStockAtualCriar);
        formValidationService.clearError(txtStockMinimoCriar, lblErroStockMinimoCriar);
        formValidationService.clearError(txtCustoCriar, lblErroCustoCriar);
    }

    private VBox criarCampo(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private VBox criarCampo(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    private boolean validarPositivo(TextField campo, Label erro, String mensagem) {
        String t = campo.getText();
        if (t == null || t.isBlank()) return true; // required already checked
        try {
            double v = Double.parseDouble(t.replace(",", "."));
            if (v <= 0) {
                formValidationService.validateRequiredText(campo, erro, mensagem); // triggers red border
                erro.setText(mensagem); erro.setVisible(true); erro.setManaged(true);
                campo.setStyle("-fx-border-color: #ef4444;");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false; // format already caught by regex
        }
    }

    private boolean validarNaoNegativo(TextField campo, Label erro, String mensagem) {
        String t = campo.getText();
        if (t == null || t.isBlank()) return true;
        try {
            double v = Double.parseDouble(t.replace(",", "."));
            if (v < 0) {
                erro.setText(mensagem); erro.setVisible(true); erro.setManaged(true);
                campo.setStyle("-fx-border-color: #ef4444;");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Double parseDoubleOuNull(String text) {
        if (text == null || text.isBlank()) return null;
        try { return Double.parseDouble(text.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseBigDecimalOuNull(String text) {
        if (text == null || text.isBlank()) return null;
        try { return new BigDecimal(text.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private void mostrarErro(String m) {
        toastService.showError(i18nService.translate("common.error"), m);
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
