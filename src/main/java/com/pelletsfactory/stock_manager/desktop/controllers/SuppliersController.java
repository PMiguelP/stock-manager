package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.FornecedorDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.FornecedorService;
import com.pelletsfactory.stock_manager.desktop.services.FormValidationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
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

@Component
public class SuppliersController {

    private final FornecedorService fornecedorService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;
    private final FormValidationService formValidationService;

    @FXML private VBox vboxContainer;
    @FXML private TextField txtSearch;
    @FXML private TableView<FornecedorSimpleDTO> tblSuppliers;
    @FXML private TableColumn<FornecedorSimpleDTO, Void> colSupplierID;
    @FXML private TableColumn<FornecedorSimpleDTO, String> colName;
    @FXML private TableColumn<FornecedorSimpleDTO, String> colTaxId;
    @FXML private TableColumn<FornecedorSimpleDTO, String> colContactPerson;
    @FXML private TableColumn<FornecedorSimpleDTO, Void> colActions;

    // Create drawer
    private VBox drawerAdicionar;
    private TextField txtNomeAdicionar, txtNifAdicionar, txtContactoAdicionar, txtEmailAdicionar;
    private Label lblErroNomeAdicionar, lblErroNifAdicionar, lblErroContactoAdicionar, lblErroEmailAdicionar;

    private PaginationControls pagination;
    private final ObservableList<FornecedorSimpleDTO> fornecedores = FXCollections.observableArrayList();

    public SuppliersController(FornecedorService fornecedorService, NavigationService navigationService,
                               ToastService toastService, I18nService i18nService,
                               FormValidationService formValidationService) {
        this.fornecedorService = fornecedorService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
        this.formValidationService = formValidationService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarFornecedores, i18nService);
        configurarTabela();
        configurarDrawerAdicionar();
        carregarFornecedores();
    }

    // ── Table ────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colSupplierID.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || getIndex() < 0 ? null
                        : String.format("SUP-%03d", pagination.currentPage() * pagination.pageSize() + getIndex() + 1));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
                setStyle("-fx-font-weight: bold;");
            }
        });

        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colName);

        colTaxId.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().nif() != null ? "PT" + cd.getValue().nif() : "—"));
        configurarColunaTexto(colTaxId);

        colContactPerson.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().contacto() != null ? cd.getValue().contacto() : "—"));
        configurarColunaTexto(colContactPerson);

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button();
            {
                btnView.getStyleClass().addAll("button-icon", "flat");
                btnView.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnView.setTooltip(new Tooltip(i18nService.translate("common.view")));
                btnView.setOnAction(e -> {
                    FornecedorSimpleDTO dto = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(dto);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnView);
                setAlignment(Pos.CENTER);
            }
        });

        tblSuppliers.setFixedCellSize(48);
        tblSuppliers.setItems(fornecedores);
    }

    private <T> void configurarColunaTexto(TableColumn<FornecedorSimpleDTO, T> coluna) {
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

    // ── Data ─────────────────────────────────────────────────────────────────

    private void carregarFornecedores() {
        try {
            String nome = (txtSearch != null && !txtSearch.getText().isEmpty()) ? txtSearch.getText() : null;
            Page<FornecedorSimpleDTO> page = fornecedorService.listarFornecedoresComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, "nome", "ASC");
            fornecedores.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML private void handleAbrirModal() { limparFormulario(); navigationService.showModal(drawerAdicionar); }
    @FXML private void handleFiltrar() { pagination.resetPage(); carregarFornecedores(); }
    @FXML private void handleLimpar() { txtSearch.clear(); pagination.resetPage(); carregarFornecedores(); }

    // ── Details (view) drawer ─────────────────────────────────────────────────

    private void handleAbrirDetalhes(FornecedorSimpleDTO dto) {
        try {
            FornecedorDetailsDTO d = fornecedorService.obterDetalhesFornecedor(dto.id());
            navigationService.showModal(criarDrawerDetalhes(d));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(FornecedorDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("suppliers.detailsTitle"), navigationService::hideModal);

        TextField txtNome = new TextField(valorOuVazio(d.nome()));
        TextField txtNif = new TextField(valorOuVazio(d.nif()));
        TextField txtContacto = new TextField(valorOuVazio(d.contacto()));
        TextField txtEmail = new TextField(valorOuVazio(d.email()));
        txtNome.setEditable(false); txtNif.setEditable(false);
        txtContacto.setEditable(false); txtEmail.setEditable(false);

        VBox form = new VBox(20,
                criarCampo(i18nService.translate("common.name"), txtNome),
                criarCampo("NIF", txtNif),
                criarCampo(i18nService.translate("suppliers.contactPerson"), txtContacto),
                criarCampo("Email", txtEmail)
        );
        form.setPadding(new Insets(30));
        ScrollPane scrollPane = UiFactory.transparentScroll(form);

        HBox footer = UiFactory.drawerFooter();
        Button btnEditar = new Button(i18nService.translate("common.edit"));
        btnEditar.getStyleClass().add("accent");
        btnEditar.setPrefHeight(44);
        btnEditar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEditar, Priority.ALWAYS);
        btnEditar.setOnAction(e -> navigationService.showModal(criarDrawerEditar(d)));
        footer.getChildren().add(btnEditar);

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    // ── Edit drawer ───────────────────────────────────────────────────────────

    private void handleAbrirEditar(FornecedorSimpleDTO dto) {
        try {
            FornecedorDetailsDTO d = fornecedorService.obterDetalhesFornecedor(dto.id());
            navigationService.showModal(criarDrawerEditar(d));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private VBox criarDrawerEditar(FornecedorDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("suppliers.editTitle"), navigationService::hideModal);

        TextField txtNome = new TextField(valorOuVazio(d.nome()));
        Label lblErroNome = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNome, lblErroNome);

        TextField txtNif = new TextField(valorOuVazio(d.nif()));
        Label lblErroNif = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNif, lblErroNif);

        TextField txtContacto = new TextField(valorOuVazio(d.contacto()));
        Label lblErroContacto = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtContacto, lblErroContacto);

        TextField txtEmail = new TextField(valorOuVazio(d.email()));
        Label lblErroEmail = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtEmail, lblErroEmail);

        Label lblErroGeral = new Label();
        lblErroGeral.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-color: #ef444420; -fx-background-radius: 6; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1;");
        lblErroGeral.setWrapText(true);
        lblErroGeral.setMaxWidth(Double.MAX_VALUE);
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);

        VBox form = new VBox(20,
                criarCampoComErro(i18nService.translate("common.name"), txtNome, lblErroNome),
                criarCampoComErro("NIF", txtNif, lblErroNif),
                criarCampoComErro(i18nService.translate("suppliers.contactPerson"), txtContacto, lblErroContacto),
                criarCampoComErro("Email", txtEmail, lblErroEmail),
                lblErroGeral
        );
        form.setPadding(new Insets(30));
        ScrollPane scrollPane = UiFactory.transparentScroll(form);

        HBox footer = UiFactory.drawerFooter();

        Button btnEliminar = new Button(i18nService.translate("common.delete"));
        btnEliminar.getStyleClass().addAll("button-outlined", "danger");
        btnEliminar.setPrefHeight(44);
        btnEliminar.setOnAction(e -> handleEliminarFornecedor(d.id(), d.nome()));

        Button btnGuardar = new Button(i18nService.translate("common.save"));
        btnGuardar.getStyleClass().add("accent");
        btnGuardar.setPrefHeight(44);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnGuardar, Priority.ALWAYS);
        btnGuardar.setOnAction(e -> {
            lblErroGeral.setVisible(false);
            lblErroGeral.setManaged(false);
            boolean valido = formValidationService.validateRequiredText(txtNome, lblErroNome, i18nService.translate("common.nameRequired"));
            valido = formValidationService.validateRequiredText(txtNif, lblErroNif, i18nService.translate("common.nifRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtContacto, lblErroContacto, i18nService.translate("suppliers.contactRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtEmail, lblErroEmail, i18nService.translate("suppliers.emailRequired")) && valido;
            if (!valido) return;
            try {
                fornecedorService.atualizarFornecedor(d.id(),
                        txtNome.getText(), txtNif.getText(),
                        txtContacto.getText(), txtEmail.getText());
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("suppliers.updated"));
                navigationService.hideModal();
                carregarFornecedores();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true);
                lblErroGeral.setManaged(true);
            }
        });

        footer.getChildren().addAll(btnEliminar, btnGuardar);
        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private void handleEliminarFornecedor(java.util.UUID id, String nome) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(i18nService.translate("common.delete"));
        confirm.setHeaderText(i18nService.translate("common.confirmDelete"));
        confirm.setContentText("\"" + nome + "\"");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            fornecedorService.apagarFornecedor(id);
            toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("suppliers.deleted"));
            navigationService.hideModal();
            carregarFornecedores();
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    // ── Create drawer ─────────────────────────────────────────────────────────

    private void configurarDrawerAdicionar() {
        drawerAdicionar = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("suppliers.newTitle"), navigationService::hideModal);

        txtNomeAdicionar = new TextField(); txtNomeAdicionar.setPromptText(i18nService.translate("suppliers.namePlaceholder"));
        lblErroNomeAdicionar = formValidationService.createErrorLabel();
        txtNifAdicionar = new TextField(); txtNifAdicionar.setPromptText(i18nService.translate("common.nifPlaceholder"));
        lblErroNifAdicionar = formValidationService.createErrorLabel();
        txtContactoAdicionar = new TextField(); txtContactoAdicionar.setPromptText(i18nService.translate("suppliers.contactPlaceholder"));
        lblErroContactoAdicionar = formValidationService.createErrorLabel();
        txtEmailAdicionar = new TextField(); txtEmailAdicionar.setPromptText("email@exemplo.com");
        lblErroEmailAdicionar = formValidationService.createErrorLabel();

        formValidationService.attachTextAutoClear(txtNomeAdicionar, lblErroNomeAdicionar);
        formValidationService.attachTextAutoClear(txtNifAdicionar, lblErroNifAdicionar);
        formValidationService.attachTextAutoClear(txtContactoAdicionar, lblErroContactoAdicionar);
        formValidationService.attachTextAutoClear(txtEmailAdicionar, lblErroEmailAdicionar);

        Label lblErroGeral = new Label();
        lblErroGeral.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-color: #ef444420; -fx-background-radius: 6; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1;");
        lblErroGeral.setWrapText(true);
        lblErroGeral.setMaxWidth(Double.MAX_VALUE);
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);

        VBox form = new VBox(20,
                criarCampoComErro(i18nService.translate("common.name"), txtNomeAdicionar, lblErroNomeAdicionar),
                criarCampoComErro("NIF", txtNifAdicionar, lblErroNifAdicionar),
                criarCampoComErro(i18nService.translate("suppliers.contactPerson"), txtContactoAdicionar, lblErroContactoAdicionar),
                criarCampoComErro("Email", txtEmailAdicionar, lblErroEmailAdicionar),
                lblErroGeral
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();
        Button btnCreate = new Button(i18nService.translate("suppliers.save"));
        btnCreate.getStyleClass().add("accent");
        btnCreate.setPrefHeight(44);
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        btnCreate.setOnAction(e -> {
            lblErroGeral.setVisible(false);
            lblErroGeral.setManaged(false);
            boolean valido = formValidationService.validateRequiredText(txtNomeAdicionar, lblErroNomeAdicionar, i18nService.translate("common.nameRequired"));
            valido = formValidationService.validateRequiredText(txtNifAdicionar, lblErroNifAdicionar, i18nService.translate("common.nifRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtContactoAdicionar, lblErroContactoAdicionar, i18nService.translate("suppliers.contactRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtEmailAdicionar, lblErroEmailAdicionar, i18nService.translate("suppliers.emailRequired")) && valido;
            if (!valido) return;
            try {
                fornecedorService.registarFornecedor(
                        txtNomeAdicionar.getText(), txtNifAdicionar.getText(),
                        txtContactoAdicionar.getText(), txtEmailAdicionar.getText());
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("suppliers.created"));
                navigationService.hideModal();
                carregarFornecedores();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true);
                lblErroGeral.setManaged(true);
            }
        });
        footer.getChildren().add(btnCreate);
        drawerAdicionar.getChildren().addAll(header, scrollPane, footer);
    }

    private void limparFormulario() {
        txtNomeAdicionar.clear();
        txtNifAdicionar.clear();
        txtContactoAdicionar.clear();
        txtEmailAdicionar.clear();
        formValidationService.clearError(txtNomeAdicionar, lblErroNomeAdicionar);
        formValidationService.clearError(txtNifAdicionar, lblErroNifAdicionar);
        formValidationService.clearError(txtContactoAdicionar, lblErroContactoAdicionar);
        formValidationService.clearError(txtEmailAdicionar, lblErroEmailAdicionar);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox criarCampo(String label, Control input) {
        Label lbl = new Label(label); lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private VBox criarCampoComErro(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label); lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    private String valorOuVazio(String s) { return s != null ? s : ""; }
}
