package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.FuncionarioRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

@Component
public class FuncionarioController {
    private final FuncionarioService funcionarioService;
    private final NavigationService navigationService;

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

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<FuncionarioSimpleDTO> funcionarios = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public FuncionarioController(FuncionarioService funcionarioService, NavigationService navigationService) {
        this.funcionarioService = funcionarioService;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarComboBoxes();
        configurarDrawerAdicionar(); // Configura o drawer de criação com 550px
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
            String nome = (txtFiltroNome != null && !txtFiltroNome.getText().isEmpty()) ? txtFiltroNome.getText() : null;
            Cargo cargo = (cmbFiltroCargo != null) ? cmbFiltroCargo.getValue() : null;

            Page<FuncionarioSimpleDTO> page = funcionarioService.listarFuncionarios(
                    paginaAtual + 1, itemsPerPage, nome, null, cargo, null, "dataAdmissao", "DESC"
            );

            funcionarios.setAll(page.getContent());
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

    private void handleAbrirDetalhes(FuncionarioSimpleDTO func) {
        try {
            FuncionarioDetailsDTO d = funcionarioService.obterDetalhes(func.id());
            VBox detalhesDrawer = criarDrawerVisualizacao(d); // Este método agora usa 550px
            navigationService.showModal(detalhesDrawer);
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerVisualizacao(FuncionarioDetailsDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Detalhes do Funcionário");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button(); btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        TextField txtNomeDetalhes = new TextField(valorOuVazio(d.nome()));
        ComboBox<Cargo> cmbCargoDetalhes = new ComboBox<>(FXCollections.observableArrayList(Cargo.values()));
        cmbCargoDetalhes.setValue(d.cargo());
        cmbCargoDetalhes.setMaxWidth(Double.MAX_VALUE);
        TextField txtNifDetalhes = new TextField(valorOuVazio(d.nif()));
        TextField txtContactoDetalhes = new TextField(valorOuVazio(d.contacto()));
        TextField txtNumeroDetalhes = new TextField(d.numeroFuncionario() != null ? String.valueOf(d.numeroFuncionario()) : "");
        TextField txtDataAdmissaoDetalhes = new TextField(d.dataAdmissao() != null ? d.dataAdmissao().format(DATE_FORMATTER) : "");

        txtNumeroDetalhes.setEditable(false);
        txtDataAdmissaoDetalhes.setEditable(false);
        txtNumeroDetalhes.setFocusTraversable(false);
        txtDataAdmissaoDetalhes.setFocusTraversable(false);

        setCamposDetalhesEditaveis(false, txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes);

        VBox form = new VBox(20,
                criarCampoFormulario("Nome Completo", txtNomeDetalhes),
                criarCampoFormulario("Cargo", cmbCargoDetalhes),
                criarCampoFormulario("NIF", txtNifDetalhes),
                criarCampoFormulario("Telemóvel", txtContactoDetalhes),
                criarCampoFormulario("Número Interno", txtNumeroDetalhes),
                criarCampoFormulario("Data de Entrada", txtDataAdmissaoDetalhes)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        Button btnGuardar = new Button("Guardar Funcionário");
        btnGuardar.getStyleClass().add("accent");
        btnGuardar.setPrefHeight(44);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        btnGuardar.setDisable(true);

        Button btnEditar = new Button("Editar");
        btnEditar.getStyleClass().add("button-outlined");
        btnEditar.setPrefHeight(44);
        btnEditar.setMaxWidth(Double.MAX_VALUE);

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.setPrefHeight(44);
        btnEliminar.setMaxWidth(Double.MAX_VALUE);
        btnEliminar.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;");

        HBox.setHgrow(btnGuardar, Priority.ALWAYS);
        HBox.setHgrow(btnEditar, Priority.ALWAYS);
        HBox.setHgrow(btnEliminar, Priority.ALWAYS);
        footer.getChildren().addAll(btnGuardar, btnEditar, btnEliminar);

        btnEditar.setOnAction(e -> {
            setCamposDetalhesEditaveis(true, txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes);
            btnGuardar.setDisable(false);
            btnEditar.setDisable(true);
            txtNomeDetalhes.requestFocus();
        });

        btnGuardar.setOnAction(e -> {
            String nome = txtNomeDetalhes.getText().trim();
            Cargo cargo = cmbCargoDetalhes.getValue();
            String nif = txtNifDetalhes.getText().trim();
            String contacto = txtContactoDetalhes.getText().trim();

            if (!validarFormulario(nome, cargo, nif, contacto)) {
                return;
            }

            try {
                funcionarioService.atualizarFuncionario(d.id(), new FuncionarioRequestDTO(
                        cargo.name(), nome, nif, contacto
                ));
                carregarFuncionarios();
                setCamposDetalhesEditaveis(false, txtNomeDetalhes, cmbCargoDetalhes, txtNifDetalhes, txtContactoDetalhes);
                btnGuardar.setDisable(true);
                btnEditar.setDisable(false);
                mostrarSucesso("Funcionário atualizado!");
            } catch (Exception ex) {
                mostrarErro("Erro ao atualizar: " + ex.getMessage());
            }
        });

        btnEliminar.setOnAction(e -> {
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacao.setTitle("Eliminar Funcionário");
            confirmacao.setHeaderText("Tem a certeza?");
            confirmacao.setContentText("Esta ação é irreversível e vai eliminar o funcionário selecionado.");

            if (confirmacao.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            try {
                funcionarioService.apagarFuncionario(d.id());
                carregarFuncionarios();
                navigationService.hideModal();
                mostrarSucesso("Funcionário eliminado!");
            } catch (Exception ex) {
                mostrarErro("Erro ao eliminar: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private VBox criarCampoFormulario(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private void setCamposDetalhesEditaveis(boolean editavel, TextField txtNomeDetalhes, ComboBox<Cargo> cmbCargoDetalhes,
                                            TextField txtNifDetalhes, TextField txtContactoDetalhes) {
        txtNomeDetalhes.setEditable(editavel);
        txtNifDetalhes.setEditable(editavel);
        txtContactoDetalhes.setEditable(editavel);

        cmbCargoDetalhes.setMouseTransparent(!editavel);
        cmbCargoDetalhes.setFocusTraversable(editavel);
    }

    private String valorOuVazio(String value) {
        return value != null ? value : "";
    }

    private void configurarDrawerAdicionar() {
        drawerRoot = new VBox(0);

        // CORREÇÃO DE LARGURA
        drawerRoot.setMinWidth(550);
        drawerRoot.setPrefWidth(550);
        drawerRoot.setMaxWidth(550);

        drawerRoot.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Novo Funcionário");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnF = new Button(); btnF.setGraphic(new FontIcon("mdi2c-close:22"));
        btnF.getStyleClass().addAll("button-icon", "flat");
        btnF.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnF);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));
        txtNome = new TextField();
        cmbCargo = new ComboBox<>(FXCollections.observableArrayList(Cargo.values()));
        cmbCargo.setMaxWidth(Double.MAX_VALUE);
        txtNif = new TextField();
        txtContacto = new TextField();

        form.getChildren().addAll(
                new VBox(8, new Label("Nome Completo"), txtNome),
                new VBox(8, new Label("Cargo"), cmbCargo),
                new VBox(8, new Label("NIF"), txtNif),
                new VBox(8, new Label("Telemóvel"), txtContacto)
        );

        HBox footer = new HBox();
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnS = new Button("Guardar Funcionário");
        btnS.getStyleClass().add("accent");
        btnS.setPrefHeight(44);
        btnS.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnS, Priority.ALWAYS);
        btnS.setOnAction(e -> handleAdicionar());
        footer.getChildren().add(btnS);

        drawerRoot.getChildren().addAll(header, new ScrollPane(form){{setFitToWidth(true); setStyle("-fx-background: transparent;");}}, footer);
    }

    private void handleAdicionar() {
        String nome = txtNome.getText().trim();
        Cargo cargo = cmbCargo.getValue();
        String nif = txtNif.getText().trim();
        String contacto = txtContacto.getText().trim();

        if (!validarFormulario(nome, cargo, nif, contacto)) {
            return;
        }

        try {
            funcionarioService.criarFuncionario(new FuncionarioRequestDTO(
                    cargo.name(), nome, nif, contacto
            ));
            paginaAtual = 0;
            carregarFuncionarios();
            navigationService.hideModal();
            mostrarSucesso("Funcionário criado!");
        } catch (Exception e) { mostrarErro("Erro: " + e.getMessage()); }
    }

    private boolean validarFormulario(String nome, Cargo cargo, String nif, String contacto) {
        if (nome.isEmpty() || cargo == null || nif.isEmpty() || contacto.isEmpty()) {
            mostrarErro("Preencha os campos obrigatórios!");
            return false;
        }

        if (!nif.matches("\\d{9}")) {
            mostrarErro("NIF deve ter exatamente 9 dígitos.");
            return false;
        }

        return true;
    }

    private void configurarPaginacao(VBox container) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(20, 0, 20, 0));
        nav.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");
        HBox left = new HBox(lblPaginaStatus); left.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(left, Priority.ALWAYS);

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarFuncionarios(); });
        HBox center = new HBox(10, new Label("Por página"), cmbItemsPerPage); center.setAlignment(Pos.CENTER); HBox.setHgrow(center, Priority.ALWAYS);

        paginationButtons = new HBox(5);
        HBox right = new HBox(paginationButtons); right.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(right, Priority.ALWAYS);

        nav.getChildren().addAll(left, center, right);
        container.getChildren().add(nav);
    }

    private void atualizarBotoesPaginacao() {
        paginationButtons.getChildren().clear();
        Button prev = new Button(); prev.setGraphic(new FontIcon("mdi2c-chevron-left"));
        prev.setDisable(paginaAtual == 0);
        prev.setOnAction(e -> { paginaAtual--; carregarFuncionarios(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int finalI = i; p.setOnAction(e -> { paginaAtual = finalI; carregarFuncionarios(); });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button(); next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarFuncionarios(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<FuncionarioSimpleDTO> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
    }

    private HBox criarBadgeCargo(Cargo cargo) {
        HBox b = new HBox(8); b.setAlignment(Pos.CENTER_LEFT); b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");
        String color = switch (cargo) {
            case ADMINISTRADOR -> "#eab308";
            case RESPONSAVEL_PRODUCAO -> "#3b82f6";
            case OPERADOR_PRODUCAO -> "#22c55e";
            case RESPONSAVEL_LOGISTICA -> "#f97316";
            case ASSISTENTE_COMERCIAL -> "#0ea5e9";
        };
        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s20; -fx-border-color: %s;", color.replace("#", ""), color));
        Label l = new Label(cargo.getDisplayName()); l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
        FontIcon ic = new FontIcon(switch(cargo){case ADMINISTRADOR->"mdi2s-shield-account"; case RESPONSAVEL_LOGISTICA->"mdi2t-truck"; default->"mdi2a-account";});
        ic.setIconColor(javafx.scene.paint.Color.web(color));
        b.getChildren().addAll(ic, l);
        return b;
    }

    private void configurarComboBoxes() { cmbFiltroCargo.setItems(FXCollections.observableArrayList(Cargo.values())); }
    private void limparFormulario() { txtNome.clear(); txtNif.clear(); txtContacto.clear(); cmbCargo.setValue(null); }
    private void mostrarSucesso(String m) { System.out.println("SUCESSO: " + m); }
    private void mostrarErro(String m) { System.err.println("ERRO: " + m); }
    @FXML private void handleFiltrar() { paginaAtual = 0; carregarFuncionarios(); }
    @FXML private void handleMostrarTodos() { txtFiltroNome.clear(); cmbFiltroCargo.setValue(null); handleFiltrar(); }
}