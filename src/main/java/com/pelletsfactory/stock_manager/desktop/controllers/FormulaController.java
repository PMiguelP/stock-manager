package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.FormulaProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.FormulaProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
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
public class FormulaController {
    private final FormulaProducaoService formulaService;
    private final StockService stockService;
    private final NavigationService navigationService;
    private final ToastService toastService;

    @FXML private TextField txtFiltroNome;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private TableView<FormulaSimpleDTO> tblFormulas;
    @FXML private VBox vboxContainer;

    @FXML private TableColumn<FormulaSimpleDTO, String> colCodigo;
    @FXML private TableColumn<FormulaSimpleDTO, String> colNome;
    @FXML private TableColumn<FormulaSimpleDTO, String> colTipoPellet;
    @FXML private TableColumn<FormulaSimpleDTO, String> colVersao;
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

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<FormulaSimpleDTO> formulas = FXCollections.observableArrayList();

    public FormulaController(FormulaProducaoService formulaService,
                                     StockService stockService,
                                     NavigationService navigationService,
                                     ToastService toastService) {
        this.formulaService = formulaService;
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarComboBoxes();
        configurarDrawerAdicionar();
        carregarFormulas();
    }

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

        colVersao.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty("v1.0"));
        configurarColunaTexto(colVersao);

        colAtiva.setCellValueFactory(cd -> new javafx.beans.property.SimpleBooleanProperty(
                cd.getValue().ativa() != null ? cd.getValue().ativa() : false
        ));

        colAtiva.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean ativa, boolean empty) {
                super.updateItem(ativa, empty);
                if (empty || ativa == null) {
                    setGraphic(null);
                } else {
                    setGraphic(criarBadgeStatus(ativa));
                }
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setTooltip(new Tooltip("Ver detalhes"));
                btnDetails.setOnAction(event -> {
                    FormulaSimpleDTO formula = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(formula);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
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

    private void carregarFormulas() {
        try {
            String nome = (txtFiltroNome != null && !txtFiltroNome.getText().isEmpty()) ? txtFiltroNome.getText() : null;
            Boolean ativa = (cmbFiltroStatus != null && cmbFiltroStatus.getValue() != null)
                    ? "Active".equals(cmbFiltroStatus.getValue()) : null;

            Page<FormulaSimpleDTO> page = formulaService.listarFormulasComFiltros(
                    paginaAtual + 1, itemsPerPage, nome, ativa, null, "nome", "ASC"
            );

            formulas.setAll(page.getContent());
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            mostrarErro("Erro ao carregar: " + e.getMessage());
        }
    }

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        navigationService.showModal(drawerRoot);
    }

    private void handleAbrirDetalhes(FormulaSimpleDTO formula) {
        try {
            FormulaProducaoResponseDTO d = formulaService.buscarPorId(formula.id());
            VBox detalhesDrawer = criarDrawerEdicao(d);
            navigationService.showModal(detalhesDrawer);
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerEdicao(FormulaProducaoResponseDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Edit Production Formula");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        // Form
        ComboBox<TipoPelletItem> cmbTipoPelletEdit = new ComboBox<>();
        cmbTipoPelletEdit.setMaxWidth(Double.MAX_VALUE);
        carregarTiposPellet(cmbTipoPelletEdit);
        // Set current value
        if (d.tipoPelletId() != null) {
            cmbTipoPelletEdit.getItems().stream()
                    .filter(item -> item.id().equals(d.tipoPelletId()))
                    .findFirst()
                    .ifPresent(cmbTipoPelletEdit::setValue);
        }

        TextField txtVersaoEdit = new TextField("v1.0");

        Label lblTotalKgEdit = new Label("0.00 kg");
        lblTotalKgEdit.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        Label lblWarningEdit = new Label("Warning: Total should equal 1.00 kg");
        lblWarningEdit.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 500;");
        lblWarningEdit.setVisible(true);

        VBox ingredientesContainerEdit = new VBox(12);
        List<IngredienteRow> ingredienteRowsEdit = new ArrayList<>();

        // Add existing ingredients from DTO
        if (d.composicoes() != null && !d.composicoes().isEmpty()) {
            d.composicoes().forEach(c -> adicionarIngrediente(
                    ingredientesContainerEdit,
                    ingredienteRowsEdit,
                    c.materiaPrimaId(),
                    c.quantidadePorKg(),
                    lblTotalKgEdit,
                    lblWarningEdit
            ));
            atualizarTotal(ingredienteRowsEdit, lblTotalKgEdit, lblWarningEdit);
        } else {
            adicionarIngrediente(ingredientesContainerEdit, ingredienteRowsEdit, null, null, lblTotalKgEdit, lblWarningEdit);
        }

        Button btnAddIngredient = new Button("+ Add Ingredient");
        btnAddIngredient.getStyleClass().add("accent");
        btnAddIngredient.setOnAction(e -> adicionarIngrediente(
                ingredientesContainerEdit,
                ingredienteRowsEdit,
                null,
                null,
                lblTotalKgEdit,
                lblWarningEdit
        ));

        CheckBox chkAtivaEdit = new CheckBox();
        chkAtivaEdit.setSelected(d.ativa() != null ? d.ativa() : true);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox form = new VBox(20,
                criarCampoFormulario("Pellet Type", cmbTipoPelletEdit),
                criarCampoFormulario("Formula Version", txtVersaoEdit),
                criarSecaoIngredientes(ingredientesContainerEdit, btnAddIngredient),
                criarCampoTotal(lblTotalKgEdit, lblWarningEdit),
                criarCampoStatus(chkAtivaEdit)
        );
        form.setPadding(new Insets(30));
        scrollPane.setContent(form);

        // Footer
        HBox footer = new HBox(12);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        Button btnGuardar = new Button("Update Formula");
        btnGuardar.getStyleClass().add("accent");
        btnGuardar.setPrefHeight(44);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);

        Button btnEliminar = new Button("Delete");
        btnEliminar.setPrefHeight(44);
        btnEliminar.setMaxWidth(Double.MAX_VALUE);
        btnEliminar.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;");

        HBox.setHgrow(btnGuardar, Priority.ALWAYS);
        HBox.setHgrow(btnEliminar, Priority.ALWAYS);
        footer.getChildren().addAll(btnGuardar, btnEliminar);

        btnGuardar.setOnAction(e -> {
            // TODO: Implement validation and update
            handleAtualizarFormula(d.id(), cmbTipoPelletEdit, txtVersaoEdit, ingredienteRowsEdit, chkAtivaEdit);
        });

        btnEliminar.setOnAction(e -> {
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacao.setTitle("Eliminar Fórmula");
            confirmacao.setHeaderText("Tem a certeza?");
            confirmacao.setContentText("Esta ação é irreversível e vai eliminar a fórmula selecionada.");

            if (confirmacao.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            try {
                formulaService.apagarFormula(d.id());
                carregarFormulas();
                navigationService.hideModal();
                mostrarSucesso("Fórmula eliminada!");
            } catch (Exception ex) {
                mostrarErro("Erro ao eliminar: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private void configurarDrawerAdicionar() {
        drawerRoot = new VBox(0);
        drawerRoot.setMinWidth(550);
        drawerRoot.setPrefWidth(550);
        drawerRoot.setMaxWidth(550);
        drawerRoot.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Create Production Formula");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnF = new Button();
        btnF.setGraphic(new FontIcon("mdi2c-close:22"));
        btnF.getStyleClass().addAll("button-icon", "flat");
        btnF.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnF);

        // Form
        cmbTipoPellet = new ComboBox<>();
        cmbTipoPellet.setMaxWidth(Double.MAX_VALUE);
        cmbTipoPellet.setPromptText("Select pellet type");
        carregarTiposPellet(cmbTipoPellet);

        txtVersao = new TextField("v1.0");

        lblTotalKg = new Label("0.00 kg");
        lblTotalKg.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        lblWarning = new Label("Warning: Total should equal 1.00 kg");
        lblWarning.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 500;");
        lblWarning.setVisible(true);

        ingredientesContainer = new VBox(12);
        ingredienteRows = new ArrayList<>();

        // Add first ingredient row
        adicionarIngrediente(ingredientesContainer, ingredienteRows, null, null, lblTotalKg, lblWarning);

        Button btnAddIngredient = new Button("+ Add Ingredient");
        btnAddIngredient.getStyleClass().add("accent");
        btnAddIngredient.setOnAction(e -> adicionarIngrediente(
                ingredientesContainer,
                ingredienteRows,
                null,
                null,
                lblTotalKg,
                lblWarning
        ));

        chkAtiva = new CheckBox();
        chkAtiva.setSelected(true);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox form = new VBox(20,
                criarCampoFormulario("Pellet Type", cmbTipoPellet),
                criarCampoFormulario("Formula Version", txtVersao),
                criarSecaoIngredientes(ingredientesContainer, btnAddIngredient),
                criarCampoTotal(lblTotalKg, lblWarning),
                criarCampoStatus(chkAtiva)
        );
        form.setPadding(new Insets(30));
        scrollPane.setContent(form);

        // Footer
        HBox footer = new HBox();
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnS = new Button("Create Formula");
        btnS.getStyleClass().add("accent");
        btnS.setPrefHeight(44);
        btnS.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnS, Priority.ALWAYS);
        btnS.setOnAction(e -> handleAdicionar());
        footer.getChildren().add(btnS);

        drawerRoot.getChildren().addAll(header, scrollPane, footer);
    }

    private VBox criarSecaoIngredientes(VBox container, Button btnAdd) {
        VBox secao = new VBox(12);

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label("Ingredients (quantities per kg of output)");
        label.getStyleClass().add("text-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(label, btnAdd);

        secao.getChildren().addAll(headerRow, container);
        return secao;
    }

    private VBox criarCampoTotal(Label lblTotal, Label lblWarn) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: -color-bg-subtle; -fx-padding: 20; -fx-border-radius: 8; -fx-background-radius: 8;");

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label("Total per kg:");
        label.setStyle("-fx-font-size: 14; -fx-text-fill: -color-fg-muted;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalRow.getChildren().addAll(label, lblTotal);

        box.getChildren().addAll(totalRow, lblWarn);
        return box;
    }

    private HBox criarCampoStatus(CheckBox checkbox) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label("Active Formula");
        label.getStyleClass().add("text-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button activeBtn = new Button("Active");
        activeBtn.getStyleClass().add("accent");
        activeBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 8 20;");

        checkbox.selectedProperty().addListener((obs, old, val) -> {
            if (val) {
                activeBtn.setText("Active");
                activeBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 8 20;");
            } else {
                activeBtn.setText("Inactive");
                activeBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 8 20;");
            }
        });

        activeBtn.setOnAction(e -> checkbox.setSelected(!checkbox.isSelected()));

        box.getChildren().addAll(label, spacer, activeBtn);
        return box;
    }

    private void adicionarIngrediente(VBox container, List<IngredienteRow> rows, UUID materialId, Double quantidade,
                                      Label lblTotal, Label lblWarn) {
        VBox ingredienteBox = new VBox(12);
        ingredienteBox.setStyle("-fx-background-color: -color-bg-subtle; -fx-padding: 20; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Raw Material
        Label lblMaterial = new Label("Raw Material");
        lblMaterial.getStyleClass().add("text-muted");
        ComboBox<MateriaPrimaItem> cmbMaterial = new ComboBox<>();
        cmbMaterial.setMaxWidth(Double.MAX_VALUE);
        cmbMaterial.setPromptText("Select raw material");
        carregarMateriasPrimas(cmbMaterial);
        if (materialId != null) {
            cmbMaterial.getItems().stream()
                    .filter(item -> item.id().equals(materialId))
                    .findFirst()
                    .ifPresent(cmbMaterial::setValue);
        }

        // Quantity
        Label lblQuantidade = new Label("Quantity per kg output");
        lblQuantidade.getStyleClass().add("text-muted");
        TextField txtQuantidade = new TextField(quantidade != null ? String.valueOf(quantidade) : "0");

        // Remove button
        Button btnRemove = new Button("Remove Ingredient");
        btnRemove.setStyle("-fx-text-fill: #ef4444; -fx-background-color: transparent; -fx-border-color: transparent;");
        btnRemove.setOnAction(e -> {
            container.getChildren().remove(ingredienteBox);
            rows.removeIf(r -> r.container() == ingredienteBox);
            atualizarTotal(rows, lblTotal, lblWarn);
        });

        ingredienteBox.getChildren().addAll(lblMaterial, cmbMaterial, lblQuantidade, txtQuantidade, btnRemove);
        container.getChildren().add(ingredienteBox);

        IngredienteRow row = new IngredienteRow(ingredienteBox, cmbMaterial, txtQuantidade);
        rows.add(row);

        // Update total on quantity change
        txtQuantidade.textProperty().addListener((obs, old, val) -> atualizarTotal(rows, lblTotal, lblWarn));
    }

    private void atualizarTotal(List<IngredienteRow> rows, Label lblTotal, Label lblWarn) {
        double total = 0.0;
        for (IngredienteRow row : rows) {
            try {
                String text = row.txtQuantidade().getText().replace(",", ".");
                total += Double.parseDouble(text);
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }

        lblTotal.setText(String.format("%.2f kg", total));

        if (Math.abs(total - 1.0) < 0.01) {
            lblTotal.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            lblWarn.setVisible(false);
        } else {
            lblTotal.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
            lblWarn.setVisible(true);
        }
    }

    private void carregarTiposPellet(ComboBox<TipoPelletItem> combo) {
        try {
            var page = stockService.listarTiposPelletComFiltros(1, 1000, null, null, "nome", "ASC");
            List<TipoPelletItem> items = page.getContent().stream()
                    .map(dto -> new TipoPelletItem(dto.id(), dto.nome()))
                    .toList();
            combo.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            mostrarErro("Erro ao carregar tipos de pellet: " + e.getMessage());
        }
    }

    private void carregarMateriasPrimas(ComboBox<MateriaPrimaItem> combo) {
        try {
            var page = stockService.listarMateriasPrimasComFiltros(1, 1000, null, null, "nome", "ASC");
            List<MateriaPrimaItem> items = page.getContent().stream()
                    .map(dto -> new MateriaPrimaItem(dto.id(), dto.nome()))
                    .toList();
            combo.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            mostrarErro("Erro ao carregar materias-primas: " + e.getMessage());
        }
    }

    private VBox criarCampoFormulario(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private void handleAdicionar() {
        if (cmbTipoPellet.getValue() == null) {
            mostrarErro("Selecione um tipo de pellet");
            return;
        }

        if (ingredienteRows.isEmpty()) {
            mostrarErro("Adicione pelo menos um ingrediente");
            return;
        }

        // TODO: Build DTO and call service
        try {
            // FormulaProducaoRequestDTO dto = ...
            // formulaService.criarFormula(dto);
            paginaAtual = 0;
            carregarFormulas();
            navigationService.hideModal();
            mostrarSucesso("Fórmula criada!");
        } catch (Exception e) {
            mostrarErro("Erro: " + e.getMessage());
        }
    }

    private void handleAtualizarFormula(UUID id, ComboBox<TipoPelletItem> cmbTipo, TextField txtVer,
                                        List<IngredienteRow> rows, CheckBox chkAtv) {
        // TODO: Implement validation and update
        try {
            // FormulaProducaoRequestDTO dto = ...
            // formulaService.atualizarFormula(id, dto);
            carregarFormulas();
            navigationService.hideModal();
            mostrarSucesso("Fórmula atualizada!");
        } catch (Exception ex) {
            mostrarErro("Erro ao atualizar: " + ex.getMessage());
        }
    }

    private void configurarPaginacao(VBox container) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(20, 0, 20, 0));
        nav.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");
        HBox left = new HBox(lblPaginaStatus);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setOnAction(e -> {
            itemsPerPage = cmbItemsPerPage.getValue();
            paginaAtual = 0;
            carregarFormulas();
        });
        HBox center = new HBox(10, new Label("Por página"), cmbItemsPerPage);
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
        prev.setOnAction(e -> {
            paginaAtual--;
            carregarFormulas();
        });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int finalI = i;
                p.setOnAction(e -> {
                    paginaAtual = finalI;
                    carregarFormulas();
                });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button();
        next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> {
            paginaAtual++;
            carregarFormulas();
        });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<FormulaSimpleDTO> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
    }

    private HBox criarBadgeStatus(Boolean ativa) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");

        String color = ativa ? "#10b981" : "#6b7280";
        String text = ativa ? "Active" : "Inactive";

        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s20; -fx-border-color: %s;",
                color.replace("#", ""), color));
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
        b.getChildren().add(l);
        return b;
    }

    private void configurarComboBoxes() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));
    }

    private void limparFormulario() {
        cmbTipoPellet.setValue(null);
        txtVersao.setText("v1.0");
        ingredientesContainer.getChildren().clear();
        ingredienteRows.clear();
        adicionarIngrediente(ingredientesContainer, ingredienteRows, null, null, lblTotalKg, lblWarning);
        chkAtiva.setSelected(true);
        atualizarTotal(ingredienteRows, lblTotalKg, lblWarning);
    }

    private void mostrarSucesso(String m) {
        toastService.showSuccess("Sucesso", m);
    }

    private void mostrarErro(String m) {
        toastService.showError("Erro", m);
    }

    @FXML
    private void handleFiltrar() {
        paginaAtual = 0;
        carregarFormulas();
    }

    @FXML
    private void handleMostrarTodos() {
        txtFiltroNome.clear();
        cmbFiltroStatus.setValue(null);
        handleFiltrar();
    }

    // Helper records
    private record IngredienteRow(VBox container, ComboBox<MateriaPrimaItem> cmbMaterial, TextField txtQuantidade) {}
    private record TipoPelletItem(UUID id, String nome) {
        @Override
        public String toString() { return nome; }
    }
    private record MateriaPrimaItem(UUID id, String nome) {
        @Override
        public String toString() { return nome; }
    }
}

