package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.FuncionarioRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.util.Duration;

@Component
public class FuncionarioController {
    private final FuncionarioService funcionarioService;
    private final NavigationService navigationService;
    private final FormValidationService formValidationService;
    private final ToastService toastService;
    private final I18nService i18nService;

    @FXML private ComboBox<Cargo> cmbFiltroCargo;
    @FXML private TextField txtFiltroNome;
    @FXML private TableView<FuncionarioSimpleDTO> tblFuncionarios;
    @FXML private VBox vboxContainer;

    @FXML private TableColumn<FuncionarioSimpleDTO, Integer> colNumero;
    @FXML private TableColumn<FuncionarioSimpleDTO, String> colNome;
    @FXML private TableColumn<FuncionarioSimpleDTO, Cargo> colCargo;
    @FXML private TableColumn<FuncionarioSimpleDTO, LocalDate> colDataAdmissao;
    @FXML private TableColumn<FuncionarioSimpleDTO, Void> colAcoes;

    private TextField txtNome, txtNif, txtContacto;
    private ComboBox<Cargo> cmbCargo;
    private VBox drawerRoot;
    private Label lblErroNomeAdicionar;
    private Label lblErroCargoAdicionar;
    private Label lblErroNifAdicionar;
    private Label lblErroContactoAdicionar;

    private PaginationControls pagination;

    private final ObservableList<FuncionarioSimpleDTO> funcionarios = FXCollections.observableArrayList();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private boolean updatingFilters;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public FuncionarioController(FuncionarioService funcionarioService,
                                 NavigationService navigationService,
                                 FormValidationService formValidationService,
                                 ToastService toastService,
                                 I18nService i18nService) {
        this.funcionarioService = funcionarioService;
        this.navigationService = navigationService;
        this.formValidationService = formValidationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarFuncionarios, i18nService);
        configurarTabela();
        configurarComboBoxes();
        configurarPesquisaDinamica();
        configurarDrawerAdicionar();
        carregarFuncionarios();
    }

    private void configurarTabela() {
        colNumero.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().numeroFuncionario()));
        configurarColunaTexto(colNumero);

        colNome.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colNome);

        colDataAdmissao.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().dataAdmissao()));
        configurarColunaTexto(colDataAdmissao);

        colCargo.setCellValueFactory(cd -> {
            try {
                return new javafx.beans.property.SimpleObjectProperty<>(Cargo.valueOf(cd.getValue().cargo()));
            } catch (Exception e) {
                return new javafx.beans.property.SimpleObjectProperty<>(null);
            }
        });

        colCargo.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Cargo cargo, boolean empty) {
                super.updateItem(cargo, empty);
                if (empty || cargo == null) {
                    setGraphic(null);
                } else {
                    setGraphic(criarBadgeCargo(cargo));
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
                btnDetails.setTooltip(new Tooltip("Ver Detalhes"));
                btnDetails.setOnAction(event -> {
                    FuncionarioSimpleDTO func = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(func);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblFuncionarios.setFixedCellSize(48);
        tblFuncionarios.setItems(funcionarios);
    }

    private <T> void configurarColunaTexto(TableColumn<FuncionarioSimpleDTO, T> coluna) {
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

    private void carregarFuncionarios() {
        try {
            String nome = txtFiltroNome != null && !txtFiltroNome.getText().isEmpty() ? txtFiltroNome.getText() : null;
            Cargo cargo = cmbFiltroCargo != null ? cmbFiltroCargo.getValue() : null;

            Page<FuncionarioSimpleDTO> page = funcionarioService.listarFuncionarios(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, cargo, null, "dataAdmissao", "DESC"
            );

            funcionarios.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    public void openCreateModal() {
        handleAbrirModal();
    }

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        navigationService.showModal(drawerRoot);
    }

    private void handleAbrirDetalhes(FuncionarioSimpleDTO func) {
        try {
            FuncionarioDetailsDTO d = funcionarioService.obterDetalhes(func.id());
            VBox detalhesDrawer = criarDrawerVisualizacao(d); // Este método agora usa 550px
            navigationService.showModal(detalhesDrawer);
        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    private VBox criarDrawerVisualizacao(FuncionarioDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("employees.detailsTitle"), navigationService::hideModal);

        TextField txtNomeDetalhes = new TextField(valorOuVazio(d.nome()));
        Label lblErroNomeDetalhes = formValidationService.createErrorLabel();
        ComboBox<Cargo> cmbCargoDetalhes = new ComboBox<>(FXCollections.observableArrayList(Cargo.values()));
        Label lblErroCargoDetalhes = formValidationService.createErrorLabel();
        cmbCargoDetalhes.setValue(d.cargo());
        cmbCargoDetalhes.setMaxWidth(Double.MAX_VALUE);
        TextField txtNifDetalhes = new TextField(valorOuVazio(d.nif()));
        Label lblErroNifDetalhes = formValidationService.createErrorLabel();
        TextField txtContactoDetalhes = new TextField(valorOuVazio(d.contacto()));
        Label lblErroContactoDetalhes = formValidationService.createErrorLabel();
        TextField txtNumeroDetalhes = new TextField(d.numeroFuncionario() != null ? String.valueOf(d.numeroFuncionario()) : "");
        TextField txtDataAdmissaoDetalhes = new TextField(d.dataAdmissao() != null ? d.dataAdmissao().format(DATE_FORMATTER) : "");

        formValidationService.attachTextAutoClear(txtNomeDetalhes, lblErroNomeDetalhes);
        formValidationService.attachComboAutoClear(cmbCargoDetalhes, lblErroCargoDetalhes);
        formValidationService.attachTextAutoClear(txtNifDetalhes, lblErroNifDetalhes);
        formValidationService.attachTextAutoClear(txtContactoDetalhes, lblErroContactoDetalhes);

        txtNumeroDetalhes.setEditable(false);
        txtDataAdmissaoDetalhes.setEditable(false);
        txtNumeroDetalhes.setFocusTraversable(false);
        txtDataAdmissaoDetalhes.setFocusTraversable(false);

        setCamposDetalhesEditaveis(false, txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes);

        String[] nomeOriginal = {valorOuVazio(d.nome())};
        Cargo[] cargoOriginal = {d.cargo()};
        String[] nifOriginal = {valorOuVazio(d.nif())};
        String[] contactoOriginal = {valorOuVazio(d.contacto())};

        VBox form = new VBox(20,
                UiFactory.formField("Nome Completo", txtNomeDetalhes, lblErroNomeDetalhes),
                UiFactory.formField("Cargo", cmbCargoDetalhes, lblErroCargoDetalhes),
                UiFactory.formField("NIF", txtNifDetalhes, lblErroNifDetalhes),
                UiFactory.formField("Telemóvel", txtContactoDetalhes, lblErroContactoDetalhes),
                UiFactory.formField("Número Interno", txtNumeroDetalhes),
                UiFactory.formField("Data de Entrada", txtDataAdmissaoDetalhes)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);

        Button btnGuardar = UiFactory.drawerPrimaryAction(
                i18nService.translate("employees.save"),
                "mdi2c-content-save-outline"
        );
        btnGuardar.setDisable(true);

        Button btnEditar = UiFactory.drawerSecondaryAction(
                i18nService.translate("common.edit"),
                "mdi2p-pencil-outline"
        );

        Button btnCancelar = UiFactory.drawerNeutralAction(
                i18nService.translate("common.cancel"),
                "mdi2c-close"
        );
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);

        Button btnEliminar = UiFactory.drawerDangerAction(
                i18nService.translate("common.delete"),
                "mdi2d-delete-outline"
        );

        HBox footer = UiFactory.drawerActionFooter(btnEliminar, btnCancelar, btnEditar, btnGuardar);

        btnEditar.setOnAction(e -> {
            setCamposDetalhesEditaveis(true, txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes);
            btnGuardar.setDisable(false);
            btnEditar.setVisible(false);
            btnEditar.setManaged(false);
            btnCancelar.setVisible(true);
            btnCancelar.setManaged(true);
            txtNomeDetalhes.requestFocus();
        });

        btnCancelar.setOnAction(e -> {
            txtNomeDetalhes.setText(nomeOriginal[0]);
            cmbCargoDetalhes.setValue(cargoOriginal[0]);
            txtNifDetalhes.setText(nifOriginal[0]);
            txtContactoDetalhes.setText(contactoOriginal[0]);
            limparErrosDetalhes(
                    txtNomeDetalhes, lblErroNomeDetalhes,
                    cmbCargoDetalhes, lblErroCargoDetalhes,
                    txtNifDetalhes, lblErroNifDetalhes,
                    txtContactoDetalhes, lblErroContactoDetalhes
            );
            setModoVisualizacaoDetalhes(
                    btnGuardar, btnEditar, btnCancelar,
                    txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes
            );
        });

        btnGuardar.setOnAction(e -> {
            if (!validarFormulario(
                    txtNomeDetalhes, lblErroNomeDetalhes,
                    cmbCargoDetalhes, lblErroCargoDetalhes,
                    txtNifDetalhes, lblErroNifDetalhes,
                    txtContactoDetalhes, lblErroContactoDetalhes
            )) {
                return;
            }

            try {
                funcionarioService.atualizarFuncionario(d.id(), new FuncionarioRequestDTO(
                        cmbCargoDetalhes.getValue().name(),
                        txtNomeDetalhes.getText().trim(),
                        txtNifDetalhes.getText().trim(),
                        txtContactoDetalhes.getText().trim(),
                        null,
                        null
                ));
                carregarFuncionarios();
                nomeOriginal[0] = txtNomeDetalhes.getText().trim();
                cargoOriginal[0] = cmbCargoDetalhes.getValue();
                nifOriginal[0] = txtNifDetalhes.getText().trim();
                contactoOriginal[0] = txtContactoDetalhes.getText().trim();
                setModoVisualizacaoDetalhes(
                        btnGuardar, btnEditar, btnCancelar,
                        txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes
                );
                mostrarSucesso(i18nService.translate("employees.updated"));
            } catch (Exception ex) {
                mostrarErro(ex.getMessage());
            }
        });

        btnEliminar.setOnAction(e -> {
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacao.setTitle(i18nService.translate("common.delete"));
            confirmacao.setHeaderText(i18nService.translate("common.confirmDelete"));
            confirmacao.setContentText("\"" + d.nome() + "\"");
            confirmacao.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

            if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }

            try {
                funcionarioService.apagarFuncionario(d.id());
                carregarFuncionarios();
                navigationService.hideModal();
                mostrarSucesso(i18nService.translate("employees.deleted"));
            } catch (Exception ex) {
                mostrarErro(ex.getMessage());
            }
        });

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private void setCamposDetalhesEditaveis(boolean editavel, TextField txtNomeDetalhes, ComboBox<Cargo> cmbCargoDetalhes,
                                            TextField txtNifDetalhes, TextField txtContactoDetalhes) {
        txtNomeDetalhes.setEditable(editavel);
        txtNifDetalhes.setEditable(editavel);
        txtContactoDetalhes.setEditable(editavel);

        cmbCargoDetalhes.setMouseTransparent(!editavel);
        cmbCargoDetalhes.setFocusTraversable(editavel);
    }

    private void setModoVisualizacaoDetalhes(Button btnGuardar, Button btnEditar, Button btnCancelar,
                                             TextField txtNomeDetalhes, ComboBox<Cargo> cmbCargoDetalhes,
                                             TextField txtNifDetalhes, TextField txtContactoDetalhes) {
        setCamposDetalhesEditaveis(false, txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes);
        btnGuardar.setDisable(true);
        btnEditar.setVisible(true);
        btnEditar.setManaged(true);
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
    }

    private void limparErrosDetalhes(TextField nomeField, Label nomeErro,
                                     ComboBox<Cargo> cargoField, Label cargoErro,
                                     TextField nifField, Label nifErro,
                                     TextField contactoField, Label contactoErro) {
        formValidationService.clearError(nomeField, nomeErro);
        formValidationService.clearError(cargoField, cargoErro);
        formValidationService.clearError(nifField, nifErro);
        formValidationService.clearError(contactoField, contactoErro);
    }

    private String valorOuVazio(String value) {
        return value != null ? value : "";
    }

    private void configurarDrawerAdicionar() {
        drawerRoot = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("employees.newTitle"), navigationService::hideModal);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));
        txtNome = new TextField();
        lblErroNomeAdicionar = formValidationService.createErrorLabel();
        cmbCargo = new ComboBox<>(FXCollections.observableArrayList(Cargo.values()));
        lblErroCargoAdicionar = formValidationService.createErrorLabel();
        cmbCargo.setMaxWidth(Double.MAX_VALUE);
        txtNif = new TextField();
        lblErroNifAdicionar = formValidationService.createErrorLabel();
        txtContacto = new TextField();
        lblErroContactoAdicionar = formValidationService.createErrorLabel();

        formValidationService.attachTextAutoClear(txtNome, lblErroNomeAdicionar);
        formValidationService.attachComboAutoClear(cmbCargo, lblErroCargoAdicionar);
        formValidationService.attachTextAutoClear(txtNif, lblErroNifAdicionar);
        formValidationService.attachTextAutoClear(txtContacto, lblErroContactoAdicionar);

        form.getChildren().addAll(
                UiFactory.formField(i18nService.translate("employees.fullName"), txtNome, lblErroNomeAdicionar),
                UiFactory.formField(i18nService.translate("employees.role"), cmbCargo, lblErroCargoAdicionar),
                UiFactory.formField(i18nService.translate("employees.nif"), txtNif, lblErroNifAdicionar),
                UiFactory.formField(i18nService.translate("employees.phone"), txtContacto, lblErroContactoAdicionar)
        );

        Button btnS = UiFactory.drawerPrimaryAction(i18nService.translate("employees.save"), "mdi2c-content-save-outline");
        btnS.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnS, Priority.ALWAYS);
        btnS.setOnAction(e -> handleAdicionar());
        HBox footer = UiFactory.drawerFooter();
        footer.getChildren().add(btnS);

        drawerRoot.getChildren().addAll(header, UiFactory.transparentScroll(form), footer);
    }

    private void handleAdicionar() {
        if (!validarFormulario(
                txtNome, lblErroNomeAdicionar,
                cmbCargo, lblErroCargoAdicionar,
                txtNif, lblErroNifAdicionar,
                txtContacto, lblErroContactoAdicionar
        )) {
            return;
        }

        try {
            funcionarioService.criarFuncionario(new FuncionarioRequestDTO(
                    cmbCargo.getValue().name(),
                    txtNome.getText().trim(),
                    txtNif.getText().trim(),
                    txtContacto.getText().trim(),
                    null,
                    null
            ));
            pagination.resetPage();
            carregarFuncionarios();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("employees.created"));
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private boolean validarFormulario(TextField nomeField, Label nomeErro,
                                      ComboBox<Cargo> cargoField, Label cargoErro,
                                      TextField nifField, Label nifErro,
                                      TextField contactoField, Label contactoErro) {
        boolean valido = true;

        valido = formValidationService.validateRequiredText(nomeField, nomeErro, i18nService.translate("employees.nameRequired")) && valido;
        valido = formValidationService.validateRequiredCombo(cargoField, cargoErro, i18nService.translate("employees.roleRequired")) && valido;
        valido = formValidationService.validateRequiredText(nifField, nifErro, i18nService.translate("employees.nifRequired")) && valido;
        valido = formValidationService.validateRequiredText(contactoField, contactoErro, i18nService.translate("employees.phoneRequired")) && valido;

        if (!nomeField.getText().trim().isEmpty()) {
            valido = formValidationService.validateRegex(
                    nomeField, nomeErro,
                    "[a-zA-ZÀ-ÿ\\s'\\-]+",
                    i18nService.translate("employees.nameInvalid")
            ) && valido;
        }

        if (!nifField.getText().trim().isEmpty()) {
            valido = formValidationService.validateRegex(
                    nifField, nifErro,
                    "\\d{9}",
                    i18nService.translate("employees.nifInvalid")
            ) && valido;
        }

        if (!contactoField.getText().trim().isEmpty()) {
            valido = formValidationService.validateRegex(
                    contactoField, contactoErro,
                    "9\\d{8}",
                    i18nService.translate("employees.phoneInvalid")
            ) && valido;
        }

        return valido;
    }

    private HBox criarBadgeCargo(Cargo cargo) {
        String color = switch (cargo) {
            case ADMINISTRADOR -> "#eab308";
            case RESPONSAVEL_PRODUCAO -> "#3b82f6";
            case OPERADOR_PRODUCAO -> "#22c55e";
            case RESPONSAVEL_LOGISTICA -> "#f97316";
            case ASSISTENTE_COMERCIAL -> "#0ea5e9";
        };
        String icon = switch (cargo) {
            case ADMINISTRADOR -> "mdi2s-shield-account:16";
            case RESPONSAVEL_LOGISTICA -> "mdi2t-truck:16";
            default -> "mdi2a-account:16";
        };
        return UiFactory.statusBadge(cargo.getDisplayName(), icon, color);
    }

    private void configurarComboBoxes() {
        cmbFiltroCargo.setItems(FXCollections.observableArrayList(Cargo.values()));
    }

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(event -> aplicarFiltrosDinamicos());
        txtFiltroNome.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                searchDebounce.playFromStart();
            }
        });
        cmbFiltroCargo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                aplicarFiltrosDinamicos();
            }
        });
    }

    private void aplicarFiltrosDinamicos() {
        pagination.resetPage();
        carregarFuncionarios();
    }

    private void limparFormulario() {
        txtNome.clear();
        txtNif.clear();
        txtContacto.clear();
        cmbCargo.setValue(null);

        formValidationService.clearError(txtNome, lblErroNomeAdicionar);
        formValidationService.clearError(cmbCargo, lblErroCargoAdicionar);
        formValidationService.clearError(txtNif, lblErroNifAdicionar);
        formValidationService.clearError(txtContacto, lblErroContactoAdicionar);
    }
    private void mostrarSucesso(String m) { toastService.showSuccess(i18nService.translate("common.success"), m); }
    private void mostrarErro(String m) { toastService.showError(i18nService.translate("common.error"), m); }
    @FXML private void handleMostrarTodos() {
        updatingFilters = true;
        searchDebounce.stop();
        txtFiltroNome.clear();
        cmbFiltroCargo.setValue(null);
        updatingFilters = false;
        aplicarFiltrosDinamicos();
    }
}
