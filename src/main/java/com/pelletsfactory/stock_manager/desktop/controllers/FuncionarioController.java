package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.controls.ModalPane;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FuncionarioController {
    private final FuncionarioService funcionarioService;

    @FXML private ComboBox<Cargo> cmbFiltroCargo;
    @FXML private TextField txtFiltroNome;
    @FXML private TextField txtFiltroNif;
    @FXML private ModalPane modalPane;
    @FXML private TableView<Funcionario> tblFuncionarios;
    @FXML private VBox vboxContainer;

    @FXML private TableColumn<Funcionario, String> colNome, colNif, colContacto;
    @FXML private TableColumn<Funcionario, Integer> colNumero;
    @FXML private TableColumn<Funcionario, Cargo> colCargo;
    @FXML private TableColumn<Funcionario, LocalDate> colDataAdmissao;

    private TextField txtNome, txtNif, txtContacto, txtNumeroFuncionario;
    private ComboBox<Cargo> cmbCargo;
    private PasswordField txtPin;
    private VBox drawerRoot;

    // Paginação
    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private ObservableList<Funcionario> funcionarios = FXCollections.observableArrayList();

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarComboBoxes();
        configurarDrawerLateral();
        carregarFuncionarios();
    }

    private void configurarTabela() {
        if (colNome == null || colNumero == null) return;

        // Configurar alinhamento e espaçamento para todas as colunas
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroFuncionario"));
        configurarColunaTexto(colNumero);

        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        configurarColunaTexto(colNome);

        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        configurarColunaTexto(colNif);

        colContacto.setCellValueFactory(new PropertyValueFactory<>("contacto"));
        configurarColunaTexto(colContacto);

        colDataAdmissao.setCellValueFactory(new PropertyValueFactory<>("dataAdmissao"));
        configurarColunaTexto(colDataAdmissao);

        // Coluna Cargo com badge customizado
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colCargo.setCellFactory(column -> new TableCell<Funcionario, Cargo>() {
            @Override
            protected void updateItem(Cargo cargo, boolean empty) {
                super.updateItem(cargo, empty);

                if (empty || cargo == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox badge = criarBadgeCargo(cargo);
                    setGraphic(badge);
                    setText(null);
                }

                // Padding e alinhamento
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        // Ajustar altura das linhas
        tblFuncionarios.setFixedCellSize(60);
        tblFuncionarios.setItems(funcionarios);
    }

    // Método auxiliar para configurar colunas de texto
    private <T> void configurarColunaTexto(TableColumn<Funcionario, T> coluna) {
        coluna.setCellFactory(column -> new TableCell<Funcionario, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }

                // Padding e alinhamento
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    private void configurarPaginacao(VBox container) {
        HBox paginationContainer = new HBox(20);
        paginationContainer.setAlignment(Pos.CENTER);
        paginationContainer.setPadding(new Insets(20, 0, 20, 0));
        paginationContainer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        // Label à esquerda - "Showing 1 to 10 of 112 results"
        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");
        HBox.setHgrow(lblPaginaStatus, Priority.NEVER);

        // Spacer para empurrar tudo
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        // ComboBox "Per page" no CENTRO
        HBox perPageBox = new HBox(10);
        perPageBox.setAlignment(Pos.CENTER);

        Label lblPerPage = new Label("Por página");
        lblPerPage.getStyleClass().add("text-muted");

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setPrefWidth(80);
        cmbItemsPerPage.getStyleClass().add("small");
        cmbItemsPerPage.setOnAction(e -> {
            itemsPerPage = cmbItemsPerPage.getValue();
            paginaAtual = 0;
            carregarFuncionarios();
        });

        perPageBox.getChildren().addAll(lblPerPage, cmbItemsPerPage);
        HBox.setHgrow(perPageBox, Priority.NEVER);

        // Spacer direito
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // Botões de paginação à DIREITA
        paginationButtons = new HBox(5);
        paginationButtons.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(paginationButtons, Priority.NEVER);

        paginationContainer.getChildren().addAll(
                lblPaginaStatus,
                leftSpacer,
                perPageBox,
                rightSpacer,
                paginationButtons
        );

        container.getChildren().add(paginationContainer);
    }

    private void atualizarBotoesPaginacao() {
        paginationButtons.getChildren().clear();

        // Botão "Anterior"
        Button btnPrev = new Button();
        btnPrev.setGraphic(new FontIcon("mdi2c-chevron-left"));
        btnPrev.getStyleClass().addAll("button-icon", "flat");
        btnPrev.setDisable(paginaAtual == 0);
        btnPrev.setOnAction(e -> {
            if (paginaAtual > 0) {
                paginaAtual--;
                carregarFuncionarios();
            }
        });
        paginationButtons.getChildren().add(btnPrev);

        // Botões numéricos
        int maxButtons = 7; // máximo de botões a mostrar
        int startPage = Math.max(0, paginaAtual - 3);
        int endPage = Math.min(totalPaginas - 1, startPage + maxButtons - 1);

        // Ajustar startPage se estivermos perto do fim
        if (endPage - startPage < maxButtons - 1) {
            startPage = Math.max(0, endPage - maxButtons + 1);
        }

        // Sempre mostrar página 1 se não estiver visível
        if (startPage > 0) {
            Button btn1 = criarBotaoPagina(0);
            paginationButtons.getChildren().add(btn1);

            if (startPage > 1) {
                Label dots = new Label("...");
                dots.getStyleClass().add("text-muted");
                dots.setPadding(new Insets(5, 10, 5, 10));
                paginationButtons.getChildren().add(dots);
            }
        }

        // Páginas intermediárias
        for (int i = startPage; i <= endPage; i++) {
            Button btnPage = criarBotaoPagina(i);
            paginationButtons.getChildren().add(btnPage);
        }

        // Sempre mostrar última página se não estiver visível
        if (endPage < totalPaginas - 1) {
            if (endPage < totalPaginas - 2) {
                Label dots = new Label("...");
                dots.getStyleClass().add("text-muted");
                dots.setPadding(new Insets(5, 10, 5, 10));
                paginationButtons.getChildren().add(dots);
            }

            Button btnLast = criarBotaoPagina(totalPaginas - 1);
            paginationButtons.getChildren().add(btnLast);
        }

        // Botão "Próxima"
        Button btnNext = new Button();
        btnNext.setGraphic(new FontIcon("mdi2c-chevron-right"));
        btnNext.getStyleClass().addAll("button-icon", "flat");
        btnNext.setDisable(paginaAtual >= totalPaginas - 1);
        btnNext.setOnAction(e -> {
            if (paginaAtual < totalPaginas - 1) {
                paginaAtual++;
                carregarFuncionarios();
            }
        });
        paginationButtons.getChildren().add(btnNext);
    }

    private Button criarBotaoPagina(int pageIndex) {
        Button btn = new Button(String.valueOf(pageIndex + 1));
        btn.setMinWidth(40);
        btn.setPrefWidth(40);

        if (pageIndex == paginaAtual) {
            btn.getStyleClass().addAll("accent");
        } else {
            btn.getStyleClass().addAll("flat");
        }

        btn.setOnAction(e -> {
            paginaAtual = pageIndex;
            carregarFuncionarios();
        });

        return btn;
    }

    private void atualizarLabelStatus(Page<Funcionario> page) {
        if (lblPaginaStatus != null) {
            int inicio = page.getNumber() * page.getSize() + 1;
            int fim = Math.min((page.getNumber() + 1) * page.getSize(), (int) page.getTotalElements());

            lblPaginaStatus.setText(String.format("Showing %d to %d of %d results",
                    inicio, fim, page.getTotalElements()));
        }
    }

    private void carregarFuncionarios() {
        try {
            String nome = (txtFiltroNome != null && !txtFiltroNome.getText().isEmpty())
                    ? txtFiltroNome.getText() : null;
            String nif = (txtFiltroNif != null && !txtFiltroNif.getText().isEmpty())
                    ? txtFiltroNif.getText() : null;
            Cargo cargo = (cmbFiltroCargo != null) ? cmbFiltroCargo.getValue() : null;

            Page<Funcionario> page = funcionarioService.listarFuncionarios(
                    paginaAtual + 1,
                    itemsPerPage,
                    nome,
                    nif,
                    cargo,
                    null,
                    "dataAdmissao",
                    "DESC"
            );

            funcionarios.setAll(page.getContent());
            totalPaginas = page.getTotalPages();

            if (lblPaginaStatus == null) {
                configurarPaginacao(vboxContainer);
            }

            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();

        } catch (Exception e) {
            System.err.println("Erro ao carregar lista: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarDrawerLateral() {
        drawerRoot = new VBox(0);
        drawerRoot.setPrefWidth(450);
        drawerRoot.setMinWidth(450);
        drawerRoot.setMaxWidth(450);
        drawerRoot.setMaxHeight(Double.MAX_VALUE);
        drawerRoot.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(25));
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Adicionar Funcionário");
        titulo.getStyleClass().add("title-3");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnFechar = new Button();
        btnFechar.getStyleClass().addAll("button-icon", "flat");
        btnFechar.setGraphic(new FontIcon("mdi2c-close:22"));
        btnFechar.setOnAction(e -> modalPane.hide(true));
        header.getChildren().addAll(titulo, spacer, btnFechar);

        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(30));
        txtNome = new TextField();
        cmbCargo = new ComboBox<>(FXCollections.observableArrayList(Cargo.values()));
        cmbCargo.setMaxWidth(Double.MAX_VALUE);
        txtNif = new TextField();
        txtContacto = new TextField();
        txtNumeroFuncionario = new TextField();
        txtPin = new PasswordField();

        formContent.getChildren().addAll(
                new VBox(8, new Label("Nome Completo"), txtNome),
                new VBox(8, new Label("Cargo"), cmbCargo),
                new VBox(8, new Label("NIF"), txtNif),
                new VBox(8, new Label("Telemóvel"), txtContacto),
                new Separator(),
                new VBox(8, new Label("Número Interno"), txtNumeroFuncionario),
                new VBox(8, new Label("PIN de Segurança"), txtPin)
        );

        ScrollPane scroll = new ScrollPane(formContent);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox footer = new HBox(15);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnSalvar = new Button("Guardar Funcionário");
        btnSalvar.getStyleClass().addAll("accent", "success");
        btnSalvar.setPrefHeight(40);
        btnSalvar.setOnAction(e -> handleAdicionar());
        footer.getChildren().add(btnSalvar);

        drawerRoot.getChildren().addAll(header, scroll, footer);
    }

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        modalPane.setAlignment(Pos.CENTER_RIGHT);
        modalPane.usePredefinedTransitionFactories(Side.RIGHT);
        modalPane.show(drawerRoot);
    }

    private HBox criarBadgeCargo(Cargo cargo) {
        HBox badge = new HBox(8);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setMinWidth(150);
        badge.setPrefWidth(150);
        badge.setMaxWidth(200);
        badge.setMinHeight(26);
        badge.setPrefHeight(26);
        badge.setMaxHeight(26);
        badge.setStyle(
                "-fx-background-radius: 6; " +
                        "-fx-border-radius: 6; " +
                        "-fx-border-width: 1.5;"
        );

        FontIcon icon = new FontIcon();
        icon.setIconSize(14);

        Label label = new Label(cargo.getDisplayName());
        label.setStyle("-fx-font-weight: 500; -fx-font-size: 12px;");

        // Cores e ícones diferentes por cargo
        switch (cargo) {
            case ADMINISTRADOR:
                // Cor AMARELA/DOURADA (como "Contractor")
                badge.setStyle(badge.getStyle() +
                        "-fx-background-color: rgba(234, 179, 8, 0.12); " +
                        "-fx-border-color: #eab308;");
                icon.setIconLiteral("mdi2s-shield-account");
                icon.setIconColor(javafx.scene.paint.Color.web("#eab308"));
                label.setStyle(label.getStyle() + "-fx-text-fill: #eab308;");
                break;

            case RESPONSAVEL_PRODUCAO:
                badge.setStyle(badge.getStyle() +
                        "-fx-background-color: rgba(59, 130, 246, 0.12); " +
                        "-fx-border-color: #3b82f6;");
                icon.setIconLiteral("mdi2a-account-star");
                icon.setIconColor(javafx.scene.paint.Color.web("#3b82f6"));
                label.setStyle(label.getStyle() + "-fx-text-fill: #3b82f6;");
                break;

            case OPERADOR_PRODUCAO:
                badge.setStyle(badge.getStyle() +
                        "-fx-background-color: rgba(34, 197, 94, 0.12); " +
                        "-fx-border-color: #22c55e;");
                icon.setIconLiteral("mdi2h-hammer-wrench");
                icon.setIconColor(javafx.scene.paint.Color.web("#22c55e"));
                label.setStyle(label.getStyle() + "-fx-text-fill: #22c55e;");
                break;

            case RESPONSAVEL_LOGISTICA:
                badge.setStyle(badge.getStyle() +
                        "-fx-background-color: rgba(249, 115, 22, 0.12); " +
                        "-fx-border-color: #f97316;");
                icon.setIconLiteral("mdi2t-truck");
                icon.setIconColor(javafx.scene.paint.Color.web("#f97316"));
                label.setStyle(label.getStyle() + "-fx-text-fill: #f97316;");
                break;

            case ASSISTENTE_COMERCIAL:
                badge.setStyle(badge.getStyle() +
                        "-fx-background-color: rgba(14, 165, 233, 0.12); " +
                        "-fx-border-color: #0ea5e9;");
                icon.setIconLiteral("mdi2c-cash-multiple");
                icon.setIconColor(javafx.scene.paint.Color.web("#0ea5e9"));
                label.setStyle(label.getStyle() + "-fx-text-fill: #0ea5e9;");
                break;

            default:
                badge.setStyle(badge.getStyle() +
                        "-fx-background-color: rgba(107, 114, 128, 0.12); " +
                        "-fx-border-color: #6b7280;");
                icon.setIconLiteral("mdi2a-account");
                icon.setIconColor(javafx.scene.paint.Color.web("#6b7280"));
                label.setStyle(label.getStyle() + "-fx-text-fill: #6b7280;");
                break;
        }

        badge.getChildren().addAll(icon, label);
        return badge;
    }

    private void handleAdicionar() {
        if (txtNome.getText().isEmpty() || cmbCargo.getValue() == null) {
            mostrarErro("Preencha os campos obrigatórios!");
            return;
        }
        try {
            Funcionario f = new Funcionario();
            f.setNome(txtNome.getText());
            f.setCargo(cmbCargo.getValue());
            f.setNif(txtNif.getText());
            f.setContacto(txtContacto.getText());
            f.setNumeroFuncionario(Integer.parseInt(txtNumeroFuncionario.getText()));
            f.setDataAdmissao(LocalDate.now());

            funcionarioService.adicionarFuncionario(f);
            paginaAtual = 0;
            carregarFuncionarios();
            modalPane.hide(true);
            mostrarSucesso("Funcionário salvo!");
        } catch (Exception e) {
            mostrarErro("Erro: " + e.getMessage());
        }
    }

    private void configurarComboBoxes() {
        if (cmbFiltroCargo != null) {
            cmbFiltroCargo.setItems(FXCollections.observableArrayList(Cargo.values()));
        }
    }

    private void limparFormulario() {
        txtNome.clear();
        txtNif.clear();
        txtContacto.clear();
        txtNumeroFuncionario.clear();
        txtPin.clear();
        cmbCargo.setValue(null);
    }

    private void mostrarSucesso(String m) {
        // Implementar toast ou notificação
        System.out.println("SUCESSO: " + m);
    }

    private void mostrarErro(String m) {
        // Implementar toast ou notificação
        System.err.println("ERRO: " + m);
    }

    @FXML
    private void handleFiltrar() {
        paginaAtual = 0;
        carregarFuncionarios();
    }

    @FXML
    private void handleMostrarTodos() {
        if (txtFiltroNome != null) txtFiltroNome.clear();
        if (txtFiltroNif != null) txtFiltroNif.clear();
        if (cmbFiltroCargo != null) cmbFiltroCargo.setValue(null);

        paginaAtual = 0;
        carregarFuncionarios();
    }
}