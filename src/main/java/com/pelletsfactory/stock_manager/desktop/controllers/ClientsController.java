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

    @FXML private TextField txtPesquisa;
    @FXML private VBox vboxContainer;

    private PaginationControls pagination;
    private final ObservableList<ClienteSimpleDTO> clientes = FXCollections.observableArrayList();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private boolean updatingSearch;

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
        configurarPesquisaDinamica();
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
            String pesquisa = txtPesquisa != null ? txtPesquisa.getText() : null;
            Page<ClienteSimpleDTO> page = clienteService.listarClientesComPesquisa(
                    pagination.pageNumberForService(), pagination.pageSize(), pesquisa, "nome", "ASC");
            clientes.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML private void handleFiltrar() { aplicarPesquisaDinamica(); }

    @FXML
    private void handleLimpar() {
        updatingSearch = true;
        searchDebounce.stop();
        txtPesquisa.clear();
        updatingSearch = false;
        aplicarPesquisaDinamica();
    }

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
        Label lblErroNome = formValidationService.createErrorLabel();
        TextField txtNif = new TextField(valorOuVazio(d.nif()));
        Label lblErroNif = formValidationService.createErrorLabel();
        TextField txtEmail = new TextField(valorOuVazio(d.email()));
        Label lblErroEmail = formValidationService.createErrorLabel();
        TextField txtContacto = new TextField(valorOuVazio(d.contacto()));
        Label lblErroContacto = formValidationService.createErrorLabel();

        formValidationService.attachTextAutoClear(txtNome, lblErroNome);
        formValidationService.attachTextAutoClear(txtNif, lblErroNif);
        formValidationService.attachTextAutoClear(txtEmail, lblErroEmail);
        formValidationService.attachTextAutoClear(txtContacto, lblErroContacto);

        setCamposClienteEditaveis(false, txtNome, txtNif, txtEmail, txtContacto);

        String[] nomeOriginal = {valorOuVazio(d.nome())};
        String[] nifOriginal = {valorOuVazio(d.nif())};
        String[] emailOriginal = {valorOuVazio(d.email())};
        String[] contactoOriginal = {valorOuVazio(d.contacto())};

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

        Button btnEliminar = UiFactory.drawerDangerAction(i18nService.translate("common.delete"), "mdi2d-delete-outline");
        Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        Button btnEditar = UiFactory.drawerSecondaryAction(i18nService.translate("common.edit"), "mdi2p-pencil-outline");
        Button btnGuardar = UiFactory.drawerPrimaryAction(i18nService.translate("clients.save"), "mdi2c-content-save-outline");

        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
        btnGuardar.setDisable(true);

        HBox footer = UiFactory.drawerActionFooter(btnEliminar, btnCancelar, btnEditar, btnGuardar);

        btnEditar.setOnAction(e -> {
            setCamposClienteEditaveis(true, txtNome, txtNif, txtEmail, txtContacto);
            btnGuardar.setDisable(false);
            btnEditar.setVisible(false);
            btnEditar.setManaged(false);
            btnCancelar.setVisible(true);
            btnCancelar.setManaged(true);
            txtNome.requestFocus();
        });

        btnCancelar.setOnAction(e -> {
            txtNome.setText(nomeOriginal[0]);
            txtNif.setText(nifOriginal[0]);
            txtEmail.setText(emailOriginal[0]);
            txtContacto.setText(contactoOriginal[0]);
            lblErroGeral.setVisible(false);
            lblErroGeral.setManaged(false);
            limparErrosCliente(txtNome, lblErroNome, txtNif, lblErroNif, txtEmail, lblErroEmail, txtContacto, lblErroContacto);
            setModoVisualizacaoCliente(btnGuardar, btnEditar, btnCancelar, txtNome, txtNif, txtEmail, txtContacto);
        });

        btnGuardar.setOnAction(e -> {
            lblErroGeral.setVisible(false);
            lblErroGeral.setManaged(false);
            if (!validarCliente(txtNome, lblErroNome, txtNif, lblErroNif, txtEmail, lblErroEmail, txtContacto, lblErroContacto)) {
                return;
            }

            try {
                ClienteDetailsDTO atualizado = clienteService.atualizarCliente(d.id(), txtNome.getText(), txtNif.getText(),
                        txtContacto.getText(), txtEmail.getText());
                nomeOriginal[0] = valorOuVazio(atualizado.nome());
                nifOriginal[0] = valorOuVazio(atualizado.nif());
                emailOriginal[0] = valorOuVazio(atualizado.email());
                contactoOriginal[0] = valorOuVazio(atualizado.contacto());
                carregarDados();
                setModoVisualizacaoCliente(btnGuardar, btnEditar, btnCancelar, txtNome, txtNif, txtEmail, txtContacto);
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("clients.updated"));
            } catch (Exception ex) {
                lblErroGeral.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                lblErroGeral.setVisible(true);
                lblErroGeral.setManaged(true);
            }
        });

        btnEliminar.setOnAction(e -> handleEliminarCliente(d.id(), d.nome()));

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

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(e -> aplicarPesquisaDinamica());
        txtPesquisa.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingSearch) {
                searchDebounce.playFromStart();
            }
        });
    }

    private void aplicarPesquisaDinamica() {
        pagination.resetPage();
        carregarDados();
    }

    private boolean validarCliente(TextField nomeField, Label nomeErro,
                                   TextField nifField, Label nifErro,
                                   TextField emailField, Label emailErro,
                                   TextField contactoField, Label contactoErro) {
        boolean valido = true;
        valido = formValidationService.validateRequiredText(nomeField, nomeErro, i18nService.translate("common.nameRequired")) && valido;
        valido = formValidationService.validateRequiredText(nifField, nifErro, i18nService.translate("common.nifRequired")) && valido;
        valido = formValidationService.validateRequiredText(emailField, emailErro, i18nService.translate("clients.emailRequired")) && valido;
        valido = formValidationService.validateRequiredText(contactoField, contactoErro, i18nService.translate("clients.contactRequired")) && valido;
        if (valido) {
            valido = formValidationService.validateRegex(contactoField, contactoErro, "2[0-9]{8}", i18nService.translate("clients.contactInvalid"));
        }
        return valido;
    }

    private void setCamposClienteEditaveis(boolean editavel, TextField... campos) {
        for (TextField campo : campos) {
            campo.setEditable(editavel);
            campo.setFocusTraversable(editavel);
        }
    }

    private void setModoVisualizacaoCliente(Button btnGuardar, Button btnEditar, Button btnCancelar, TextField... campos) {
        setCamposClienteEditaveis(false, campos);
        btnGuardar.setDisable(true);
        btnEditar.setVisible(true);
        btnEditar.setManaged(true);
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
    }

    private void limparErrosCliente(TextField nomeField, Label nomeErro,
                                    TextField nifField, Label nifErro,
                                    TextField emailField, Label emailErro,
                                    TextField contactoField, Label contactoErro) {
        formValidationService.clearError(nomeField, nomeErro);
        formValidationService.clearError(nifField, nifErro);
        formValidationService.clearError(emailField, emailErro);
        formValidationService.clearError(contactoField, contactoErro);
    }

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
