package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.ClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.ClienteService;
import com.pelletsfactory.stock_manager.desktop.services.FormValidationService;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClientsController {

    private final ClienteService clienteService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;
    private final FormValidationService formValidationService;

    @FXML private TableView<ClienteSimpleDTO> tblClients;
    @FXML private TableColumn<ClienteSimpleDTO, String> colId, colNome, colNif, colContacto;
    @FXML private TableColumn<ClienteSimpleDTO, Void> colAcoes;

    @FXML private TextField txtFiltroNome;
    @FXML private TextField txtFiltroNif;
    @FXML private VBox vboxContainer;

    private PaginationControls pagination;
    private final ObservableList<ClienteSimpleDTO> clientes = FXCollections.observableArrayList();

    public ClientsController(ClienteService clienteService, NavigationService navigationService,
                             ToastService toastService, I18nService i18nService,
                             FormValidationService formValidationService) {
        this.clienteService = clienteService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
        this.formValidationService = formValidationService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarDados, i18nService);
        configurarTabela();
        carregarDados();
    }

    // ── Table ────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                "CLI-" + cd.getValue().id().toString().substring(0, 4).toUpperCase()));
        configurarColunaTexto(colId);

        colNome.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colNome);

        colNif.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().nif()));
        configurarColunaTexto(colNif);

        colContacto.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().contacto()));
        configurarColunaTexto(colContacto);

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button();
            {
                btnView.getStyleClass().addAll("button-icon", "flat");
                btnView.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnView.setTooltip(new Tooltip(i18nService.translate("common.view")));
                btnView.setOnAction(e -> {
                    ClienteSimpleDTO dto = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(dto.id());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnView);
                setAlignment(Pos.CENTER);
            }
        });

        tblClients.setFixedCellSize(48);
        tblClients.setItems(clientes);
    }

    private <T> void configurarColunaTexto(TableColumn<ClienteSimpleDTO, T> coluna) {
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

    // ── Data ─────────────────────────────────────────────────────────────────

    private void carregarDados() {
        try {
            String nome = (txtFiltroNome != null && !txtFiltroNome.getText().isEmpty()) ? txtFiltroNome.getText() : null;
            String nif = (txtFiltroNif != null && !txtFiltroNif.getText().isEmpty()) ? txtFiltroNif.getText() : null;
            Page<ClienteSimpleDTO> page = clienteService.listarClientesComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, nif, "nome", "ASC");
            clientes.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML private void handleFiltrar() { pagination.resetPage(); carregarDados(); }
    @FXML private void handleLimpar() { txtFiltroNome.clear(); txtFiltroNif.clear(); handleFiltrar(); }

    @FXML
    private void handleNovoCliente() {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("clients.newTitle"), navigationService::hideModal);

        TextField txtNome = new TextField(); txtNome.setPromptText(i18nService.translate("clients.namePlaceholder"));
        Label lblErroNome = formValidationService.createErrorLabel();
        TextField txtNif = new TextField(); txtNif.setPromptText(i18nService.translate("common.nifPlaceholder"));
        Label lblErroNif = formValidationService.createErrorLabel();
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email");
        Label lblErroEmail = formValidationService.createErrorLabel();
        TextField txtContacto = new TextField(); txtContacto.setPromptText(i18nService.translate("clients.contactPlaceholder"));
        Label lblErroContacto = formValidationService.createErrorLabel();

        formValidationService.attachTextAutoClear(txtNome, lblErroNome);
        formValidationService.attachTextAutoClear(txtNif, lblErroNif);
        formValidationService.attachTextAutoClear(txtEmail, lblErroEmail);
        formValidationService.attachTextAutoClear(txtContacto, lblErroContacto);

        Label lblErroGeral = criarErroGeral();

        VBox form = new VBox(20,
                criarCampoComErro(i18nService.translate("common.name"), txtNome, lblErroNome),
                criarCampoComErro("NIF", txtNif, lblErroNif),
                criarCampoComErro("Email", txtEmail, lblErroEmail),
                criarCampoComErro(i18nService.translate("clients.contact"), txtContacto, lblErroContacto),
                lblErroGeral
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();
        Button btnSave = new Button(i18nService.translate("clients.save"));
        btnSave.setPrefHeight(44);
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.getStyleClass().add("accent");
        HBox.setHgrow(btnSave, Priority.ALWAYS);
        btnSave.setOnAction(e -> {
            lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
            boolean valido = formValidationService.validateRequiredText(txtNome, lblErroNome, i18nService.translate("common.nameRequired"));
            valido = formValidationService.validateRequiredText(txtNif, lblErroNif, i18nService.translate("common.nifRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtEmail, lblErroEmail, i18nService.translate("clients.emailRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtContacto, lblErroContacto, i18nService.translate("clients.contactRequired")) && valido;
            if (valido) valido = formValidationService.validateRegex(txtContacto, lblErroContacto, "2[0-9]{8}", i18nService.translate("clients.contactInvalid")) && valido;
            if (!valido) return;
            try {
                clienteService.registarCliente(txtNome.getText(), txtNif.getText(), txtContacto.getText(), txtEmail.getText());
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("clients.created"));
                navigationService.hideModal();
                carregarDados();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });
        footer.getChildren().add(btnSave);
        root.getChildren().addAll(header, scrollPane, footer);
        navigationService.showModal(root);
    }

    // ── Details (view) drawer ─────────────────────────────────────────────────

    private void handleAbrirDetalhes(UUID clienteId) {
        try {
            ClienteDetailsDTO d = clienteService.obterDetalhesCliente(clienteId);
            navigationService.showModal(criarDrawerDetalhes(d));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(ClienteDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("clients.detailsTitle"), navigationService::hideModal);

        TextField txtNome = new TextField(valorOuVazio(d.nome()));
        TextField txtNif = new TextField(valorOuVazio(d.nif()));
        TextField txtEmail = new TextField(valorOuVazio(d.email()));
        TextField txtContacto = new TextField(valorOuVazio(d.contacto()));
        txtNome.setEditable(false); txtNif.setEditable(false);
        txtEmail.setEditable(false); txtContacto.setEditable(false);

        int totalOrders = d.encomendas() != null ? d.encomendas().size() : 0;
        Label lblOrders = new Label(i18nService.translate("orders.title") + ": " + totalOrders);
        lblOrders.getStyleClass().add("text-muted");

        VBox form = new VBox(20,
                criarCampo(i18nService.translate("common.name"), txtNome),
                criarCampo("NIF", txtNif),
                criarCampo("Email", txtEmail),
                criarCampo(i18nService.translate("clients.contact"), txtContacto),
                lblOrders
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

    private void handleAbrirEditar(UUID clienteId) {
        try {
            ClienteDetailsDTO d = clienteService.obterDetalhesCliente(clienteId);
            navigationService.showModal(criarDrawerEditar(d));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private VBox criarDrawerEditar(ClienteDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("clients.editTitle"), navigationService::hideModal);

        TextField txtNome = new TextField(valorOuVazio(d.nome()));
        Label lblErroNome = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNome, lblErroNome);

        TextField txtNif = new TextField(valorOuVazio(d.nif()));
        Label lblErroNif = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNif, lblErroNif);

        TextField txtEmail = new TextField(valorOuVazio(d.email()));
        Label lblErroEmail = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtEmail, lblErroEmail);

        TextField txtContacto = new TextField(valorOuVazio(d.contacto()));
        Label lblErroContacto = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtContacto, lblErroContacto);

        // Summary line: total orders
        int totalOrders = d.encomendas() != null ? d.encomendas().size() : 0;
        Label lblOrders = new Label(i18nService.translate("orders.title") + ": " + totalOrders);
        lblOrders.getStyleClass().add("text-muted");

        Label lblErroGeral = criarErroGeral();

        VBox form = new VBox(20,
                criarCampoComErro(i18nService.translate("common.name"), txtNome, lblErroNome),
                criarCampoComErro("NIF", txtNif, lblErroNif),
                criarCampoComErro("Email", txtEmail, lblErroEmail),
                criarCampoComErro(i18nService.translate("clients.contact"), txtContacto, lblErroContacto),
                lblOrders,
                lblErroGeral
        );
        form.setPadding(new Insets(30));
        ScrollPane scrollPane = UiFactory.transparentScroll(form);

        HBox footer = UiFactory.drawerFooter();

        Button btnEliminar = new Button(i18nService.translate("common.delete"));
        btnEliminar.getStyleClass().addAll("button-outlined", "danger");
        btnEliminar.setPrefHeight(44);
        btnEliminar.setOnAction(e -> handleEliminarCliente(d.id(), d.nome()));

        Button btnGuardar = new Button(i18nService.translate("clients.save"));
        btnGuardar.getStyleClass().add("accent");
        btnGuardar.setPrefHeight(44);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnGuardar, Priority.ALWAYS);
        btnGuardar.setOnAction(e -> {
            lblErroGeral.setVisible(false); lblErroGeral.setManaged(false);
            boolean valido = formValidationService.validateRequiredText(txtNome, lblErroNome, i18nService.translate("common.nameRequired"));
            valido = formValidationService.validateRequiredText(txtNif, lblErroNif, i18nService.translate("common.nifRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtEmail, lblErroEmail, i18nService.translate("clients.emailRequired")) && valido;
            valido = formValidationService.validateRequiredText(txtContacto, lblErroContacto, i18nService.translate("clients.contactRequired")) && valido;
            if (valido) valido = formValidationService.validateRegex(txtContacto, lblErroContacto, "2[0-9]{8}", i18nService.translate("clients.contactInvalid")) && valido;
            if (!valido) return;
            try {
                clienteService.atualizarCliente(d.id(), txtNome.getText(), txtNif.getText(),
                        txtContacto.getText(), txtEmail.getText());
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("clients.updated"));
                navigationService.hideModal();
                carregarDados();
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true); lblErroGeral.setManaged(true);
            }
        });

        footer.getChildren().addAll(btnEliminar, btnGuardar);
        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private void handleEliminarCliente(UUID id, String nome) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(i18nService.translate("common.delete"));
        confirm.setHeaderText(i18nService.translate("common.confirmDelete"));
        confirm.setContentText("\"" + nome + "\"");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            clienteService.apagarCliente(id);
            toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("clients.deleted"));
            navigationService.hideModal();
            carregarDados();
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
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

    private Label criarErroGeral() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-color: #ef444420; -fx-background-radius: 6; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private String valorOuVazio(String s) { return s != null ? s : ""; }
}
