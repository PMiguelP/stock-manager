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
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private boolean updatingSearch;

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
        configurarPesquisaDinamica();
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

    @FXML
    private void handleLimpar() {
        updatingSearch = true;
        searchDebounce.stop();
        txtSearch.clear();
        updatingSearch = false;
        aplicarPesquisaDinamica();
    }

    // ── Dynamic search ────────────────────────────────────────────────────────

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(e -> aplicarPesquisaDinamica());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingSearch) searchDebounce.playFromStart();
        });
    }

    private void aplicarPesquisaDinamica() {
        pagination.resetPage();
        carregarFornecedores();
    }

    // ── Details (view/edit) drawer ────────────────────────────────────────────

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
        Label lblErroNome = formValidationService.createErrorLabel();
        TextField txtNif = new TextField(valorOuVazio(d.nif()));
        Label lblErroNif = formValidationService.createErrorLabel();
        TextField txtContacto = new TextField(valorOuVazio(d.contacto()));
        Label lblErroContacto = formValidationService.createErrorLabel();
        TextField txtEmail = new TextField(valorOuVazio(d.email()));
        Label lblErroEmail = formValidationService.createErrorLabel();

        formValidationService.attachTextAutoClear(txtNome, lblErroNome);
        formValidationService.attachTextAutoClear(txtNif, lblErroNif);
        formValidationService.attachTextAutoClear(txtContacto, lblErroContacto);
        formValidationService.attachTextAutoClear(txtEmail, lblErroEmail);

        setCamposEditaveis(false, txtNome, txtNif, txtContacto, txtEmail);

        String[] nomeOrig = {valorOuVazio(d.nome())};
        String[] nifOrig = {valorOuVazio(d.nif())};
        String[] contactoOrig = {valorOuVazio(d.contacto())};
        String[] emailOrig = {valorOuVazio(d.email())};

        Label lblErroGeral = criarErroGeral();

        VBox form = new VBox(20,
                criarCampoComErro(i18nService.translate("common.name"), txtNome, lblErroNome),
                criarCampoComErro("NIF", txtNif, lblErroNif),
                criarCampoComErro(i18nService.translate("suppliers.contactPerson"), txtContacto, lblErroContacto),
                criarCampoComErro("Email", txtEmail, lblErroEmail),
                lblErroGeral
        );
        form.setPadding(new Insets(30));
        ScrollPane scrollPane = UiFactory.transparentScroll(form);

        Button btnEliminar = UiFactory.drawerDangerAction(i18nService.translate("common.delete"), "mdi2d-delete-outline");
        Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        Button btnEditar = UiFactory.drawerSecondaryAction(i18nService.translate("common.edit"), "mdi2p-pencil-outline");
        Button btnGuardar = UiFactory.drawerPrimaryAction(i18nService.translate("common.save"), "mdi2c-content-save-outline");

        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
        btnGuardar.setDisable(true);

        HBox footer = UiFactory.drawerActionFooter(btnEliminar, btnCancelar, btnEditar, btnGuardar);

        btnEditar.setOnAction(e -> {
            setCamposEditaveis(true, txtNome, txtNif, txtContacto, txtEmail);
            btnGuardar.setDisable(false);
            btnEditar.setVisible(false);
            btnEditar.setManaged(false);
            btnCancelar.setVisible(true);
            btnCancelar.setManaged(true);
            txtNome.requestFocus();
        });

        btnCancelar.setOnAction(e -> {
            txtNome.setText(nomeOrig[0]);
            txtNif.setText(nifOrig[0]);
            txtContacto.setText(contactoOrig[0]);
            txtEmail.setText(emailOrig[0]);
            lblErroGeral.setVisible(false);
            lblErroGeral.setManaged(false);
            formValidationService.clearError(txtNome, lblErroNome);
            formValidationService.clearError(txtNif, lblErroNif);
            formValidationService.clearError(txtContacto, lblErroContacto);
            formValidationService.clearError(txtEmail, lblErroEmail);
            setModoVisualizacao(btnGuardar, btnEditar, btnCancelar, txtNome, txtNif, txtContacto, txtEmail);
        });

        btnGuardar.setOnAction(e -> {
            lblErroGeral.setVisible(false);
            lblErroGeral.setManaged(false);
            boolean valido = formValidationService.validateRequiredText(txtNome, lblErroNome, i18nService.translate("common.nameRequired"));
            valido = formValidationService.validateRequiredText(txtNif, lblErroNif, i18nService.translate("common.nifRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtContacto, lblErroContacto, i18nService.translate("suppliers.contactRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtEmail, lblErroEmail, i18nService.translate("suppliers.emailRequired")) && valido;
            if (!valido) return;
            try {
                fornecedorService.atualizarFornecedor(d.id(), txtNome.getText(), txtNif.getText(),
                        txtContacto.getText(), txtEmail.getText());
                nomeOrig[0] = txtNome.getText();
                nifOrig[0] = txtNif.getText();
                contactoOrig[0] = txtContacto.getText();
                emailOrig[0] = txtEmail.getText();
                carregarFornecedores();
                setModoVisualizacao(btnGuardar, btnEditar, btnCancelar, txtNome, txtNif, txtContacto, txtEmail);
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("suppliers.updated"));
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true);
                lblErroGeral.setManaged(true);
            }
        });

        btnEliminar.setOnAction(e -> handleEliminarFornecedor(d.id(), d.nome()));

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private void handleEliminarFornecedor(UUID id, String nome) {
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
        txtEmailAdicionar = new TextField(); txtEmailAdicionar.setPromptText(i18nService.translate("common.emailPlaceholder"));
        lblErroEmailAdicionar = formValidationService.createErrorLabel();

        formValidationService.attachTextAutoClear(txtNomeAdicionar, lblErroNomeAdicionar);
        formValidationService.attachTextAutoClear(txtNifAdicionar, lblErroNifAdicionar);
        formValidationService.attachTextAutoClear(txtContactoAdicionar, lblErroContactoAdicionar);
        formValidationService.attachTextAutoClear(txtEmailAdicionar, lblErroEmailAdicionar);

        Label lblErroGeral = criarErroGeral();

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

    private void setCamposEditaveis(boolean editavel, TextField... campos) {
        for (TextField campo : campos) {
            campo.setEditable(editavel);
            campo.setFocusTraversable(editavel);
        }
    }

    private void setModoVisualizacao(Button btnGuardar, Button btnEditar, Button btnCancelar, TextField... campos) {
        setCamposEditaveis(false, campos);
        btnGuardar.setDisable(true);
        btnEditar.setVisible(true);
        btnEditar.setManaged(true);
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
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

    private VBox criarCampoComErro(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label); lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    private String valorOuVazio(String s) { return s != null ? s : ""; }
}
